package ru.terra.discosuspension.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

import pt.lighthouselabs.obd.commands.ObdCommand;
import ru.terra.discosuspension.Logger;
import ru.terra.discosuspension.R;
import ru.terra.discosuspension.obd.commands.disco3.CurrentGearCommand;
import ru.terra.discosuspension.obd.commands.disco3.DriveShiftPositionCommand;
import ru.terra.discosuspension.obd.commands.disco3.GearBoxTempCommand;
import ru.terra.discosuspension.obd.commands.disco3.RearDiffBlockCommand;
import ru.terra.discosuspension.obd.commands.disco3.RearDiffTempCommand;
import ru.terra.discosuspension.obd.commands.disco3.SelectControlModuleCommand;
import ru.terra.discosuspension.obd.commands.disco3.SuspensionHeightCommand;
import ru.terra.discosuspension.obd.commands.disco3.TransferCaseRotEngCommand;
import ru.terra.discosuspension.obd.commands.disco3.TransferCaseSolenoidPositionCommand;
import ru.terra.discosuspension.obd.commands.disco3.TransferCaseTempCommand;
import ru.terra.discosuspension.obd.constants.ControlModuleIDs;
import ru.terra.discosuspension.obd.io.AbstractGatewayService;
import ru.terra.discosuspension.obd.io.ObdCommandJob;
import ru.terra.discosuspension.obd.io.ObdGatewayService;

public class FourXFourInfoActivity extends AppCompatActivity {
    private static final String TAG = FourXFourInfoActivity.class.getName();
    private AbstractGatewayService service;
    private boolean isServiceBound;
    private static final Map<Class, CommandHandler> dispatch = new HashMap<>();
    private SelectControlModuleCommand scmcRearDiff = new SelectControlModuleCommand(ControlModuleIDs.REAR_DIFF_CONTROL_MODULE);
    private SelectControlModuleCommand scmcSuspension = new SelectControlModuleCommand(ControlModuleIDs.SUSPENSION_CONTROL_MODULE);
    private SelectControlModuleCommand scmcTC = new SelectControlModuleCommand(ControlModuleIDs.TRANSFER_CASE_CONTROL_MODULE);
    private SelectControlModuleCommand scmcGearBox = new SelectControlModuleCommand(ControlModuleIDs.GEARBOX_CONTROL_MODULE);

    private TextView tv_gb_temp, tv_tb_temp, tv_rd_temp, tv_gear, tv_curr_gear, tv_tc_rot, tv_tc_sol_len, tv_range, tv_gb_shit_pos;
    private TextView tv_w_fl, tv_w_rl, tv_w_rr, tv_w_fr;
    private ProgressBar pb_front_left, pb_front_right, pb_rear_left, pb_rear_right;
    private ImageView iv_rear_diff_lock, iv_central_diff_lock;

    private long lastSuspensionRequest, lastGBRequest = System.currentTimeMillis();
    private final static long SUSP_REQ_DIFF = 10000;
    private final static long GB_REQ_DIFF = 5000;

    private int OBD_SLEEP_UPDATE = 0;
    private int OBD_SLEEP_SELECT_CM = 0;
    private int TCCM_ENG_ROT_BLOCK = 0;

    private interface CommandHandler {
        void handle(ObdCommand cmd);
    }

