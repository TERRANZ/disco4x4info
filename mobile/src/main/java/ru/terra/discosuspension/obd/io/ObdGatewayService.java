package ru.terra.discosuspension.obd.io;

import android.content.SharedPreferences;
import android.os.Binder;
import android.preference.PreferenceManager;

import org.acra.ACRA;

import pt.lighthouselabs.obd.enums.ObdProtocols;
import ru.terra.discosuspension.Logger;
import ru.terra.discosuspension.R;
import ru.terra.discosuspension.activity.ConfigActivity;
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
            ACRA.getErrorReporter().handleSilentException(e);
            stopService();
            return false;
        }

        try {
            connectionHelper.connect();
        } catch (BTOBDConnectionException e) {
            Logger.e(TAG, "There was an error while establishing connection", e);
            ACRA.getErrorReporter().handleSilentException(e);
            stopService();
            return false;
        }

        connectionHelper.doResetAdapter(ctx);

        ObdProtocols prot = ObdProtocols.valueOf(prefs.getString(getString(R.string.obd_protocol), String.valueOf(ObdProtocols.ISO_15765_4_CAN_B.getValue())));
        try {
            connectionHelper.doSelectProtocol(prot, ctx);
        } catch (BTOBDConnectionException e) {
            Logger.e(TAG, "There was an error while selecting protocol", e);
            ACRA.getErrorReporter().handleSilentException(e);
            stopService();
            return false;
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
        Logger.d(TAG, "Executing queue..");
        isQueueRunning = true;
        while (!jobsQueue.isEmpty()) {
            ObdCommandJob job = null;
            try {
                job = jobsQueue.take();
                // log job
                Logger.d(TAG, "Taking job[" + job.getId() + "] from queue..");
                if (job.getState().equals(ObdCommandJob.ObdCommandJobState.NEW)) {
                    Logger.d(TAG, "Job state is NEW. Run it..");
                    job.setState(ObdCommandJob.ObdCommandJobState.RUNNING);
                    connectionHelper.executeCommand(job.getCommand(), ctx);
                } else
                    // log not new job
                    Logger.w(TAG, "Job state was not new, so it shouldn't be in queue. BUG ALERT!");
            } catch (Exception e) {
                if (job != null) {
                    job.setState(ObdCommandJob.ObdCommandJobState.EXECUTION_ERROR);
                    Logger.e(TAG, "Failed to run command", e);
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