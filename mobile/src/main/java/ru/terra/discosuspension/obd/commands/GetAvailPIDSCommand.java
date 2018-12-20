package ru.terra.discosuspension.obd.commands;

import pt.lighthouselabs.obd.commands.protocol.ObdProtocolCommand;

/**
 * Date: 08.01.15
 * Time: 16:37
 */
public class GetAvailPIDSCommand extends ObdProtocolCommand {
    public GetAvailPIDSCommand() {
        super("01 00");
    }

    @Override
    public String getFormattedResult() {
        return getResult();
    }

    @Override
    public String getName() {
        return "Get available PIDs command";
    }
}
