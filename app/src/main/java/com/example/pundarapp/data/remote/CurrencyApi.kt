package com.example.pundarapp.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object CurrencyApi {
    private const val TAG = "CurrencyApi"
    
    /**
     * Fetches the live PHP to XLM exchange rate.
     * Returns the number of XLM you get for 1 PHP.
     * If the network call fails, falls back to a default rate (e.g., 1 PHP = 0.16 XLM).
     */
    suspend fun getPhpToXlmRate(): Double = withContext(Dispatchers.IO) {
        try {
            // Using CoinGecko API: Get price of stellar in PHP
            // Example response: {"stellar":{"php":5.78}} -> 1 XLM = 5.78 PHP
            val url = URL("https://api.coingecko.com/api/v3/simple/price?ids=stellar&vs_currencies=php")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val phpPerXlm = json.getJSONObject("stellar").getDouble("php")
                
                // We want how many XLM you get for 1 PHP
                if (phpPerXlm > 0) {
                    return@withContext 1.0 / phpPerXlm
                }
            } else {
                Log.e(TAG, "Failed to fetch exchange rate, response code: ${connection.responseCode}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching exchange rate", e)
        }
        
        // Fallback: Assume 1 XLM = ~5.0 PHP -> 1 PHP = 0.2 XLM
        0.2
    }
}