    private ServiceConnection serviceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            Logger.d(FourXFourInfoActivity.this, TAG, className.toString() + " service is bound");
            isServiceBound = true;
            service = ((AbstractGatewayService.AbstractGatewayServiceBinder) binder).getService();
            service.setContext(FourXFourInfoActivity.this);
            Toast.makeText(FourXFourInfoActivity.this, "Подключение успешно", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            Logger.d(FourXFourInfoActivity.this, TAG, className.toString() + " service is unbound");
            isServiceBound = false;
        }
    };

    private final Runnable mQueueCommands = new Runnable() {
        public void run() {
            if (service != null) {
                if (!service.isRunning())
                    if (!service.startService()) {
                        finish();
                        return;
                    }
                if (service.getCurrentQueueSize() == 0)
                    if (isServiceBound) {
                        //Rear diff
                        addRDCMCommands();
                        //suspension
                        addSuspensionCommands();
                        //transfer case
                        addTCComamnds();
                        //gearbox
                        addGearBoxCommands();
                    }
            }
            new Handler().postDelayed(mQueueCommands, OBD_SLEEP_UPDATE);
        }
    };

    private void addRDCMCommands() {
        //RDCM
        service.queueJob(new ObdCommandJob(scmcRearDiff));
        doSleep();
        service.queueJob(new ObdCommandJob(new RearDiffTempCommand()));
        service.queueJob(new ObdCommandJob(new RearDiffBlockCommand()));
        doSleep();
    }

    private void addSuspensionCommands() {
        if (System.currentTimeMillis() - lastSuspensionRequest > SUSP_REQ_DIFF) {
            service.queueJob(new ObdCommandJob(scmcSuspension));
            doSleep();
            service.queueJob(new ObdCommandJob(new SuspensionHeightCommand(SuspensionHeightCommand.FRONT_LEFT)));
            service.queueJob(new ObdCommandJob(new SuspensionHeightCommand(SuspensionHeightCommand.FRONT_RIGHT)));
            service.queueJob(new ObdCommandJob(new SuspensionHeightCommand(SuspensionHeightCommand.REAR_LEFT)));
            service.queueJob(new ObdCommandJob(new SuspensionHeightCommand(SuspensionHeightCommand.REAR_RIGHT)));
            doSleep();
            lastSuspensionRequest = System.currentTimeMillis();
        }
    }

    private void addTCComamnds() {
        service.queueJob(new ObdCommandJob(scmcTC));
        doSleep();
        service.queueJob(new ObdCommandJob(new TransferCaseTempCommand()));
        service.queueJob(new ObdCommandJob(new TransferCaseRotEngCommand()));
        service.queueJob(new ObdCommandJob(new TransferCaseSolenoidPositionCommand()));
        doSleep();
    }

    private void addGearBoxCommands() {
        if (System.currentTimeMillis() - lastGBRequest > GB_REQ_DIFF) {
            service.queueJob(new ObdCommandJob(scmcGearBox));
            doSleep();
            service.queueJob(new ObdCommandJob(new CurrentGearCommand()));
            service.queueJob(new ObdCommandJob(new GearBoxTempCommand()));
            service.queueJob(new ObdCommandJob(new DriveShiftPositionCommand()));
            doSleep();
            lastGBRequest = System.currentTimeMillis();
        }
    }

    private void doSleep() {
        try {
            Thread.sleep(OBD_SLEEP_SELECT_CM);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_4x4_info);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        OBD_SLEEP_SELECT_CM = sp.getInt(getString(R.string.obd_sleep_select_cm), 20);
        OBD_SLEEP_UPDATE = sp.getInt(getString(R.string.obd_sleep_update), 20);
        TCCM_ENG_ROT_BLOCK = sp.getInt(getString(R.string.tccm_eng_rot_block), 180);

        tv_gb_temp = findViewById(R.id.tv_gb_temp);
        tv_tb_temp = findViewById(R.id.tv_tb_temp);
        tv_rd_temp = findViewById(R.id.tv_rd_temp);
        tv_gear = findViewById(R.id.tv_gear);
        tv_curr_gear = findViewById(R.id.tv_curr_gear);
        tv_tc_rot = findViewById(R.id.tv_tc_rot);
        tv_tc_sol_len = findViewById(R.id.tv_tc_sol_pos);
        tv_range = findViewById(R.id.tv_range);
        tv_gb_shit_pos = findViewById(R.id.tv_gb_shit_pos);

        pb_front_left = findViewById(R.id.pb_front_left);
        pb_front_right = findViewById(R.id.pb_front_right);
        pb_rear_left = findViewById(R.id.pb_rear_left);
        pb_rear_right = findViewById(R.id.pb_rear_right);

        tv_w_fl = findViewById(R.id.tv_w_fl);
        tv_w_fr = findViewById(R.id.tv_w_fr);
        tv_w_rl = findViewById(R.id.tv_w_rl);
        tv_w_rr = findViewById(R.id.tv_w_rr);

        iv_rear_diff_lock = findViewById(R.id.iv_rear_diff_lock);
        iv_central_diff_lock = findViewById(R.id.iv_central_diff_lock);


        dispatch.put(RearDiffTempCommand.class, cmd -> tv_rd_temp.setText(cmd.getFormattedResult()));

        dispatch.put(RearDiffBlockCommand.class, cmd -> {
            if (cmd.getFormattedResult().equalsIgnoreCase("on")) {
                iv_rear_diff_lock.setImageResource(R.drawable.locked);
            } else {
                iv_rear_diff_lock.setImageResource(R.drawable.unlocked);
            }
        });

        dispatch.put(TransferCaseRotEngCommand.class, cmd -> {
            tv_tc_rot.setText(cmd.getFormattedResult());
            TransferCaseRotEngCommand tcrec = (TransferCaseRotEngCommand) cmd;
            if (tcrec.getRes() > TCCM_ENG_ROT_BLOCK) {
                iv_central_diff_lock.setImageResource(R.drawable.locked);
            } else {
                iv_central_diff_lock.setImageResource(R.drawable.unlocked);
            }

        });


        dispatch.put(TransferCaseSolenoidPositionCommand.class, cmd -> {
            tv_tc_sol_len.setText(cmd.getFormattedResult());
            tv_range.setText(((TransferCaseSolenoidPositionCommand) cmd).isHi() ? "Hi" : "Lo");
        });


        dispatch.put(TransferCaseTempCommand.class, cmd -> tv_tb_temp.setText(cmd.getFormattedResult()));

        dispatch.put(CurrentGearCommand.class, cmd -> tv_curr_gear.setText(cmd.getFormattedResult()));

        dispatch.put(GearBoxTempCommand.class, cmd -> tv_gb_temp.setText(cmd.getFormattedResult()));

        dispatch.put(SuspensionHeightCommand.class, cmd -> {
            SuspensionHeightCommand shc = (SuspensionHeightCommand) cmd;
            switch (shc.getWheel()) {
                case SuspensionHeightCommand.FRONT_LEFT: {
                    pb_front_left.setProgress((int) (shc.calc() + 10));
                    tv_w_fl.setText(cmd.getFormattedResult());
                }
                break;
                case SuspensionHeightCommand.FRONT_RIGHT: {
                    pb_front_right.setProgress((int) (shc.calc() + 10));
                    tv_w_fr.setText(cmd.getFormattedResult());
                }
                break;
                case SuspensionHeightCommand.REAR_LEFT: {
                    pb_rear_left.setProgress((int) (shc.calc() + 10));
                    tv_w_rl.setText(cmd.getFormattedResult());
                }
                break;
                case SuspensionHeightCommand.REAR_RIGHT: {
                    pb_rear_right.setProgress((int) (shc.calc() + 10));
                    tv_w_rr.setText(cmd.getFormattedResult());
                }
                break;

            }
        });

        dispatch.put(DriveShiftPositionCommand.class, cmd -> {
            tv_gb_shit_pos.setText(cmd.getFormattedResult());
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        startLiveData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }

    protected void onResume() {
        super.onResume();
        Logger.d(this, TAG, "Resuming..");
    }

    private void startLiveData() {
        Logger.d(this, TAG, "Starting live data..");
        doBindService();
        new Handler().post(mQueueCommands);
    }

    private void doBindService() {
        if (!isServiceBound) {
            Logger.d(this, TAG, "Binding OBD service..");
            bindService(new Intent(this, ObdGatewayService.class), serviceConn, Context.BIND_AUTO_CREATE);
        }
    }

    private void doUnbindService() {
        if (isServiceBound) {
            Logger.d(FourXFourInfoActivity.this, TAG, "Unbinding OBD service..");
            unbindService(serviceConn);
            isServiceBound = false;
        }
    }

    public void stateUpdate(ObdCommand cmd) {
        CommandHandler h = dispatch.get(cmd.getClass());
        Logger.i(this, TAG, "Command " + cmd.getClass().getCanonicalName() + " result: " + cmd.getResult());
        if (h != null) {
            h.handle(cmd);
        }
    }
}
