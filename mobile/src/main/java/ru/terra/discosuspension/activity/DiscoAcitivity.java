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
import ru.terra.discosuspension.obd.io.AbstractGatewayService;
import ru.terra.discosuspension.obd.io.ObdCommandJob;
import ru.terra.discosuspension.obd.io.ObdGatewayService;

public class DiscoAcitivity extends AppCompatActivity {
    private static final String TAG = DiscoAcitivity.class.getName();
    private AbstractGatewayService service;
    private boolean isServiceBound;
    private static final Map<Class, CommandHandler> dispatch = new HashMap<>();

    private TextView tvRDTemp, tvRDBlock;

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
                        //select control module RDCM
                        service.queueJob(new ObdCommandJob(new SelectControlModuleCommand(ControlModuleIDs.REAR_DIFF_CONTROL_MODULE)));
                        service.queueJob(new ObdCommandJob(new RearDiffTempCommand()));
                        service.queueJob(new ObdCommandJob(new RearDiffBlockCommand()));
                    }
            }
            new Handler().postDelayed(mQueueCommands, 10);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_disco);

        tvRDTemp = findViewById(R.id.tv_rd_temp);
        tvRDBlock = findViewById(R.id.tv_rd_block);

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
        if (h != null) {
            h.handle(cmd);
        }
    }
}
