package ru.terra.discosuspension.obd.commands.disco3;

import pt.lighthouselabs.obd.commands.protocol.ObdProtocolCommand;
import ru.terra.discosuspension.obd.io.helper.HexUtil;

public class GearBoxTempCommand extends ObdProtocolCommand {
    public GearBoxTempCommand() {
        super("221FE8");
    }

    @Override
    public String getFormattedResult() {
        return String.valueOf(HexUtil.extractDigitA(getResult()) - 40);
    }

    @Override
    public String getName() {
        return "Gearbox temperature";
    }
}
