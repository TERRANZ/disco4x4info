package ru.terra.discosuspension.obd;

import ru.terra.discosuspension.obd.constants.CommandID;

public final class HexUtil {

    private static final String DELIMITER = " ";

    public static int extractDigitA(final String data, final CommandID cmd) {
        //        <   hdr  >  <???> <cmd> <info>
        //        0  1  2  3  4  5  6  7  8A 9B C  D
        //example 18 DA F1 1A 04 62 19 6D 31 temp
        //example 18 DA F1 1A 07 62 D1 20 01 01 00 00 rear block
        //example 18 DA F1 2B 05 62 3B 03 00 EF suspension height

        //example 18 DA F1 18 04 62 1F D1 08 - P
        //example 18 DA F1 18 04 62 1F D1 07 - R
        //example 18 DA F1 18 04 62 1F D1 00 - N
        //example 18 DA F1 18 04 62 1F D1 09 - D
        //example 18 DA F1 18 04 62 1F D1 0A - M
        try {
            return hex2Decimal(data.split(DELIMITER)[8]);
        } catch (Exception e) {
//            ACRA.getErrorReporter().handleException(new ParseException("Received: " + data + " but expected " + cmd.name(), 8));
        }
        return 0;
    }

    public static int extractDigitB(final String data, final CommandID cmd) {
        try {
            return hex2Decimal(data.split(" ")[9]);
        } catch (Exception e) {
//            ACRA.getErrorReporter().handleException(new ParseException("Received: " + data + " but expected " + cmd.name(), 9));
        }
        return 0;
    }

    private static int hex2Decimal(final String s) {
        final String digits = "0123456789ABCDEF";
        final String tmp = s.toUpperCase();
        int val = 0;
        for (int i = 0; i < tmp.length(); i++) {
            char c = tmp.charAt(i);
            int d = digits.indexOf(c);
            val = 16 * val + d;
        }
        return val;
    }
}
