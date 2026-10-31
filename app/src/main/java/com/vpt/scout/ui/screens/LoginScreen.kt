package com.vpt.scout.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation

@Composable
fun LoginScreen(
    initialEmail: String,
    isLoading: Boolean,
    errorMessage: String?,
    onLogin: (email: String, password: String) -> Unit,
    infoMessage: String? = null,
    onForgotPassword: (email: String) -> Unit = {},
    onCreateAccount: (email: String) -> Unit = {}
) {
    var email by rememberSaveable { mutableStateOf(initialEmail) }
    var password by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(initialEmail) {
        if (initialEmail.isNotBlank() && email.isBlank()) {
            email = initialEmail
        }
    }

    AuthCard {
        Text(
            text = "Sign in to BARN",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "BARN Scout requires a subscription. New accounts get a 30-day free trial.",
            style = MaterialTheme.typography.bodyMedium
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        if (!infoMessage.isNullOrBlank()) {
            Text(
                text = infoMessage,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (!errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Button(
            onClick = { onLogin(email.trim(), password) },
            enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isLoading) "Signing in..." else "Sign In")
        }

        TextButton(
            onClick = { onForgotPassword(email.trim()) },
            enabled = !isLoading,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Forgot password?")
        }

        TextButton(
            onClick = { onCreateAccount(email.trim()) },
            enabled = !isLoading,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("New here? Create an account")
        }
    }
}
