package ru.terra.discosuspension.activity.components;

import java.io.Serializable;
import java.util.Date;

public class ObdState implements Serializable, Cloneable {
    public String rdTemp = "", tcRotation = "", tcSolLen = "", tcSolPos = "", tcTemp = "", currentGear = "", gbTemp = "", driveShiftPos = "";
    public String suspFLText = "", suspRLText = "", suspFRText = "", suspRRText = "";
    public Integer suspFLVal = 0, suspFRVal = 0, suspRLVal = 0, suspRRVal = 0;
    public Integer wheelPos = 0;
    public Boolean rdBlock = false, tcBlock = false;
    public Long timestamp;

    public ObdState() {
        timestamp = new Date().getTime();
    }

    @Override
    public ObdState clone() throws CloneNotSupportedException {
        return (ObdState) super.clone();
    }

    public String toCSV() {
        return rdTemp + ";"
                + tcRotation + ";"
                + tcSolLen + ";"
                + tcSolPos + ";"
                + tcTemp + ";"
                + currentGear + ";"
                + gbTemp + ";"
                + driveShiftPos + ";"
                + suspFLText + ";"
                + suspRLText + ";"
                + suspFRText + ";"
                + suspRRText + ";"
                + wheelPos + ";"
                + rdBlock + ";"
                + tcBlock + ";"
                + timestamp + ";";
    }

    @Override
    public String toString() {
        return "ObdResult{" +
                "rdTemp='" + rdTemp + '\'' +
                ", tcRotation='" + tcRotation + '\'' +
                ", tcSolLen='" + tcSolLen + '\'' +
                ", tcSolPos='" + tcSolPos + '\'' +
                ", tcTemp='" + tcTemp + '\'' +
                ", currentGear='" + currentGear + '\'' +
                ", gbTemp='" + gbTemp + '\'' +
                ", driveShiftPos='" + driveShiftPos + '\'' +
                ", suspFLText='" + suspFLText + '\'' +
                ", suspRLText='" + suspRLText + '\'' +
                ", suspFRText='" + suspFRText + '\'' +
                ", suspRRText='" + suspRRText + '\'' +
                ", suspFLVal=" + suspFLVal +
                ", suspFRVal=" + suspFRVal +
                ", suspRLVal=" + suspRLVal +
                ", suspRRVal=" + suspRRVal +
                ", wheelPos=" + wheelPos +
                ", rdBlock=" + rdBlock +
                ", tcBlock=" + tcBlock +
                '}';
    }
}
