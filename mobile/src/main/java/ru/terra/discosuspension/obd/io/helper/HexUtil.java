package ru.terra.discosuspension.obd.io.helper;

public final class HexUtil {
    public static String getLastByte(final String data) {
        return data.substring(data.lastIndexOf(" "), data.length()).trim();
    }

    public static int extractDigitA(final String data) {
        //        <   hdr  >  <???> <cmd> <info>
        //        0  1  2  3  4  5  6  7  8A 9B C  D
        //example 18 DA F1 1A 04 62 19 6D 31 temp
        //example 18 DA F1 1A 07 62 D1 20 01 01 00 00 rear block
        //example 18 DA F1 2B 05 62 3B 03 00 EF suspension height
        return hex2Decimal(data.split(" ")[8]);
    }

    public static int extractDigitB(final String data) {
        return hex2Decimal(data.split(" ")[9]);
    }

    public static int hex2Decimal(String s) {
        String digits = "0123456789ABCDEF";
        s = s.toUpperCase();
        int val = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            int d = digits.indexOf(c);
            val = 16 * val + d;
        }
        return val;
    }
}
