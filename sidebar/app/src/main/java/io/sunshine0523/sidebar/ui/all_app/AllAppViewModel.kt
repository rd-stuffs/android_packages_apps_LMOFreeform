package io.sunshine0523.sidebar.ui.all_app

import android.app.Application
import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.UserHandle
import android.os.UserManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.sunshine0523.sidebar.bean.AppInfo
import io.sunshine0523.sidebar.systemapi.UserHandleHidden
import io.sunshine0523.sidebar.utils.Logger
import io.sunshine0523.sidebar.utils.getInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.Collator
import java.util.Collections
import java.util.Locale

/**
 * @author KindBrave
 * @since 2023/10/25
 */
class AllAppViewModel(private val application: Application): AndroidViewModel(application) {
    private val logger = Logger("AllAppViewModel")
    private val allAppList = ArrayList<AppInfo>()
    val appListFlow: StateFlow<List<AppInfo>>
        get() = _appList.asStateFlow()
    private val _appList = MutableStateFlow<List<AppInfo>>(emptyList())
    private val appComparator = AppComparator()

    private val userManager = application.getSystemService(Context.USER_SERVICE) as UserManager
    private val launcherApps = application.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

    private val launcherAppsCallback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) {
            logger.d("onPackageRemoved: $packageName")
            viewModelScope.launch(Dispatchers.IO) {
                allAppList.remove(allAppList.getInfo(packageName, user))
                _appList.value = allAppList.toList()
            }
        }

        override fun onPackageAdded(packageName: String, user: UserHandle) {
            logger.d("onPackageAdded: $packageName")
            viewModelScope.launch(Dispatchers.IO) {
                allAppList.remove(allAppList.getInfo(packageName, user))
            }

            runCatching {
                val info = application.packageManager.getApplicationInfo(packageName, PackageManager.GET_ACTIVITIES)
                val launchIntent = application.packageManager.getLaunchIntentForPackage(packageName)
                val userId = UserHandleHidden.getUserId(user)
                if (launchIntent != null && launchIntent.component != null) {
                    viewModelScope.launch(Dispatchers.IO) {
                        allAppList.add(
                            AppInfo(
                                "${info.loadLabel(application.packageManager)}${if (userId != 0) -userId else ""}",
                                info.loadIcon(application.packageManager),
                                info.packageName,
                                launchIntent.component!!.className,
                                userId
                            )
                        )
                        Collections.sort(allAppList, appComparator)
                        _appList.value = allAppList.toList()
                    }
                }
            }
        }

        override fun onPackageChanged(packageName: String, user: UserHandle) {
            logger.d("onPackageChanged: $packageName")
            val appInfo = application.packageManager.getApplicationInfo(packageName, 0)
            if (!appInfo.enabled) {
                onPackageRemoved(packageName, user)
            }
        }

        override fun onPackagesAvailable(
            packageNames: Array<out String>?,
            user: UserHandle?,
            replacing: Boolean
        ) {

        }

        override fun onPackagesUnavailable(
            packageNames: Array<out String>?,
            user: UserHandle?,
            replacing: Boolean
        ) {

        }
    }

    init {
        logger.d("init")
        initAllAppList()
        launcherApps.registerCallback(launcherAppsCallback)
    }

    override fun onCleared() {
        logger.d("onCleared")
        launcherApps.unregisterCallback(launcherAppsCallback)
    }

    private fun initAllAppList() {
        val userHandleMap = HashMap<Int, UserHandle>()
        userManager.userProfiles.forEach {
            userHandleMap[UserHandleHidden.getUserId(it)] = it
        }
        viewModelScope.launch(Dispatchers.IO) {
            userManager.userProfiles.forEach { userHandle ->
                val list = launcherApps.getActivityList(null, userHandle)
                list.forEach {info ->
                    val userId = UserHandleHidden.getUserId(userHandle)
                    allAppList.add(
                        AppInfo(
                            "${info.label}${if (userId != 0) -userId else ""}",
                            info.applicationInfo.loadIcon(application.packageManager),
                            info.componentName.packageName,
                            info.componentName.className,
                            userId
                        )
                    )
                }
            }
            Collections.sort(allAppList, appComparator)
            _appList.value = allAppList.toList()
        }
    }

    private inner class AppComparator : Comparator<AppInfo> {
        override fun compare(p0: AppInfo, p1: AppInfo): Int {
            return Collator.getInstance().compare(p0.label, p1.label)
        }
    }
}
