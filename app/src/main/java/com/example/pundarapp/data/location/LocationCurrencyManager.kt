package com.example.pundarapp.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

private const val TAG = "LocationCurrencyManager"

/** Uses GPS to get the user's current country code (ISO 3166-1 alpha-2) */
object LocationCurrencyManager {

    suspend fun getCountryCode(context: Context): String? {
        // Check if we have location permission
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            Log.d(TAG, "Location permission not granted")
            return null
        }

        return try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val cancellationSource = CancellationTokenSource()
            val priority = if (hasFine) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY

            val location = suspendCancellableCoroutine { cont ->
                fusedClient.getCurrentLocation(priority, cancellationSource.token)
                    .addOnSuccessListener { loc -> cont.resume(loc) }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "getCurrentLocation failed: ${e.message}")
                        // Fall back to last known location
                        fusedClient.lastLocation
                            .addOnSuccessListener { lastLoc -> cont.resume(lastLoc) }
                            .addOnFailureListener { cont.resume(null) }
                    }
                cont.invokeOnCancellation { cancellationSource.cancel() }
            }

            if (location == null) {
                Log.d(TAG, "Location is null")
                return null
            }

            Log.d(TAG, "Got location: ${location.latitude}, ${location.longitude}")
            reverseGeocodeCountry(context, location.latitude, location.longitude)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting location: ${e.message}")
            null
        }
    }

    private suspend fun reverseGeocodeCountry(context: Context, lat: Double, lon: Double): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Modern async API (Android 13+)
            suspendCancellableCoroutine { cont ->
                try {
                    Geocoder(context, Locale.getDefault()).getFromLocation(lat, lon, 1) { addresses ->
                        val country = addresses.firstOrNull()?.countryCode?.uppercase()
                        Log.d(TAG, "Geocoded country (async): $country")
                        cont.resume(country)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Geocoder error: ${e.message}")
                    cont.resume(null)
                }
            }
        } else {
            // Legacy synchronous API
            try {
                @Suppress("DEPRECATION")
                val addresses = Geocoder(context, Locale.getDefault()).getFromLocation(lat, lon, 1)
                val country = addresses?.firstOrNull()?.countryCode?.uppercase()
                Log.d(TAG, "Geocoded country (sync): $country")
                country
            } catch (e: Exception) {
                Log.e(TAG, "Geocoder error: ${e.message}")
                null
            }
        }
    }
}
