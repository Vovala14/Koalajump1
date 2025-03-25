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
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.lavrik.koalajump.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Helper class for Google Sign-In integration with Firebase
 */
class GoogleSignInHelper(
    private val context: Context,
    private val activity: ComponentActivity
) {
    companion object {
        private const val TAG = "GoogleSignInHelper"

        // Status codes for Google Sign In
        private const val STATUS_SIGN_IN_CANCELLED = 12501
        private const val STATUS_SIGN_IN_CURRENTLY_IN_PROGRESS = 12502
        private const val STATUS_SIGN_IN_FAILED = 12500
    }

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val googleSignInClient: GoogleSignInClient
    private val signInLauncher: ActivityResultLauncher<Intent>
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

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
                onSignInFailureListener?.onSignInFailure("Google sign-in failed: ${getSignInErrorMessage(e.statusCode)}")
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
     * Check if the user is currently signed in with a Google account
     */
    fun isSignedIn(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return account != null && auth.currentUser != null
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
     * Get current user email
     */
    fun getUserEmail(): String? {
        return auth.currentUser?.email
    }

    /**
     * Get friendly error message based on Google Sign-In error code
     */
    private fun getSignInErrorMessage(statusCode: Int): String {
        return when (statusCode) {
            STATUS_SIGN_IN_CANCELLED -> "Sign-in was cancelled"
            STATUS_SIGN_IN_CURRENTLY_IN_PROGRESS -> "Sign-in is currently in progress"
            STATUS_SIGN_IN_FAILED -> "Sign-in failed"
            CommonStatusCodes.NETWORK_ERROR -> "Network error occurred"
            CommonStatusCodes.DEVELOPER_ERROR -> "Configuration error"
            CommonStatusCodes.INTERNAL_ERROR -> "Internal error occurred"
            CommonStatusCodes.TIMEOUT -> "Connection timed out"
            else -> "Unknown error (code: $statusCode)"
        }
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

                // Save user data to Firestore
                user?.let { firebaseUser ->
                    saveUserToFirestore(
                        firebaseUser.uid,
                        firebaseUser.displayName ?: "Player",
                        firebaseUser.email ?: "",
                        firebaseUser.photoUrl?.toString()
                    )
                }

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

    /**
     * Save user info to Firestore
     */
    private fun saveUserToFirestore(uid: String, displayName: String, email: String, photoUrl: String?) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val userData = hashMapOf(
                    "displayName" to displayName,
                    "email" to email,
                    "photoUrl" to (photoUrl ?: ""),
                    "provider" to "google", // Mark as Google account
                    "lastSignIn" to System.currentTimeMillis(),
                    "totalSignIns" to 1
                )

                // First check if the user already exists
                try {
                    val userDocument = db.collection("users").document(uid).get()

                    // Use Task's addOnSuccessListener instead of await
                    userDocument.addOnSuccessListener { document ->
                        if (document.exists()) {
                            // Update existing user
                            val updates = hashMapOf<String, Any>(
                                "lastSignIn" to System.currentTimeMillis(),
                                "totalSignIns" to (document.getLong("totalSignIns") ?: 0) + 1,
                                "photoUrl" to (photoUrl ?: "")  // Always update photo URL for Google accounts
                            )

                            // Only update display name if it has changed
                            if (displayName != document.getString("displayName")) {
                                updates["displayName"] = displayName
                            }

                            db.collection("users").document(uid).update(updates)
                            Log.d(TAG, "User document updated")
                        } else {
                            // Create new user document
                            db.collection("users").document(uid).set(userData)
                            Log.d(TAG, "User document created")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking user document", e)
                    // Fallback to just creating the document
                    db.collection("users").document(uid).set(userData)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving user to Firestore", e)
            }
        }
    }
}