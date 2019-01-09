package ru.terra.discosuspension.obd.commands.disco3;

import pt.lighthouselabs.obd.commands.protocol.ObdProtocolCommand;
import ru.terra.discosuspension.obd.HexUtil;

import static ru.terra.discosuspension.obd.constants.CommandID.TC_TEMP;

public class TransferCaseTempCommand extends ObdProtocolCommand {
    public TransferCaseTempCommand() {
        super(TC_TEMP.getCmd());
    }

    @Override
    public String getFormattedResult() {
        return String.valueOf(HexUtil.extractDigitA(getResult(), TC_TEMP) - 40);
    }

    @Override
    public String getName() {
        return "Transfer case temperature";
    }
}
