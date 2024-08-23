package io.sunshine0523.sidebar.ui.sidebar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.android.settingslib.spa.framework.theme.SettingsTheme

/**
 * @author KindBrave
 * @since 2023/10/21
 */
class SidebarSettingsActivity : ComponentActivity() {
    private val viewModel by lazy { SidebarSettingsViewModel(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            SettingsTheme {
                SidebarSettingsPage(viewModel = viewModel)
            }
        }
    }
}
