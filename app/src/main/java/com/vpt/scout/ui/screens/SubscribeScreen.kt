package com.vpt.scout.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

const val SUBSCRIPTION_PRICE_TEXT = "\$100/month"
const val TRIAL_DAYS = 30

/** Paywall shown to signed-in accounts without an active subscription. */
@Composable
fun SubscribeScreen(
    email: String?,
    isLoading: Boolean,
    errorMessage: String?,
    infoMessage: String?,
    onStartTrial: () -> Unit,
    onCheckAgain: () -> Unit,
    onUseDifferentAccount: () -> Unit
) {
    AuthCard {
        Text(text = "Start your free trial", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "BARN Scout is $SUBSCRIPTION_PRICE_TEXT. New subscribers get the first $TRIAL_DAYS " +
                "days free, and you can cancel anytime before the trial ends without being charged.",
            style = MaterialTheme.typography.bodyMedium
        )
        if (!email.isNullOrBlank()) {
            Text(text = "Signed in as $email", style = MaterialTheme.typography.bodySmall)
        }

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

        Button(onClick = onStartTrial, enabled = !isLoading, modifier = Modifier.fillMaxWidth()) {
            Text(if (isLoading) "Working..." else "Start $TRIAL_DAYS-day free trial")
        }
        OutlinedButton(onClick = onCheckAgain, enabled = !isLoading, modifier = Modifier.fillMaxWidth()) {
            Text("I've subscribed")
        }
        TextButton(
            onClick = onUseDifferentAccount,
            enabled = !isLoading,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Use a different account")
        }
    }
}
