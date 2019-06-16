package ru.terra.discosuspension.obd.io;

import pt.lighthouselabs.obd.commands.ObdCommand;

public class ObdCommandJob {

    private Long _id;
    private ObdCommand _command;

    public ObdCommandJob(ObdCommand command) {
        _command = command;
    }

    public Long getId() {
        return _id;
    }

    public void setId(Long id) {
        _id = id;
    }

    public ObdCommand getCommand() {
        return _command;
    }

}