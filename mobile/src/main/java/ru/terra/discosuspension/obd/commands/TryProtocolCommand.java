package ru.terra.discosuspension.obd.commands;

import pt.lighthouselabs.obd.commands.protocol.ObdProtocolCommand;
import pt.lighthouselabs.obd.enums.ObdProtocols;

/**
 * Date: 05.01.15
 * Time: 18:24
 */
public class TryProtocolCommand extends ObdProtocolCommand {

    private final ObdProtocols protocol;

    /**
     * @param protocol
     */
    public TryProtocolCommand(final ObdProtocols protocol) {
        super("ATTP" + protocol.getValue());
        this.protocol = protocol;
    }

    @Override
    public String getFormattedResult() {
        return getResult();
    }

    @Override
    public String getName() {
        return "Trying Protocol " + protocol.name();
    }

}
