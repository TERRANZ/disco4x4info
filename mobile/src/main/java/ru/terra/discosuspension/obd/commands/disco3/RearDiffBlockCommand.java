package ru.terra.discosuspension.obd.commands.disco3;

import pt.lighthouselabs.obd.commands.protocol.ObdProtocolCommand;

public class RearDiffBlockCommand extends ObdProtocolCommand {
    public RearDiffBlockCommand() {
        super("22D120");
    }

    @Override
    public String getFormattedResult() {
        return getResult();
    }

    @Override
    public String getName() {
        return "Get Read Diff Block Status";
    }
}
