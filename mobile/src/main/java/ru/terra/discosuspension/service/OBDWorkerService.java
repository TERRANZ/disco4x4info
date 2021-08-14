package ru.terra.discosuspension.service;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import pt.lighthouselabs.obd.commands.ObdCommand;
import pt.lighthouselabs.obd.commands.protocol.ObdProtocolCommand;
import ru.terra.discosuspension.Logger;
import ru.terra.discosuspension.R;
import ru.terra.discosuspension.activity.components.ObdState;
import ru.terra.discosuspension.obd.commands.disco3.CurrentGearCommand;
import ru.terra.discosuspension.obd.commands.disco3.DriveShiftPositionCommand;
import ru.terra.discosuspension.obd.commands.disco3.GearBoxTempCommand;
import ru.terra.discosuspension.obd.commands.disco3.RearDiffBlockCommand;
import ru.terra.discosuspension.obd.commands.disco3.RearDiffTempCommand;
import ru.terra.discosuspension.obd.commands.disco3.SelectControlModuleCommand;
import ru.terra.discosuspension.obd.commands.disco3.SteeringWheelPositionCommand;
import ru.terra.discosuspension.obd.commands.disco3.SuspensionHeightCommand;
import ru.terra.discosuspension.obd.commands.disco3.TransferCaseRotEngCommand;
import ru.terra.discosuspension.obd.commands.disco3.TransferCaseSolenoidPositionCommand;
import ru.terra.discosuspension.obd.commands.disco3.TransferCaseTempCommand;
import ru.terra.discosuspension.obd.io.ObdGatewayService;
import ru.terra.discosuspension.obd.io.StateUpdater;
import ru.terra.discosuspension.telemetry.TelemetryManager;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;
import static ru.terra.discosuspension.Constants.UPDATE_INTENT_ACTION;
import static ru.terra.discosuspension.Constants.UPDATE_INTENT_RESULT;
import static ru.terra.discosuspension.obd.constants.ControlModuleID.GEARBOX_CONTROL_MODULE;
import static ru.terra.discosuspension.obd.constants.ControlModuleID.REAR_DIFF_CONTROL_MODULE;
import static ru.terra.discosuspension.obd.constants.ControlModuleID.STEERING_WHEEL_CONTROL_MODULE;
import static ru.terra.discosuspension.obd.constants.ControlModuleID.SUSPENSION_CONTROL_MODULE;
import static ru.terra.discosuspension.obd.constants.ControlModuleID.TRANSFER_CASE_CONTROL_MODULE;

public class OBDWorkerService extends IntentService implements StateUpdater {
    private static final String TAG = OBDWorkerService.class.getName();
    private static final Map<Class<? extends ObdProtocolCommand>, CommandHandler> dispatch = new HashMap<>();
    private final static long SUSP_REQ_DIFF = 1000;
    private final static long GB_REQ_DIFF = 1000;
    private static final int DEF_VALUE_TIMING = 20;
    private static final int DEF_VALUE_ENG_ROT = 180;
    private static final int SUSP_SHIFT = 10;

    private final ObdState obdState = new ObdState();
    private final SelectControlModuleCommand scmcRearDiff = new SelectControlModuleCommand(REAR_DIFF_CONTROL_MODULE.getCmId());
    private final SelectControlModuleCommand scmcSuspension = new SelectControlModuleCommand(SUSPENSION_CONTROL_MODULE.getCmId());
    private final SelectControlModuleCommand scmcTC = new SelectControlModuleCommand(TRANSFER_CASE_CONTROL_MODULE.getCmId());
    private final SelectControlModuleCommand scmcGearBox = new SelectControlModuleCommand(GEARBOX_CONTROL_MODULE.getCmId());
    private final SelectControlModuleCommand scmcSteeringWheel = new SelectControlModuleCommand(STEERING_WHEEL_CONTROL_MODULE.getCmId());
    private ObdGatewayService service;
    private int OBD_SLEEP_UPDATE = 0;
    private int OBD_SLEEP_SELECT_CM = 0;
    private long lastSuspensionRequest, lastGBRequest = System.currentTimeMillis();
    private int TCCM_ENG_ROT_BLOCK = 0;
    private boolean stop = false;
    private final TelemetryManager telemetryManager;
    private final Date startTime;

