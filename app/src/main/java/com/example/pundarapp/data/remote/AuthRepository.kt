package com.example.pundarapp.data.remote

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import com.example.pundarapp.data.stellar.PayStellarClient
import com.example.pundarapp.data.stellar.StellarWalletManager
import com.example.pundarapp.ui.data.AppState
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object AuthRepository {
    private const val TAG = "AuthRepository"
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    /** Observable session — screens recompose when this changes. */
    val currentUserState = mutableStateOf<UserData?>(null)
    
    private var prefs: android.content.SharedPreferences? = null

    fun initPrefs(context: android.content.Context) {
        prefs = context.getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE)
        val id = prefs?.getString("user_id", null)
        val name = prefs?.getString("user_name", null)
        val phone = prefs?.getString("user_phone", null)
        val pk = prefs?.getString("user_pk", null)
        val pi = prefs?.getString("user_pi", null)
        
        if (id != null && name != null && phone != null) {
            currentUserState.value = UserData(
                id = id,
                name = name,
                phone = phone,
                stellarPublicKey = pk,
                profileImageUrl = pi
            )
        }
    }
    
    private fun saveSession(user: UserData) {
        prefs?.edit()?.apply {
            putString("user_id", user.id)
            putString("user_name", user.name)
            putString("user_phone", user.phone)
            putString("user_pk", user.stellarPublicKey)
            putString("user_pi", user.profileImageUrl)
            apply()
        }
    }


    data class UserData(
        val id: String,
        val name: String,
        val phone: String,
        val stellarPublicKey: String? = null,
        val profileImageUrl: String? = null,
        val walletBalance: Double = 0.0,
        val pundarScore: Int = 800
    )

    suspend fun registerWithPhone(phone: String, fullName: String, mpin: String): Result<Boolean> {
        return try {
            val cleanPhone = phone.trim()
            Log.d(TAG, "registerWithPhone: phone=$cleanPhone")

            // Check if phone number already exists with a timeout
            val existing = kotlinx.coroutines.withTimeout(5000) { usersCollection.document(cleanPhone).get().await() }
            if (existing.exists()) {
                Log.w(TAG, "registerWithPhone: phone already exists")
                return Result.failure(Exception("This mobile number is already registered."))
            }

            // Generate Stellar Wallet
            val keyPair = PayStellarClient.generateKeyPair()
            val publicKey = keyPair.getAccountId()
            val seed = keyPair.getSecretSeed() ?: throw IllegalStateException("Keypair has no seed")

            // Fund account on Testnet via Friendbot (20s timeout for mobile networks)
            try {
                kotlinx.coroutines.withTimeout(20000) {
                    PayStellarClient.fundTestnetAccount(publicKey)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Friendbot funding failed or timed out. Continuing registration.", e)
            }

            // Fetch initial on-chain balance if funded
            val initialBalance = try {
                val bal = PayStellarClient.getXlmBalance(publicKey).getOrNull()
                if (bal != null) bal.toDouble() / com.example.pundarapp.data.stellar.Stroops.PER_XLM else 0.0
            } catch (_: Exception) { 0.0 }

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
                "isCustodial" to true,
                "wallet_balance" to initialBalance,
                "walletBalance" to initialBalance
            )
            kotlinx.coroutines.withTimeout(5000) { usersCollection.document(cleanPhone).set(userData).await() }
            Log.d(TAG, "registerWithPhone: success (initialBalance=$initialBalance)")

            // Auto-login: set session & load balance
            val user = UserData(
                id = cleanPhone,
                name = fullName.trim(),
                phone = cleanPhone,
                stellarPublicKey = publicKey,
                walletBalance = initialBalance
            )
            currentUserState.value = user
            saveSession(user)
            AppState.loadUserBalance(cleanPhone, initialBalance)

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
            val doc = kotlinx.coroutines.withTimeout(5000) { usersCollection.document(cleanPhone).get().await() }

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
            val profileImage = doc.getString("profile_image_url")
            val balance = doc.getDouble("wallet_balance")
                ?: doc.getDouble("walletBalance")
                ?: 0.0

            val user = UserData(
                id = cleanPhone,
                name = name,
                phone = cleanPhone,
                stellarPublicKey = publicKey,
                profileImageUrl = profileImage,
                walletBalance = balance,
                pundarScore = doc.getLong("pundar_score")?.toInt() ?: 800
            )
            currentUserState.value = user
            saveSession(user)
            AppState.loadUserBalance(cleanPhone, balance)
            Log.d(TAG, "loginWithPhone: success (balance=$balance)")

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

    fun isUserLoggedIn(): Boolean = currentUserState.value != null

    fun getCurrentUserId(): String? = currentUserState.value?.id

    fun getCurrentUserName(): String = currentUserState.value?.name ?: "User"

    fun getCurrentUserPhone(): String = currentUserState.value?.phone ?: ""

    fun getCurrentUserStellarPublicKey(): String? = currentUserState.value?.stellarPublicKey

    fun getCurrentUserProfileImageUrl(): String? = currentUserState.value?.profileImageUrl

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

    suspend fun changeMpin(currentMpin: String, newMpin: String, confirmMpin: String): Result<Boolean> {
        return try {
            if (currentMpin.length != 4) {
                return Result.failure(Exception("Current PIN must be 4 digits."))
            }
            if (newMpin.length != 4) {
                return Result.failure(Exception("New PIN must be 4 digits."))
            }
            if (newMpin != confirmMpin) {
                return Result.failure(Exception("New PIN and confirmation do not match."))
            }
            if (newMpin == currentMpin) {
                return Result.failure(Exception("New PIN must be different from your current PIN."))
            }

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
                return Result.failure(Exception("Incorrect current PIN."))
            }

            usersCollection.document(phone).update("mpin", newMpin.trim()).await()
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "changeMpin failed", e)
            val friendly = mapFirestoreError(e)
            Result.failure(Exception(friendly, e))
        }
    }

    suspend fun verifyMpin(mpin: String): Result<Boolean> {
        return try {
            val phone = getCurrentUserPhone()
            if (phone.isBlank()) return Result.failure(Exception("User not logged in."))

            val doc = usersCollection.document(phone).get().await()
            if (!doc.exists()) return Result.failure(Exception("Account not found."))

            val storedMpin = doc.getString("mpin") ?: ""
            if (storedMpin != mpin.trim()) {
                return Result.failure(Exception("Incorrect PIN."))
            }
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "verifyMpin failed", e)
            val friendly = mapFirestoreError(e)
            Result.failure(Exception(friendly, e))
        }
    }

    fun logout() {
        currentUserState.value = null
        AppState.clearSession()
        prefs?.edit()?.clear()?.apply()
    }

    suspend fun updateProfileImageUrl(imageUrl: String): Result<Boolean> {
        return try {
            val phone = getCurrentUserPhone()
            if (phone.isBlank()) return Result.failure(Exception("User not logged in."))
            val trimmed = imageUrl.trim()
            usersCollection.document(phone).update("profile_image_url", trimmed).await()
            currentUserState.value = currentUserState.value?.copy(profileImageUrl = trimmed.ifBlank { null })
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "updateProfileImageUrl failed", e)
            Result.failure(e)
        }
    }

    suspend fun searchUsers(query: String): List<UserData> {
        if (query.isBlank()) return emptyList()
        val lowerQuery = query.lowercase()

        return try {
            val snapshot = kotlinx.coroutines.withTimeout(5000) { usersCollection.get().await() }
            val dbUsers = snapshot.documents.mapNotNull { doc ->
                val phone = doc.getString("phone_number") ?: return@mapNotNull null
                val name = doc.getString("full_name") ?: return@mapNotNull null
                
                if (phone == currentUserState.value?.id) return@mapNotNull null
                
                if (phone.contains(lowerQuery) || name.lowercase().contains(lowerQuery)) {
                    val stellarPublicKey = doc.getString("stellarPublicKey")
                    UserData(id = phone, name = name, phone = phone, stellarPublicKey = stellarPublicKey)
                } else {
                    null
                }
            }
            dbUsers.distinctBy { it.id }
        } catch (e: Exception) {
            Log.e(TAG, "searchUsers failed or timed out", e)
            emptyList()
        }
    }

    /**
     * Exact lookup of a REGISTERED PUNDAR account by phone number (any common format)
     * or Stellar public key. Returns null if no such registered account exists.
     */
    suspend fun findRegisteredUser(identifier: String): UserData? {
        val raw = identifier.trim()
        if (raw.isBlank()) return null
        return try {
            kotlinx.coroutines.withTimeout(8000) {
                fun toUser(doc: com.google.firebase.firestore.DocumentSnapshot): UserData? {
                    if (!doc.exists()) return null
                    val phone = doc.getString("phone_number") ?: doc.id
                    val name = doc.getString("full_name") ?: return null
                    return UserData(
                        id = doc.id,
                        name = name,
                        phone = phone,
                        stellarPublicKey = doc.getString("stellarPublicKey")
                    )
                }

                if (raw.startsWith("G") && raw.length == 56) {
                    val snap = usersCollection.whereEqualTo("stellarPublicKey", raw).limit(1).get().await()
                    return@withTimeout snap.documents.firstOrNull()?.let { toUser(it) }
                }

                val digits = raw.filter { it.isDigit() }
                val national = when {
                    digits.startsWith("63") && digits.length >= 12 -> digits.drop(2)
                    digits.startsWith("0") -> digits.drop(1)
                    else -> digits
                }
                val candidates = linkedSetOf(raw, digits, national, "0$national", "63$national", "+63$national")
                    .filter { it.isNotBlank() }
                for (candidate in candidates) {
                    val user = toUser(usersCollection.document(candidate).get().await())
                    if (user != null) return@withTimeout user
                }
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "findRegisteredUser failed", e)
            null
        }
    }
}
