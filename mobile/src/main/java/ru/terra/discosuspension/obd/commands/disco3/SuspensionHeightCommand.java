package ru.terra.discosuspension.obd.commands.disco3;

import android.util.Log;

import pt.lighthouselabs.obd.commands.protocol.ObdProtocolCommand;

import static ru.terra.discosuspension.obd.HexUtil.extractDigitA;
import static ru.terra.discosuspension.obd.HexUtil.extractDigitB;
import static ru.terra.discosuspension.obd.constants.CommandID.SUSP_HEIGHT;

public class SuspensionHeightCommand extends ObdProtocolCommand {
    public static final String FRONT_LEFT = "3";
    public static final String FRONT_RIGHT = "4";
    public static final String REAR_LEFT = "5";
    public static final String REAR_RIGHT = "6";
    private String wheel;

    public SuspensionHeightCommand(final String wheel) {
        super(SUSP_HEIGHT.getCmd() + wheel);
        this.wheel = wheel;
    }

    @Override
    public String getFormattedResult() {
        try {
            return String.format("%.1f", calc());
        } catch (Exception e) {
            Log.e(this.getClass().getCanonicalName(), "Unable to calculate: ", e);
            return getResult();
        }
    }

    public double calc() {
        final int A = extractDigitA(getResult(), SUSP_HEIGHT);
        final int B = extractDigitB(getResult(), SUSP_HEIGHT);
        return (double) (A * 256 + B) / 10 - 20;
    }

    @Override
    public String getName() {
        return "Suspension height command";
    }

    public String getWheel() {
        return wheel;
    }

    public void setWheel(String wheel) {
        this.wheel = wheel;
    }
}
