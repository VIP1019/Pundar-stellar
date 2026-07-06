package com.example.pundarapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.pundarapp.ui.navigation.PundarNavigation
import com.example.pundarapp.ui.theme.PundarAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PundarAppTheme {
                PundarNavigation()
            }
        }
    }
}