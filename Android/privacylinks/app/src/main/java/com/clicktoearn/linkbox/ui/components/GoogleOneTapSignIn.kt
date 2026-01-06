package com.clicktoearn.linkbox.ui.components

import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.clicktoearn.linkbox.R
import com.clicktoearn.linkbox.viewmodel.LinkBoxViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.GoogleAuthProvider
import com.clicktoearn.linkbox.utils.findActivity
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.CancellationException


/**
 * Google One Tap Sign-In handler using Credential Manager API
 * This provides a seamless authentication experience with auto-selected Google account
 */
@Composable
fun GoogleOneTapSignIn(
    viewModel: LinkBoxViewModel,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    onDismiss: () -> Unit,
    trigger: Boolean = false
) {
    val context = LocalContext.current
    val credentialManager = remember { CredentialManager.create(context) }
    
    // Capture latest callbacks to avoid stale references in LaunchedEffect
    val currentOnSuccess by rememberUpdatedState(onSuccess)
    val currentOnError by rememberUpdatedState(onError)
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    
    LaunchedEffect(trigger) {
        if (!trigger) return@LaunchedEffect
        
        try {
            // Generate a nonce for security
            val rawNonce = UUID.randomUUID().toString()
            val bytes = rawNonce.toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }
            
            // Configure Google ID option for One Tap
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false) // Show all accounts
                .setServerClientId(context.getString(R.string.default_web_client_id))
                .setNonce(hashedNonce)
                .setAutoSelectEnabled(true)
                .build()
            
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            
            Log.d("OneTapSignIn", "Launching One Tap sign-in...")
            
            val activity = context.findActivity()
            if (activity == null) {
                Log.e("OneTapSignIn", "Activity not found, cannot launch One Tap")
                currentOnError("Activity context missing")
                return@LaunchedEffect
            }
            
            val result = credentialManager.getCredential(
                request = request,
                context = activity
            )

            // Handle the credential result
            val credential = result.credential
            
            if (credential is androidx.credentials.CustomCredential && 
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                
                try {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val googleIdToken = googleIdTokenCredential.idToken
                    Log.d("OneTapSignIn", "Successfully received Google ID token")
                    
                    // Sign in to Firebase with the Google ID token
                    val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    viewModel.signInWithGoogle(firebaseCredential) { success, errorMessage ->
                        if (success) {
                            Log.d("OneTapSignIn", "Firebase authentication successful")
                            currentOnSuccess()
                        } else {
                            Log.e("OneTapSignIn", "Firebase authentication failed: $errorMessage")
                            currentOnError(errorMessage ?: "Authentication failed.")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("OneTapSignIn", "Failed to parse Google ID Token Credential", e)
                    currentOnError("Failed to parse credential.")
                }
            } else {
                Log.e("OneTapSignIn", "Unexpected credential type: ${credential::class.java} Type: ${credential.type}")
                currentOnError("Unexpected credential type received.")
            }
            
        } catch (e: GetCredentialException) {
            // detailed logging for debugging "not working" issues
            when (e) {
                is androidx.credentials.exceptions.GetCredentialCancellationException -> {
                    Log.d("OneTapSignIn", "User cancelled One Tap")
                }
                is androidx.credentials.exceptions.NoCredentialException -> {
                    Log.d("OneTapSignIn", "No credentials available for One Tap (showing fallback)")
                }
                else -> {
                    Log.e("OneTapSignIn", "One Tap sign-in failed: ${e.message}", e)
                    // Unexpected error? Show toast?
                }
            }
            // User cancelled or One Tap not available - normal flow to fallback
            currentOnDismiss()
        } catch (e: CancellationException) {
            Log.d("OneTapSignIn", "One Tap sign-in job cancelled")
            throw e
        } catch (e: Exception) {
            Log.e("OneTapSignIn", "Unexpected error during One Tap sign-in", e)
            currentOnError("Sign-in error: ${e.localizedMessage}")
        }
    }
}
