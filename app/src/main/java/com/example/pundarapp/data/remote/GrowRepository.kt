package com.example.pundarapp.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object GrowRepository {
    private const val TAG = "GrowRepository"
    private val db = FirebaseFirestore.getInstance()
    private val favoritesCollection = db.collection("favorites")

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
}
