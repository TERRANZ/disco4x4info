package ru.terra.discosuspension.obd.commands;

import pt.lighthouselabs.obd.commands.temperature.TemperatureObdCommand;

public class EngineRPMCommand extends TemperatureObdCommand {
    public EngineRPMCommand() {
        super("010C");
    }

    @Override
    public String getName() {
        return "engine rpm";
    }
}
