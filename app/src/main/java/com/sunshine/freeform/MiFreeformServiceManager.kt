package com.sunshine.freeform

import android.app.PendingIntent
import android.os.Build
import android.os.IBinder
import android.os.ServiceManager
import android.util.Log
import io.sunshine0523.freeform.IMiFreeformUIService
import java.util.Date

object MiFreeformServiceManager {
    private const val TAG = "MiFreeformServiceManager"
    private var iMiFreeformService: IMiFreeformUIService? = null

    fun init() {
        try {
            val r = ServiceManager.getService("mi_freeform")
            iMiFreeformService = IMiFreeformUIService.Stub.asInterface(r)
            iMiFreeformService?.ping()
        } catch (e: Exception) {
            Log.e(TAG, "$e")
            e.printStackTrace()
        }
    }

    fun ping(): Boolean {
        return try {
            iMiFreeformService!!.ping()
            true
        } catch (e: Exception) {
            Log.e(TAG, "$e")
            e.printStackTrace()
            false
        }
    }

    fun createWindow(packageName: String, activityName: String, userId: Int, width: Int, height: Int, densityDpi: Int) {
        iMiFreeformService?.startAppInFreeform(
            packageName,
            activityName,
            userId,
            null,
            width,
            height,
            densityDpi,
            120.0f,
            false,
            true,
            false,
            "com.sunshine.freeform",
            "view_freeform"
        )
    }

    fun createWindow(pendingIntent: PendingIntent?, width: Int, height: Int, densityDpi: Int) {
        iMiFreeformService?.startAppInFreeform(
            pendingIntent?.creatorPackage?:"pendingIntentCreatorPackage",
            "unknownActivity-${Date().time}",
            -100,
            pendingIntent,
            width,
            height,
            densityDpi,
            120.0f,
            false,
            true,
            false,
            "com.sunshine.freeform",
            "view_freeform"
        )
    }

    fun removeFreeform(freeformId: String) {
        iMiFreeformService?.removeFreeform(freeformId)
    }

    fun getLog(): String {
        return iMiFreeformService?.log ?: "Maybe Mi-Freeform can`t link mi_freeform service. You can get log at /data/system/mi_freeform/log.log"
    }

    fun clearLog() {
        iMiFreeformService?.clearLog()
    }
}
