package com.yalcinkaya.util;

public class MathUtil {

    public static String getReadableSeconds(int totalSecs) {
        int hours = totalSecs / 3600;
        int minutes = (totalSecs % 3600) / 60;
        int seconds = totalSecs % 60;
        if (hours == 0) {
            return String.format("%02d:%02d", minutes, seconds);
        }
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public static double roundDouble(double a) {
        return Math.round(a * 100) / 100;
    }

}
