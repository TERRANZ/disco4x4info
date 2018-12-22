package ru.terra.discosuspension.obd.commands.disco3;

import android.util.Log;

import pt.lighthouselabs.obd.commands.protocol.ObdProtocolCommand;

import static ru.terra.discosuspension.obd.io.helper.HexUtil.extractDigitA;
import static ru.terra.discosuspension.obd.io.helper.HexUtil.extractDigitB;

public class SuspensionHeightCommand extends ObdProtocolCommand {
    public static final String FRONT_LEFT = "3";
    public static final String FRONT_RIGHT = "4";
    public static final String REAR_LEFT = "5";
    public static final String REAR_RIGHT = "6";
    private String wheel;

    public SuspensionHeightCommand(final String wheel) {
        super("223B0" + wheel);
        this.wheel = wheel;
    }

    @Override
    public String getFormattedResult() {
        try {
            final int A = extractDigitA(getResult());
            final int B = extractDigitB(getResult());
            double res = (double) (A * 256 + B) / 10 - 20;
            return String.format("%.1f", res);
        } catch (Exception e) {
            Log.e(this.getClass().getCanonicalName(), "Unable to calculate: ", e);
            return getResult();
        }
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
