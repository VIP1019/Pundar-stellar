package com.example.pundarapp.data.remote

import com.example.pundarapp.ui.data.HomeActivity
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable

@Serializable
data class HomeActivityDto(
    val id: String,
    val user_id: String,
    val icon: String,
    val title: String,
    val subtitle: String,
    val amount: String,
    val is_positive: Boolean,
    val module: String,
    val created_at: String
) {
    fun toHomeActivity(): HomeActivity {
        return HomeActivity(
            icon = icon,
            title = title,
            subtitle = subtitle,
            amount = amount,
            isPositive = is_positive,
            module = module
        )
    }
}

object HomeRepository {
    private val client = Supabase.client

    suspend fun getRecentActivities(userId: String): Result<List<HomeActivity>> {
        return try {
            val dtos = client.postgrest["home_activities"]
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<HomeActivityDto>()
            
            // Sort by descending created_at
            val sortedDtos = dtos.sortedByDescending { it.created_at }
            
            Result.success(sortedDtos.map { it.toHomeActivity() })
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
