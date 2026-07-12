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

    suspend fun fetchAll(userId: String): Result<List<AppNotification>> = try {
        val snap = collection(userId)
            .orderBy("created_at", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .await()
        val list = snap.documents.mapNotNull { d ->
            val title = d.getString("title") ?: return@mapNotNull null
            val message = d.getString("message") ?: return@mapNotNull null
            AppNotification(
                id = d.id,
                title = title,
                message = message,
                timestamp = d.getLong("created_at") ?: 0L,
                isRead = d.getBoolean("is_read") ?: false,
                kind = d.getString("kind") ?: "INFO"
            )
        }
        Result.success(list)
    } catch (e: Exception) {
        Log.e(TAG, "fetchAll failed (will return empty list)", e)
        // If the collection doesn't exist yet, treat as empty (not an error).
        Result.success(emptyList())
    }

    suspend fun markRead(userId: String, notificationId: String): Result<Boolean> = try {
        collection(userId).document(notificationId).update("is_read", true).await()
        Result.success(true)
    } catch (e: Exception) {
        Log.e(TAG, "markRead failed", e)
        Result.failure(e)
    }

    suspend fun markAllRead(userId: String): Result<Boolean> = try {
        val unread = collection(userId).whereEqualTo("is_read", false).get().await()
        val batch = db.batch()
        unread.documents.forEach { d -> batch.update(d.reference, "is_read", true) }
        batch.commit().await()
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
}
