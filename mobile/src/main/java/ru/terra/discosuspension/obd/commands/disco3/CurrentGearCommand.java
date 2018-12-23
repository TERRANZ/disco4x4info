package ru.terra.discosuspension.obd.commands.disco3;

import pt.lighthouselabs.obd.commands.protocol.ObdProtocolCommand;
import ru.terra.discosuspension.obd.io.helper.HexUtil;

public class CurrentGearCommand extends ObdProtocolCommand {
    public CurrentGearCommand() {
        super("221FD0");
    }

    @Override
    public String getFormattedResult() {
        return String.valueOf(HexUtil.extractDigitA(getResult()));
    }

    @Override
    public String getName() {
        return "Current gear";
    }
}
