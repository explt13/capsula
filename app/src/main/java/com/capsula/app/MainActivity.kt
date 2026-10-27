package com.capsula.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.capsula.app.ui.CapsulaApp
import com.capsula.app.ui.theme.CapsulaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CapsulaTheme {
                CapsulaApp()
            }
        }
    }
}
