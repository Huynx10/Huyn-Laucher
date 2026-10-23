package com.example.ui.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

private const val TAG = "GoogleSignInHelper"

object GoogleSignInHelper {

    /**
     * Interactive sign-in invoked when the user taps the "Đăng nhập với Google" button.
     * Uses GetSignInWithGoogleOption exclusively as required by Credential Manager best practices.
     */
    suspend fun signInInteractive(
        activityContext: Context,
        onIdTokenReceived: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val credentialManager = CredentialManager.create(activityContext)
        val webClientId = configuredWebClientId(activityContext) ?: run {
            Log.e(TAG, "Missing default_web_client_id string resource")
            onError("Chưa cấu hình OAuth Web Client ID cho Google Sign-In.")
            return
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = webClientId)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        try {
            val result = credentialManager.getCredential(
                request = request,
                context = activityContext
            )
            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                onIdTokenReceived(googleIdTokenCredential.idToken)
            } else {
                onError("Loại chứng chỉ đăng nhập không được hỗ trợ.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.i(TAG, "User cancelled Google Sign-In", e)
        } catch (e: Exception) {
            Log.e(TAG, "Interactive Google Sign-In failed", e)
            onError(e.localizedMessage ?: "Không thể đăng nhập bằng Google. Vui lòng kiểm tra tài khoản Google trên thiết bị.")
        }
    }

    /**
     * Silent auto-sign-in attempt for returning users using GetGoogleIdOption in a separate request.
     */
    suspend fun trySilentSignIn(
        activityContext: Context,
        onIdTokenReceived: (String) -> Unit
    ) {
        val credentialManager = CredentialManager.create(activityContext)
        val webClientId = configuredWebClientId(activityContext) ?: return

        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(webClientId)
            .setFilterByAuthorizedAccounts(true)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        try {
            val result = credentialManager.getCredential(
                request = request,
                context = activityContext
            )
            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                onIdTokenReceived(googleIdTokenCredential.idToken)
            }
        } catch (_: NoCredentialException) {
            // Silent sign-in not available; user can tap interactive Sign-In button
        } catch (e: Exception) {
            Log.d(TAG, "Silent sign-in skipped: ${e.message}")
        }
    }

    /**
     * Clears Credential Manager state on sign-out so the account picker appears on next sign-in.
     */
    suspend fun clearCredentialSession(context: Context) {
        try {
            val credentialManager = CredentialManager.create(context)
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clear credential manager state", e)
        }
    }

    private fun configuredWebClientId(context: Context): String? {
        val resourceId = context.resources.getIdentifier(
            "default_web_client_id", "string", context.packageName
        )
        if (resourceId == 0) return null
        return runCatching { context.getString(resourceId).takeIf(String::isNotBlank) }.getOrNull()
    }
}
