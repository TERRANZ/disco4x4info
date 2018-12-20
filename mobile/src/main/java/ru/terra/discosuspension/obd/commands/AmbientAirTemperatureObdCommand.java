package ru.terra.discosuspension.obd.commands;

import pt.lighthouselabs.obd.commands.temperature.TemperatureObdCommand;
import pt.lighthouselabs.obd.enums.AvailableCommandNames;

/**
 * Date: 11.02.15
 * Time: 13:15
 */
public class AmbientAirTemperatureObdCommand extends TemperatureObdCommand {

    /**
     * @param cmd
     */
    public AmbientAirTemperatureObdCommand() {
        super("0146");
    }

    /**
     * @param other
     */
    public AmbientAirTemperatureObdCommand(TemperatureObdCommand other) {
        super(other);
    }

    @Override
    public String getName() {
        return AvailableCommandNames.AMBIENT_AIR_TEMP.getValue();
    }

}
