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

    /** Realistic fallback for 1 XLM in PHP when offline or before first fetch */
    const val DEFAULT_PHP_PER_XLM = 6.50

    private val DEFAULT_RATES = mapOf(
        "PHP" to DEFAULT_PHP_PER_XLM,
        "USD" to 0.115,
        "EUR" to 0.105,
        "JPY" to 17.50,
        "KRW" to 160.0,
        "GBP" to 0.09,
        "SGD" to 0.155,
        "IDR" to 1820.0,
        "VND" to 2900.0,
        "AUD" to 0.175,
        "CAD" to 0.16,
        "CHF" to 0.10,
        "CNY" to 0.83,
        "HKD" to 0.90,
        "INR" to 9.60,
        "MYR" to 0.51,
        "NZD" to 0.19,
        "THB" to 3.95,
        "TWD" to 3.70
    )

    // Cache variables
    @Volatile
    private var cachedRates: Map<String, Double> = DEFAULT_RATES
    @Volatile
    private var lastFetchTime: Long = 0
    private const val CACHE_DURATION_MS = 60_000L // 60 seconds

    fun getDefaultRate(currencyCode: String): Double {
        return DEFAULT_RATES[currencyCode.uppercase()] ?: DEFAULT_PHP_PER_XLM
    }

    fun getCachedPhpPerXlm(): Double {
        return cachedRates["PHP"]?.takeIf { it > 0.0 } ?: DEFAULT_PHP_PER_XLM
    }

    fun xlmToPhp(xlmAmount: Double, phpPerXlm: Double = getCachedPhpPerXlm()): Double {
        val validRate = if (phpPerXlm > 0.0) phpPerXlm else DEFAULT_PHP_PER_XLM
        return xlmAmount * validRate
    }

    fun phpToXlm(phpAmount: Double, phpPerXlm: Double = getCachedPhpPerXlm()): Double {
        val validRate = if (phpPerXlm > 0.0) phpPerXlm else DEFAULT_PHP_PER_XLM
        return phpAmount / validRate
    }

    suspend fun getPhpPerXlmRate(forceRefresh: Boolean = false): Double {
        val rates = getXlmRates(forceRefresh)
        return rates["PHP"]?.takeIf { it > 0.0 } ?: getCachedPhpPerXlm()
    }

    suspend fun getXlmRates(forceRefresh: Boolean = false): Map<String, Double> = withContext(Dispatchers.IO) {
        val currentTime = System.currentTimeMillis()
        if (!forceRefresh && lastFetchTime > 0L && (currentTime - lastFetchTime) < CACHE_DURATION_MS) {
            Log.d(TAG, "Returning cached rates")
            return@withContext cachedRates
        }

        try {
            Log.d(TAG, "Fetching live rates from CoinGecko")
            val connection = (URL(COINGECKO_URL).openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "PundarStellarApp/1.0")
            }

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                // Expected JSON: {"stellar":{"php":..., "usd":..., ...}}
                val json = Json.parseToJsonElement(response).jsonObject
                val stellarObject = json["stellar"]?.jsonObject

                if (stellarObject != null) {
                    val newRates = DEFAULT_RATES.toMutableMap()
                    stellarObject.forEach { (currency, valueElement) ->
                        val parsed = valueElement.jsonPrimitive.content.toDoubleOrNull()
                        if (parsed != null && parsed > 0.0) {
                            newRates[currency.uppercase()] = parsed
                        }
                    }

                    cachedRates = newRates
                    lastFetchTime = currentTime
                    return@withContext cachedRates
                }
            } else {
                Log.w(TAG, "CoinGecko returned HTTP ${connection.responseCode}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch XLM rates", e)
        }

        // Return cached rates (seeded with realistic defaults) if fetch failed
        cachedRates
    }

    fun getLastFetchTime(): Long = lastFetchTime
}
