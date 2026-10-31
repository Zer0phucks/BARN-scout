package com.vpt.scout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.vpt.scout.ui.screens.AuthStatusScreen
import com.vpt.scout.ui.screens.LoginScreen
import com.vpt.scout.ui.screens.SignUpScreen
import com.vpt.scout.ui.screens.SubscribeScreen
import kotlinx.coroutines.launch

/** Which signed-out screen is showing. */
enum class AuthStep { SIGN_IN, SIGN_UP, CONFIRM_EMAIL, SUBSCRIBE }

/** Next step after a sign-in attempt fails: accounts without a subscription get the paywall. */
fun authStepAfterSignInFailure(error: Throwable?): AuthStep =
    if (error is NotApprovedException) AuthStep.SUBSCRIBE else AuthStep.SIGN_IN

fun authStepAfterSignUp(outcome: SignUpOutcome): AuthStep = when (outcome) {
    is SignUpOutcome.ConfirmEmail -> AuthStep.CONFIRM_EMAIL
    is SignUpOutcome.PendingApproval -> AuthStep.SUBSCRIBE
    // The auth state flips to signed in and ScoutApp leaves this flow.
    is SignUpOutcome.SignedIn -> AuthStep.SIGN_IN
}

/** Message for the paywall after re-checking access. */
fun accessRecheckMessage(result: AccessCheck): String? = when (result) {
    AccessCheck.APPROVED -> null
    AccessCheck.NOT_APPROVED ->
        "We don't see an active subscription yet. If you just subscribed, wait a few seconds and try again."
    AccessCheck.UNKNOWN -> "Couldn't check your subscription. Check your connection, or sign in again."
}

/**
 * Signed-out experience: sign in, create an account, confirm email, subscribe.
 * [notice] is an error raised outside this flow, such as access revoked since last launch.
 */
@Composable
fun AuthFlow(
    authManager: SupabaseAuthManager,
    initialEmail: String,
    notice: String? = null
) {
    val scope = rememberCoroutineScope()
    var step by rememberSaveable { mutableStateOf(AuthStep.SIGN_IN) }
    var email by rememberSaveable { mutableStateOf(initialEmail) }
    var loading by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var info by rememberSaveable { mutableStateOf<String?>(null) }
    var checkoutOpened by rememberSaveable { mutableStateOf(false) }
    val pendingSession by authManager.pendingSession.collectAsState()
    val uriHandler = LocalUriHandler.current
    val lifecycleOwner = LocalLifecycleOwner.current

    fun goTo(next: AuthStep) {
        step = next
        error = null
        info = null
    }

    fun recheck(quiet: Boolean = false) {
        scope.launch {
            loading = true
            error = null
            val result = authManager.recheckPendingAccess()
            loading = false
            // On APPROVED the auth state flips to signed in and ScoutApp leaves this flow.
            if (!quiet || result == AccessCheck.UNKNOWN) error = accessRecheckMessage(result)
        }
    }

    // A session without a subscription (just signed up, or lapsed since last launch) goes to the paywall.
    LaunchedEffect(pendingSession) {
        if (pendingSession != null) {
            step = AuthStep.SUBSCRIBE
        } else if (step == AuthStep.SUBSCRIBE) {
            step = AuthStep.SIGN_IN
        }
    }

    // Coming back from Stripe Checkout in the browser: check whether the subscription is live.
    DisposableEffect(lifecycleOwner, step, checkoutOpened) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && step == AuthStep.SUBSCRIBE && checkoutOpened) {
                recheck(quiet = true)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    when (step) {
        AuthStep.SIGN_IN -> LoginScreen(
            initialEmail = email,
            isLoading = loading,
            errorMessage = error ?: notice,
            infoMessage = info,
            onLogin = { enteredEmail, password ->
                email = enteredEmail
                scope.launch {
                    loading = true
                    error = null
                    info = null
                    val result = authManager.signIn(enteredEmail, password)
                    loading = false
                    if (result.isFailure) {
                        val failure = result.exceptionOrNull()
                        step = authStepAfterSignInFailure(failure)
                        if (step == AuthStep.SIGN_IN) error = failure?.message ?: "Login failed."
                    }
                }
            },
            onForgotPassword = { enteredEmail ->
                email = enteredEmail
                error = null
                info = null
                if (enteredEmail.isBlank()) {
                    error = "Enter your email first, then tap \"Forgot password?\"."
                } else {
                    scope.launch {
                        loading = true
                        val result = authManager.requestPasswordReset(enteredEmail)
                        loading = false
                        if (result.isSuccess) {
                            info = "If that email has an account, a reset link is on its way."
                        } else {
                            error = result.exceptionOrNull()?.message ?: "Could not send reset email."
                        }
                    }
                }
            },
            onCreateAccount = { enteredEmail ->
                email = enteredEmail
                goTo(AuthStep.SIGN_UP)
            }
        )

        AuthStep.SIGN_UP -> SignUpScreen(
            initialEmail = email,
            isLoading = loading,
            errorMessage = error,
            onSignUp = { fullName, enteredEmail, password ->
                email = enteredEmail
                scope.launch {
                    loading = true
                    error = null
                    val result = authManager.signUp(enteredEmail, password, fullName)
                    loading = false
                    result.onSuccess { outcome -> goTo(authStepAfterSignUp(outcome)) }
                        .onFailure { error = it.message ?: "Sign up failed." }
                }
            },
            onBackToSignIn = { goTo(AuthStep.SIGN_IN) }
        )

        AuthStep.CONFIRM_EMAIL -> AuthStatusScreen(
            title = "Check your email",
            message = "We sent a confirmation link to $email. Open it to finish creating " +
                "your account, then sign in to start your free trial.",
            isLoading = loading,
            infoMessage = info,
            errorMessage = error,
            secondaryActionLabel = "Resend email",
            onSecondaryAction = {
                scope.launch {
                    loading = true
                    error = null
                    info = null
                    val result = authManager.resendConfirmation(email)
                    loading = false
                    if (result.isSuccess) {
                        info = "Sent. It can take a minute to arrive."
                    } else {
                        error = result.exceptionOrNull()?.message ?: "Could not resend email."
                    }
                }
            },
            onBackToSignIn = { goTo(AuthStep.SIGN_IN) }
        )

        AuthStep.SUBSCRIBE -> SubscribeScreen(
            email = pendingSession?.email ?: email,
            isLoading = loading,
            errorMessage = error,
            infoMessage = info,
            onStartTrial = {
                scope.launch {
                    loading = true
                    error = null
                    info = null
                    val result = authManager.startSubscriptionCheckout()
                    loading = false
                    result.onSuccess { start ->
                        when (start) {
                            is CheckoutStart.Redirect -> {
                                checkoutOpened = true
                                info = "Finish checkout in your browser, then come back here."
                                runCatching { uriHandler.openUri(start.url) }
                                    .onFailure { error = "Couldn't open the browser." }
                            }
                            CheckoutStart.AlreadySubscribed -> recheck()
                        }
                    }.onFailure { error = it.message ?: "Could not start checkout." }
                }
            },
            onCheckAgain = { recheck() },
            onUseDifferentAccount = {
                authManager.clearPendingSession()
                checkoutOpened = false
                goTo(AuthStep.SIGN_IN)
            }
        )
    }
}
