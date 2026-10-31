package com.vpt.scout.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

const val MIN_PASSWORD_LENGTH = 8

private val EMAIL_PATTERN = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

/** Returns a message describing the first problem with the sign-up form, or null if it's valid. */
fun validateSignUp(email: String, password: String, confirmPassword: String): String? = when {
    !EMAIL_PATTERN.matches(email.trim()) -> "Enter a valid email address."
    password.length < MIN_PASSWORD_LENGTH -> "Password must be at least $MIN_PASSWORD_LENGTH characters."
    password != confirmPassword -> "Passwords don't match."
    else -> null
}

@Composable
fun SignUpScreen(
    initialEmail: String,
    isLoading: Boolean,
    errorMessage: String?,
    onSignUp: (fullName: String, email: String, password: String) -> Unit,
    onBackToSignIn: () -> Unit
) {
    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf(initialEmail) }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var showValidation by rememberSaveable { mutableStateOf(false) }

    val validationError = validateSignUp(email, password, confirmPassword)
    val shownError = if (showValidation && validationError != null) validationError else errorMessage

    AuthCard {
        Text(text = "Create an account", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "BARN Scout is \$100/month after a 30-day free trial.",
            style = MaterialTheme.typography.bodyMedium
        )

        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            supportingText = { Text("At least $MIN_PASSWORD_LENGTH characters") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        if (!shownError.isNullOrBlank()) {
            Text(
                text = shownError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Button(
            onClick = {
                showValidation = true
                if (validationError == null) onSignUp(fullName.trim(), email.trim(), password)
            },
            enabled = !isLoading && email.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isLoading) "Creating account..." else "Create Account")
        }

        TextButton(
            onClick = onBackToSignIn,
            enabled = !isLoading,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Already have an account? Sign in")
        }
    }
}

/** Shown after sign-up: either "confirm your email" or "waiting for an admin". */
@Composable
fun AuthStatusScreen(
    title: String,
    message: String,
    isLoading: Boolean = false,
    infoMessage: String? = null,
    errorMessage: String? = null,
    secondaryActionLabel: String? = null,
    onSecondaryAction: () -> Unit = {},
    onBackToSignIn: () -> Unit
) {
    AuthCard {
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        Text(text = message, style = MaterialTheme.typography.bodyMedium)

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

        Button(onClick = onBackToSignIn, enabled = !isLoading, modifier = Modifier.fillMaxWidth()) {
            Text("Back to Sign In")
        }
        if (secondaryActionLabel != null) {
            OutlinedButton(
                onClick = onSecondaryAction,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isLoading) "Sending..." else secondaryActionLabel)
            }
        }
    }
}

@Composable
internal fun AuthCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                content()
            }
        }
    }
}
