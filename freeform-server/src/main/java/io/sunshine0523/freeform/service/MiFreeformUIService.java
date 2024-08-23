package io.sunshine0523.freeform.service;

import static android.content.Context.CONTEXT_IGNORE_SECURITY;
import static android.content.Context.CONTEXT_INCLUDE_CODE;

import android.app.PendingIntent;
import android.content.Context;
import android.hardware.display.DisplayManagerInternal;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.ArrayMap;
import android.util.Slog;
import android.view.Surface;

import java.util.Map;

import io.sunshine0523.freeform.IMiFreeformDisplayCallback;
import io.sunshine0523.freeform.IMiFreeformUIService;
import io.sunshine0523.freeform.ui.freeform.FreeformWindowManager;

public class MiFreeformUIService extends IMiFreeformUIService.Stub {

    private static final String TAG = "Mi-Freeform/MiFreeformUIService";

    private Context systemContext = null;
    private DisplayManagerInternal displayManager = null;
    private MiFreeformService miFreeformService = null;
    // private Handler uiHandler = null;
    private Handler handler = new Handler();

    public MiFreeformUIService(Context context, DisplayManagerInternal displayManager, MiFreeformService miFreeformService) {
        if (null == context || null == displayManager || null == miFreeformService) return;

        this.systemContext = context;
        this.displayManager = displayManager;
        this.miFreeformService = miFreeformService;
        // this.uiHandler = displayManager.getUiHandler();
        // this.handler = displayManager.getHandler();

        SystemServiceHolder.init(() -> {
            try {
                ServiceManager.addService("mi_freeform", this);
                Map<String, IBinder> cache = new ArrayMap<>();
                cache.put("mi_freeform", this);
                ServiceManager.initServiceCache(cache);
                Slog.i(TAG, "add mi_freeform SystemService: " + ServiceManager.getService("mi_freeform"));
            } catch (Exception e) {
                Slog.e(TAG, "add mi_freeform service failed, " + e);
            }
            if (ServiceManager.getService("mi_freeform") == null) return;
        });
    }

    @Override
    public void startAppInFreeform(
            String packageName, String activityName, int userId, PendingIntent pendingIntent,
            int width, int height, int densityDpi, float refreshRate,
            boolean secure, boolean ownContentOnly, boolean shouldShowSystemDecorations,
            String resPkg, String layoutName) {
        Slog.i(TAG, "startAppInFreeform");
        FreeformWindowManager.addWindow(
                handler, systemContext,
                packageName, activityName, userId, pendingIntent,
                width, height, densityDpi, refreshRate,
                secure, ownContentOnly, shouldShowSystemDecorations,
                resPkg, layoutName);
    }

    @Override
    public void removeFreeform(String freeformId) {
        FreeformWindowManager.removeWindow(freeformId);
    }

    @Override
    public void createFreeformInUser(
            String name, int width, int height, int densityDpi, float refreshRate,
            boolean secure, boolean ownContentOnly, boolean shouldShowSystemDecorations,
            Surface surface, IMiFreeformDisplayCallback callback
    ) {
        displayManager.createFreeformLocked(
                name, callback,
                width, height, densityDpi,
                secure, ownContentOnly, shouldShowSystemDecorations,
                surface, refreshRate, 1666666L
        );
    }

    @Override
    public void resizeFreeform(IBinder appToken, int width, int height, int densityDpi) {
        displayManager.resizeFreeform(appToken, width, height, densityDpi);
    }

    @Override
    public void releaseFreeform(IBinder appToken) {
        displayManager.releaseFreeform(appToken);
    }

    @Override
    public boolean ping() {
        // need inputManager is not null
        return miFreeformService.isRunning();
    }
}
