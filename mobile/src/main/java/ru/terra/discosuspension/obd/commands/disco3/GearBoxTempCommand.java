package ru.terra.discosuspension.obd.commands.disco3;

import pt.lighthouselabs.obd.commands.protocol.ObdProtocolCommand;
import ru.terra.discosuspension.obd.io.helper.HexUtil;

import static ru.terra.discosuspension.obd.constants.CommandID.GB_TEMP;

public class GearBoxTempCommand extends ObdProtocolCommand {
    public GearBoxTempCommand() {
        super(GB_TEMP.getCmd());
    }

    @Override
    public String getFormattedResult() {
        return String.valueOf(HexUtil.extractDigitA(getResult(), GB_TEMP) - 40);
    }

    @Override
    public String getName() {
        return "Gearbox temperature";
    }
}
