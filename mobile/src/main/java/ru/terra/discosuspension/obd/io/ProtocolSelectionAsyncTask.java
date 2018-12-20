package ru.terra.discosuspension.obd.io;

import android.content.Context;

import pt.lighthouselabs.obd.commands.ObdCommand;
import pt.lighthouselabs.obd.enums.ObdProtocols;
import ru.terra.discosuspension.Logger;
import ru.terra.discosuspension.obd.AsyncTaskEx;
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
        super(300000l, a);
        this.connectionHelper = connectionHelper;
        showDialog("Определение протокола", "Запуск...");
    }


    @Override
    protected String doInBackground(Void... p) {
        //TODO: device ID;
        final String remoteDevice = "device_id";
        if (remoteDevice == null || remoteDevice.isEmpty()) {
            publishProgress("Не выбран bluetooth адаптер");
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
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
                e2.printStackTrace();
            }
            stopTask();
        }

        connect(remoteDevice);

        int tryes = 0;

        while (!found) {
            if (tryes > ObdProtocols.values().length + 5) {
                Logger.w(context, TAG, "Too many tries");
                break;
            }
            try {
                publishProgress("Сброс адаптера");
                Logger.d(context, TAG, "Сброс адаптера");
                try {
                    tryes++;
                    connectionHelper.doResetAdapter(context);
                } catch (Exception e) {
                    Logger.e(context, TAG, "Controller unable to ATZ command, reconnect", e);
//                    ACRA.getErrorReporter().handleSilentException(e);
//                    stopTask();
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e1) {
                        e.printStackTrace();
                    }
                    connect(remoteDevice);
                    continue;
                }

                ObdProtocols protocol = ObdProtocols.values()[currentProtocol];
                publishProgress("Пробуем протокол: " + protocol.name());
                Logger.d(context, TAG, "Trying protocol " + protocol.name());
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Logger.w(context, TAG, "Sleeping interrupted", e);
                }
                try {
                    if (execCommand(new TryProtocolCommand(protocol))) {
                        GetAvailPIDSCommand tryCmd = new GetAvailPIDSCommand();
                        if (execCommand(tryCmd)) {
                            Logger.d(context, TAG, "Try cmd result = " + tryCmd.getResult());
                            found = true;
                            Logger.i(context, TAG, "Протокол " + protocol.name() + " подходит");
                            publishProgress("Протокол " + protocol.name() + " подходит");
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                            }
                        } else {
                            Logger.w(context, TAG, "Unable to get avail pids");
                            publishProgress("Протокол " + protocol.name() + " не подходит");
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                            }

                        }
                    } else
                        Logger.w(context, TAG, "Unable to try protocol");

                } catch (Exception e) {
                    publishProgress("Протокол " + protocol.name() + " не подходит");
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e2) {
                    }
                }

            } catch (Exception e) {
                publishProgress("Ошибка: " + e.getMessage());
                e.printStackTrace();
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
//                e.printStackTrace();
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
        }

        stopTask();

        return null;
    }

    private void connect(String remoteDevice) {
        connectionHelper.disconnect();
        try {
            connectionHelper.connect();
            connectionHelper.doResetAdapter(context);
        } catch (BTOBDConnectionException e) {
            publishProgress("Ошибка при подключении: " + e.getMessage());
            try {
                Thread.sleep(500);
            } catch (InterruptedException e2) {
                e2.printStackTrace();
            }
            stopTask();
        }
    }

    private boolean execCommand(ObdCommand cmd) {
        return connectionHelper.executeCommand(cmd, context);

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
        dlg.setMessage(values[0]);
    }

    @Override
    protected void onPostExecute(String s) {
        dismissDialog();
    }
}