    @Override
    public void stateUpdate(final ObdCommand cmd) {
        final CommandHandler h = dispatch.get(cmd.getClass());
        if (h != null) {
            h.handle(cmd);
        }

        final Intent resultIntent = new Intent();
        resultIntent.setAction(UPDATE_INTENT_ACTION);
        try {
            final ObdState result = obdState.clone();
            resultIntent.putExtra(UPDATE_INTENT_RESULT, result);
            telemetryManager.appendTelemetry(result, startTime);
            Log.d(TAG, "Updating state with " + result.toString());
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }

        sendBroadcast(resultIntent);
    }

    private interface CommandHandler {
        void handle(ObdCommand cmd);
    }

    public OBDWorkerService() {
        super("Obd Worker Service");
        telemetryManager = new TelemetryManager(this);
        startTime = new Date();
    }

    private final ServiceConnection serviceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            service = ((ObdGatewayService.ObdGatewayServiceServiceBinder) binder).getService();
            service.setStateUpdater(OBDWorkerService.this);
            Logger.i(TAG, getString(R.string.msg_service_connected));

            if (!service.isRunning()) {
                if (!service.initObdBackend()) {
                    stop = true;
                    stopSelf();
                } else {
                    new Handler().post(mQueueCommands);
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
        }
    };

    @Override
    protected void onHandleIntent(Intent intent) {
        final SharedPreferences sp = getDefaultSharedPreferences(this);
        OBD_SLEEP_SELECT_CM = sp.getInt(getString(R.string.obd_sleep_select_cm), DEF_VALUE_TIMING);
        OBD_SLEEP_UPDATE = sp.getInt(getString(R.string.obd_sleep_update), DEF_VALUE_TIMING);
        TCCM_ENG_ROT_BLOCK = sp.getInt(getString(R.string.tccm_eng_rot_block), DEF_VALUE_ENG_ROT);

        fillDispatcher();

        final Intent serviceIntent = new Intent(this, ObdGatewayService.class);
        startService(serviceIntent);
        bindService(serviceIntent, serviceConn, Context.BIND_AUTO_CREATE);
        while (!stop) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(serviceConn);
        service = null;
        stopService(new Intent(this, ObdGatewayService.class));
        Logger.i(TAG, getString(R.string.msg_stopping_service));
    }

    private final Runnable mQueueCommands = new Runnable() {
        public void run() {
            if (service != null && service.getCurrentQueueSize() == 0) {
                final SharedPreferences sp = getDefaultSharedPreferences(OBDWorkerService.this);
                //Rear diff
                addRDCMCommands();
                //suspension
                if (sp.getBoolean(getString(R.string.pref_key_susp), true)) {
                    addSuspensionCommands();
                }
                //transfer case
                addTCComamnds();
                //gearbox
                addGearBoxCommands();
                //steering wheel
                if (sp.getBoolean(getString(R.string.pref_key_wheel), true)) {
                    addSteeringWheelCommands();
                }
            }
            new Handler().postDelayed(mQueueCommands, OBD_SLEEP_UPDATE);
        }
    };

    private void addRDCMCommands() {
        //RDCM
        service.queueCmd(scmcRearDiff);
        doSleep();
        service.queueCmd(new RearDiffTempCommand());
        service.queueCmd(new RearDiffBlockCommand());
        doSleep();
    }

    private void addSuspensionCommands() {
        if (System.currentTimeMillis() - lastSuspensionRequest > SUSP_REQ_DIFF) {
            service.queueCmd(scmcSuspension);
            doSleep();
            service.queueCmd(new SuspensionHeightCommand(SuspensionHeightCommand.FRONT_LEFT));
            service.queueCmd(new SuspensionHeightCommand(SuspensionHeightCommand.FRONT_RIGHT));
            service.queueCmd(new SuspensionHeightCommand(SuspensionHeightCommand.REAR_LEFT));
            service.queueCmd(new SuspensionHeightCommand(SuspensionHeightCommand.REAR_RIGHT));
            doSleep();
            lastSuspensionRequest = System.currentTimeMillis();
        }
    }

