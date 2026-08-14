package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.SettingsRepository
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.HtmlAppBuilderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settingsRepository = remember { SettingsRepository(applicationContext) }
            val appSettings by settingsRepository.settings.collectAsStateWithLifecycle()

            HtmlAppBuilderTheme(
                themeMode = appSettings.themeMode,
                accentColor = appSettings.accentColor
            ) {
                AppNavigation()
            }
        }
    }
}
