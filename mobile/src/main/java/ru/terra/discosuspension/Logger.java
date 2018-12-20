package ru.terra.discosuspension;

import android.content.Context;
import android.util.Log;

public class Logger {
    public static void i(Context context, String tag, String msg) {
        if (tag != null && msg != null) {
            Log.i(tag, msg);
        }
    }

    public static void w(Context context, String tag, String msg) {
        if (tag != null && msg != null) {
            Log.w(tag, msg);
        }
    }

    public static void w(Context context, String tag, String msg, Throwable t) {
        if (tag != null && msg != null) {
            Log.w(tag, msg, t);
        }
    }

    public static void d(Context context, String tag, String msg) {
        if (tag != null && msg != null) {
            Log.d(tag, msg);
        }
    }

    public static void e(Context context, String tag, String msg, Throwable t) {
        if (tag != null && msg != null) {
            Log.e(tag, msg, t);
        }
    }
}
