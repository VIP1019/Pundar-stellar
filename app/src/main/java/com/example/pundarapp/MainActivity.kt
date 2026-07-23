package com.example.pundarapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.pundarapp.ui.navigation.PundarNavigation
import com.example.pundarapp.ui.theme.PundarAppTheme
import com.example.pundarapp.ui.data.AppState

class MainActivity : ComponentActivity() {

    // Permission launcher — fires when the user responds to the location request
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            // Permission just granted — update currency from GPS
            AppState.updateCurrencyFromGps(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppState.initPreferences(this)
        enableEdgeToEdge()
        setContent {
            PundarAppTheme {
                PundarNavigation()
            }
        }
        // Ask for location permission if not yet granted
        requestLocationPermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        // Every time the app comes to foreground, update currency from GPS location
        AppState.updateCurrencyFromGps(this)
    }

    private fun requestLocationPermissionIfNeeded() {
        val hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
}