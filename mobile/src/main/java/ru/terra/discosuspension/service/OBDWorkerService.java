package ru.terra.discosuspension.service;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;

import java.util.HashMap;
import java.util.Map;

import pt.lighthouselabs.obd.commands.ObdCommand;
import ru.terra.discosuspension.Logger;
import ru.terra.discosuspension.R;
import ru.terra.discosuspension.activity.components.ObdResult;
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

import static ru.terra.discosuspension.Constants.UPDATE_INTENT_ACTION;
import static ru.terra.discosuspension.Constants.UPDATE_INTENT_RESULT;
import static ru.terra.discosuspension.obd.constants.ControlModuleID.GEARBOX_CONTROL_MODULE;
import static ru.terra.discosuspension.obd.constants.ControlModuleID.REAR_DIFF_CONTROL_MODULE;
import static ru.terra.discosuspension.obd.constants.ControlModuleID.STEERING_WHEEL_CONTROL_MODULE;
import static ru.terra.discosuspension.obd.constants.ControlModuleID.SUSPENSION_CONTROL_MODULE;
import static ru.terra.discosuspension.obd.constants.ControlModuleID.TRANSFER_CASE_CONTROL_MODULE;

public class OBDWorkerService extends IntentService implements StateUpdater {
    private static final String TAG = OBDWorkerService.class.getName();
    private static final Map<Class, CommandHandler> dispatch = new HashMap<>();
    private final static long SUSP_REQ_DIFF = 1000;
    private final static long GB_REQ_DIFF = 1000;
    private static final int DEF_VALUE_TIMING = 20;
    private static final int DEF_VALUE_ENG_ROT = 180;
    private static final int SUSP_SHIFT = 10;

    private final ObdResult obdResult = new ObdResult();
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
    private SharedPreferences sp;

    @Override
    public void stateUpdate(final ObdCommand cmd) {
        final CommandHandler h = dispatch.get(cmd.getClass());
        if (h != null) {
            h.handle(cmd);
        }

        final Intent resultIntent = new Intent();
        resultIntent.setAction(UPDATE_INTENT_ACTION);
        resultIntent.putExtra(UPDATE_INTENT_RESULT, obdResult);
        LocalBroadcastManager.getInstance(this).sendBroadcast(resultIntent);
    }

    private interface CommandHandler {
        void handle(ObdCommand cmd);
    }

    public OBDWorkerService() {
        super("Obd Worker Service");
    }

    private ServiceConnection serviceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            service = ((ObdGatewayService.ObdGatewayServiceServiceBinder) binder).getService();
            service.setStateUpdater(OBDWorkerService.this);
            Logger.i(TAG, getString(R.string.msg_service_connected));

            if (!service.isRunning()) {
                if (!service.initObdBackend()) {
//                        Toast.makeText(getApplicationContext(), R.string.msg_err_service_not_started, Toast.LENGTH_LONG).show();
                    stop = true;
                    stopSelf();
                } else {
                    new Handler(new HandlerThread("ObdThread").getLooper()).post(mQueueCommands);
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
        }
    };

    @Override
    protected void onHandleIntent(Intent intent) {
        sp = PreferenceManager.getDefaultSharedPreferences(this);
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
        dispatch.put(RearDiffTempCommand.class, cmd -> obdResult.rdTemp = cmd.getFormattedResult());

        dispatch.put(RearDiffBlockCommand.class, cmd -> {
            obdResult.rdBlock = cmd.getFormattedResult().equalsIgnoreCase("on");
        });

        dispatch.put(TransferCaseRotEngCommand.class, cmd -> {
            obdResult.tcRotation = cmd.getFormattedResult();

            final TransferCaseRotEngCommand tcrec = (TransferCaseRotEngCommand) cmd;
            obdResult.tcBlock = tcrec.getRes() > TCCM_ENG_ROT_BLOCK;
        });

        dispatch.put(TransferCaseSolenoidPositionCommand.class, cmd -> {
            obdResult.tcSolLen = cmd.getFormattedResult();
            obdResult.tcSolPos = ((TransferCaseSolenoidPositionCommand) cmd).isHi() ? getString(R.string.ui_tc_sol_pos_hi) : getString(R.string.ui_tc_sol_pos_lo);
        });

        dispatch.put(TransferCaseTempCommand.class, cmd ->
                obdResult.tcTemp = cmd.getFormattedResult()
        );

        dispatch.put(CurrentGearCommand.class, cmd ->
                obdResult.currentGear = cmd.getFormattedResult()
        );

        dispatch.put(GearBoxTempCommand.class, cmd ->
                obdResult.gbTemp = cmd.getFormattedResult()
        );

        dispatch.put(SuspensionHeightCommand.class, cmd -> {
            final SuspensionHeightCommand shc = (SuspensionHeightCommand) cmd;
            switch (shc.getWheel()) {
                case SuspensionHeightCommand.FRONT_LEFT: {
                    obdResult.suspFLVal = (int) (shc.calc() + SUSP_SHIFT);
                    obdResult.suspFLText = cmd.getFormattedResult();
                }
                break;
                case SuspensionHeightCommand.FRONT_RIGHT: {
                    obdResult.suspFRVal = (int) (shc.calc() + SUSP_SHIFT);
                    obdResult.suspFRText = cmd.getFormattedResult();
                }
                break;
                case SuspensionHeightCommand.REAR_LEFT: {
                    obdResult.suspRLVal = (int) (shc.calc() + SUSP_SHIFT);
                    obdResult.suspRLText = cmd.getFormattedResult();
                }
                break;
                case SuspensionHeightCommand.REAR_RIGHT: {
                    obdResult.suspRRVal = (int) (shc.calc() + SUSP_SHIFT);
                    obdResult.suspRRText = cmd.getFormattedResult();
                }
                break;

            }
        });

        dispatch.put(DriveShiftPositionCommand.class, cmd -> obdResult.driveShiftPos = cmd.getFormattedResult());

        dispatch.put(SteeringWheelPositionCommand.class, cmd -> obdResult.wheelPos = ((SteeringWheelPositionCommand) cmd).calc());
    }
}
