package ru.terra.discosuspension.obd.commands.disco3;

import pt.lighthouselabs.obd.commands.protocol.ObdProtocolCommand;
import ru.terra.discosuspension.obd.io.helper.HexUtil;

public class TransferCaseRotEngCommand extends ObdProtocolCommand {
    public TransferCaseRotEngCommand() {
        super("22D124");
    }

    private double res;

    @Override
    public String getFormattedResult() {
        res = ((HexUtil.extractDigitA(getResult()) * 2039) / 127) + (HexUtil.extractDigitB(getResult()) * 20 / 127);
        return String.format("%.1f", res);
    }

    @Override
    public String getName() {
        return "Transfer case rotate engine degrees";
    }

    public double getRes() {
        return res;
    }

    public void setRes(double res) {
        this.res = res;
    }
}
