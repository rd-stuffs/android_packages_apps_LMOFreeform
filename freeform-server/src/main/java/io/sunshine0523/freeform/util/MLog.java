package io.sunshine0523.freeform.util;

import android.util.Slog;

import java.io.PrintWriter;
import java.io.StringWriter;

public class MLog {
    public static void i(String tag, String message) {
        Slog.i(tag, message);
        // DataHelper.INSTANCE.appendLog("[i] " + tag + " " + message);
    }

    public static void w(String tag, String message) {
        Slog.w(tag, message);
        // DataHelper.INSTANCE.appendLog("[w] " + tag + " " + message);
    }

    public static void e(String tag, String message) {
        Slog.e(tag, message);
        // DataHelper.INSTANCE.appendLog("[e] " + tag + " " + message);
    }

    public static void e(String tag, String message, Exception e) {
        // StringWriter writer = new StringWriter();
        // PrintWriter printWriter = new PrintWriter(writer);
        // e.printStackTrace(printWriter);
        // Throwable cause = e.getCause();
        // while (cause != null) {
        //     cause.printStackTrace(printWriter);
        //     cause = cause.getCause();
        // }
        // printWriter.close();
        // String result = writer.toString();

        Slog.e(tag, message, e);
        // DataHelper.INSTANCE.appendLog("[e] " + tag + " " + message + " " + result);
    }
}
