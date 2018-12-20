package ru.terra.discosuspension.obd.commands.disco3;

import pt.lighthouselabs.obd.commands.protocol.ObdProtocolCommand;

public class RearDiffTempCommand extends ObdProtocolCommand {
    public RearDiffTempCommand() {
        super("22196D");
    }

    @Override
    public String getFormattedResult() {
        return getResult();
    }

    @Override
    public String getName() {
        return "Get Read Diff Temperature";
    }
}
