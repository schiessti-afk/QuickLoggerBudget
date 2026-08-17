package com.quicklogger.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.quicklogger.app.presentation.navigation.QuickLoggerNavHost
import com.quicklogger.app.presentation.theme.QuickLoggerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QuickLoggerTheme {
                QuickLoggerNavHost()
            }
        }
    }
}
