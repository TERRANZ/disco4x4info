package ru.terra.discosuspension.obd.constants;

public enum CommandID {
    CURR_GEAR("221FD0"),
    GB_TEMP("221FE8"),
    RD_BLOCK("22D120"),
    RD_TEMP("22196D"),
    SCM("ATSH"),
    SUSP_HEIGHT("223B0"),
    TC_ROT_ENG("22D124"),
    TC_SOL_POS("22D123"),
    TC_TEMP("22D11C"),
    DRIVE_SHIFT_POS("221FD1"),
    STEERING_WHEEL_POS("223302");
    private String cmd;

    CommandID(final String cmd) {
        this.cmd = cmd;
    }

    public String getCmd() {
        return cmd;
    }
}
