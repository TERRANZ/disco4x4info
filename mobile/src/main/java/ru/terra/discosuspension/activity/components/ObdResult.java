package ru.terra.discosuspension.activity.components;

import java.io.Serializable;

public class ObdResult implements Serializable {
    public String rdTemp = "", tcRotation = "", tcSolLen = "", tcSolPos = "", tcTemp = "", currentGear = "", gbTemp = "", driveShiftPos = "";
    public String suspFLText = "", suspRLText = "", suspFRText = "", suspRRText = "";
    public Integer suspFLVal = 0, suspFRVal = 0, suspRLVal = 0, suspRRVal = 0;
    public Integer wheelPos = 0;
    public Boolean rdBlock = false, tcBlock = false;
}
