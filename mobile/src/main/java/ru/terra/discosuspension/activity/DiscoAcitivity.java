package ru.terra.discosuspension.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
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
import ru.terra.discosuspension.obd.ControlModuleIDs;
import ru.terra.discosuspension.obd.commands.disco3.CurrentGearCommand;
import ru.terra.discosuspension.obd.commands.disco3.GearBoxTempCommand;
import ru.terra.discosuspension.obd.commands.disco3.RearDiffBlockCommand;
import ru.terra.discosuspension.obd.commands.disco3.RearDiffTempCommand;
import ru.terra.discosuspension.obd.commands.disco3.SelectControlModuleCommand;
import ru.terra.discosuspension.obd.commands.disco3.SuspensionHeightCommand;
import ru.terra.discosuspension.obd.commands.disco3.TransferCaseTempCommand;
import ru.terra.discosuspension.obd.io.AbstractGatewayService;
import ru.terra.discosuspension.obd.io.ObdCommandJob;
import ru.terra.discosuspension.obd.io.ObdGatewayService;

public class DiscoAcitivity extends AppCompatActivity {
    private static final String TAG = DiscoAcitivity.class.getName();
    private AbstractGatewayService service;
    private boolean isServiceBound;
    private static final Map<Class, CommandHandler> dispatch = new HashMap<>();
    private SelectControlModuleCommand scmcRearDiff = new SelectControlModuleCommand(ControlModuleIDs.REAR_DIFF_CONTROL_MODULE);
    private SelectControlModuleCommand scmcSuspension = new SelectControlModuleCommand(ControlModuleIDs.SUSPENSION_CONTROL_MODULE);
    private SelectControlModuleCommand scmcTC = new SelectControlModuleCommand(ControlModuleIDs.TRANSFER_CASE_CONTROL_MODULE);
    private SelectControlModuleCommand scmcGearBox = new SelectControlModuleCommand(ControlModuleIDs.GEARBOX_CONTROL_MODULE);

    private TextView tv_gb_temp, tv_tb_temp, tv_rd_temp, tv_gear, tv_curr_gear;
    private ProgressBar pb_front_left, pb_front_right, pb_rear_left, pb_rear_right;
    private ImageView iv_rear_diff_lock, iv_central_diff_lock;

    private interface CommandHandler {
        void handle(ObdCommand cmd);
    }

    private ServiceConnection serviceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            Logger.d(DiscoAcitivity.this, TAG, className.toString() + " service is bound");
            isServiceBound = true;
            service = ((AbstractGatewayService.AbstractGatewayServiceBinder) binder).getService();
            service.setContext(DiscoAcitivity.this);
            Toast.makeText(DiscoAcitivity.this, "Подключение успешно", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            Logger.d(DiscoAcitivity.this, TAG, className.toString() + " service is unbound");
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
                        //RDCM
                        service.queueJob(new ObdCommandJob(scmcRearDiff));
                        sleep50();
                        service.queueJob(new ObdCommandJob(new RearDiffTempCommand()));
                        sleep50();
                        service.queueJob(new ObdCommandJob(new RearDiffBlockCommand()));
                        sleep50();
                        //suspension
                        service.queueJob(new ObdCommandJob(scmcSuspension));
                        sleep50();
                        service.queueJob(new ObdCommandJob(new SuspensionHeightCommand(SuspensionHeightCommand.FRONT_LEFT)));
                        sleep50();
                        service.queueJob(new ObdCommandJob(new SuspensionHeightCommand(SuspensionHeightCommand.FRONT_RIGHT)));
                        sleep50();
                        service.queueJob(new ObdCommandJob(new SuspensionHeightCommand(SuspensionHeightCommand.REAR_LEFT)));
                        sleep50();
                        service.queueJob(new ObdCommandJob(new SuspensionHeightCommand(SuspensionHeightCommand.REAR_RIGHT)));
                        sleep50();
                        //transfer case
                        service.queueJob(new ObdCommandJob(scmcTC));
                        sleep50();
                        service.queueJob(new ObdCommandJob(new TransferCaseTempCommand()));
                        sleep50();
                        //gearbox
                        service.queueJob(new ObdCommandJob(scmcGearBox));
                        sleep50();
                        service.queueJob(new ObdCommandJob(new CurrentGearCommand()));
                        sleep50();
                        service.queueJob(new ObdCommandJob(new GearBoxTempCommand()));
                        sleep50();
                    }
            }
            new Handler().postDelayed(mQueueCommands, 500);
        }
    };

    private static void sleep50() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_4x4_info);

        tv_gb_temp = findViewById(R.id.tv_gb_temp);
        tv_tb_temp = findViewById(R.id.tv_tb_temp);
        tv_rd_temp = findViewById(R.id.tv_rd_temp);
        tv_gear = findViewById(R.id.tv_gear);
        tv_curr_gear = findViewById(R.id.tv_curr_gear);

        pb_front_left = findViewById(R.id.pb_front_left);
        pb_front_right = findViewById(R.id.pb_front_right);
        pb_rear_left = findViewById(R.id.pb_rear_left);
        pb_rear_right = findViewById(R.id.pb_rear_right);

        iv_rear_diff_lock = findViewById(R.id.iv_rear_diff_lock);
        iv_central_diff_lock = findViewById(R.id.iv_central_diff_lock);


        dispatch.put(RearDiffTempCommand.class, new CommandHandler() {
            @Override
            public void handle(ObdCommand cmd) {
                tv_rd_temp.setText(cmd.getFormattedResult());
            }
        });

        dispatch.put(RearDiffBlockCommand.class, new CommandHandler() {
            @Override
            public void handle(ObdCommand cmd) {
                if (cmd.getFormattedResult().equalsIgnoreCase("on")) {
                    iv_rear_diff_lock.setImageResource(R.drawable.locked);
                } else {
                    iv_rear_diff_lock.setImageResource(R.drawable.unlocked);
                }
            }
        });

        dispatch.put(TransferCaseTempCommand.class, new CommandHandler() {
            @Override
            public void handle(ObdCommand cmd) {
                tv_tb_temp.setText(cmd.getFormattedResult());
            }
        });

        dispatch.put(CurrentGearCommand.class, new CommandHandler() {
            @Override
            public void handle(ObdCommand cmd) {
                tv_curr_gear.setText(cmd.getFormattedResult());
            }
        });

        dispatch.put(GearBoxTempCommand.class, new CommandHandler() {
            @Override
            public void handle(ObdCommand cmd) {
                tv_gb_temp.setText(cmd.getFormattedResult());
            }
        });

        dispatch.put(SuspensionHeightCommand.class, new CommandHandler() {
            @Override
            public void handle(ObdCommand cmd) {
                SuspensionHeightCommand shc = (SuspensionHeightCommand) cmd;
                switch (shc.getWheel()) {
                    case SuspensionHeightCommand.FRONT_LEFT: {
                        pb_front_left.setProgress((int) (shc.calc() + 10));
                    }
                    break;
                    case SuspensionHeightCommand.FRONT_RIGHT: {
                        pb_front_right.setProgress((int) (shc.calc() + 10));
                    }
                    break;
                    case SuspensionHeightCommand.REAR_LEFT: {
                        pb_rear_left.setProgress((int) (shc.calc() + 10));
                    }
                    break;
                    case SuspensionHeightCommand.REAR_RIGHT: {
                        pb_rear_right.setProgress((int) (shc.calc() + 10));
                    }
                    break;

                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
//        startLiveData();
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
            Logger.d(DiscoAcitivity.this, TAG, "Unbinding OBD service..");
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
