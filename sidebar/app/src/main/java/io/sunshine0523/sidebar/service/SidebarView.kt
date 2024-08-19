package io.sunshine0523.sidebar.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import io.sunshine0523.sidebar.bean.AppInfo
import io.sunshine0523.sidebar.ui.theme.SidebarTheme
import io.sunshine0523.sidebar.utils.Debug
import io.sunshine0523.sidebar.utils.Logger
import kotlin.math.roundToInt

/**
 * @author KindBrave
 * @since 2023/9/26
 */
class SidebarView(
    private val context: Context,
    private val viewModel: ServiceViewModel,
    private val callback: Callback
) : SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle get() = lifecycleRegistry

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private lateinit var composeView: View
    private var sidebarPositionX = 0
    private var sidebarPositionY = 0
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val layoutParams = LayoutParams()
    private val logger = Logger(TAG)

    companion object {
        private const val SIDELINE_POSITION_X = "sideline_position_x"
        private const val OFFSET_X = 90
        private const val PACKAGE = "com.sunshine.freeform"
        private const val ACTION = "com.sunshine.freeform.start_freeform"
        private const val TAG = "SidebarView"
    }

    init {
        savedStateRegistryController.performRestore(null)
    }

    private fun onClick(appInfo: AppInfo) {
        val intent = Intent(ACTION).apply {
            setPackage(PACKAGE)
            putExtra("packageName", appInfo.packageName)
            putExtra("activityName", appInfo.activityName)
            putExtra("userId", appInfo.userId)
        }
        context.sendBroadcast(intent)
        removeView()
    }

    @SuppressLint("ClickableViewAccessibility")
    fun showView() {
        val screenWidth = context.resources.displayMetrics.widthPixels
        val screenHeight = context.resources.displayMetrics.heightPixels
        val sidebarHeight = if (context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            screenHeight / 3
        } else {
            (screenHeight * 0.8f).roundToInt()
        }

        sidebarPositionX = viewModel.getIntSp(SIDELINE_POSITION_X, -1)
        sidebarPositionY = if (context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            -screenHeight / 6
        } else {
            0
        }

        initComposeView()

        layoutParams.apply {
            type = LayoutParams.TYPE_APPLICATION_OVERLAY
            width = LayoutParams.WRAP_CONTENT
            height = sidebarHeight
            x = sidebarPositionX * (screenWidth / 2 - OFFSET_X)
            y = sidebarPositionY
            flags = LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    LayoutParams.FLAG_HARDWARE_ACCELERATED
            format = PixelFormat.RGBA_8888
            windowAnimations = android.R.style.Animation_Dialog
        }

        composeView.translationX = sidebarPositionX * 1.0f * 200

        composeView.setOnTouchListener { view, event ->
            if (Debug.isDebug) logger.d("$view $event")
            if (event.action == MotionEvent.ACTION_UP) {
                removeView()
                true
            }
            false
        }

        runCatching {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            windowManager.addView(composeView, layoutParams)
            composeView.animate().translationX(0f).setDuration(300).start()
        }
    }

    fun removeView() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        runCatching { windowManager.removeViewImmediate(composeView) }
        callback.onRemove()
    }

    private fun initComposeView() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(this@SidebarView)
            setViewTreeSavedStateRegistryOwner(this@SidebarView)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SidebarTheme {
                    SidebarComposeView(
                        viewModel = viewModel,
                        onClick = { onClick(it) },
                        modifier = Modifier
                            .fillMaxHeight()
                            .wrapContentWidth()
                    )
                }
            }
        }
    }

    interface Callback {
        fun onRemove()
    }
}