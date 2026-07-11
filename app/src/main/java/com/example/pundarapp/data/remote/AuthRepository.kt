package com.example.pundarapp.data.remote

import android.util.Log
import com.example.pundarapp.data.stellar.StellarWalletManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object AuthRepository {
    private const val TAG = "AuthRepository"
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    // In-memory session (simple dev approach - no Firebase Auth needed)
    private var currentUserData: UserData? = null

    data class UserData(
        val id: String,
        val name: String,
        val phone: String,
        val stellarPublicKey: String? = null
    )

    suspend fun registerWithPhone(phone: String, fullName: String, mpin: String): Result<Boolean> {
        return try {
            val cleanPhone = phone.trim()
            Log.d(TAG, "registerWithPhone: phone=$cleanPhone")

            // Check if phone number already exists
            val existing = usersCollection.document(cleanPhone).get().await()
            if (existing.exists()) {
                Log.w(TAG, "registerWithPhone: phone already exists")
                return Result.failure(Exception("This mobile number is already registered."))
            }

            // Generate Stellar Wallet
            val keyPair = StellarWalletManager.generateKeyPair()
            val publicKey = keyPair.accountId
            val seed = keyPair.secretSeed

            // Fund account
            val fundingResult = StellarWalletManager.fundTestnetAccount(publicKey)
            if (fundingResult.isFailure) {
                return Result.failure(Exception("Failed to initialize wallet: ${fundingResult.exceptionOrNull()?.message}"))
            }

            // Encrypt seed
            val encryptedSeed = StellarWalletManager.encryptSecretSeed(seed, mpin)
            // Clear seed
            StellarWalletManager.clearCharArray(seed)

            // Store user directly in Firestore
            val userData = hashMapOf(
                "phone_number" to cleanPhone,
                "full_name" to fullName.trim(),
                "mpin" to mpin.trim(),
                "pundar_score" to 800,
                "created_at" to System.currentTimeMillis(),
                "stellarPublicKey" to publicKey,
                "encryptedStellarSeed" to encryptedSeed,
                "isCustodial" to true
            )
            usersCollection.document(cleanPhone).set(userData).await()
            Log.d(TAG, "registerWithPhone: success")

            // Auto-login: set session
            currentUserData = UserData(
                id = cleanPhone,
                name = fullName.trim(),
                phone = cleanPhone,
                stellarPublicKey = publicKey
            )

            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "registerWithPhone failed", e)
            // Map common Firestore failures to user-friendly messages
            val friendly = mapFirestoreError(e)
            Result.failure(Exception(friendly, e))
        }
    }

    suspend fun loginWithPhone(phone: String, mpin: String): Result<Boolean> {
        return try {
            val cleanPhone = phone.trim()
            Log.d(TAG, "loginWithPhone: phone=$cleanPhone")

            // Look up user in Firestore by phone number
            val doc = usersCollection.document(cleanPhone).get().await()

            if (!doc.exists()) {
                Log.w(TAG, "loginWithPhone: account not found")
                return Result.failure(Exception("Account not found. Please register first."))
            }

            // Verify MPIN
            val storedMpin = doc.getString("mpin") ?: ""
            if (storedMpin != mpin.trim()) {
                Log.w(TAG, "loginWithPhone: incorrect mpin")
                return Result.failure(Exception("Incorrect MPIN. Please try again."))
            }

            // Set session
            val name = doc.getString("full_name") ?: "User"
            val publicKey = doc.getString("stellarPublicKey")
            currentUserData = UserData(
                id = cleanPhone,
                name = name,
                phone = cleanPhone,
                stellarPublicKey = publicKey
            )
            Log.d(TAG, "loginWithPhone: success")

            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "loginWithPhone failed", e)
            val friendly = mapFirestoreError(e)
            Result.failure(Exception(friendly, e))
        }
    }

    private fun mapFirestoreError(e: Exception): String {
        val msg = e.message.orEmpty()
        return when {
            msg.contains("UNAVAILABLE", ignoreCase = true) ||
                msg.contains("Failed to connect", ignoreCase = true) ->
                "Cannot reach Firebase. Check your internet connection."
            msg.contains("PERMISSION_DENIED", ignoreCase = true) ->
                "Firestore permission denied. Update your Firestore security rules in the Firebase Console to allow reads/writes on the 'users' collection."
            msg.contains("NOT_FOUND", ignoreCase = true) ||
                msg.contains("does not exist", ignoreCase = true) ->
                "Firestore database has not been created yet. Open the Firebase Console and create a Firestore database for project 'pundar-app'."
            else -> "Auth error: $msg"
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

    fun getCurrentUserStellarPublicKey(): String? {
        return currentUserData?.stellarPublicKey
    }

    suspend fun getCurrentUserEncryptedSeed(): String? {
        val phone = getCurrentUserPhone()
        if (phone.isBlank()) return null
        return try {
            val doc = usersCollection.document(phone).get().await()
            doc.getString("encryptedStellarSeed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch encrypted seed", e)
            null
        }
    }

    fun getCurrentUserInitials(): String {
        val name = getCurrentUserName()
        if (name == "User" || name.isBlank()) return "U"
        val parts = name.split(" ")
        if (parts.size == 1) return parts[0].take(1).uppercase()
        return (parts.first().take(1) + parts.last().take(1)).uppercase()
    }

    suspend fun changeMpin(currentMpin: String, newMpin: String): Result<Boolean> {
        return try {
            val phone = getCurrentUserPhone()
            if (phone.isBlank()) {
                return Result.failure(Exception("User not logged in."))
            }

            val doc = usersCollection.document(phone).get().await()
            if (!doc.exists()) {
                return Result.failure(Exception("Account not found."))
            }

            val storedMpin = doc.getString("mpin") ?: ""
            if (storedMpin != currentMpin.trim()) {
                return Result.failure(Exception("Incorrect current MPIN."))
            }

            usersCollection.document(phone).update("mpin", newMpin.trim()).await()
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "changeMpin failed", e)
            val friendly = mapFirestoreError(e)
            Result.failure(Exception(friendly, e))
        }
    }

    suspend fun logout() {
        currentUserData = null
    }

    suspend fun searchUsers(query: String): List<UserData> {
        return try {
            if (query.isBlank()) return emptyList()
            
            // Get all users and filter locally (acceptable for prototyping)
            val snapshot = usersCollection.get().await()
            val lowerQuery = query.lowercase()
            
            snapshot.documents.mapNotNull { doc ->
                val phone = doc.getString("phone_number") ?: return@mapNotNull null
                val name = doc.getString("full_name") ?: return@mapNotNull null
                
                // Exclude current user from search results
                if (phone == currentUserData?.id) return@mapNotNull null
                
                if (phone.contains(lowerQuery) || name.lowercase().contains(lowerQuery)) {
                    val stellarPublicKey = doc.getString("stellarPublicKey")
                    UserData(id = phone, name = name, phone = phone, stellarPublicKey = stellarPublicKey)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "searchUsers failed", e)
            emptyList()
        }
    }
}
