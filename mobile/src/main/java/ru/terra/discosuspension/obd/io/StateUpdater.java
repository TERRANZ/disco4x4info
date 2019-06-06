package ru.terra.discosuspension.obd.io;

import pt.lighthouselabs.obd.commands.ObdCommand;

public interface StateUpdater {
    void stateUpdate(final ObdCommand cmd);
}
