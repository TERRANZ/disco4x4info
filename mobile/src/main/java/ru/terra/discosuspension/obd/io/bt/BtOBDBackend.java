package ru.terra.discosuspension.obd.io.bt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.UUID;

import pt.lighthouselabs.obd.commands.ObdCommand;
import pt.lighthouselabs.obd.enums.ObdProtocols;
import pt.lighthouselabs.obd.exceptions.ObdResponseException;
import ru.terra.discosuspension.Logger;
import ru.terra.discosuspension.NotificationInstance;
import ru.terra.discosuspension.obd.OBDBackend;
import ru.terra.discosuspension.obd.commands.DisplayHeaderCommand;
import ru.terra.discosuspension.obd.commands.ObdResetFixCommand;
import ru.terra.discosuspension.obd.commands.SelectProtocolObdCommand;
import ru.terra.discosuspension.obd.constants.ConnectionStatus;
import ru.terra.discosuspension.obd.io.bt.exception.BTOBDConnectionException;

import static ru.terra.discosuspension.obd.constants.ConnectionStatus.CONNECTED;
import static ru.terra.discosuspension.obd.constants.ConnectionStatus.ERROR;
import static ru.terra.discosuspension.obd.constants.ConnectionStatus.PROTOCOL_SELECTED;
import static ru.terra.discosuspension.obd.constants.ConnectionStatus.RESETTED;

/**
 * Date: 12.02.15
 * Time: 21:20
 */
public class BtOBDBackend implements OBDBackend {
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String TAG = BtOBDBackend.class.getName();
    private BluetoothSocket sock = null;
    private String remoteDevice;
    private ConnectionStatus connectionStatus = ConnectionStatus.NC;

    @Override
    public void start(final String remoteDevice) throws BTOBDConnectionException {
        this.remoteDevice = remoteDevice;
        Logger.d(TAG, "Starting service..");
        if (remoteDevice == null || remoteDevice.isEmpty()) {
            connectionStatus = ConnectionStatus.NC;
            throw new BTOBDConnectionException("No Bluetooth device has been selected.");
        }
        connectionStatus = ConnectionStatus.DEV_SELECTED;
    }

    @Override
    public void connect() throws BTOBDConnectionException {
        final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        final BluetoothDevice dev = btAdapter.getRemoteDevice(remoteDevice);
        btAdapter.cancelDiscovery();
        Logger.d(TAG, "Starting OBD connection..");
        sendStatus("Подключение", false);
        try {
            // Instantiate a BluetoothSocket for the remote device and connect it.
            sock = dev.createInsecureRfcommSocketToServiceRecord(MY_UUID);
            sock.connect();
            sendStatus("Подключено", true);
            connectionStatus = CONNECTED;
        } catch (final Exception e1) {
            Logger.e(TAG, "There was an error while establishing Bluetooth connection. Falling back..", e1);
            if (sock == null) {
                sendStatus("Ошибка: Bluetooth отключен", false);
                disconnect();
                connectionStatus = ERROR;
                throw new BTOBDConnectionException("Ошибка: Невозможно подключиться к адаптеру");
            }
            Class<?> clazz = sock.getRemoteDevice().getClass();
            Class<?>[] paramTypes = new Class<?>[]{Integer.TYPE};
            try {
                final Method m = clazz.getMethod("createRfcommSocket", paramTypes);
                final BluetoothSocket sockFallback = (BluetoothSocket) m.invoke(sock.getRemoteDevice(), new Object[]{1});
                sockFallback.connect();
                sock = sockFallback;
                sendStatus("Подключено", true);
                connectionStatus = CONNECTED;
            } catch (final Exception e2) {
                Logger.e(TAG, "Couldn't fallback while establishing Bluetooth connection. Stopping app..", e2);
                sendStatus("Ошибка: Невозможно подключиться к адаптеру", false);
                disconnect();
                connectionStatus = ERROR;
                throw new BTOBDConnectionException("Ошибка: Невозможно подключиться к адаптеру");
            }
        }
    }

    @Override
    public void disconnect() {
        if (sock != null)
            // close socket
            try {
                sock.close();
            } catch (IOException e) {
                Logger.e(TAG, e.getMessage(), e);
            }

        connectionStatus = ConnectionStatus.NC;
        sendStatus("Отключено", true);
    }

    @Override
    public void doResetAdapter() throws ObdResponseException {
        if (executeCommand(new ObdResetFixCommand())) {
            sendStatus("Сброс адаптера", false);
            if (executeCommand(new DisplayHeaderCommand())) {
                connectionStatus = RESETTED;
            } else {
                connectionStatus = ERROR;
            }
        } else {
            connectionStatus = ERROR;
        }

    }

    @Override
    public void doSelectProtocol(final ObdProtocols prot)
            throws BTOBDConnectionException {
        // For now set protocol to AUTO
        Logger.d(TAG, "Selecting protocol: " + prot.name());
        if (executeCommand(new SelectProtocolObdCommand(prot))) {
            sendStatus("Выставление протокола", false);
            connectionStatus = PROTOCOL_SELECTED;
            sendStatus("В работе", true);
            connectionStatus = ConnectionStatus.INWORK;
        } else {
            Logger.w(TAG, "Unable to select protocol");
            connectionStatus = ERROR;
            throw new BTOBDConnectionException("Unable to select protocol");
        }
    }

    @Override
    public boolean executeCommand(final ObdCommand cmd)
            throws ObdResponseException {
        boolean ret;
        try {
            cmd.run(sock.getInputStream(), sock.getOutputStream());
            ret = true;
        } catch (Exception e) {
            ret = false;
        }

        return ret;
    }

    @Override
    public ConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }

    private void sendStatus(final String text, final boolean finalMessage) {
        NotificationInstance.getInstance().createInfoNotification(text, finalMessage);
    }
}
