package ru.terra.discosuspension.obd.io;

import android.content.SharedPreferences;
import android.os.Binder;
import android.preference.PreferenceManager;
import android.util.Log;

import org.acra.ACRA;

import pt.lighthouselabs.obd.enums.ObdProtocols;
import ru.terra.discosuspension.Logger;
import ru.terra.discosuspension.NotificationInstance;
import ru.terra.discosuspension.R;
import ru.terra.discosuspension.activity.ConfigActivity;
import ru.terra.discosuspension.obd.constants.ConnectionStatus;
import ru.terra.discosuspension.obd.io.bt.BtObdConnectionHelper;
import ru.terra.discosuspension.obd.io.bt.exception.BTOBDConnectionException;

public class ObdGatewayService extends AbstractGatewayService {

    private static final String TAG = ObdGatewayService.class.getName();

    private BtObdConnectionHelper connectionHelper;
    private SharedPreferences prefs;

    public boolean startService() {
        connectionHelper = BtObdConnectionHelper.getInstance(getApplicationContext());
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        boolean stop = false;

        while (!stop) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            doStep();

            if (connectionHelper.getConnectionStatus() == ConnectionStatus.INWORK) {
                stop = true;
                queueCounter = 0L;
                Logger.d(TAG, "Gateway service started");
                isRunning = true;
            } else if (connectionHelper.getConnectionStatus() == ConnectionStatus.ERROR) {
                Logger.d(TAG, "Gateway service error!");
                connectionHelper.disconnect();
            }
        }

        return isRunning;
    }

    private void doStep() {
        Log.i(TAG, "doStep: curr status: " + connectionHelper.getConnectionStatus().name());
        switch (connectionHelper.getConnectionStatus()) {
            case NC:
                if (!doStart()) {
                    Log.i(TAG, "doStep: not started");
                }
                break;
            case DEV_SELECTED:
                if (!doConnect()) {
                    Log.i(TAG, "doStep: not connected");
                }
                break;
            case CONNECTED:
                if (!doReset()) {
                    Log.i(TAG, "doStep: not resetted");
                }
                break;
            case RESETTED:
                if (!doSelectProtocol()) {
                    Log.i(TAG, "doStep: not selected protocol");
                }
                break;
        }
    }

    private boolean doSelectProtocol() {
        final String storedProtocolName =
                prefs.getString(getApplicationContext().getString(R.string.obd_protocol), null);
        final ObdProtocols prot = storedProtocolName != null ?
                ObdProtocols.valueOf(storedProtocolName) : ObdProtocols.ISO_15765_4_CAN_B;
        try {
            connectionHelper.doSelectProtocol(prot, ctx.get());
        } catch (final BTOBDConnectionException e) {
            Logger.e(TAG, "There was an error while selecting protocol", e);
            NotificationInstance.getInstance().createInfoNotification(getApplicationContext(), "Невозможно выбрать протокол", false);
            return false;
        }
        return true;
    }

    private boolean doReset() {
        try {
            connectionHelper.doResetAdapter(ctx.get());
        } catch (final Exception e) {
            Logger.e(TAG, "There was an error while resetting adapter", e);
            NotificationInstance.getInstance().createInfoNotification(getApplicationContext(), "Невозможно сбросить адаптер OBD2", false);
            return false;
        }
        return true;
    }

    private boolean doConnect() {
        try {
            connectionHelper.connect();
        } catch (final BTOBDConnectionException e) {
            Logger.e(TAG, "There was an error while establishing connection", e);
            NotificationInstance.getInstance().createInfoNotification(getApplicationContext(), "Невозможно подключиться к адаптеру OBD2", false);
            return false;
        }
        return true;
    }

    private boolean doStart() {
        try {
            connectionHelper.start(prefs.getString(ConfigActivity.BLUETOOTH_LIST_KEY, null));
        } catch (final BTOBDConnectionException e) {
            Logger.e(TAG, "There was an error while starting connection", e);
            NotificationInstance.getInstance().createInfoNotification(getApplicationContext(), "Не выбран OBD2 адаптер", false);
            return false;
        }
        return true;
    }


    /**
     * Runs the queue until the service is stopped
     */
    protected void executeQueue() {
        isQueueRunning = true;
        while (!jobsQueue.isEmpty()) {
            ObdCommandJob job = null;
            try {
                job = jobsQueue.take();
                // log job
                if (job.getState().equals(ObdCommandJob.ObdCommandJobState.NEW)) {
                    job.setState(ObdCommandJob.ObdCommandJobState.RUNNING);
                    connectionHelper.executeCommand(job.getCommand(), ctx.get());
                }
            } catch (final Exception e) {
                if (job != null) {
                    job.setState(ObdCommandJob.ObdCommandJobState.EXECUTION_ERROR);
                    ACRA.getErrorReporter().handleSilentException(e);
                }
            }
        }
        // will run next time a job is queued
        isQueueRunning = false;
    }

    /**
     * Stop OBD connection and queue processing.
     */
    public void stopService() {
        Logger.d(TAG, "Stopping service..");
        jobsQueue.clear();
        isRunning = false;

        connectionHelper.disconnect();

        stopSelf();
    }

    public boolean isRunning() {
        return isRunning;
    }

    public class ObdGatewayServiceBinder extends Binder {
        public ObdGatewayService getService() {
            return ObdGatewayService.this;
        }
    }

}