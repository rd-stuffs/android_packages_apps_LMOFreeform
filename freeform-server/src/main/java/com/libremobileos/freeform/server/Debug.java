package com.libremobileos.freeform.server;

import android.util.Log;

public class Debug {
    private static final String MAIN_TAG = "LMOFreeform";

    public static void dlog(String tag, String msg) {
        if (Log.isLoggable(MAIN_TAG, Log.DEBUG)) {
            Log.d(tag, msg);
        }
    }
}
