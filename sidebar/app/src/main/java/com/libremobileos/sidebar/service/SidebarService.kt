package com.libremobileos.sidebar.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.IBinder
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import com.android.internal.policy.SystemBarUtils
import com.libremobileos.sidebar.R
import com.libremobileos.sidebar.app.SidebarApplication
import com.libremobileos.sidebar.utils.Logger

class SidebarService : Service(), SharedPreferences.OnSharedPreferenceChangeListener,
    GestureListener.Callback {
    private val logger = Logger(TAG)
    private lateinit var viewModel: ServiceViewModel
    private lateinit var windowManager: WindowManager
    private lateinit var sidebarView: SidebarView
    private lateinit var sharedPrefs: SharedPreferences
    private var showSideline = false
    private var isShowingSidebar = false
    private var isShowingSideline = false
    private var sidelinePositionX = 0
    private var sidelinePositionY = 0
    private var screenWidth = 0
    private var screenHeight = 0
    private val layoutParams = LayoutParams()
    private val sideLineView by lazy {
        val gestureManager = MGestureManager(this@SidebarService, GestureListener(this@SidebarService))
        View(this).apply {
            background = AppCompatResources.getDrawable(this@SidebarService, R.drawable.ic_line)
            setOnTouchListener { _, event ->
                gestureManager.onTouchEvent(event)
                true
            }
        }
    }

    private val isPortrait: Boolean
        get() = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    private val offset: Int
        get() = if (isPortrait) OFFSET_PORTRAIT else OFFSET_LANDSCAPE

    companion object {
        private const val TAG = "SidebarService"
        private const val SIDELINE_WIDTH = 100
        //侧边条移动时的宽度
        private const val SIDELINE_MOVE_WIDTH = 200
        private const val SIDELINE_HEIGHT = 200
        //侧边条屏幕边缘偏移量
        private const val OFFSET_PORTRAIT = 20
        private const val OFFSET_LANDSCAPE = 0

        //是否展示侧边条
        const val SIDELINE = "sideline"
        const val SIDELINE_POSITION_X = "sideline_position_x"
        const val SIDELINE_POSITION_Y_PORTRAIT = "sideline_position_y_portrait"
        const val SIDELINE_POSITION_Y_LANDSCAPE = "sideline_position_y_landscape"
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logger.d("starting service")
        viewModel = ServiceViewModel(application)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        screenWidth = resources.displayMetrics.widthPixels
        screenHeight = resources.displayMetrics.heightPixels
        sharedPrefs = application.applicationContext.getSharedPreferences(SidebarApplication.CONFIG, Context.MODE_PRIVATE)
        sharedPrefs.registerOnSharedPreferenceChangeListener(this)
        sidebarView = SidebarView(this@SidebarService, viewModel, object : SidebarView.Callback {
            override fun onRemove() {
                logger.d("sidebar view removed")
                if (isShowingSidebar && showSideline) animateShowSideline()
                isShowingSidebar = false
            }
        })
        showSideline = sharedPrefs.getBoolean(SIDELINE, false)
        logger.d("screenWidth=$screenWidth screenHeight=$screenHeight showSideline=$showSideline")
        if (showSideline) showView()
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        screenWidth = resources.displayMetrics.widthPixels
        screenHeight = resources.displayMetrics.heightPixels
        logger.d("onConfigChanged: screenWidth=$screenWidth height=$screenHeight isShowingSideline=$isShowingSideline")
        if (isShowingSideline) {
            updateSidelinePosition()
            updateViewLayout()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(this)
        removeView(force = true)
        viewModel.destroy()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        when (key) {
            SIDELINE -> {
                showSideline = sharedPrefs.getBoolean(SIDELINE, false)
                if (showSideline) {
                    showView()
                } else {
                    removeView(force = true)
                }
            }
        }
    }

    override fun showSidebar() {
        logger.d("showSidebar")
        sidebarView.showView()
        isShowingSidebar = true
        animateHideSideline()
    }

    override fun beginMoveSideline() {
        logger.d("beginMoveSideline")
        layoutParams.apply {
            width = SIDELINE_MOVE_WIDTH
        }
        updateViewLayout()
    }

    /**
     * @param xChanged x轴变化
     * @param yChanged y轴变化
     * @param positionX 触摸的x轴绝对位置。用来判断是否需要变化侧边条展示位置
     * @param positionY 触摸的y轴绝对位置
     */
    override fun moveSideline(xChanged: Int, yChanged: Int, positionX: Int, positionY: Int) {
        logger.d("moveSideline xChanged=$xChanged yChanged=$yChanged x=$positionX y=$positionY")
        sidelinePositionX = if (positionX > screenWidth / 2) 1 else -1
        layoutParams.apply {
            x = sidelinePositionX * (screenWidth / 2 - offset)
            y = layoutParams.y + yChanged
        }
        updateViewLayout()
    }

    override fun endMoveSideline() {
        logger.d("endMoveSideline")
        layoutParams.apply {
            width = SIDELINE_WIDTH
            y = constrainY(y)
        }
        updateViewLayout()
        setIntSp(SIDELINE_POSITION_X, sidelinePositionX)
        if (isPortrait) {
            setIntSp(SIDELINE_POSITION_Y_PORTRAIT, layoutParams.y)
        } else {
            setIntSp(SIDELINE_POSITION_Y_LANDSCAPE, layoutParams.y)
        }
    }

    private fun constrainY(y: Int): Int {
        // Avoid moving sideline into statusbar or navbar region
        val sbHeight = SystemBarUtils.getStatusBarHeight(this)
        val navbarHeight = if (isPortrait) {
            resources.getDimensionPixelSize(com.android.internal.R.dimen.navigation_bar_height)
        } else {
            0
        }
        val newY = y.coerceIn(
            -(screenHeight / 2 - sbHeight - SIDELINE_HEIGHT / 2),
            screenHeight / 2 - navbarHeight - SIDELINE_HEIGHT / 2
        )
        logger.d("constrainY: $y -> $newY")
        return newY
    }

    /**
     * 启动侧边条
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun showView() {
        if (isShowingSideline) return

        logger.d("showView")

        layoutParams.apply {
            type = LayoutParams.TYPE_APPLICATION_OVERLAY
            width = SIDELINE_WIDTH
            height = SIDELINE_HEIGHT
            flags = LayoutParams.FLAG_NOT_FOCUSABLE or
                    LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    LayoutParams.FLAG_HARDWARE_ACCELERATED
            privateFlags = LayoutParams.SYSTEM_FLAG_SHOW_FOR_ALL_USERS or
                    LayoutParams.PRIVATE_FLAG_TRUSTED_OVERLAY or
                    LayoutParams.PRIVATE_FLAG_USE_BLAST or
                    LayoutParams.PRIVATE_FLAG_SYSTEM_APPLICATION_OVERLAY
            format = PixelFormat.RGBA_8888
            windowAnimations = android.R.style.Animation_Dialog
        }

        sideLineView.setSystemGestureExclusionRects(
            listOf(Rect(0, 0, SIDELINE_WIDTH, SIDELINE_HEIGHT))
        )

        updateSidelinePosition()

        runCatching {
            windowManager.addView(sideLineView, layoutParams)
            viewModel.registerCallbacks()
            isShowingSideline = true
        }.onFailure { e ->
            logger.e("failed to add sideline view: ", e)
        }
    }

    private fun updateSidelinePosition() {
        sidelinePositionX = sharedPrefs.getInt(SIDELINE_POSITION_X, 1)
        sidelinePositionY =
            if (isPortrait)
                sharedPrefs.getInt(SIDELINE_POSITION_Y_PORTRAIT, -screenHeight / 6)
            else
                sharedPrefs.getInt(SIDELINE_POSITION_Y_LANDSCAPE, -screenHeight / 6)

        layoutParams.apply {
            x = sidelinePositionX * (screenWidth / 2 - offset)
            y = constrainY(sidelinePositionY)
            logger.d("updateSidelinePosition: ($x,$y)")

            if (isPortrait) {
                layoutInDisplayCutoutMode = LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                flags = (flags and LayoutParams.FLAG_LAYOUT_IN_SCREEN.inv()) or
                    LayoutParams.FLAG_LAYOUT_NO_LIMITS
            } else {
                // avoid going into navbar in landscape
                layoutInDisplayCutoutMode = LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                flags = (flags and LayoutParams.FLAG_LAYOUT_NO_LIMITS.inv()) or
                    LayoutParams.FLAG_LAYOUT_IN_SCREEN
            }
        }
    }

    private fun updateViewLayout() {
        runCatching {
            windowManager.updateViewLayout(sideLineView, layoutParams)
        }.onFailure { e ->
            logger.e("failed to updateViewLayout: ", e)
        }
    }

    private fun removeView(force: Boolean = false) {
        if (!isShowingSideline && !force) return

        logger.d("removeView")
        viewModel.unregisterCallbacks()

        runCatching {
            windowManager.removeViewImmediate(sideLineView)
        }.onFailure { e ->
            logger.e("failed to remove sideline view: $e")
        }

        sidebarView.removeView(force)
        isShowingSideline = false
    }

    private fun animateHideSideline() {
        logger.d("animateHideSideline")
        sideLineView.animate().translationX(sidelinePositionX * 1.0f * SIDELINE_WIDTH).setDuration(300).start()
    }

    private fun animateShowSideline() {
        logger.d("animateShowSideline")
        sideLineView.animate().translationX(0f).setDuration(300).start()
    }

    private fun setIntSp(name: String, value: Int) {
        sharedPrefs.edit().apply {
            putInt(name, value)
            apply()
        }
    }
}
