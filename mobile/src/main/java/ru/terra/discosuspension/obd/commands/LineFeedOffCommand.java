package ru.terra.discosuspension.obd.commands;

import pt.lighthouselabs.obd.commands.protocol.ObdProtocolCommand;

/**
 * Date: 11.02.15
 * Time: 13:19
 */
public class LineFeedOffCommand extends ObdProtocolCommand {

    public LineFeedOffCommand() {
        super("ATL0");
    }

    /**
     * @param other
     */
    public LineFeedOffCommand(LineFeedOffCommand other) {
        super(other);
    }

    @Override
    public String getFormattedResult() {
        return getResult();
    }

    @Override
    public String getName() {
        return "Line Feed Off";
    }

}