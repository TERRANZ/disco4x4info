package ru.terra.discosuspension.obd.commands;

import pt.lighthouselabs.obd.commands.protocol.ObdProtocolCommand;

/**
 * Date: 11.02.15
 * Time: 13:13
 */
public class EchoOffObdCommand extends ObdProtocolCommand {

    public EchoOffObdCommand() {
        super("ATE0");
    }

    /**
     * @param other
     */
    public EchoOffObdCommand(EchoOffObdCommand other) {
        super(other);
    }

    @Override
    public String getFormattedResult() {
        return getResult();
    }

    @Override
    public String getName() {
        return "Echo Off";
    }

}