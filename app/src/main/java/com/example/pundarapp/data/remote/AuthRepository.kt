package com.example.pundarapp.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object AuthRepository {
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    // In-memory session (simple dev approach - no Firebase Auth needed)
    private var currentUserData: UserData? = null

    data class UserData(
        val id: String,
        val name: String,
        val phone: String
    )

    suspend fun registerWithPhone(phone: String, fullName: String, mpin: String): Result<Boolean> {
        return try {
            val cleanPhone = phone.trim()

            // Check if phone number already exists
            val existing = usersCollection.document(cleanPhone).get().await()
            if (existing.exists()) {
                return Result.failure(Exception("This mobile number is already registered."))
            }

            // Store user directly in Firestore
            val userData = hashMapOf(
                "phone_number" to cleanPhone,
                "full_name" to fullName.trim(),
                "mpin" to mpin.trim(),
                "pundar_score" to 800,
                "created_at" to System.currentTimeMillis()
            )
            usersCollection.document(cleanPhone).set(userData).await()

            // Auto-login: set session
            currentUserData = UserData(
                id = cleanPhone,
                name = fullName.trim(),
                phone = cleanPhone
            )

            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun loginWithPhone(phone: String, mpin: String): Result<Boolean> {
        return try {
            val cleanPhone = phone.trim()

            // Look up user in Firestore by phone number
            val doc = usersCollection.document(cleanPhone).get().await()

            if (!doc.exists()) {
                return Result.failure(Exception("Account not found. Please register first."))
            }

            // Verify MPIN
            val storedMpin = doc.getString("mpin") ?: ""
            if (storedMpin != mpin.trim()) {
                return Result.failure(Exception("Incorrect MPIN. Please try again."))
            }

            // Set session
            val name = doc.getString("full_name") ?: "User"
            currentUserData = UserData(
                id = cleanPhone,
                name = name,
                phone = cleanPhone
            )

            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun isUserLoggedIn(): Boolean {
        return currentUserData != null
    }

    fun getCurrentUserId(): String? {
        return currentUserData?.id
    }

    fun getCurrentUserName(): String {
        return currentUserData?.name ?: "User"
    }

    fun getCurrentUserPhone(): String {
        return currentUserData?.phone ?: ""
    }

    suspend fun logout() {
        currentUserData = null
    }
}
