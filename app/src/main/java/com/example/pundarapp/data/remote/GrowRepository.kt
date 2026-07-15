package com.example.pundarapp.data.remote

import android.util.Log
import com.example.pundarapp.ui.data.RoundUpInvestment
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object GrowRepository {
    private const val TAG = "GrowRepository"
    private val db = FirebaseFirestore.getInstance()
    private val favoritesCollection = db.collection("favorites")
    private val roundUpsCollection = db.collection("grow_roundups")

    /**
     * Get user's favorite stock tickers
     */
    suspend fun getFavorites(userId: String): Set<String> {
        return try {
            val doc = favoritesCollection.document(userId).get().await()
            val tickers = doc.get("tickers") as? List<*>
            tickers?.filterIsInstance<String>()?.toSet() ?: emptySet()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get favorites", e)
            emptySet()
        }
    }

    /**
     * Toggle favorite status for a stock
     */
    suspend fun toggleFavorite(userId: String, ticker: String): Result<Boolean> {
        return try {
            val doc = favoritesCollection.document(userId).get().await()
            val currentFavorites = (doc.get("tickers") as? List<*>)
                ?.filterIsInstance<String>()
                ?.toMutableSet() 
                ?: mutableSetOf()

            val isFavorite = if (ticker in currentFavorites) {
                currentFavorites.remove(ticker)
                false
            } else {
                currentFavorites.add(ticker)
                true
            }

            favoritesCollection.document(userId)
                .set(hashMapOf("tickers" to currentFavorites.toList()))
                .await()

            Log.d(TAG, "Toggled favorite for $ticker: $isFavorite")
            Result.success(isFavorite)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle favorite for $ticker", e)
            Result.failure(e)
        }
    }

    /**
     * Check if a ticker is favorited
     */
    suspend fun isFavorite(userId: String, ticker: String): Boolean {
        return try {
            val favorites = getFavorites(userId)
            ticker in favorites
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check favorite for $ticker", e)
            false
        }
    }

    suspend fun loadRoundUpBalance(userId: String): Double {
        return try {
            val doc = roundUpsCollection.document(userId).get().await()
            doc.getDouble("round_up_balance") ?: 0.0
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load round-up balance", e)
            0.0
        }
    }

    suspend fun saveRoundUpBalance(userId: String, balance: Double) {
        try {
            roundUpsCollection.document(userId)
                .set(
                    hashMapOf(
                        "user_id" to userId,
                        "round_up_balance" to balance,
                        "updated_at" to System.currentTimeMillis()
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save round-up balance", e)
        }
    }

    suspend fun saveRoundUpInvestment(userId: String, investment: RoundUpInvestment) {
        try {
            val data = hashMapOf(
                "reference" to investment.reference,
                "source_reference" to investment.sourceReference,
                "source_amount" to investment.sourceAmount,
                "round_up_amount" to investment.roundUpAmount,
                "converted_amount" to investment.convertedAmount,
                "ticker" to investment.ticker,
                "company_name" to investment.companyName,
                "fractional_shares" to investment.fractionalShares,
                "price_per_share" to investment.pricePerShare,
                "provider" to investment.provider,
                "status" to investment.status,
                "created_at" to investment.createdAt
            )

            val userDoc = roundUpsCollection.document(userId)
            userDoc.collection("investments").document(investment.reference).set(data).await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save round-up investment", e)
        }
    }
}
