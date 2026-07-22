package com.example.pundarapp.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URL

data class CurrencyData(val code: String, val name: String, val symbol: String, val flag: String)

val supportedCurrencies = listOf(
    CurrencyData("PHP", "Philippine Peso", "₱", "🇵🇭"),
    CurrencyData("USD", "US Dollar", "$", "🇺🇸"),
    CurrencyData("EUR", "Euro", "€", "🇪🇺"),
    CurrencyData("JPY", "Japanese Yen", "¥", "🇯🇵"),
    CurrencyData("KRW", "South Korean Won", "₩", "🇰🇷"),
    CurrencyData("GBP", "British Pound", "£", "🇬🇧"),
    CurrencyData("SGD", "Singapore Dollar", "S$", "🇸🇬"),
    CurrencyData("IDR", "Indonesian Rupiah", "Rp", "🇮🇩"),
    CurrencyData("VND", "Vietnamese Dong", "₫", "🇻🇳"),
    CurrencyData("AUD", "Australian Dollar", "A$", "🇦🇺"),
    CurrencyData("CAD", "Canadian Dollar", "C$", "🇨🇦"),
    CurrencyData("CHF", "Swiss Franc", "Fr", "🇨🇭"),
    CurrencyData("CNY", "Chinese Yuan", "¥", "🇨🇳"),
    CurrencyData("HKD", "Hong Kong Dollar", "HK$", "🇭🇰"),
    CurrencyData("INR", "Indian Rupee", "₹", "🇮🇳"),
    CurrencyData("MYR", "Malaysian Ringgit", "RM", "🇲🇾"),
    CurrencyData("NZD", "New Zealand Dollar", "NZ$", "🇳🇿"),
    CurrencyData("THB", "Thai Baht", "฿", "🇹🇭"),
    CurrencyData("TWD", "New Taiwan Dollar", "NT$", "🇹🇼")
)

object CurrencyRepository {
    private const val TAG = "CurrencyRepository"
    // CoinGecko public API - returns simple price for stellar in various fiat currencies
    private const val COINGECKO_URL = "https://api.coingecko.com/api/v3/simple/price?ids=stellar&vs_currencies=php,usd,eur,jpy,krw,gbp,sgd,idr,vnd,aud,cad,chf,cny,hkd,inr,myr,nzd,thb,twd"
    
    // Cache variables
    private var cachedRates: Map<String, Double> = emptyMap()
    private var lastFetchTime: Long = 0
    private const val CACHE_DURATION_MS = 60_000L // 60 seconds

    suspend fun getXlmRates(forceRefresh: Boolean = false): Map<String, Double> = withContext(Dispatchers.IO) {
        val currentTime = System.currentTimeMillis()
        if (!forceRefresh && cachedRates.isNotEmpty() && (currentTime - lastFetchTime) < CACHE_DURATION_MS) {
            Log.d(TAG, "Returning cached rates")
            return@withContext cachedRates
        }

        try {
            Log.d(TAG, "Fetching live rates from CoinGecko")
            val response = URL(COINGECKO_URL).readText()
            
            // Expected JSON: {"stellar":{"php":..., "usd":..., ...}}
            val json = Json.parseToJsonElement(response).jsonObject
            val stellarObject = json["stellar"]?.jsonObject
            
            if (stellarObject != null) {
                val newRates = mutableMapOf<String, Double>()
                stellarObject.forEach { (currency, valueElement) ->
                    newRates[currency.uppercase()] = valueElement.jsonPrimitive.content.toDoubleOrNull() ?: 0.0
                }
                
                cachedRates = newRates
                lastFetchTime = currentTime
                return@withContext cachedRates
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch XLM rates", e)
        }
        
        // Return cached rates if fetch failed, or an empty map if no cache exists
        cachedRates
    }
    
    fun getLastFetchTime(): Long = lastFetchTime
}
