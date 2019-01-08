package ru.terra.discosuspension.obd.commands.disco3;

import pt.lighthouselabs.obd.commands.protocol.ObdProtocolCommand;

import static ru.terra.discosuspension.obd.constants.CommandID.SCM;

public class SelectControlModuleCommand extends ObdProtocolCommand {
    public SelectControlModuleCommand(final String blockId) {
        super(SCM.getCmd() + blockId);
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
