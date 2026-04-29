package com.rork.nexa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.rork.nexa.data.AppState
import com.rork.nexa.ui.navigation.AppNavigation
import com.rork.nexa.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme(mode = AppState.themeMode) {
                AppNavigation()
            }
        }
    }
}
