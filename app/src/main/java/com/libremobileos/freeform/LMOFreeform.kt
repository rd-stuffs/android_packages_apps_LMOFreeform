package com.libremobileos.freeform

import android.app.Application

class LMOFreeform: Application() {

    override fun onCreate() {
        super.onCreate()
        LMOFreeformServiceManager.init()
    }

    companion object {
        private const val TAG = "LMOFreeform"
        const val CONFIG = "config"
    }
}
