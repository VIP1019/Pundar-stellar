package com.example.pundarapp.data.remote

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import com.example.pundarapp.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken

object GoogleSignInHelper {

    suspend fun signInWithGoogle(activity: Activity): Result<Boolean> {
        return try {
            val credentialManager = CredentialManager.create(activity)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                .setFilterByAuthorizedAccounts(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val credentialResponse = credentialManager.getCredential(activity, request)
            val credential = credentialResponse.credential

            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                // Sign in to Supabase with the Google ID token
                Supabase.client.auth.signInWith(IDToken) {
                    this.provider = Google
                    this.idToken = idToken
                }

                Result.success(true)
            } else {
                Result.failure(Exception("Unexpected credential type"))
            }
        } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
            // User cancelled the picker - not an error
            Result.failure(Exception("cancelled"))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
