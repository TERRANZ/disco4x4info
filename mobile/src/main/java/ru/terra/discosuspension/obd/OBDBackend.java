package ru.terra.discosuspension.obd;

import pt.lighthouselabs.obd.commands.ObdCommand;
import pt.lighthouselabs.obd.enums.ObdProtocols;
import pt.lighthouselabs.obd.exceptions.ObdResponseException;
import ru.terra.discosuspension.obd.constants.ConnectionStatus;
import ru.terra.discosuspension.obd.io.bt.exception.BTOBDConnectionException;

public interface OBDBackend {
    void start(String remoteDevice) throws BTOBDConnectionException;

    void connect() throws BTOBDConnectionException;

    void disconnect();

    void doResetAdapter() throws ObdResponseException;

    void doSelectProtocol(ObdProtocols prot) throws BTOBDConnectionException;

    boolean executeCommand(ObdCommand cmd) throws ObdResponseException;

    ConnectionStatus getConnectionStatus();
}
