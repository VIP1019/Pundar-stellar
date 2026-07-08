package com.example.pundarapp.data.remote

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email

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

    suspend fun register(email: String, password: String): Result<Boolean> {
        return try {
            auth.signUpWith(Email) {
                this.email = email
                this.password = password
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
