package com.lavrik.koalajump

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
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Interface between the game and external services like Firebase
 */
class GameInterface(private val context: Context, private val activity: ComponentActivity) {

    private val TAG = "GameInterface"
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private lateinit var googleSignInClient: GoogleSignInClient
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    // Auth related properties
    val isSignedIn: Boolean
        get() = auth.currentUser != null

    val uid: String?
        get() = auth.currentUser?.uid

    val displayName: String?
        get() = auth.currentUser?.displayName

    val photoUrl: String?
        get() = auth.currentUser?.photoUrl?.toString()

    // Initialize Google Sign-in
    private lateinit var signInLauncher: ActivityResultLauncher<Intent>

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
            coroutineScope.launch {
                try {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    val account = task.getResult(ApiException::class.java)
                    val user = firebaseAuthWithGoogle(account)
                    if (user != null) {
                        onSignInWithGoogle(user)
                    } else {
                        onSignInError("Authentication failed")
                    }
                } catch (e: ApiException) {
                    Log.w(TAG, "Google sign in failed", e)
                    onSignInError("Google sign-in error: ${e.message}")
                } catch (e: Exception) {
                    Log.e(TAG, "Sign in error", e)
                    onSignInError("Sign-in error: ${e.message}")
                }
            }
        }
    }

    // Sign in with Google
    fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    // Private method to authenticate with Firebase using Google credentials
    private suspend fun firebaseAuthWithGoogle(account: GoogleSignInAccount): FirebaseUser? {
        Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)

        return try {
            val authResult = auth.signInWithCredential(credential).await()
            Log.d(TAG, "signInWithCredential:success")
            authResult.user
        } catch (e: Exception) {
            Log.w(TAG, "signInWithCredential:failure", e)
            null
        }
    }

    // Sign out
    fun onSignOut() {
        auth.signOut()
        googleSignInClient.signOut()
        Log.d(TAG, "User signed out")
    }

    // Submit score to Firebase
    fun submitScore(score: Int, characterId: Int) {
        if (!isSignedIn) {
            Log.d(TAG, "Cannot submit score: User not signed in")
            onScoreSubmitError("User not signed in")
            return
        }

        val playerName = displayName ?: "Anonymous"

        coroutineScope.launch {
            try {
                val scoreData = hashMapOf(
                    "playerName" to playerName,
                    "score" to score,
                    "characterId" to characterId,
                    "timestamp" to System.currentTimeMillis(),
                    "uid" to uid
                )

                db.collection("high_scores")
                    .add(scoreData)
                    .await()

                withContext(Dispatchers.Main) {
                    onScoreSubmitted(score)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error submitting score", e)
                withContext(Dispatchers.Main) {
                    onScoreSubmitError("Error: ${e.message}")
                }
            }
        }
    }

    // Get high scores
    fun fetchHighScores(callback: (List<Map<String, Any>>) -> Unit) {
        coroutineScope.launch {
            try {
                val result = db.collection("high_scores")
                    .orderBy("score", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(10)
                    .get()
                    .await()

                val highScores = result.documents.mapNotNull { it.data }

                withContext(Dispatchers.Main) {
                    callback(highScores)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching high scores", e)
                withContext(Dispatchers.Main) {
                    callback(emptyList())
                }
            }
        }
    }

    // Callback methods
    fun onSignInWithGoogle(user: FirebaseUser) {
        Log.d(TAG, "Signed in as ${user.displayName}")
        // Implement any additional logic you need after sign-in
    }

    fun onSignInError(errorMsg: String) {
        Log.e(TAG, "Sign in error: $errorMsg")
        // Handle sign-in errors
    }

    fun onScoreSubmitted(score: Int) {
        Log.d(TAG, "Score submitted: $score")
        // Handle successful score submission
    }

    fun onScoreSubmitError(errorMsg: String) {
        Log.e(TAG, "Score submission error: $errorMsg")
        // Handle score submission errors
    }
}