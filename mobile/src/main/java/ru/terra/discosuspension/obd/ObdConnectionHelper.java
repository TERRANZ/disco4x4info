package ru.terra.discosuspension.obd;

import android.content.Context;

import pt.lighthouselabs.obd.commands.ObdCommand;
import pt.lighthouselabs.obd.enums.ObdProtocols;
import pt.lighthouselabs.obd.exceptions.ObdResponseException;
import ru.terra.discosuspension.obd.constants.ConnectionStatus;
import ru.terra.discosuspension.obd.io.bt.exception.BTOBDConnectionException;

public interface ObdConnectionHelper {
    void start(String remoteDevice) throws BTOBDConnectionException;

    void connect() throws BTOBDConnectionException;

    void disconnect();

    void doResetAdapter(Context runContext) throws ObdResponseException;

    void doSelectProtocol(ObdProtocols prot, Context runContext)
            throws BTOBDConnectionException;

    boolean executeCommand(ObdCommand cmd, Context runContext)
            throws ObdResponseException;

    ConnectionStatus getConnectionStatus();
}
