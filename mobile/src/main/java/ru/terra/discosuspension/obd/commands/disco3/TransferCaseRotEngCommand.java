package ru.terra.discosuspension.obd.commands.disco3;

import pt.lighthouselabs.obd.commands.protocol.ObdProtocolCommand;
import ru.terra.discosuspension.obd.HexUtil;

import static ru.terra.discosuspension.obd.constants.CommandID.TC_ROT_ENG;

public class TransferCaseRotEngCommand extends ObdProtocolCommand {
    public TransferCaseRotEngCommand() {
        super(TC_ROT_ENG.getCmd());
    }

    private double res;

    @Override
    public String getFormattedResult() {
        res = ((HexUtil.extractDigitA(getResult(), TC_ROT_ENG) * 2039) / 127) + (HexUtil.extractDigitB(getResult(), TC_ROT_ENG) * 20 / 127);
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
