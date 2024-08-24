package com.libremobileos.freeform.server.ui

import android.app.PendingIntent
import android.content.ComponentName

data class AppConfig(
    val packageName: String,
    val activityName: String,
    val pendingIntent: PendingIntent?,
    val userId: Int
)
