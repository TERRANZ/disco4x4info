package ru.terra.discosuspension.obd.commands.disco3;

import pt.lighthouselabs.obd.commands.protocol.ObdProtocolCommand;

import static ru.terra.discosuspension.obd.HexUtil.extractDigitA;
import static ru.terra.discosuspension.obd.constants.CommandID.CURR_GEAR;


public class CurrentGearCommand extends ObdProtocolCommand {
    public CurrentGearCommand() {
        super(CURR_GEAR.getCmd());
    }

    @Override
    public String getFormattedResult() {
        return String.valueOf(extractDigitA(getResult(), CURR_GEAR));
    }

    @Override
    public String getName() {
        return "Current gear";
    }
}
