package com.lavrik.koalajump.auth

import android.content.Context
import android.util.Log
import android.util.Patterns
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest

/**
 * Helper class for Email Sign-In integration
 */
class EmailSignInHelper(private val context: Context) {
    companion object {
        private const val TAG = "EmailSignInHelper"
        private const val MIN_PASSWORD_LENGTH = 6
    }

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // Callback interfaces
    interface OnSignInSuccessListener {
        fun onSignInSuccess(displayName: String?, photoUrl: String?)
    }

    interface OnSignInFailureListener {
        fun onSignInFailure(errorMessage: String)
    }

    interface OnSignUpSuccessListener {
        fun onSignUpSuccess(email: String)
    }

    interface OnSignUpFailureListener {
        fun onSignUpFailure(errorMessage: String)
    }

    // Listeners
    private var onSignInSuccessListener: OnSignInSuccessListener? = null
    private var onSignInFailureListener: OnSignInFailureListener? = null
    private var onSignUpSuccessListener: OnSignUpSuccessListener? = null
    private var onSignUpFailureListener: OnSignUpFailureListener? = null

    /**
     * Set sign-in success listener
     */
    fun setOnSignInSuccessListener(listener: OnSignInSuccessListener) {
        onSignInSuccessListener = listener
    }

    /**
     * Set sign-in failure listener
     */
    fun setOnSignInFailureListener(listener: OnSignInFailureListener) {
        onSignInFailureListener = listener
    }

    /**
     * Set sign-up success listener
     */
    fun setOnSignUpSuccessListener(listener: OnSignUpSuccessListener) {
        onSignUpSuccessListener = listener
    }

    /**
     * Set sign-up failure listener
     */
    fun setOnSignUpFailureListener(listener: OnSignUpFailureListener) {
        onSignUpFailureListener = listener
    }

    /**
     * Sign in with email and password
     */
    fun signIn(email: String, password: String) {
        // Validate inputs
        if (!isEmailValid(email)) {
            onSignInFailureListener?.onSignInFailure("Invalid email format")
            return
        }

        if (password.isEmpty()) {
            onSignInFailureListener?.onSignInFailure("Password cannot be empty")
            return
        }

        // Attempt sign in
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                Log.d(TAG, "signInWithEmail:success")
                val user = auth.currentUser
                onSignInSuccessListener?.onSignInSuccess(
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
                onSignInFailureListener?.onSignInFailure(errorMessage)
            }
    }

    /**
     * Sign up with email, password and display name
     */
    fun signUp(email: String, password: String, displayName: String) {
        // Validate inputs
        if (!isEmailValid(email)) {
            onSignUpFailureListener?.onSignUpFailure("Invalid email format")
            return
        }

        if (password.length < MIN_PASSWORD_LENGTH) {
            onSignUpFailureListener?.onSignUpFailure("Password must be at least $MIN_PASSWORD_LENGTH characters")
            return
        }

        if (displayName.isEmpty()) {
            onSignUpFailureListener?.onSignUpFailure("Display name cannot be empty")
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
                        onSignUpSuccessListener?.onSignUpSuccess(email)
                    }
                    ?.addOnFailureListener { e ->
                        Log.w(TAG, "Failed to update display name", e)
                        // Still consider signup successful even if profile update fails
                        onSignUpSuccessListener?.onSignUpSuccess(email)
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
                onSignUpFailureListener?.onSignUpFailure(errorMessage)
            }
    }

    /**
     * Sign out current user
     */
    fun signOut(onComplete: () -> Unit = {}) {
        auth.signOut()
        Log.d(TAG, "User signed out")
        onComplete()
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
     * Validate email format
     */
    private fun isEmailValid(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}