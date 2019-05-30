package ru.terra.discosuspension.activity.components;

import java.io.Serializable;

public class ObdResult implements Serializable {
    public String rdTemp, tcRotation, tcSolLen, tcSolPos, tcTemp, currentGear, gbTemp, driveShiftPos;
    public String suspFLText, suspRLText, suspFRText, suspRRText;
    public Integer suspFLVal, suspFRVal, suspRLVal, suspRRVal;
    public Integer wheelPos;
    public Boolean rdBlock, tcBlock;
}
