package ru.terra.discosuspension.obd.io;

import pt.lighthouselabs.obd.commands.ObdCommand;

public class ObdCommandJob {

    private ObdCommand _command;

    public ObdCommandJob(ObdCommand command) {
        _command = command;
    }

    public ObdCommand getCommand() {
        return _command;
    }

}