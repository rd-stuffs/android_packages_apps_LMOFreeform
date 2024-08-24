package com.libremobileos.freeform.server.ui

import android.view.IRotationWatcher

/**
 * Default Display Rotation Listener
 */
class RotationWatcher(private val window: FreeformWindow): IRotationWatcher.Stub() {

    override fun onRotationChanged(rotation: Int) {
        window.defaultDisplayWidth = window.context.resources.displayMetrics.widthPixels
        window.defaultDisplayHeight = window.context.resources.displayMetrics.heightPixels
        window.handler.post {
            if (window.freeformConfig.isHangUp) window.toHangUp()
            else window.makeSureFreeformInScreen()
        }
    }
}
