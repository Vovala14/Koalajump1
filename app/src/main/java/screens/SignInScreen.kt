package com.lavrik.koalajump.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.lavrik.koalajump.GamePreferences
import com.lavrik.koalajump.GameState
import com.lavrik.koalajump.auth.AuthHelper
import com.lavrik.koalajump.ui.components.AnimatedCloudsBackground

/**
 * Authentication screen that allows users to sign in with multiple methods
 * to save their progress and scores
 */
@Composable
fun SignInScreen(
    gameState: GameState,
    navController: NavController,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val gamePreferences = remember { GamePreferences(context) }
    val focusManager = LocalFocusManager.current

    // UI state
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }

    // Auth helper
    val authHelper = remember {
        AuthHelper(context, context as androidx.activity.ComponentActivity).apply {
            setOnAuthSuccessListener(object : AuthHelper.OnAuthSuccessListener {
                override fun onAuthSuccess(displayName: String?, photoUrl: String?) {
                    // Handle successful sign-in
                    gamePreferences.setSignedIn(true)
                    gamePreferences.setDisplayName(displayName ?: "Player")
                    onClose()
                }
            })

            setOnAuthFailureListener(object : AuthHelper.OnAuthFailureListener {
                override fun onAuthFailure(errorMsg: String) {
                    // Show error message
                    errorMessage = errorMsg
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
                .padding(16.dp),
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
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                if (successMessage.isNotEmpty()) {
                    Text(
                        text = successMessage,
                        color = Color(0xFF4CAF50),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Email field
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        errorMessage = ""
                        successMessage = ""
                    },
                    label = { Text("Email") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
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
                    singleLine = true
                )

                // Password field
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = ""
                        successMessage = ""
                    },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = if (isSignUp) 8.dp else 16.dp),
                    visualTransformation = PasswordVisualTransformation(),
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
                                authHelper.signInWithEmail(email, password)
                            }
                        }
                    ),
                    singleLine = true
                )

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
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Display Name") },
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
                                authHelper.signUpWithEmail(email, password, displayName)
                            }
                        ),
                        singleLine = true
                    )
                }

                // Sign in/up button
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        if (isSignUp) {
                            authHelper.signUpWithEmail(email, password, displayName)
                        } else {
                            authHelper.signInWithEmail(email, password)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    border = null // FIXED: Remove border to fix background color issue
                ) {
                    Text(
                        text = if (isSignUp) "Create Account" else "Sign In",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Toggle between sign in and sign up
                TextButton(
                    onClick = {
                        isSignUp = !isSignUp
                        errorMessage = ""
                        successMessage = ""
                    }
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
                    onClick = { authHelper.signInWithGoogle() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4285F4) // Google blue
                    ),
                    border = null // FIXED: Remove border to fix background color issue
                ) {
                    Text(
                        text = "Sign in with Google",
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Skip sign in button
                TextButton(
                    onClick = onClose
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