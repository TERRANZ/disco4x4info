package ru.terra.discosuspension.obd.commands;

import pt.lighthouselabs.obd.commands.protocol.ObdProtocolCommand;

/**
 * Date: 11.02.15
 * Time: 13:14
 */
public class TimeoutObdCommand extends ObdProtocolCommand {

    /**
     * @param timeout value between 0 and 255 that multiplied by 4 results in the
     *                desired timeout in milliseconds (ms).
     */
    public TimeoutObdCommand(int timeout) {
        super("ATST" + Integer.toHexString(0xFF & timeout));
    }

    /**
     * @param other
     */
    public TimeoutObdCommand(TimeoutObdCommand other) {
        super(other);
    }

    @Override
    public String getFormattedResult() {
        return getResult();
    }

    @Override
    public String getName() {
        return "Timeout";
    }

}