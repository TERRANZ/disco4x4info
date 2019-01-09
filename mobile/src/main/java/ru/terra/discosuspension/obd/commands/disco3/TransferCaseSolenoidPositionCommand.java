package ru.terra.discosuspension.obd.commands.disco3;

import pt.lighthouselabs.obd.commands.protocol.ObdProtocolCommand;
import ru.terra.discosuspension.obd.HexUtil;

import static ru.terra.discosuspension.obd.constants.CommandID.TC_SOL_POS;

public class TransferCaseSolenoidPositionCommand extends ObdProtocolCommand {
    public TransferCaseSolenoidPositionCommand() {
        super(TC_SOL_POS.getCmd());
    }

    @Override
    public String getFormattedResult() {
        return String.valueOf((byte) HexUtil.extractDigitA(getResult(), TC_SOL_POS));
    }

    public boolean isHi() {
        return (byte) HexUtil.extractDigitA(getResult(), TC_SOL_POS) > 0;
    }

    @Override
    public String getName() {
        return "Transfer Case Solenoid Position";
    }
}
