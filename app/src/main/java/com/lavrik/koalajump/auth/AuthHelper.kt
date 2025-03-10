package com.lavrik.koalajump.auth

import android.content.Context
import android.content.Intent
import android.util.Log
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.lavrik.koalajump.R

/**
 * Helper class for authentication using Google and Email
 */
class AuthHelper(
    private val context: Context,
    private val activity: ComponentActivity
) {
    companion object {
        private const val TAG = "AuthHelper"
        private const val MIN_PASSWORD_LENGTH = 6
    }

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val googleSignInClient: GoogleSignInClient
    private val signInLauncher: ActivityResultLauncher<Intent>

    // Callback interfaces
    interface OnAuthSuccessListener {
        fun onAuthSuccess(displayName: String?, photoUrl: String?)
    }

    interface OnAuthFailureListener {
        fun onAuthFailure(errorMessage: String)
    }

    // Listeners
    private var onAuthSuccessListener: OnAuthSuccessListener? = null
    private var onAuthFailureListener: OnAuthFailureListener? = null

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
                onAuthFailureListener?.onAuthFailure("Sign-in failed: ${e.statusCode}")
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected sign in error", e)
                onAuthFailureListener?.onAuthFailure("Unexpected error: ${e.message}")
            }
        }
    }

    /**
     * Set success listener
     */
    fun setOnAuthSuccessListener(listener: OnAuthSuccessListener) {
        onAuthSuccessListener = listener
    }

    /**
     * Set failure listener
     */
    fun setOnAuthFailureListener(listener: OnAuthFailureListener) {
        onAuthFailureListener = listener
    }

    /**
     * Start Google sign-in flow
     */
    fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    /**
     * Sign in with email and password
     */
    fun signInWithEmail(email: String, password: String) {
        // Validate inputs
        if (!isEmailValid(email)) {
            onAuthFailureListener?.onAuthFailure("Invalid email format")
            return
        }

        if (password.isEmpty()) {
            onAuthFailureListener?.onAuthFailure("Password cannot be empty")
            return
        }

        // Attempt sign in
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                Log.d(TAG, "signInWithEmail:success")
                val user = auth.currentUser
                onAuthSuccessListener?.onAuthSuccess(
                    user?.displayName,
                    user?.photoUrl?.toString()
                )
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "signInWithEmail:failure", e)
                val errorMessage = when (e) {
                    is FirebaseAuthInvalidCredentialsException -> "Invalid email or password"
                    else -> "Authentication failed: ${e.message}"
                }
                onAuthFailureListener?.onAuthFailure(errorMessage)
            }
    }

    /**
     * Sign up with email, password and display name
     */
    fun signUpWithEmail(email: String, password: String, displayName: String) {
        // Validate inputs
        if (!isEmailValid(email)) {
            onAuthFailureListener?.onAuthFailure("Invalid email format")
            return
        }

        if (password.length < MIN_PASSWORD_LENGTH) {
            onAuthFailureListener?.onAuthFailure("Password must be at least $MIN_PASSWORD_LENGTH characters")
            return
        }

        if (displayName.isEmpty()) {
            onAuthFailureListener?.onAuthFailure("Display name cannot be empty")
            return
        }

        // Create user
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                Log.d(TAG, "createUserWithEmail:success")

                // Update profile with display name
                val user = auth.currentUser
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()

                user?.updateProfile(profileUpdates)
                    ?.addOnSuccessListener {
                        Log.d(TAG, "User profile updated with display name")
                        onAuthSuccessListener?.onAuthSuccess(displayName, null)
                    }
                    ?.addOnFailureListener { e ->
                        Log.w(TAG, "Failed to update display name", e)
                        // Still consider signup successful even if profile update fails
                        onAuthSuccessListener?.onAuthSuccess(displayName, null)
                    }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "createUserWithEmail:failure", e)
                val errorMessage = when (e) {
                    is FirebaseAuthWeakPasswordException -> "Password is too weak"
                    is FirebaseAuthUserCollisionException -> "Account already exists with this email"
                    is FirebaseAuthInvalidCredentialsException -> "Invalid email format"
                    else -> "Registration failed: ${e.message}"
                }
                onAuthFailureListener?.onAuthFailure(errorMessage)
            }
    }

    /**
     * Sign out current user
     */
    fun signOut(onComplete: () -> Unit = {}) {
        auth.signOut()
        googleSignInClient.signOut().addOnCompleteListener {
            Log.d(TAG, "User signed out")
            onComplete()
        }
    }

    /**
     * Reset password for the given email
     */
    fun resetPassword(email: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        if (!isEmailValid(email)) {
            onFailure("Invalid email format")
            return
        }

        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                Log.d(TAG, "Password reset email sent")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Failed to send password reset email", e)
                onFailure("Failed to send reset email: ${e.message}")
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
     * Get current user email
     */
    fun getUserEmail(): String? {
        return auth.currentUser?.email
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
                onAuthSuccessListener?.onAuthSuccess(
                    user?.displayName,
                    user?.photoUrl?.toString()
                )
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "signInWithCredential:failure", e)
                onAuthFailureListener?.onAuthFailure("Authentication failed: ${e.message}")
            }
    }

    /**
     * Validate email format
     */
    private fun isEmailValid(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}