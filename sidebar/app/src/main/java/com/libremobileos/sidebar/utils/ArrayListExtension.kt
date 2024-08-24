package com.libremobileos.sidebar.utils

import android.os.UserHandle
import com.libremobileos.sidebar.bean.AppInfo
import com.libremobileos.sidebar.room.SidebarAppsEntity
import com.libremobileos.sidebar.systemapi.UserHandleHidden

/**
 * @author KindBrave
 * @since 2023/8/29
 */
fun <T> List<T>.contains(element: T, predicate: (T, T) -> Boolean): Boolean {
    for (item in this) {
        if (predicate(item, element)) {
            return true
        }
    }
    return false
}

fun List<AppInfo>.getInfo(packageName: String, userHandle: UserHandle): AppInfo? {
    for (item in this) {
        if (
            item.packageName == packageName &&
            item.userId == UserHandleHidden.getUserId(userHandle)) return item
    }
    return null
}

/**
 * @author KindBrave
 * @since 2023/10/21
 * 判断侧边栏数据库列表中是否包含该包名、活动名、userId的内容
 */
fun List<SidebarAppsEntity>.contains(packageName: String, activityName: String, userId: Int): Boolean {
    for (item in this) {
        if (
            item.packageName == packageName &&
            item.activityName == activityName &&
            item.userId == userId
        ) return true
    }
    return false
}

fun List<SidebarAppsEntity>.contains(appInfo: AppInfo): Boolean {
    return this.contains(
        appInfo.packageName,
        appInfo.activityName,
        appInfo.userId
    )
}
