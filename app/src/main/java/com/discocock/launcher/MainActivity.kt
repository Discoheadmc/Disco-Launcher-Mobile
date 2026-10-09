package com.discocock.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.discocock.launcher.ui.DiscoLauncherApp
import com.discocock.launcher.ui.theme.DiscoTheme
import com.discocock.launcher.ui.theme.accentColorOf

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = (application as DiscoApplication).container.settings
        setContent {
            val accent by settings.accentColor.collectAsState(initial = "indigo")
            DiscoTheme(accentColor = accentColorOf(accent)) {
                DiscoLauncherApp()
            }
        }
    }
}
