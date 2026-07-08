package com.example.pundarapp.data.remote

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object AuthRepository {
    private val auth = Supabase.client.auth

    suspend fun login(email: String, password: String): Result<Boolean> {
        return try {
            auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun register(email: String, password: String, fullName: String): Result<Boolean> {
        return try {
            auth.signUpWith(Email) {
                this.email = email
                this.password = password
                data = buildJsonObject {
                    put("full_name", fullName)
                }
            }
            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun isUserLoggedIn(): Boolean {
        return auth.currentSessionOrNull() != null
    }

    fun getCurrentUserId(): String? {
        return auth.currentSessionOrNull()?.user?.id
    }

    fun getCurrentUserName(): String {
        val user = auth.currentSessionOrNull()?.user
        val name = user?.userMetadata?.get("full_name")?.toString()?.replace("\"", "")
        return name ?: "User"
    }

    suspend fun logout() {
        auth.signOut()
    }

    suspend fun sendOtp(phone: String): Result<Boolean> {
        return try {
            auth.signInWith(io.github.jan.supabase.auth.providers.builtin.OTP) {
                this.phone = phone
            }
            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun verifyOtp(phone: String, token: String): Result<Boolean> {
        return try {
            auth.verifyPhoneOtp(
                type = io.github.jan.supabase.auth.OtpType.Phone.SMS,
                phone = phone,
                token = token
            )
            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
