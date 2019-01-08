package ru.terra.discosuspension.obd.commands.disco3;

import pt.lighthouselabs.obd.commands.protocol.ObdProtocolCommand;

import static ru.terra.discosuspension.obd.constants.CommandID.DRIVE_SHIFT_POS;

public class DriveShiftPositionCommand extends ObdProtocolCommand {
    public DriveShiftPositionCommand() {
        super(DRIVE_SHIFT_POS.getCmd());
    }

    @Override
    public String getFormattedResult() {
        return getResult();
    }

    @Override
    public String getName() {
        return "Drive shift position";
    }
}
