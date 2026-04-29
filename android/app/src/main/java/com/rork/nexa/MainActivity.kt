package com.rork.nexa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.rork.nexa.data.AppState
import com.rork.nexa.data.auth.AuthRepository
import com.rork.nexa.ui.navigation.AppNavigation
import com.rork.nexa.ui.theme.AppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AuthRepository.get(applicationContext)
        setContent {
            AppTheme(mode = AppState.themeMode) {
                AppNavigation()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (isFinishing) {
            CoroutineScope(Dispatchers.IO).launch {
                AuthRepository.get(applicationContext).signOutIfNotRemembered()
            }
        }
    }
}
