package com.libremobileos.freeform.server;

import android.app.IActivityManager;
import android.app.IActivityTaskManager;
import android.content.Context;
import android.hardware.input.IInputManager;
import android.os.ServiceManager;
import android.util.Slog;
import android.view.IWindowManager;

public class SystemServiceHolder {

    private static final String TAG = "LMOFreeform/SystemServiceHolder";

    static IInputManager inputManagerService;
    public static IActivityManager activityManager;
    public static IActivityTaskManager activityTaskManager;
    public static IWindowManager windowManager;

    static void init() {
        activityTaskManager = IActivityTaskManager.Stub.asInterface(ServiceManager.getService(Context.ACTIVITY_TASK_SERVICE));
        activityManager = IActivityManager.Stub.asInterface(ServiceManager.getService(Context.ACTIVITY_SERVICE));
        inputManagerService = IInputManager.Stub.asInterface(ServiceManager.getService(Context.INPUT_SERVICE));
        windowManager = IWindowManager.Stub.asInterface(ServiceManager.getService(Context.WINDOW_SERVICE));
    }
}
