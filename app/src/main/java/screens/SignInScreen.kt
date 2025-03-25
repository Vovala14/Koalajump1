package screens

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.lavrik.koalajump.GameInterface
import com.lavrik.koalajump.GamePreferences
import com.lavrik.koalajump.GameState
import com.lavrik.koalajump.auth.EmailSignInHelper
import com.lavrik.koalajump.auth.GoogleSignInHelper
import com.lavrik.koalajump.ui.components.AnimatedCloudsBackground
import android.widget.Toast
import androidx.activity.ComponentActivity

private const val TAG = "SignInScreen"

/**
 * Authentication screen that allows users to sign in with multiple methods
 * to save their progress and scores - with improved navigation
 */
@Composable
fun SignInScreen(
    gameState: GameState,
    navController: NavController,
    onClose: () -> Unit
) {
    // Ensure we log entry to this screen
    Log.d(TAG, "SignInScreen being composed")

    val context = LocalContext.current
    val gamePreferences = remember { GamePreferences(context) }
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    // Initialize interfaces
    val gameInterface = remember { GameInterface(context, context as ComponentActivity) }

    // UI state
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }
    var showResetPassword by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // IMPROVED: Safe navigation function for more reliable navigation
    val safeNavigateBack = {
        Log.d(TAG, "Attempting safe navigation back")
        try {
            // First try onClose callback
            onClose()
            Log.d(TAG, "onClose callback executed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "onClose callback failed: ${e.message}", e)
            // Fall back to direct navigation
            try {
                // Navigate back to previous screen
                val success = navController.popBackStack()
                Log.d(TAG, "Direct popBackStack result: $success")

                if (!success) {
                    // Navigate to main menu as fallback
                    navController.navigate("mainMenu") {
                        popUpTo(0)
                    }
                    Log.d(TAG, "Fallback navigation to main menu executed")
                } else {
                    // If popBackStack was successful, log it
                    Log.d(TAG, "Successfully navigated back")
                }
            } catch (e2: Exception) {
                Log.e(TAG, "Direct navigation failed: ${e2.message}")
                // Last resort
                try {
                    navController.navigate("mainMenu")
                    Log.d(TAG, "Last resort navigation attempted")
                } catch (e3: Exception) {
                    Log.e(TAG, "Last resort navigation also failed: ${e3.message}")
                }
            }
        }
    }

    // Auth helpers
    val googleSignInHelper = remember {
        GoogleSignInHelper(context, context as ComponentActivity).apply {
            setOnSignInSuccessListener(object : GoogleSignInHelper.OnSignInSuccessListener {
                override fun onSignInSuccess(displayName: String?, photoUrl: String?) {
                    // Handle successful sign-in
                    gamePreferences.setSignedIn(true)
                    gamePreferences.setDisplayName(displayName ?: "Player")

                    // Submit the score if coming from game over screen
                    val finalScore = gameState.finalScore.value
                    if (finalScore > 0) {
                        gameInterface.submitFinalScore(gameState)
                        Toast.makeText(
                            context,
                            "Score submitted to leaderboard!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    successMessage = "Successfully signed in as $displayName!"
                    isLoading = false

                    // IMPROVED: Return to previous screen after short delay
                    Handler(Looper.getMainLooper()).postDelayed({
                        safeNavigateBack()
                    }, 1500)
                }
            })

            setOnSignInFailureListener(object : GoogleSignInHelper.OnSignInFailureListener {
                override fun onSignInFailure(errorMsg: String) {
                    // Show error message
                    errorMessage = errorMsg
                    isLoading = false
                }
            })
        }
    }

    val emailSignInHelper = remember {
        EmailSignInHelper(context).apply {
            setOnSignInSuccessListener(object : EmailSignInHelper.OnSignInSuccessListener {
                override fun onSignInSuccess(displayName: String?, photoUrl: String?) {
                    // Handle successful sign-in
                    gamePreferences.setSignedIn(true)
                    gamePreferences.setDisplayName(displayName ?: "Player")

                    // Submit the score if coming from game over screen
                    val finalScore = gameState.finalScore.value
                    if (finalScore > 0) {
                        gameInterface.submitFinalScore(gameState)
                        Toast.makeText(
                            context,
                            "Score submitted to leaderboard!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    successMessage = "Successfully signed in as $displayName!"
                    isLoading = false

                    // IMPROVED: Return to previous screen after short delay
                    Handler(Looper.getMainLooper()).postDelayed({
                        safeNavigateBack()
                    }, 1500)
                }
            })

            setOnSignInFailureListener(object : EmailSignInHelper.OnSignInFailureListener {
                override fun onSignInFailure(errorMsg: String) {
                    // Show error message
                    errorMessage = errorMsg
                    isLoading = false
                }
            })

            setOnSignUpSuccessListener(object : EmailSignInHelper.OnSignUpSuccessListener {
                override fun onSignUpSuccess(email: String) {
                    // Handle successful sign-up
                    gamePreferences.setSignedIn(true)
                    gamePreferences.setDisplayName(displayName)

                    successMessage = "Account created successfully! You are now signed in."
                    isLoading = false

                    // IMPROVED: Return to previous screen after short delay
                    Handler(Looper.getMainLooper()).postDelayed({
                        safeNavigateBack()
                    }, 1500)
                }
            })

            setOnSignUpFailureListener(object : EmailSignInHelper.OnSignUpFailureListener {
                override fun onSignUpFailure(errorMsg: String) {
                    // Show error message
                    errorMessage = errorMsg
                    isLoading = false
                }
            })
        }
    }

    // Sky gradient background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF87CEEB),  // Sky blue at top
                        Color(0xFF5D8AA8)   // Deeper blue at bottom
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Animated background clouds
        AnimatedCloudsBackground()

        // Main content card
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp)
                .verticalScroll(scrollState),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.9f)
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = if (isSignUp) "Create Account" else "Sign In",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Subtitle
                Text(
                    text = "Save your progress and compete on leaderboards",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Error or success message
                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = Color.Red,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }

                if (successMessage.isNotEmpty()) {
                    Text(
                        text = successMessage,
                        color = Color(0xFF4CAF50),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }

                if (showResetPassword) {
                    // Password reset UI
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = {
                            resetEmail = it
                            errorMessage = ""
                        },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Filled.Email, contentDescription = "Email") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (resetEmail.isNotEmpty()) {
                                    isLoading = true
                                    emailSignInHelper.resetPassword(
                                        resetEmail,
                                        onSuccess = {
                                            successMessage = "Password reset email sent. Check your inbox."
                                            isLoading = false
                                            // Switch back to sign in after a delay
                                            Handler(Looper.getMainLooper()).postDelayed({
                                                showResetPassword = false
                                            }, 3000)
                                        },
                                        onFailure = { error ->
                                            errorMessage = error
                                            isLoading = false
                                        }
                                    )
                                } else {
                                    errorMessage = "Please enter your email address"
                                }
                            }
                        ),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            if (resetEmail.isNotEmpty()) {
                                isLoading = true
                                emailSignInHelper.resetPassword(
                                    resetEmail,
                                    onSuccess = {
                                        successMessage = "Password reset email sent. Check your inbox."
                                        isLoading = false
                                        // Switch back to sign in after a delay
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            showResetPassword = false
                                        }, 3000)
                                    },
                                    onFailure = { error ->
                                        errorMessage = error
                                        isLoading = false
                                    }
                                )
                            } else {
                                errorMessage = "Please enter your email address"
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        border = null,
                        enabled = !isLoading
                    ) {
                        Text(
                            text = "Send Reset Link",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = {
                            showResetPassword = false
                            errorMessage = ""
                            successMessage = ""
                        }
                    ) {
                        Text(
                            text = "Back to Sign In",
                            color = Color(0xFF4CAF50)
                        )
                    }
                } else {
                    // Regular sign in/up UI
                    // Email field
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            errorMessage = ""
                            successMessage = ""
                        },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Filled.Email, contentDescription = "Email") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        singleLine = true,
                        enabled = !isLoading
                    )

                    // Password field with text-based visibility toggle
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = ""
                            successMessage = ""
                        },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = "Password") },
                        trailingIcon = {
                            // Simple text button instead of icon
                            TextButton(
                                onClick = { passwordVisible = !passwordVisible },
                                modifier = Modifier.padding(0.dp),
                                contentPadding = PaddingValues(4.dp)
                            ) {
                                Text(
                                    text = if (passwordVisible) "Hide" else "Show",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = if (isSignUp) ImeAction.Next else ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                if (isSignUp) {
                                    focusManager.moveFocus(FocusDirection.Down)
                                }
                            },
                            onDone = {
                                focusManager.clearFocus()
                                if (!isSignUp) {
                                    if (email.isNotEmpty() && password.isNotEmpty()) {
                                        isLoading = true
                                        emailSignInHelper.signIn(email, password)
                                    } else {
                                        errorMessage = "Please enter email and password"
                                    }
                                }
                            }
                        ),
                        singleLine = true,
                        enabled = !isLoading
                    )

                    // Forgot password link (only for sign in)
                    if (!isSignUp) {
                        TextButton(
                            onClick = {
                                showResetPassword = true
                                resetEmail = email // Pre-fill with current email
                                errorMessage = ""
                                successMessage = ""
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(
                                text = "Forgot Password?",
                                color = Color(0xFF4CAF50),
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Display name field (only for sign up)
                    if (isSignUp) {
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = {
                                displayName = it
                                errorMessage = ""
                                successMessage = ""
                            },
                            label = { Text("Display Name") },
                            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = "Display Name") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    if (email.isNotEmpty() && password.isNotEmpty() && displayName.isNotEmpty()) {
                                        isLoading = true
                                        emailSignInHelper.signUp(email, password, displayName)
                                    } else {
                                        errorMessage = "Please fill all fields"
                                    }
                                }
                            ),
                            singleLine = true,
                            enabled = !isLoading
                        )
                    }

                    // Sign in/up button
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            if (isSignUp) {
                                // Sign up
                                if (email.isNotEmpty() && password.isNotEmpty() && displayName.isNotEmpty()) {
                                    isLoading = true
                                    emailSignInHelper.signUp(email, password, displayName)
                                } else {
                                    errorMessage = "Please fill all fields"
                                }
                            } else {
                                // Sign in
                                if (email.isNotEmpty() && password.isNotEmpty()) {
                                    isLoading = true
                                    emailSignInHelper.signIn(email, password)
                                } else {
                                    errorMessage = "Please enter email and password"
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        border = null,
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (isSignUp) "Create Account" else "Sign In",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Toggle between sign in and sign up
                    TextButton(
                        onClick = {
                            isSignUp = !isSignUp
                            errorMessage = ""
                            successMessage = ""
                        },
                        enabled = !isLoading
                    ) {
                        Text(
                            text = if (isSignUp) "Already have an account? Sign In" else "Don't have an account? Sign Up",
                            color = Color(0xFF4CAF50)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Divider with "or" text
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Divider(
                            modifier = Modifier.weight(1f),
                            color = Color.Gray.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "or",
                            modifier = Modifier.padding(horizontal = 8.dp),
                            color = Color.Gray
                        )
                        Divider(
                            modifier = Modifier.weight(1f),
                            color = Color.Gray.copy(alpha = 0.5f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Google sign-in button
                    Button(
                        onClick = {
                            isLoading = true
                            googleSignInHelper.signIn()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4285F4) // Google blue
                        ),
                        border = null,
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // You can add a Google icon here if available
                                Text(
                                    text = "Sign in with Google",
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // IMPROVED: Skip sign in button with robust navigation
                    TextButton(
                        onClick = {
                            Log.d(TAG, "Skip Sign In button clicked")
                            safeNavigateBack()
                        },
                        enabled = !isLoading
                    ) {
                        Text(
                            text = "Skip Sign In",
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }

    // Special DisposableEffect to catch back navigation
    DisposableEffect(Unit) {
        onDispose {
            Log.d(TAG, "SignInScreen being disposed")
        }
    }
}