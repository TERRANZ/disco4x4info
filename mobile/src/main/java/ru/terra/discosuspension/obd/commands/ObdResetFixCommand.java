package ru.terra.discosuspension.obd.commands;

import pt.lighthouselabs.obd.commands.protocol.ObdProtocolCommand;

/**
 * Date: 06.01.15
 * Time: 18:10
 */
public class ObdResetFixCommand extends ObdProtocolCommand {

    public  ObdResetFixCommand() {
        super("ATZ");
    }

    /**
     * @param other
     */
    public ObdResetFixCommand(ObdResetFixCommand other) {
        super(other);
    }

    @Override
    public String getFormattedResult() {
        return getResult();
    }

    @Override
    public String getName() {
        return "Reset OBD";
    }

}