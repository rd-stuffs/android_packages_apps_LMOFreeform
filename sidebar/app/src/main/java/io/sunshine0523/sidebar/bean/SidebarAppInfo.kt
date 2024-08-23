package io.sunshine0523.sidebar.bean

import android.graphics.drawable.Drawable

data class SidebarAppInfo(
    val label: String,
    val icon: Drawable,
    val packageName: String,
    val activityName: String,
    val userId: Int,
    // 当前APP是否在侧边栏中展示
    var isSidebarApp: Boolean = false
)
