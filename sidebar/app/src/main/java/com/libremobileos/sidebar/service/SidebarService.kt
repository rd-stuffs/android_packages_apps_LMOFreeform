package com.libremobileos.sidebar.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import com.libremobileos.sidebar.R
import com.libremobileos.sidebar.utils.Logger

class SidebarService : Service(), SharedPreferences.OnSharedPreferenceChangeListener,
    GestureListener.Callback {
    private val logger = Logger(TAG)
    private lateinit var viewModel: ServiceViewModel
    private lateinit var windowManager: WindowManager
    private lateinit var sidebarView: SidebarView
    private var showSideline = false
    private var showSidebar = false
    private var sidelinePositionX = 0
    private var sidelinePositionY = 0
    private var screenWidth = 0
    private var screenHeight = 0
    private val layoutParams = WindowManager.LayoutParams()
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

    companion object {
        private const val TAG = "SidebarService"
        private const val SIDELINE_WIDTH = 100
        //侧边条移动时的宽度
        private const val SIDELINE_MOVE_WIDTH = 200
        private const val SIDELINE_HEIGHT = 200
        //侧边条屏幕边缘偏移量
        private const val OFFSET = 20

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
        viewModel.registerSpChangeListener(this)
        sidebarView = SidebarView(this@SidebarService, viewModel, object : SidebarView.Callback {
            override fun onRemove() {
                logger.d("sidebar view removed")
                if (showSidebar) animateShowSideline()
                showSidebar = false
            }
        })
        showSideline = viewModel.getBooleanSp(SIDELINE, false)
        logger.d("screenWidth=$screenWidth screenHeight=$screenHeight showSideline=$showSideline")
        if (showSideline) showView()
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        screenWidth = resources.displayMetrics.widthPixels
        screenHeight = resources.displayMetrics.heightPixels
        logger.d("onConfigChanged: screenWidth=$screenWidth height=$screenHeight")
        showView()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (this::viewModel.isInitialized) viewModel.unregisterSpChangeListener(this)
        removeView()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        when (key) {
            SIDELINE -> {
                showSideline = viewModel.getBooleanSp(SIDELINE, false)
                if (showSideline) {
                    showView()
                } else {
                    removeView()
                }
            }
            SIDELINE_POSITION_X -> {
                sidelinePositionX = viewModel.getIntSp(SIDELINE_POSITION_X, 1)
                updateView()
            }
            SIDELINE_POSITION_Y_PORTRAIT -> {
                sidelinePositionY = viewModel.getIntSp(SIDELINE_POSITION_Y_PORTRAIT, -screenHeight / 6)
                updateView()
            }
            SIDELINE_POSITION_Y_LANDSCAPE -> {
                sidelinePositionY = viewModel.getIntSp(SIDELINE_POSITION_Y_LANDSCAPE, -screenHeight / 6)
                updateView()
            }
        }
    }

    override fun showSidebar() {
        logger.d("showSidebar")
        sidebarView.showView()
        showSidebar = true
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
            x = sidelinePositionX * (screenWidth / 2 - OFFSET)
            y = layoutParams.y + yChanged
        }
        updateViewLayout()
    }

    override fun endMoveSideline() {
        logger.d("endMoveSideline")
        layoutParams.apply {
            width = SIDELINE_WIDTH
        }
        updateViewLayout()
        viewModel.setIntSp(SIDELINE_POSITION_X, sidelinePositionX)
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            viewModel.setIntSp(SIDELINE_POSITION_Y_PORTRAIT, layoutParams.y)
        } else {
            viewModel.setIntSp(SIDELINE_POSITION_Y_LANDSCAPE, layoutParams.y)
        }
    }

    /**
     * 启动侧边条
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun showView() {
        logger.d("showView")
        removeView()
        sidelinePositionX = viewModel.getIntSp(SIDELINE_POSITION_X, 1)
        sidelinePositionY =
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
                viewModel.getIntSp(SIDELINE_POSITION_Y_PORTRAIT, -screenHeight / 6)
            else
                viewModel.getIntSp(SIDELINE_POSITION_Y_LANDSCAPE, -screenHeight / 6)
        runCatching {
            layoutParams.apply {
                type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                width = SIDELINE_WIDTH
                height = SIDELINE_HEIGHT
                x = sidelinePositionX * (screenWidth / 2 - OFFSET)
                y = sidelinePositionY
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                privateFlags = WindowManager.LayoutParams.SYSTEM_FLAG_SHOW_FOR_ALL_USERS or WindowManager.LayoutParams.PRIVATE_FLAG_IS_ROUNDED_CORNERS_OVERLAY or WindowManager.LayoutParams.PRIVATE_FLAG_USE_BLAST or WindowManager.LayoutParams.PRIVATE_FLAG_TRUSTED_OVERLAY
                format = PixelFormat.RGBA_8888
                windowAnimations = android.R.style.Animation_Dialog
            }
            windowManager.addView(sideLineView, layoutParams)
        }.onFailure { e ->
            logger.e("failed to add sideline view: ", e)
        }
    }

    private fun updateView() {
        runCatching {
            layoutParams.apply {
                x = sidelinePositionX * (screenWidth / 2 - OFFSET)
                y = sidelinePositionY
            }
            windowManager.updateViewLayout(sideLineView, layoutParams)
        }.onFailure { e ->
            logger.e("failed to updateView: ", e)
        }
    }

    private fun updateViewLayout() {
        runCatching {
            windowManager.updateViewLayout(sideLineView, layoutParams)
        }.onFailure { e ->
            logger.e("failed to updateViewLayout: ", e)
        }
    }

    private fun removeView() {
        logger.d("removeView")
        runCatching {
            windowManager.removeViewImmediate(sideLineView)
        }.onFailure { e ->
            logger.d("failed to remove sideline view: ", e)
        }

        runCatching {
            sidebarView.removeView()
        }.onFailure { e ->
            logger.d("failed to remove sidebar view: ", e)
        }
    }

    private fun animateHideSideline() {
        logger.d("animateHideSideline")
        sideLineView.animate().translationX(sidelinePositionX * 1.0f * SIDELINE_WIDTH).setDuration(300).start()
    }

    private fun animateShowSideline() {
        logger.d("animateShowSideline")
        sideLineView.animate().translationX(0f).setDuration(300).start()
    }
}
