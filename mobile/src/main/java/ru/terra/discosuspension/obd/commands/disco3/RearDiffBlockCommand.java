package ru.terra.discosuspension.obd.commands.disco3;

import pt.lighthouselabs.obd.commands.protocol.ObdProtocolCommand;
import ru.terra.discosuspension.obd.io.helper.HexUtil;

import static ru.terra.discosuspension.obd.constants.CommandID.RD_BLOCK;

public class RearDiffBlockCommand extends ObdProtocolCommand {
    public RearDiffBlockCommand() {
        super(RD_BLOCK.getCmd());
    }

    @Override
    public String getFormattedResult() {
        final Integer result = HexUtil.extractDigitA(getResult(), RD_BLOCK);
        return result == 1 ? "ON" : "OFF";
    }

    @Override
    public String getName() {
        return "Get Read Diff Block Status";
    }
}
