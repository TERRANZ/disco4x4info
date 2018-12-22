package ru.terra.discosuspension.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

import pt.lighthouselabs.obd.commands.ObdCommand;
import ru.terra.discosuspension.Logger;
import ru.terra.discosuspension.R;
import ru.terra.discosuspension.obd.ControlModuleIDs;
import ru.terra.discosuspension.obd.commands.disco3.RearDiffBlockCommand;
import ru.terra.discosuspension.obd.commands.disco3.RearDiffTempCommand;
import ru.terra.discosuspension.obd.commands.disco3.SelectControlModuleCommand;
import ru.terra.discosuspension.obd.commands.disco3.SuspensionHeightCommand;
import ru.terra.discosuspension.obd.io.AbstractGatewayService;
import ru.terra.discosuspension.obd.io.ObdCommandJob;
import ru.terra.discosuspension.obd.io.ObdGatewayService;

public class DiscoAcitivity extends AppCompatActivity {
    private static final String TAG = DiscoAcitivity.class.getName();
    private AbstractGatewayService service;
    private boolean isServiceBound;
    private static final Map<Class, CommandHandler> dispatch = new HashMap<>();
    private SelectControlModuleCommand scmcRearDiff = new SelectControlModuleCommand(ControlModuleIDs.REAR_DIFF_CONTROL_MODULE);
    private SelectControlModuleCommand scmcSuspension = new SelectControlModuleCommand(ControlModuleIDs.REAR_DIFF_CONTROL_MODULE);

    private TextView tvRDTemp, tvRDBlock, tv_fl, tv_fr, tv_rl, tv_rr;

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
                        service.queueJob(new ObdCommandJob(scmcRearDiff));
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
        setContentView(R.layout.a_disco);

        tvRDTemp = findViewById(R.id.tv_rd_temp);
        tvRDBlock = findViewById(R.id.tv_rd_block);
        tv_fl = findViewById(R.id.tv_front_left);
        tv_fr = findViewById(R.id.tv_fron_right);
        tv_rl = findViewById(R.id.tv_rear_left);
        tv_rr = findViewById(R.id.tv_rear_right);

        dispatch.put(RearDiffTempCommand.class, new CommandHandler() {
            @Override
            public void handle(ObdCommand cmd) {
                tvRDTemp.setText(cmd.getFormattedResult());
            }
        });

        dispatch.put(RearDiffBlockCommand.class, new CommandHandler() {
            @Override
            public void handle(ObdCommand cmd) {
                tvRDBlock.setText(cmd.getFormattedResult());
            }
        });

        dispatch.put(SuspensionHeightCommand.class, new CommandHandler() {
            @Override
            public void handle(ObdCommand cmd) {
                SuspensionHeightCommand shc = (SuspensionHeightCommand) cmd;
                switch (shc.getWheel()) {
                    case SuspensionHeightCommand.FRONT_LEFT: {
                        tv_fl.setText(cmd.getFormattedResult());
                    }
                    break;
                    case SuspensionHeightCommand.FRONT_RIGHT: {
                        tv_fr.setText(cmd.getFormattedResult());
                    }
                    break;
                    case SuspensionHeightCommand.REAR_LEFT: {
                        tv_rl.setText(cmd.getFormattedResult());
                    }
                    break;
                    case SuspensionHeightCommand.REAR_RIGHT: {
                        tv_rr.setText(cmd.getFormattedResult());
                    }
                    break;

                }
            }
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
