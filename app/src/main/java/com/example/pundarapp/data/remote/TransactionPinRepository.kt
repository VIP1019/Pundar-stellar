package com.example.pundarapp.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Verifies a 4-digit transaction PIN against the Firestore-stored `mpin`
 * field on the user's profile document.
 *
 * NOTE: PIN is stored in plain text in Firestore today. This is acceptable
 * for the current prototype; production should hash with bcrypt/argon2.
 */
object TransactionPinRepository {
    private const val TAG = "TransactionPinRepository"
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    /**
     * Returns Success(true) if the pin matches, Success(false) if it doesn't,
     * Failure otherwise.
     */
    suspend fun verifyPin(userPhone: String, pin: String): Result<Boolean> {
        return try {
            if (userPhone.isBlank()) {
                return Result.failure(Exception("User not logged in."))
            }
            val doc = usersCollection.document(userPhone).get().await()
            if (!doc.exists()) {
                return Result.failure(Exception("Account not found."))
            }
            val stored = doc.getString("mpin") ?: ""
            if (stored == pin.trim()) {
                Result.success(true)
            } else {
                Result.success(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "verifyPin failed", e)
            Result.failure(e)
        }
    }
}
