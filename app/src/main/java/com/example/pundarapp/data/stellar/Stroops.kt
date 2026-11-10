package com.example.pundarapp.data.stellar

import java.math.BigDecimal
import java.math.RoundingMode

// 1 XLM = 10^7 stroops. Pay logic uses Long stroops; BigDecimal only at the edges.
object Stroops {
    const val PER_XLM = 10_000_000L

    /** Stroops to a 7-decimal string, e.g. 15000000 -> "1.5000000". */
    fun toXlmString(stroops: Long): String {
        require(stroops >= 0) { "Negative stroops" }
        val whole = stroops / PER_XLM
        val frac = (stroops % PER_XLM).toString().padStart(7, '0')
        return "$whole.$frac"
    }

    /** Decimal XLM text to stroops; extra decimals are rounded down. Null if invalid. */
    fun fromXlmString(text: String): Long? = try {
        BigDecimal(text.trim())
            .setScale(7, RoundingMode.DOWN)
            .movePointRight(7)
            .longValueExact()
    } catch (e: Exception) {
        null
    }
}
