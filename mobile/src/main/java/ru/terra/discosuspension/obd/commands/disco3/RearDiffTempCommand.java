package ru.terra.discosuspension.obd.commands.disco3;

import pt.lighthouselabs.obd.commands.protocol.ObdProtocolCommand;

import static ru.terra.discosuspension.obd.HexUtil.extractDigitA;
import static ru.terra.discosuspension.obd.constants.CommandID.RD_TEMP;

public class RearDiffTempCommand extends ObdProtocolCommand {
    private static final int TEMP_CONST = 40;

    public RearDiffTempCommand() {
        super(RD_TEMP.getCmd());
    }

    @Override
    public String getFormattedResult() {
        //example data : 18 DA F1 1A 04 62 19 6D 31
        //need to extract last digits: 31
        //convert hex to dec: 31 => 49
        //49-40 = 9 degrees
        final Integer extractedValue = extractDigitA(getResult(), RD_TEMP);
        return String.valueOf(extractedValue - TEMP_CONST);
    }

    @Override
    public String getName() {
        return "Get Read Diff Temperature";
    }


}
