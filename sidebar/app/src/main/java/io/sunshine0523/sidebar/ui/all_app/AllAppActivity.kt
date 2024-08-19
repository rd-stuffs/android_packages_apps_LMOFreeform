package io.sunshine0523.sidebar.ui.all_app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import io.sunshine0523.sidebar.bean.AppInfo
import io.sunshine0523.sidebar.ui.theme.SidebarTheme
import io.sunshine0523.sidebar.utils.Logger

/**
 * @author KindBrave
 * @since 2023/10/25
 */
class AllAppActivity: ComponentActivity() {
    private val logger = Logger(TAG)
    private lateinit var viewModel: AllAppViewModel

    companion object {
        private const val PACKAGE = "com.sunshine.freeform"
        private const val ACTION = "com.sunshine.freeform.start_freeform"
        private const val TAG = "AllAppActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = AllAppViewModel(application)

        setContent {
            SidebarTheme {
                AllAppGridView(
                    viewModel = viewModel,
                    onClick = { appInfo -> onClick(appInfo) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    private fun onClick(appInfo: AppInfo) {
        val intent = Intent(ACTION).apply {
            setPackage(PACKAGE)
            putExtra("packageName", appInfo.packageName)
            putExtra("activityName", appInfo.activityName)
            putExtra("userId", appInfo.userId)
        }
        sendBroadcast(intent)
        finish()
    }
}