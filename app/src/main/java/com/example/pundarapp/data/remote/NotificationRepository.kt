package com.example.pundarapp.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class AppNotification(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val kind: String = "INFO"  // SAVINGS | GOAL | INVESTMENT | BILL | SECURITY | SYSTEM
)

/**
 * Loads and updates user notifications. The collection is keyed by user phone
 * and a sub-collection "notifications" holds individual items.
 *
 * Schema (per document):
 *   title: String
 *   message: String
 *   kind: String
 *   created_at: Long (epoch ms)
 *   is_read: Boolean
 */
object NotificationRepository {
    private const val TAG = "NotificationRepository"
    private val db = FirebaseFirestore.getInstance()

    fun collection(userId: String) =
        db.collection("users").document(userId).collection("notifications")

    @Suppress("UNCHECKED_CAST")
    suspend fun fetchAll(userId: String): Result<List<AppNotification>> = try {
        val byId = linkedMapOf<String, AppNotification>()

        runCatching {
            val snap = collection(userId)
                .orderBy("created_at", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
            snap.documents.forEach { d ->
                val title = d.getString("title") ?: return@forEach
                val message = d.getString("message") ?: return@forEach
                byId[d.id] = AppNotification(
                    id = d.id,
                    title = title,
                    message = message,
                    timestamp = d.getLong("created_at") ?: 0L,
                    isRead = d.getBoolean("is_read") ?: false,
                    kind = d.getString("kind") ?: "INFO"
                )
            }
        }

        // Also read fallback notifications embedded in users/{userId}.user_notifications
        runCatching {
            val userDoc = db.collection("users").document(userId).get().await()
            val rawMap = userDoc.get("user_notifications") as? Map<String, Any>
            rawMap?.forEach { (nid, raw) ->
                val m = raw as? Map<String, Any> ?: return@forEach
                val title = m["title"] as? String ?: return@forEach
                val message = m["message"] as? String ?: return@forEach
                if (!byId.containsKey(nid)) {
                    byId[nid] = AppNotification(
                        id = nid,
                        title = title,
                        message = message,
                        timestamp = (m["created_at"] as? Number)?.toLong() ?: 0L,
                        isRead = m["is_read"] as? Boolean ?: false,
                        kind = m["kind"] as? String ?: "INFO"
                    )
                }
            }
        }

        Result.success(byId.values.sortedByDescending { it.timestamp })
    } catch (e: Exception) {
        Log.e(TAG, "fetchAll failed (will return empty list)", e)
        Result.success(emptyList())
    }

    suspend fun markRead(userId: String, notificationId: String): Result<Boolean> = try {
        runCatching { collection(userId).document(notificationId).update("is_read", true).await() }
        runCatching {
            db.collection("users").document(userId)
                .update("user_notifications.$notificationId.is_read", true)
                .await()
        }
        Result.success(true)
    } catch (e: Exception) {
        Log.e(TAG, "markRead failed", e)
        Result.failure(e)
    }

    suspend fun markAllRead(userId: String): Result<Boolean> = try {
        runCatching {
            val unread = collection(userId).whereEqualTo("is_read", false).get().await()
            val batch = db.batch()
            unread.documents.forEach { d -> batch.update(d.reference, "is_read", true) }
            batch.commit().await()
        }
        runCatching {
            val userDoc = db.collection("users").document(userId).get().await()
            @Suppress("UNCHECKED_CAST")
            val rawMap = userDoc.get("user_notifications") as? Map<String, Any>
            if (!rawMap.isNullOrEmpty()) {
                val updates = mutableMapOf<String, Any>()
                rawMap.keys.forEach { nid -> updates["user_notifications.$nid.is_read"] = true }
                db.collection("users").document(userId).update(updates).await()
            }
        }
        Result.success(true)
    } catch (e: Exception) {
        Log.e(TAG, "markAllRead failed", e)
        Result.failure(e)
    }

    /** Seed demo notifications when Firestore collection is empty. */
    suspend fun seedDefaultsIfEmpty(userId: String) {
        val existing = fetchAll(userId).getOrDefault(emptyList())
        if (existing.isNotEmpty()) return
        val now = System.currentTimeMillis()
        val defaults = listOf(
            hashMapOf("title" to "Savings Update", "message" to "Your wallet balance was refreshed.", "kind" to "SAVINGS", "created_at" to now - 3_600_000, "is_read" to false),
            hashMapOf("title" to "Goal Reached!", "message" to "You've reached your Boracay Trip savings goal.", "kind" to "GOAL", "created_at" to now - 7_200_000, "is_read" to false),
            hashMapOf("title" to "Investment Update", "message" to "Your PH Equities portfolio gained 1.2% today.", "kind" to "INVESTMENT", "created_at" to now - 86_400_000, "is_read" to true),
            hashMapOf("title" to "Bill Reminder", "message" to "Dinner split of ₱1,200 is due tomorrow.", "kind" to "BILL", "created_at" to now - 172_800_000, "is_read" to false),
            hashMapOf("title" to "Security Alert", "message" to "New login detected on your account.", "kind" to "SECURITY", "created_at" to now - 259_200_000, "is_read" to true),
            hashMapOf("title" to "System Announcement", "message" to "QR payments are now available in PUNDAR.", "kind" to "SYSTEM", "created_at" to now - 345_600_000, "is_read" to false)
        )
        try {
            val batch = db.batch()
            defaults.forEach { data ->
                batch.set(collection(userId).document(), data)
            }
            batch.commit().await()
        } catch (e: Exception) {
            Log.w(TAG, "seedDefaultsIfEmpty skipped: ${e.message}")
        }
    }

    suspend fun sendNotification(userId: String, title: String, message: String, kind: String = "INFO"): Result<Boolean> {
        return try {
            val cleanId = userId.trim()
            if (cleanId.isBlank()) return Result.success(false)
            val notifId = "ntf_${System.currentTimeMillis()}_${(1000..9999).random()}"
            val data = hashMapOf(
                "title" to title,
                "message" to message,
                "kind" to kind,
                "created_at" to System.currentTimeMillis(),
                "is_read" to false
            )
            runCatching {
                collection(cleanId).document(notifId).set(data).await()
            }
            runCatching {
                db.collection("users").document(cleanId).set(
                    mapOf("user_notifications" to mapOf(notifId to data)),
                    com.google.firebase.firestore.SetOptions.merge()
                ).await()
            }
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "sendNotification failed", e)
            Result.failure(e)
        }
    }
}
