package ru.terra.discosuspension.obd.commands.disco3;

import android.util.SparseArray;

import pt.lighthouselabs.obd.commands.protocol.ObdProtocolCommand;
import ru.terra.discosuspension.obd.HexUtil;

import static ru.terra.discosuspension.obd.constants.CommandID.DRIVE_SHIFT_POS;

public class DriveShiftPositionCommand extends ObdProtocolCommand {
    //18 DA F1 18 04 62 1F D1 08 - P
    //18 DA F1 18 04 62 1F D1 07 - R
    //18 DA F1 18 04 62 1F D1 00 - N
    //18 DA F1 18 04 62 1F D1 09 - D
    //18 DA F1 18 04 62 1F D1 0A - M

    private SparseArray<String> positions = new SparseArray<>();

    public DriveShiftPositionCommand() {
        super(DRIVE_SHIFT_POS.getCmd());
        positions.put(8, "P");
        positions.put(7, "R");
        positions.put(0, "N");
        positions.put(9, "D");
        positions.put(10, "M");
    }

    @Override
    public String getFormattedResult() {
        final String res = positions.get(HexUtil.extractDigitA(getResult(), DRIVE_SHIFT_POS));
        return res != null ? res : "F";
    }

    @Override
    public String getName() {
        return "Drive shift position";
    }
}
