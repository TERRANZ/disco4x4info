package ru.terra.discosuspension.obd.commands.disco3;

import pt.lighthouselabs.obd.commands.protocol.ObdProtocolCommand;
import ru.terra.discosuspension.obd.HexUtil;

import static ru.terra.discosuspension.obd.constants.CommandID.STEERING_WHEEL_POS;

public class SteeringWheelPositionCommand extends ObdProtocolCommand {
    public SteeringWheelPositionCommand() {
        super(STEERING_WHEEL_POS.getCmd());
    }

    @Override
    public String getFormattedResult() {
        return String.valueOf(calc());
    }

    public int calc() {
        int a = HexUtil.extractDigitA(getResult(), STEERING_WHEEL_POS);
        int b = HexUtil.extractDigitB(getResult(), STEERING_WHEEL_POS);
        return (int) ((a * 256 + b) * 0.1 - 780);
    }

    @Override
    public String getName() {
        return "Steering wheel position";
    }
}
