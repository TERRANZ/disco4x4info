package ru.terra.discosuspension.obd.commands.disco3;

import pt.lighthouselabs.obd.commands.protocol.ObdProtocolCommand;
import ru.terra.discosuspension.obd.io.helper.HexUtil;

public class RearDiffBlockCommand extends ObdProtocolCommand {
    public RearDiffBlockCommand() {
        super("22D120");
    }

    @Override
    public String getFormattedResult() {
        final Integer result = HexUtil.extractDigitA(getResult());
        return result == 1 ? "ON" : "OFF";
    }

    @Override
    public String getName() {
        return "Get Read Diff Block Status";
    }
}
