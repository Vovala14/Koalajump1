package com.lavrik.koalajump.auth

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.lavrik.koalajump.R
import kotlinx.coroutines.tasks.await

/**
 * Helper class for Google Sign-In integration
 */
class GoogleSignInHelper(
    private val context: Context,
    private val activity: ComponentActivity
) {
    companion object {
        private const val TAG = "GoogleSignInHelper"
    }

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val googleSignInClient: GoogleSignInClient
    private val signInLauncher: ActivityResultLauncher<Intent>

    // Callback interfaces
    interface OnSignInSuccessListener {
        fun onSignInSuccess(displayName: String?, photoUrl: String?)
    }

    interface OnSignInFailureListener {
        fun onSignInFailure(errorMessage: String)
    }

    // Listeners
    private var onSignInSuccessListener: OnSignInSuccessListener? = null
    private var onSignInFailureListener: OnSignInFailureListener? = null

    init {
        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(context, gso)

        // Register for activity result
        signInLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                Log.w(TAG, "Google sign in failed", e)
                onSignInFailureListener?.onSignInFailure("Sign-in failed: ${e.statusCode}")
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected sign in error", e)
                onSignInFailureListener?.onSignInFailure("Unexpected error: ${e.message}")
            }
        }
    }

    /**
     * Set success listener
     */
    fun setOnSignInSuccessListener(listener: OnSignInSuccessListener) {
        onSignInSuccessListener = listener
    }

    /**
     * Set failure listener
     */
    fun setOnSignInFailureListener(listener: OnSignInFailureListener) {
        onSignInFailureListener = listener
    }

    /**
     * Start Google sign-in flow
     */
    fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    /**
     * Sign out from Google
     */
    fun signOut(onComplete: () -> Unit = {}) {
        auth.signOut()
        googleSignInClient.signOut().addOnCompleteListener {
            Log.d(TAG, "User signed out")
            onComplete()
        }
    }

    /**
     * Check if user is currently signed in
     */
    fun isSignedIn(): Boolean {
        return auth.currentUser != null
    }

    /**
     * Get current user display name
     */
    fun getUserDisplayName(): String? {
        return auth.currentUser?.displayName
    }

    /**
     * Get current user photo URL
     */
    fun getUserPhotoUrl(): String? {
        return auth.currentUser?.photoUrl?.toString()
    }

    /**
     * Get current user ID
     */
    fun getUserId(): String? {
        return auth.currentUser?.uid
    }

    /**
     * Firebase authentication with Google credentials
     */
    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        Log.d(TAG, "firebaseAuthWithGoogle: ${account.id}")
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)

        auth.signInWithCredential(credential)
            .addOnSuccessListener {
                Log.d(TAG, "signInWithCredential:success")
                val user = auth.currentUser
                onSignInSuccessListener?.onSignInSuccess(
                    user?.displayName,
                    user?.photoUrl?.toString()
                )
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "signInWithCredential:failure", e)
                onSignInFailureListener?.onSignInFailure("Authentication failed: ${e.message}")
            }
    }
}