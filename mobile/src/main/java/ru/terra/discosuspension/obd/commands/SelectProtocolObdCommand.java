package ru.terra.discosuspension.obd.commands;

import pt.lighthouselabs.obd.commands.protocol.ObdProtocolCommand;
import pt.lighthouselabs.obd.enums.ObdProtocols;

/**
 * Date: 11.02.15
 * Time: 13:14
 */
public class SelectProtocolObdCommand extends ObdProtocolCommand {

    private final ObdProtocols protocol;

    /**
     * @param protocol
     */
    public SelectProtocolObdCommand(final ObdProtocols protocol) {
        super("ATSP" + protocol.getValue());
        this.protocol = protocol;
    }

    @Override
    public String getFormattedResult() {
        return getResult();
    }

    @Override
    public String getName() {
        return "Select Protocol " + protocol.name();
    }

}