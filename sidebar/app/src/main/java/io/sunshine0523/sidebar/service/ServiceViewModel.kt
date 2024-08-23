package io.sunshine0523.sidebar.service

import android.app.Application
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.UserHandle
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.sunshine0523.sidebar.R
import io.sunshine0523.sidebar.app.SidebarApplication
import io.sunshine0523.sidebar.bean.AppInfo
import io.sunshine0523.sidebar.room.DatabaseRepository
import io.sunshine0523.sidebar.systemapi.UserHandleHidden
import io.sunshine0523.sidebar.utils.Logger
import io.sunshine0523.sidebar.utils.contains
import io.sunshine0523.sidebar.utils.getInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * @author KindBrave
 * @since 2023/8/25
 */
class ServiceViewModel(private val application: Application): AndroidViewModel(application) {
    private val logger = Logger("ServiceViewModel")

    private val repository = DatabaseRepository(application)
    private val sp = application.applicationContext.getSharedPreferences(SidebarApplication.CONFIG, Context.MODE_PRIVATE)

    val sidebarAppListFlow: StateFlow<List<AppInfo>>
        get() = _sidebarAppList.asStateFlow()
    private val _sidebarAppList = MutableStateFlow<List<AppInfo>>(emptyList())

    private val recentAppList = MutableStateFlow<List<AppInfo>>(emptyList())

    private val launcherApps: LauncherApps = application.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val usageStatsManager = application.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    private val lastTimeUsedComparator = LastTimeUsedComparator()

    val allAppActivity = AppInfo(
        "",
        AppCompatResources.getDrawable(application.applicationContext, R.drawable.ic_all)!!,
        ALL_APP_PACKAGE,
        ALL_APP_ACTIVITY,
        0
    )

    private val launcherAppsCallback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) {
            logger.d("onPackageRemoved: $packageName")
            _sidebarAppList.value.getInfo(packageName, user)?.let {
                repository.deleteSidebarApp(it.packageName, it.activityName, it.userId)
            }
        }

        override fun onPackageAdded(packageName: String, user: UserHandle) {

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

    companion object {
        private const val ALL_APP_PACKAGE = "io.sunshine0523.sidebar"
        private const val ALL_APP_ACTIVITY = "io.sunshine0523.sidebar.ui.all_app.AllAppActivity"
        private const val MAX_RECENT_APPS = 5
    }

    init {
        logger.d("init")
        initSidebarAppList()
        launcherApps.registerCallback(launcherAppsCallback)
    }

    override fun onCleared() {
        logger.d("onCleared")
        launcherApps.unregisterCallback(launcherAppsCallback)
    }

    private fun initSidebarAppList() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getAllSidebarAppsByFlow()
                .combine(recentAppList) { sidebarApps, recentApps ->
                    mutableListOf<AppInfo>().apply {
                        logger.d("sidebarApps=$sidebarApps recentApps=$recentApps")
                        sidebarApps?.forEach { entity ->
                            runCatching {
                                val info = application.packageManager.getApplicationInfo(entity.packageName, PackageManager.GET_ACTIVITIES)
                                if (!info.enabled) {
                                    throw Exception("package is disabled.")
                                }
                                add(
                                    AppInfo(
                                        "${info.loadLabel(application.packageManager)}",
                                        info.loadIcon(application.packageManager),
                                        entity.packageName,
                                        entity.activityName,
                                        entity.userId
                                    )
                                )
                            }.onFailure { e ->
                                logger.e("initSidebarAppList: failed to add $entity: ", e)
                                repository.deleteSidebarApp(entity.packageName, entity.activityName, entity.userId)
                            }
                        }
                        addAll(
                            recentApps
                                .filter { sidebarApps?.contains(it)?.not() ?: true }
                                .take(MAX_RECENT_APPS)
                        )
                    }
                }
                .collect { sidebarAppList ->
                    logger.d("initSidebarAppList: combinedList=$sidebarAppList")
                    _sidebarAppList.value = sidebarAppList.toList()
                }
        }
    }

    private suspend fun updateRecentAppListFlow() {
        val currentTime = System.currentTimeMillis()
        val startTime = currentTime - 1000 * 60 * 60
        val usageStatsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            currentTime
        )
        logger.d("updateRecentAppListFlow: size=${usageStatsList.size}")

        val recentList = ArrayList<AppInfo>()
        usageStatsList
            .sortedWith(lastTimeUsedComparator)
            .forEach { usageStats ->
                runCatching {
                    val info = application.packageManager.getApplicationInfo(usageStats.packageName, PackageManager.GET_ACTIVITIES)
                    val launchIntent = application.packageManager.getLaunchIntentForPackage(usageStats.packageName)
                    if (launchIntent != null && launchIntent.component != null) {
                        val appInfo = AppInfo(
                            "${info.loadLabel(application.packageManager)}",
                            info.loadIcon(application.packageManager),
                            info.packageName,
                            launchIntent.component!!.className,
                            0
                        )
                        recentList.add(appInfo)
                    }
                }.onFailure {
                    logger.e("updateRecentAppListFlow failed: $it")
                }
        }
        recentAppList.value = recentList.toList()
    }

    fun refreshRecentAppList() {
        viewModelScope.launch(Dispatchers.IO) {
            updateRecentAppListFlow()
        }
    }

    fun getIntSp(name: String, defaultValue: Int): Int {
        return sp.getInt(name, defaultValue)
    }

    fun getBooleanSp(name: String, defaultValue: Boolean): Boolean {
        return sp.getBoolean(name, defaultValue)
    }

    fun setIntSp(name: String, value: Int) {
        sp.edit().apply {
            putInt(name, value)
            apply()
        }
    }

    fun registerSpChangeListener(listener: OnSharedPreferenceChangeListener) {
        sp.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterSpChangeListener(listener: OnSharedPreferenceChangeListener) {
        sp.unregisterOnSharedPreferenceChangeListener(listener)
    }

    private inner class LastTimeUsedComparator : Comparator<UsageStats> {
        override fun compare(a: UsageStats, b: UsageStats): Int {
            return (b.lastTimeUsed - a.lastTimeUsed).toInt()
        }
    }
}

