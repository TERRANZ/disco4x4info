package ru.terra.discosuspension.obd.commands.disco3;

import pt.lighthouselabs.obd.commands.protocol.ObdProtocolCommand;
import ru.terra.discosuspension.obd.io.helper.HexUtil;

public class TransferCaseTempCommand extends ObdProtocolCommand {
    public TransferCaseTempCommand() {
        super("22D11C");
    }

    @Override
    public String getFormattedResult() {
        return String.valueOf(HexUtil.extractDigitA(getResult()) - 40);
    }

    @Override
    public String getName() {
        return "Transfer case temperature";
    }
}
