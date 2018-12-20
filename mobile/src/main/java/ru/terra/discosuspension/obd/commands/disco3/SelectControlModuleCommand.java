package ru.terra.discosuspension.obd.commands.disco3;

import pt.lighthouselabs.obd.commands.protocol.ObdProtocolCommand;

public class SelectControlModuleCommand extends ObdProtocolCommand {
    public SelectControlModuleCommand(final String blockId) {
        super("ATSH" + blockId);
    }

    @Override
    public String getFormattedResult() {
        return null;
    }

    @Override
    public String getName() {
        return "Select control module command";
    }
}
