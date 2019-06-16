package ru.terra.discosuspension.obd.constants;

public enum ControlModuleID {
    REAR_DIFF_CONTROL_MODULE("DA1AF1"),
    SUSPENSION_CONTROL_MODULE("DA2BF1"),
    TRANSFER_CASE_CONTROL_MODULE("DA19F1"),
    GEARBOX_CONTROL_MODULE("DA18F1"),
    STEERING_WHEEL_CONTROL_MODULE("DA32F1");

    private String cmId;

    ControlModuleID(final String cmId) {
        this.cmId = cmId;
    }

    public String getCmId() {
        return cmId;
    }
}
