package com.example.pundarapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.pundarapp.ui.navigation.PundarNavigation
import com.example.pundarapp.ui.theme.PundarAppTheme
import com.example.pundarapp.ui.data.AppState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppState.initPreferences(this)
        enableEdgeToEdge()
        setContent {
            PundarAppTheme {
                PundarNavigation()
            }
        }
    }
}