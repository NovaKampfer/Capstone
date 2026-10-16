package com.MatthewBaerg.multitasked.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.MatthewBaerg.multitasked.util.Result

/**
 * Combines login and registration into one screen.
 * Talks to AuthViewModel and reacts to authState changes.
 */
@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthSuccess: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoginMode by remember { mutableStateOf(true) }

    // When auth succeeds, trigger navigation.
    LaunchedEffect(authState) {
        if (authState is Result.Success) {
            onAuthSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (isLoginMode) "Login" else "Register",
                style = MaterialTheme.typography.headlineMedium
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            if (authState is Result.Error) {
                Text(
                    text = (authState as Result.Error).message,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = {
                    if (isLoginMode) {
                        viewModel.login(email, password)
                    } else {
                        viewModel.register(email, password)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                if (authState is Result.Loading) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text(if (isLoginMode) "Login" else "Sign up")
                }
            }

            TextButton(
                onClick = { isLoginMode = !isLoginMode },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    if (isLoginMode)
                        "Need an account? Register"
                    else
                        "Already have an account? Login"
                )
            }
        }
    }
}
