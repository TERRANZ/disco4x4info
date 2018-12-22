package ru.terra.discosuspension.obd.commands;

import pt.lighthouselabs.obd.commands.protocol.ObdProtocolCommand;

public class DisplayHeaderCommand extends ObdProtocolCommand {
    public DisplayHeaderCommand() {
        super("ATH1");
    }

    @Override
    public String getFormattedResult() {
        return getResult();
    }

    @Override
    public String getName() {
        return "Set display headers parameter.";
    }
}
