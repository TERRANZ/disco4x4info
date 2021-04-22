package ru.terra.discosuspension.obd.io;

import android.content.SharedPreferences;
import android.os.Binder;
import android.preference.PreferenceManager;
import android.util.Log;

import org.acra.ACRA;

import pt.lighthouselabs.obd.commands.protocol.ObdProtocolCommand;
import pt.lighthouselabs.obd.enums.ObdProtocols;
import ru.terra.discosuspension.Logger;
import ru.terra.discosuspension.NotificationInstance;
import ru.terra.discosuspension.R;
import ru.terra.discosuspension.activity.ConfigActivity;
import ru.terra.discosuspension.obd.OBDBackend;
import ru.terra.discosuspension.obd.constants.ConnectionStatus;
import ru.terra.discosuspension.obd.io.bt.BtOBDBackend;
import ru.terra.discosuspension.obd.io.bt.exception.BTOBDConnectionException;

import static pt.lighthouselabs.obd.enums.ObdProtocols.ISO_15765_4_CAN_B;

public class ObdGatewayService extends AbstractGatewayService {

    private static final String TAG = ObdGatewayService.class.getName();

    private OBDBackend backEnd;
    private SharedPreferences prefs;

    public boolean startService() {
        backEnd = new BtOBDBackend();
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        boolean stop = false;

        while (!stop) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            doStep();

            if (backEnd.getConnectionStatus() == ConnectionStatus.INWORK) {
                Logger.d(TAG, "Gateway service started");
                stop = true;
                isRunning = true;
            } else if (backEnd.getConnectionStatus() == ConnectionStatus.ERROR) {
                Logger.d(TAG, "Gateway service error!");
                backEnd.disconnect();
                stop = true;
                isRunning = false;
            }
        }

        return isRunning;
    }

    private void doStep() {
        Log.i(TAG, "doStep: curr status: " + backEnd.getConnectionStatus().name());
        switch (backEnd.getConnectionStatus()) {
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
        final String storedProtocolName = prefs.getString(getApplicationContext().getString(R.string.obd_protocol), null);
        final ObdProtocols prot = storedProtocolName != null ? ObdProtocols.valueOf(storedProtocolName) : ISO_15765_4_CAN_B;
        try {
            backEnd.doSelectProtocol(prot);
        } catch (final BTOBDConnectionException e) {
            Logger.e(TAG, "There was an error while selecting protocol", e);
            NotificationInstance.getInstance().createInfoNotification("Невозможно выбрать протокол", false);
            return false;
        }
        return true;
    }

    private boolean doReset() {
        try {
            backEnd.doResetAdapter();
        } catch (final Exception e) {
            Logger.e(TAG, "There was an error while resetting adapter", e);
            NotificationInstance.getInstance().createInfoNotification("Невозможно сбросить адаптер OBD2", false);
            return false;
        }
        return true;
    }

    private boolean doConnect() {
        try {
            backEnd.connect();
        } catch (final BTOBDConnectionException e) {
            Logger.e(TAG, "There was an error while establishing connection", e);
            NotificationInstance.getInstance().createInfoNotification("Невозможно подключиться к адаптеру OBD2", false);
            return false;
        }
        return true;
    }

    private boolean doStart() {
        try {
            backEnd.start(prefs.getString(ConfigActivity.BLUETOOTH_LIST_KEY, null));
        } catch (final BTOBDConnectionException e) {
            Logger.e(TAG, "There was an error while starting connection", e);
            NotificationInstance.getInstance().createInfoNotification("Не выбран OBD2 адаптер", false);
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
            try {
                final ObdProtocolCommand cmd = jobsQueue.take();
                if (backEnd.executeCommand(cmd) && stateUpdater != null) {
                    stateUpdater.stateUpdate(cmd);
                }
            } catch (final Exception e) {
                ACRA.getErrorReporter().handleSilentException(e);
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

        backEnd.disconnect();

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
