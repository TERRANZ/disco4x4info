package ru.terra.discosuspension.obd.io.helper;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.UUID;

import pt.lighthouselabs.obd.commands.ObdCommand;
import pt.lighthouselabs.obd.enums.ObdProtocols;
import pt.lighthouselabs.obd.exceptions.ObdResponseException;
import ru.terra.discosuspension.Logger;
import ru.terra.discosuspension.NotificationInstance;
import ru.terra.discosuspension.activity.FourXFourInfoActivity;
import ru.terra.discosuspension.obd.commands.DisplayHeaderCommand;
import ru.terra.discosuspension.obd.commands.EngineRPMCommand;
import ru.terra.discosuspension.obd.commands.ObdResetFixCommand;
import ru.terra.discosuspension.obd.commands.SelectProtocolObdCommand;
import ru.terra.discosuspension.obd.constants.ConnectionStatus;
import ru.terra.discosuspension.obd.io.helper.exception.BTOBDConnectionException;

/**
 * Date: 12.02.15
 * Time: 21:20
 */
public class BtObdConnectionHelper {
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String TAG = BtObdConnectionHelper.class.getName();
    private static BtObdConnectionHelper instance = new BtObdConnectionHelper();
    private WeakReference<Context> context;
    private BluetoothSocket sock = null;
    private String remoteDevice;
    private ConnectionStatus connectionStatus = ConnectionStatus.NC;

    private BtObdConnectionHelper() {
    }

    public static BtObdConnectionHelper getInstance(final Context context) {
        instance.context = new WeakReference<>(context);
        return instance;
    }

    public void start(String remoteDevice) throws BTOBDConnectionException {
        this.remoteDevice = remoteDevice;
        Logger.d(TAG, "Starting service..");
        if (remoteDevice == null || remoteDevice.isEmpty())
            throw new BTOBDConnectionException("No Bluetooth device has been selected.");
        connectionStatus = ConnectionStatus.DEV_SELECTED;
    }

    public void connect() throws BTOBDConnectionException {
        final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice dev = btAdapter.getRemoteDevice(remoteDevice);
        btAdapter.cancelDiscovery();
        Logger.d(TAG, "Starting OBD connection..");
        sendStatus("Подключение", false);
        try {
            // Instantiate a BluetoothSocket for the remote device and connect it.
            sock = dev.createInsecureRfcommSocketToServiceRecord(MY_UUID);
            sock.connect();
            sendStatus("Подключено", false);
            connectionStatus = ConnectionStatus.CONNECTED;
        } catch (Exception e1) {
            Logger.e(TAG, "There was an error while establishing Bluetooth connection. Falling back..", e1);
            if (sock == null) {
                sendStatus("Ошибка: Bluetooth отключен", false);
                disconnect();
                throw new BTOBDConnectionException("Ошибка: Невозможно подключиться к адаптеру");
            }
            Class<?> clazz = sock.getRemoteDevice().getClass();
            Class<?>[] paramTypes = new Class<?>[]{Integer.TYPE};
            try {
                Method m = clazz.getMethod("createRfcommSocket", paramTypes);
                BluetoothSocket sockFallback = (BluetoothSocket) m.invoke(sock.getRemoteDevice(), new Object[]{1});
                sockFallback.connect();
                sock = sockFallback;
                sendStatus("Подключено", false);
                connectionStatus = ConnectionStatus.CONNECTED;
            } catch (Exception e2) {
                Logger.e(TAG, "Couldn't fallback while establishing Bluetooth connection. Stopping app..", e2);
                sendStatus("Ошибка: Невозможно подключиться к адаптеру", false);
                disconnect();
                throw new BTOBDConnectionException("Ошибка: Невозможно подключиться к адаптеру");
            }
        }
    }

    public void disconnect() {
        if (sock != null)
            // close socket
            try {
                sock.close();
                connectionStatus = ConnectionStatus.DISCONNECTED;
                sendStatus("Отключено", true);
            } catch (IOException e) {
                Logger.e(TAG, e.getMessage(), e);
            }
        else
            connectionStatus = ConnectionStatus.NC;
    }

    public void doResetAdapter(Context runContext) throws ObdResponseException {
        Logger.d(TAG, "Queing jobs for connection configuration..");
        if (executeCommand(new ObdResetFixCommand(), runContext)) {
            sendStatus("Сброс адаптера", false);
            if (executeCommand(new DisplayHeaderCommand(), runContext))
                connectionStatus = ConnectionStatus.RESETTED;
        }

    }

    public void doSelectProtocol(ObdProtocols prot, Context runContext) throws BTOBDConnectionException {
        // For now set protocol to AUTO
        Logger.d(TAG, "Selecting protocol: " + prot.name());
        executeCommand(new SelectProtocolObdCommand(prot), runContext);
        sendStatus("Выставление протокола", false);
        connectionStatus = ConnectionStatus.PROTOCOL_SELECTED;
        // Job for returning dummy data
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (!executeCommand(new EngineRPMCommand(), runContext)) {
            Logger.w(TAG, "Unable to select protocol");
            throw new BTOBDConnectionException("Unable to select protocol");
        }
        sendStatus("В работе", true);
        connectionStatus = ConnectionStatus.INWORK;
    }

    public boolean executeCommand(final ObdCommand cmd, final Context runContext) throws ObdResponseException {
        try {
            cmd.run(sock.getInputStream(), sock.getOutputStream());
        } catch (Exception e) {
//            Logger.w(TAG, "Unable to execute command", e);
            return false;
        }
        if (runContext instanceof FourXFourInfoActivity)
            ((FourXFourInfoActivity) runContext).runOnUiThread(() -> ((FourXFourInfoActivity) runContext).stateUpdate(cmd));
        return true;
    }

    public BluetoothSocket getSock() {
        return sock;
    }

    public ConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }

    private void sendStatus(String text, boolean finalMessage) {
        NotificationInstance.getInstance().createInfoNotification(context.get(), text, finalMessage);
    }
}
