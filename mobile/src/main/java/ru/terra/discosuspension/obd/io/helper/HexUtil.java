package ru.terra.discosuspension.obd.io.helper;

public final class HexUtil {
    public static int extractDigitA(final String data) {
        //example 18 DA F1 1A 04 62 19 6D 31
        return hex2Decimal(data.substring(data.lastIndexOf(" "), data.length()).trim());
    }

    public static int extractDigitB(final String data) {
        //example 18 DA F1 1A 04 62 19 6D 31
        return hex2Decimal(data.substring(data.indexOf(" ", 6), data.lastIndexOf(" ")).trim());
    }

    public static int hex2Decimal(String s) {
        String digits = "0123456789ABCDEF";
        s = s.toUpperCase();
        int val = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            int d = digits.indexOf(c);
            val = 16*val + d;
        }
        return val;
    }
}
