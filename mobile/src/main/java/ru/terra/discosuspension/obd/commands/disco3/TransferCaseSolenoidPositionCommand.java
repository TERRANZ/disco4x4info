package ru.terra.discosuspension.obd.commands.disco3;

import pt.lighthouselabs.obd.commands.protocol.ObdProtocolCommand;
import ru.terra.discosuspension.obd.io.helper.HexUtil;

public class TransferCaseSolenoidPositionCommand extends ObdProtocolCommand {
    public TransferCaseSolenoidPositionCommand() {
        super("22D123");
    }

    @Override
    public String getFormattedResult() {
        return String.valueOf((byte) HexUtil.extractDigitA(getResult()));
    }

    @Override
    public String getName() {
        return "Transfer Case Solenoid Position";
    }
}
