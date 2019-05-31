package ru.terra.discosuspension.service;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

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
import ru.terra.discosuspension.obd.io.AbstractGatewayService;
import ru.terra.discosuspension.obd.io.ObdGatewayService;

import static ru.terra.discosuspension.obd.constants.ControlModuleIDs.GEARBOX_CONTROL_MODULE;
import static ru.terra.discosuspension.obd.constants.ControlModuleIDs.REAR_DIFF_CONTROL_MODULE;
import static ru.terra.discosuspension.obd.constants.ControlModuleIDs.STEERING_WHEEL_CONTROL_MODULE;
import static ru.terra.discosuspension.obd.constants.ControlModuleIDs.SUSPENSION_CONTROL_MODULE;
import static ru.terra.discosuspension.obd.constants.ControlModuleIDs.TRANSFER_CASE_CONTROL_MODULE;

public class OBDWorkerService extends IntentService {
    private static final String TAG = OBDWorkerService.class.getName();

    private static final Map<Class, CommandHandler> dispatch = new HashMap<>();
    private AbstractGatewayService service;
    private final SelectControlModuleCommand scmcRearDiff = new SelectControlModuleCommand(REAR_DIFF_CONTROL_MODULE);
    private final SelectControlModuleCommand scmcSuspension = new SelectControlModuleCommand(SUSPENSION_CONTROL_MODULE);
    private final SelectControlModuleCommand scmcTC = new SelectControlModuleCommand(TRANSFER_CASE_CONTROL_MODULE);
    private final SelectControlModuleCommand scmcGearBox = new SelectControlModuleCommand(GEARBOX_CONTROL_MODULE);
    private final SelectControlModuleCommand scmcSteeringWheel = new SelectControlModuleCommand(STEERING_WHEEL_CONTROL_MODULE);
    private int OBD_SLEEP_UPDATE = 0;
    private int OBD_SLEEP_SELECT_CM = 0;
    private long lastSuspensionRequest, lastGBRequest = System.currentTimeMillis();
    private final static long SUSP_REQ_DIFF = 1000;
    private final static long GB_REQ_DIFF = 1000;
    private int TCCM_ENG_ROT_BLOCK = 0;
    private boolean stop = false;
    private final ObdResult obdResult = new ObdResult();

    public void stateUpdate(ObdCommand cmd) {
        CommandHandler h = dispatch.get(cmd.getClass());
        Logger.i(TAG, "Command " + cmd.getClass().getCanonicalName() + " result: " + cmd.getResult());
        if (h != null) {
            h.handle(cmd);
        }

        Intent resultIntent = new Intent();
        resultIntent.setAction("update");
        resultIntent.putExtra("result", obdResult);
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
            service = ((AbstractGatewayService.AbstractGatewayServiceBinder) binder).getService();
            service.setContext(OBDWorkerService.this);
            Logger.i(TAG, "Service connected, starting queue");

            if (!service.isRunning())
                if (!service.startService()) {
                    Toast.makeText(getApplicationContext(), "Не запустился сервис", Toast.LENGTH_LONG).show();
                    stop = true;
                    stopSelf();
                    return;
                }

            new Handler().post(mQueueCommands);
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
        }
    };

    @Override
    protected void onHandleIntent(Intent intent) {
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        OBD_SLEEP_SELECT_CM = sp.getInt(getString(R.string.obd_sleep_select_cm), 20);
        OBD_SLEEP_UPDATE = sp.getInt(getString(R.string.obd_sleep_update), 20);
        TCCM_ENG_ROT_BLOCK = sp.getInt(getString(R.string.tccm_eng_rot_block), 180);

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
        Logger.i(TAG, "Stopping service");
    }

    private final Runnable mQueueCommands = new Runnable() {
        public void run() {
            if (service != null && service.getCurrentQueueSize() == 0) {
                //Rear diff
                addRDCMCommands();
                //suspension
                addSuspensionCommands();
                //transfer case
                addTCComamnds();
                //gearbox
                addGearBoxCommands();
                //steering wheel
                addSteeringWheelCommands();
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
            obdResult.tcSolPos = ((TransferCaseSolenoidPositionCommand) cmd).isHi() ? "Hi" : "Lo";
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
                    obdResult.suspFLVal = (int) (shc.calc() + 10);
                    obdResult.suspFLText = cmd.getFormattedResult();
                }
                break;
                case SuspensionHeightCommand.FRONT_RIGHT: {
                    obdResult.suspFRVal = (int) (shc.calc() + 10);
                    obdResult.suspFRText = cmd.getFormattedResult();
                }
                break;
                case SuspensionHeightCommand.REAR_LEFT: {
                    obdResult.suspRLVal = (int) (shc.calc() + 10);
                    obdResult.suspRLText = cmd.getFormattedResult();
                }
                break;
                case SuspensionHeightCommand.REAR_RIGHT: {
                    obdResult.suspRRVal = (int) (shc.calc() + 10);
                    obdResult.suspRRText = cmd.getFormattedResult();
                }
                break;

            }
        });

        dispatch.put(DriveShiftPositionCommand.class, cmd -> obdResult.driveShiftPos = cmd.getFormattedResult());

        dispatch.put(SteeringWheelPositionCommand.class, cmd -> obdResult.wheelPos = ((SteeringWheelPositionCommand) cmd).calc());
    }
}
