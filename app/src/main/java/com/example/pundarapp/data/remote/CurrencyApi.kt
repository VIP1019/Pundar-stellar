package com.example.pundarapp.data.remote

object CurrencyApi {
    /**
     * Fetches the live PHP to XLM exchange rate via the unified [CurrencyRepository].
     * Returns the number of XLM you get for 1 PHP (i.e. 1.0 / phpPerXlm).
     */
    suspend fun getPhpToXlmRate(forceRefresh: Boolean = false): Double {
        val phpPerXlm = CurrencyRepository.getPhpPerXlmRate(forceRefresh)
        return if (phpPerXlm > 0.0) 1.0 / phpPerXlm else 1.0 / CurrencyRepository.DEFAULT_PHP_PER_XLM
    }

    /**
     * Fetches the live XLM to PHP exchange rate (PHP per 1 XLM).
     */
    suspend fun getXlmToPhpRate(forceRefresh: Boolean = false): Double {
        return CurrencyRepository.getPhpPerXlmRate(forceRefresh)
    }
}