    private void addTCComamnds() {
        service.queueCmd(scmcTC);
        doSleep();
        service.queueCmd(new TransferCaseTempCommand());
        service.queueCmd(new TransferCaseRotEngCommand());
        service.queueCmd(new TransferCaseSolenoidPositionCommand());
        doSleep();
    }

    private void addGearBoxCommands() {
        if (System.currentTimeMillis() - lastGBRequest > GB_REQ_DIFF) {
            service.queueCmd(scmcGearBox);
            doSleep();
            service.queueCmd(new CurrentGearCommand());
            service.queueCmd(new GearBoxTempCommand());
            service.queueCmd(new DriveShiftPositionCommand());
            doSleep();
            lastGBRequest = System.currentTimeMillis();
        }
    }

    private void addSteeringWheelCommands() {
        service.queueCmd(scmcSteeringWheel);
        doSleep();
        service.queueCmd(new SteeringWheelPositionCommand());
    }

    private void doSleep() {
        try {
            Thread.sleep(OBD_SLEEP_SELECT_CM);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void fillDispatcher() {
        dispatch.put(RearDiffTempCommand.class, cmd -> obdState.rdTemp = cmd.getFormattedResult());

        dispatch.put(RearDiffBlockCommand.class, cmd -> {
            obdState.rdBlock = cmd.getFormattedResult().equalsIgnoreCase("on");
        });

        dispatch.put(TransferCaseRotEngCommand.class, cmd -> {
            obdState.tcRotation = cmd.getFormattedResult();

            final TransferCaseRotEngCommand tcrec = (TransferCaseRotEngCommand) cmd;
            obdState.tcBlock = tcrec.getRes() > TCCM_ENG_ROT_BLOCK;
        });

        dispatch.put(TransferCaseSolenoidPositionCommand.class, cmd -> {
            obdState.tcSolLen = cmd.getFormattedResult();
            obdState.tcSolPos = ((TransferCaseSolenoidPositionCommand) cmd).isHi() ? getString(R.string.ui_tc_sol_pos_hi) : getString(R.string.ui_tc_sol_pos_lo);
        });

        dispatch.put(TransferCaseTempCommand.class, cmd ->
                obdState.tcTemp = cmd.getFormattedResult()
        );

        dispatch.put(CurrentGearCommand.class, cmd ->
                obdState.currentGear = cmd.getFormattedResult()
        );

        dispatch.put(GearBoxTempCommand.class, cmd ->
                obdState.gbTemp = cmd.getFormattedResult()
        );

        dispatch.put(SuspensionHeightCommand.class, cmd -> {
            final SuspensionHeightCommand shc = (SuspensionHeightCommand) cmd;
            switch (shc.getWheel()) {
                case SuspensionHeightCommand.FRONT_LEFT: {
                    obdState.suspFLVal = (int) (shc.calc() + SUSP_SHIFT);
                    obdState.suspFLText = cmd.getFormattedResult();
                }
                break;
                case SuspensionHeightCommand.FRONT_RIGHT: {
                    obdState.suspFRVal = (int) (shc.calc() + SUSP_SHIFT);
                    obdState.suspFRText = cmd.getFormattedResult();
                }
                break;
                case SuspensionHeightCommand.REAR_LEFT: {
                    obdState.suspRLVal = (int) (shc.calc() + SUSP_SHIFT);
                    obdState.suspRLText = cmd.getFormattedResult();
                }
                break;
                case SuspensionHeightCommand.REAR_RIGHT: {
                    obdState.suspRRVal = (int) (shc.calc() + SUSP_SHIFT);
                    obdState.suspRRText = cmd.getFormattedResult();
                }
                break;

            }
        });

        dispatch.put(DriveShiftPositionCommand.class, cmd -> obdState.driveShiftPos = cmd.getFormattedResult());

        dispatch.put(SteeringWheelPositionCommand.class, cmd -> obdState.wheelPos = ((SteeringWheelPositionCommand) cmd).calc());
    }
}
