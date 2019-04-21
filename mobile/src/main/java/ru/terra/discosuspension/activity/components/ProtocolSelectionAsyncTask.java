package ru.terra.discosuspension.activity.components;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import pt.lighthouselabs.obd.commands.ObdCommand;
import pt.lighthouselabs.obd.enums.ObdProtocols;
import ru.terra.discosuspension.Logger;
import ru.terra.discosuspension.R;
import ru.terra.discosuspension.activity.ConfigActivity;
import ru.terra.discosuspension.obd.commands.GetAvailPIDSCommand;
import ru.terra.discosuspension.obd.commands.TryProtocolCommand;
import ru.terra.discosuspension.obd.io.helper.BtObdConnectionHelper;
import ru.terra.discosuspension.obd.io.helper.exception.BTOBDConnectionException;

/**
 * Date: 05.01.15
 * Time: 17:42
 */
public class ProtocolSelectionAsyncTask extends AsyncTaskEx<Void, String, String> {

    private static final String TAG = ProtocolSelectionAsyncTask.class.getName();
    private BtObdConnectionHelper connectionHelper;

    public ProtocolSelectionAsyncTask(Context a, BtObdConnectionHelper connectionHelper) {
        super(300000L, a);
        this.connectionHelper = connectionHelper;
        showDialog("Определение протокола", "Запуск...");
    }


    @Override
    protected String doInBackground(Void... p) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.get());
        final String remoteDevice = prefs.getString(ConfigActivity.BLUETOOTH_LIST_KEY, null);
        if (remoteDevice == null || remoteDevice.isEmpty()) {
            publishProgress("Не выбран bluetooth адаптер");
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Logger.w(TAG, "Sleeping interrupted", e);
            }
            stopTask();
            return null;
        }

        boolean found = false;
        int currentProtocol = ObdProtocols.values().length - 1;
        try {
            connectionHelper.start(remoteDevice);
        } catch (BTOBDConnectionException e) {
            publishProgress("Ошибка при подключении: " + e.getMessage());
            try {
                Thread.sleep(500);
            } catch (InterruptedException e2) {
                Logger.w(TAG, "Sleeping interrupted", e);
            }
            stopTask();
        }

        connect();

        int tries = 0;

        while (!found) {
            if (tries > ObdProtocols.values().length + 5) {
                Logger.w(TAG, "Too many tries");
                break;
            }
            try {
                publishProgress("Сброс адаптера");
                Logger.d(TAG, "Сброс адаптера");
                try {
                    tries++;
                    connectionHelper.doResetAdapter(context.get());
                } catch (Exception e) {
                    Logger.e(TAG, "Controller unable to ATZ command, reconnect", e);
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e1) {
                        Logger.w(TAG, "Sleeping interrupted", e);
                    }
                    connect();
                    continue;
                }

                final ObdProtocols protocol = ObdProtocols.values()[currentProtocol];
                publishProgress("Пробуем протокол: " + protocol.name());
                Logger.d(TAG, "Trying protocol " + protocol.name());
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Logger.w(TAG, "Sleeping interrupted", e);
                }
                try {
                    if (execCommand(new TryProtocolCommand(protocol))) {
                        final GetAvailPIDSCommand tryCmd = new GetAvailPIDSCommand();
                        if (execCommand(tryCmd)) {
                            Logger.d(TAG, "Try cmd result = " + tryCmd.getResult());
                            found = true;
                            Logger.i(TAG, "Протокол " + protocol.name() + " подходит");
                            publishProgress("Протокол " + protocol.name() + " подходит");
                            prefs.edit().putString(context.get().getString(R.string.obd_protocol), protocol.name()).apply();
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                Logger.w(TAG, "Sleeping interrupted", e);
                            }
                        } else {
                            Logger.w(TAG, "Unable to get avail pids");
                            publishProgress("Протокол " + protocol.name() + " не подходит");
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                Logger.w(TAG, "Sleeping interrupted", e);
                            }

                        }
                    } else
                        Logger.w(TAG, "Unable to try protocol");

                } catch (Exception e) {
                    publishProgress("Протокол " + protocol.name() + " не подходит");
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e2) {
                        Logger.w(TAG, "Sleeping interrupted", e);
                    }
                }

            } catch (Exception e) {
                publishProgress("Ошибка: " + e.getMessage());
                e.printStackTrace();
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Logger.w(TAG, "Sleeping interrupted", e);
            }

            currentProtocol--;
            if (currentProtocol < 0) {
                publishProgress("Ни один протокол не подошёл");
                found = true;
            }
        }
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Logger.w(TAG, "Sleeping interrupted", e);
        }

        stopTask();

        return null;
    }

    private void connect() {
        connectionHelper.disconnect();
        try {
            connectionHelper.connect();
            connectionHelper.doResetAdapter(context.get());
        } catch (BTOBDConnectionException e) {
            publishProgress("Ошибка при подключении: " + e.getMessage());
            try {
                Thread.sleep(500);
            } catch (InterruptedException e2) {
                Logger.w(TAG, "Sleeping interrupted", e);
            }
            stopTask();
        }
    }

    private boolean execCommand(ObdCommand cmd) {
        return connectionHelper.executeCommand(cmd, context.get());

    }

    private void stopTask() {
        connectionHelper.disconnect();
    }

    @Override
    protected void onCancelled() {
        stopTask();
        dismissDialog();
    }

    @Override
    protected void onProgressUpdate(String... values) {
        progressDialog.setMessage(values[0]);
    }

    @Override
    protected void onPostExecute(String s) {
        dismissDialog();
    }
}