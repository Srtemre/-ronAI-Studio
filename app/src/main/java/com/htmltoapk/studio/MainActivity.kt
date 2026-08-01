package com.htmltoapk.studio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.htmltoapk.studio.ui.AppRoot
import com.htmltoapk.studio.ui.theme.HtmlToApkTheme
import com.htmltoapk.studio.ui.theme.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsVm: SettingsViewModel = hiltViewModel()
            val state by settingsVm.state.collectAsStateWithLifecycle()
            HtmlToApkTheme(
                themeMode = state.themeMode,
                accent = state.accent,
                dynamicColor = state.dynamicColor,
                locale = state.locale
            ) {
                AppRoot()
            }
        }
    }
}
