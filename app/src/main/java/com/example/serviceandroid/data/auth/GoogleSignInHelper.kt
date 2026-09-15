package com.example.serviceandroid.data.auth

import android.content.Context
import androidx.annotation.StringRes
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.serviceandroid.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

sealed interface GoogleSignInRequestResult {
    data class IdToken(val value: String) : GoogleSignInRequestResult
    data object Cancelled : GoogleSignInRequestResult
    data class Error(@StringRes val messageRes: Int) : GoogleSignInRequestResult
}

object GoogleSignInHelper {
    suspend fun requestIdToken(context: Context): GoogleSignInRequestResult {
        val clientIdResource = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName,
        )
        if (clientIdResource == 0) {
            return GoogleSignInRequestResult.Error(R.string.auth_config_missing)
        }
        val serverClientId = context.getString(clientIdResource)
        if (serverClientId.isBlank()) {
            return GoogleSignInRequestResult.Error(R.string.auth_config_missing)
        }

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(serverClientId).build())
            .build()

        return try {
            val credential = CredentialManager.create(context)
                .getCredential(context, request)
                .credential
            if (
                credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                GoogleSignInRequestResult.IdToken(
                    GoogleIdTokenCredential.createFrom(credential.data).idToken
                )
            } else {
                GoogleSignInRequestResult.Error(R.string.auth_invalid_credential)
            }
        } catch (_: GetCredentialCancellationException) {
            GoogleSignInRequestResult.Cancelled
        } catch (_: GetCredentialException) {
            GoogleSignInRequestResult.Error(R.string.auth_google_unavailable)
        } catch (_: Exception) {
            GoogleSignInRequestResult.Error(R.string.auth_invalid_credential)
        }
    }
}
