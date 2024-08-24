package com.libremobileos.freeform.server;

import android.app.IActivityManager;
import android.app.IActivityTaskManager;
import android.content.Context;
import android.os.Build;
import android.os.ServiceManager;
import android.util.Slog;
import android.view.IWindowManager;

import com.android.server.input.InputManagerService;

public class SystemServiceHolder {

    private static final String TAG = "LMOFreeform/SystemServiceHolder";

    static InputManagerService inputManagerService;
    public static IActivityManager activityManager;
    public static IActivityTaskManager activityTaskManager;
    public static IWindowManager windowManager;

    static void init(ServiceCallback callback) {
        new Thread(() -> {
            waitSystemService("activity_task");
            waitSystemService("activity");
            waitSystemService("input");
            waitSystemService("window");
            activityTaskManager = IActivityTaskManager.Stub.asInterface(ServiceManager.getService("activity_task"));
            activityManager = IActivityManager.Stub.asInterface(ServiceManager.getService("activity"));
            inputManagerService = (InputManagerService) ServiceManager.getService("input");
            windowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
            callback.allAdded();
        }).start();
    }

    public static void waitSystemService(String name) {
        int count = 20;
        try {
            while (count-- > 0 && null == ServiceManager.getService(name)) {
                Thread.sleep(1000);
                Slog.d(TAG, name + " not start, wait 1s");
            }
        } catch (Exception ignored) { }
    }

    interface ServiceCallback {
        void allAdded();
    }
}
