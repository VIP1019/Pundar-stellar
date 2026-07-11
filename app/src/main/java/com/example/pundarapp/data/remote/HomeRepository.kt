package com.example.pundarapp.data.remote

import com.example.pundarapp.ui.data.HomeActivity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object HomeRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getRecentActivities(userId: String): Result<List<HomeActivity>> {
        return try {
            val snapshot = db.collection("home_activities")
                .whereEqualTo("user_id", userId)
                .get()
                .await()
                
            val activities = snapshot.documents.mapNotNull { doc ->
                val icon = doc.getString("icon") ?: return@mapNotNull null
                val title = doc.getString("title") ?: return@mapNotNull null
                val subtitle = doc.getString("subtitle") ?: return@mapNotNull null
                val amount = doc.getString("amount") ?: return@mapNotNull null
                val isPositive = doc.getBoolean("is_positive") ?: false
                val module = doc.getString("module") ?: return@mapNotNull null
                val createdAt = doc.getString("created_at") ?: ""
                
                Pair(
                    createdAt,
                    HomeActivity(
                        icon = icon,
                        title = title,
                        subtitle = subtitle,
                        amount = amount,
                        isPositive = isPositive,
                        module = module
                    )
                )
            }
            
            // Sort by descending created_at
            val sortedActivities = activities.sortedByDescending { it.first }.map { it.second }
            
            Result.success(sortedActivities)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun createActivity(userId: String, activity: HomeActivity): Result<Boolean> {
        return try {
            val data = hashMapOf(
                "user_id" to userId,
                "icon" to activity.icon,
                "title" to activity.title,
                "subtitle" to activity.subtitle,
                "amount" to activity.amount,
                "is_positive" to activity.isPositive,
                "module" to activity.module,
                "created_at" to System.currentTimeMillis().toString()
            )
            db.collection("home_activities").add(data).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
