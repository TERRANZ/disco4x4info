package ru.terra.discosuspension.obd.io;

import android.content.SharedPreferences;
import android.os.Binder;
import android.preference.PreferenceManager;

import org.acra.ACRA;

import pt.lighthouselabs.obd.enums.ObdProtocols;
import ru.terra.discosuspension.Logger;
import ru.terra.discosuspension.NotificationInstance;
import ru.terra.discosuspension.R;
import ru.terra.discosuspension.activity.ConfigActivity;
import ru.terra.discosuspension.obd.constants.ConnectionStatus;
import ru.terra.discosuspension.obd.io.helper.BtObdConnectionHelper;
import ru.terra.discosuspension.obd.io.helper.exception.BTOBDConnectionException;

/**
 * This service is primarily responsible for establishing and maintaining a
 * permanent connection between the device where the application runs and a more
 * OBD Bluetooth interface.
 * <p/>
 * Secondarily, it will serve as a repository of ObdCommandJobs and at the same
 * time the application state-machine.
 */
public class ObdGatewayService extends AbstractGatewayService {

    private static final String TAG = ObdGatewayService.class.getName();

    public BtObdConnectionHelper connectionHelper;

    public boolean startService() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        connectionHelper = BtObdConnectionHelper.getInstance(getApplicationContext());

        try {
            connectionHelper.start(prefs.getString(ConfigActivity.BLUETOOTH_LIST_KEY, null));
        } catch (BTOBDConnectionException e) {
            Logger.e(TAG, "There was an error while starting connection", e);
            stopService();
            NotificationInstance.getInstance().createInfoNotification(getApplicationContext(), "Не выбран OBD2 адаптер", false);
            return false;
        }

        while (connectionHelper.getConnectionStatus() != ConnectionStatus.CONNECTED) {

            try {
                connectionHelper.connect();
            } catch (BTOBDConnectionException e) {
                Logger.e(TAG, "There was an error while establishing connection", e);
                NotificationInstance.getInstance().createInfoNotification(getApplicationContext(), "Невозможно подключиться к адаптеру OBD2", false);
//            stopService();
//            return false;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }

        while (connectionHelper.getConnectionStatus() != ConnectionStatus.RESETTED) {

            try {
                connectionHelper.doResetAdapter(ctx.get());
            } catch (Exception e) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
        final String storedProtocolName = prefs.getString(getApplicationContext().getString(R.string.obd_protocol), null);
        ObdProtocols prot = ObdProtocols.ISO_15765_4_CAN_B;
        if (storedProtocolName != null) {
            prot = ObdProtocols.valueOf(storedProtocolName);
        }

        while (connectionHelper.getConnectionStatus() != ConnectionStatus.INWORK) {

            try {
                connectionHelper.doSelectProtocol(prot, ctx.get());
            } catch (BTOBDConnectionException e) {
                Logger.e(TAG, "There was an error while selecting protocol", e);
                ACRA.getErrorReporter().handleSilentException(e);
//                stopService();
//                return false;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }

        queueCounter = 0L;
        Logger.d(TAG, "Initialization jobs queued.");
        isRunning = true;
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
            } catch (Exception e) {
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