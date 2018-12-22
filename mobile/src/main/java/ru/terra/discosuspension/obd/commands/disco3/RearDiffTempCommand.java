package ru.terra.discosuspension.obd.commands.disco3;

import pt.lighthouselabs.obd.commands.protocol.ObdProtocolCommand;

import static ru.terra.discosuspension.obd.io.helper.HexUtil.extractDigitA;

public class RearDiffTempCommand extends ObdProtocolCommand {
    private static final int TEMP_CONST = 40;

    public RearDiffTempCommand() {
        super("22196D");
    }

    @Override
    public String getFormattedResult() {
        //example data : 18 DA F1 1A 04 62 19 6D 31
        //need to extract last digits: 31
        //convert hex to dec: 31 => 49
        //49-40 = 9 degrees
        final Integer extractedValue = extractDigitA(getResult());
        return String.valueOf(extractedValue - TEMP_CONST);
    }

    @Override
    public String getName() {
        return "Get Read Diff Temperature";
    }


}
