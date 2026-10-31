package com.vpt.scout

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

data class SupabaseAuthState(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val userId: String? = null,
    val email: String? = null
) {
    val isAuthenticated: Boolean
        get() = !accessToken.isNullOrBlank()
}

/** Result of asking the backend whether the signed-in user is an approved scout or admin. */
enum class AccessCheck { APPROVED, NOT_APPROVED, UNKNOWN }

/** The account exists and the password is right, but it has no active subscription. */
class NotApprovedException : IllegalStateException(SupabaseAuthManager.NOT_APPROVED_MESSAGE)

/** What happened after a successful sign-up request. */
sealed class SignUpOutcome {
    /** Supabase sent a confirmation email; the account can't sign in until it's confirmed. */
    data class ConfirmEmail(val email: String) : SignUpOutcome()

    /** The account is live but needs a subscription before it can use the app. */
    data class PendingApproval(val email: String) : SignUpOutcome()

    /** The account already had access (e.g. an admin) and is now signed in. */
    data class SignedIn(val state: SupabaseAuthState) : SignUpOutcome()
}

/** Result of asking the backend to start a Stripe Checkout session. */
sealed class CheckoutStart {
    data class Redirect(val url: String) : CheckoutStart()
    object AlreadySubscribed : CheckoutStart()
}

class SupabaseAuthManager(
    private val prefs: SharedPreferences,
    private val httpClient: OkHttpClient,
    private val supabaseUrl: String,
    private val supabaseAnonKey: String,
    /** Where password-reset emails send people (the web UI's /reset-password page). */
    private val passwordResetRedirectUrl: String = ""
) {
    constructor(context: Context) : this(
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
        httpClient = OkHttpClient(),
        supabaseUrl = BuildConfig.SUPABASE_URL.trimEnd('/'),
        supabaseAnonKey = BuildConfig.SUPABASE_ANON_KEY,
        passwordResetRedirectUrl = BuildConfig.WEB_APP_URL.trimEnd('/')
            .takeIf { it.startsWith("http") }
            ?.let { "$it/reset-password" }
            .orEmpty()
    )

    private val _state = MutableStateFlow(loadState())
    val state: StateFlow<SupabaseAuthState> = _state.asStateFlow()

    /**
     * A signed-in session that has no access yet (no subscription). Kept in memory only, so the
     * subscribe screen can open Checkout and re-check access; it never reaches the data client.
     */
    private val _pendingSession = MutableStateFlow<SupabaseAuthState?>(null)
    val pendingSession: StateFlow<SupabaseAuthState?> = _pendingSession.asStateFlow()

    val projectUrl: String
        get() = supabaseUrl

    val anonKey: String
        get() = supabaseAnonKey

    fun getAccessToken(): String? = _state.value.accessToken

    /** Clear the session on this device only. */
    fun signOut() {
        prefs.edit().clear().apply()
        _state.value = SupabaseAuthState()
        _pendingSession.value = null
    }

    /** Forget the unpaid session, e.g. to sign in with a different account. */
    fun clearPendingSession() {
        _pendingSession.value = null
    }

    /** Revoke the session on the server (best effort), then clear it locally. */
    suspend fun logout() = withContext(Dispatchers.IO) {
        val token = _state.value.accessToken ?: _pendingSession.value?.accessToken
        if (!token.isNullOrBlank() && supabaseUrl.isNotBlank()) {
            runCatching {
                val request = Request.Builder()
                    .url("$supabaseUrl/auth/v1/logout")
                    .addHeader("apikey", supabaseAnonKey)
                    .addHeader("Authorization", "Bearer $token")
                    .post("".toRequestBody(JSON_MEDIA_TYPE))
                    .build()
                httpClient.newCall(request).execute().close()
            }
        }
        signOut()
    }

    suspend fun signIn(email: String, password: String): Result<SupabaseAuthState> =
        withContext(Dispatchers.IO) {
            try {
                if (supabaseUrl.isBlank() || supabaseAnonKey.isBlank()) {
                    return@withContext Result.failure(
                        IllegalStateException("Supabase is not configured in app resources.")
                    )
                }

                val payload = JsonObject().apply {
                    addProperty("email", email.trim())
                    addProperty("password", password)
                }

                val request = Request.Builder()
                    .url("$supabaseUrl/auth/v1/token?grant_type=password")
                    .addHeader("apikey", supabaseAnonKey)
                    .addHeader("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                val authState = httpClient.newCall(request).execute().use { response ->
                    val rawBody = response.body?.string().orEmpty()

                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IllegalStateException(errorMessage(rawBody, "Login failed (${response.code})"))
                        )
                    }

                    val json = JsonParser().parse(rawBody).asJsonObject
                    val accessToken = json.string("access_token")
                        ?: return@withContext Result.failure(
                            IllegalStateException("Supabase login did not return an access token.")
                        )
                    val user = json.objectValue("user")
                    SupabaseAuthState(
                        accessToken = accessToken,
                        refreshToken = json.string("refresh_token"),
                        userId = user?.string("id"),
                        email = user?.string("email") ?: email.trim()
                    )
                }

                // A valid password is not enough: the account must be approved.
                when (checkAccess(authState.accessToken!!)) {
                    AccessCheck.APPROVED -> Unit
                    AccessCheck.NOT_APPROVED -> {
                        _pendingSession.value = authState
                        return@withContext Result.failure(NotApprovedException())
                    }
                    AccessCheck.UNKNOWN -> return@withContext Result.failure(
                        IllegalStateException("Could not check account access. Try again.")
                    )
                }

                persistState(authState)
                _state.value = authState
                Result.success(authState)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Create an account. New accounts can't use the app until an admin grants them the
     * scout role, so an unapproved session is never stored on the device.
     */
    suspend fun signUp(email: String, password: String, fullName: String = ""): Result<SignUpOutcome> =
        withContext(Dispatchers.IO) {
            try {
                if (supabaseUrl.isBlank() || supabaseAnonKey.isBlank()) {
                    return@withContext Result.failure(
                        IllegalStateException("Supabase is not configured in app resources.")
                    )
                }
                val trimmedEmail = email.trim()
                val payload = JsonObject().apply {
                    addProperty("email", trimmedEmail)
                    addProperty("password", password)
                    if (fullName.isNotBlank()) {
                        add("data", JsonObject().apply { addProperty("full_name", fullName.trim()) })
                    }
                }
                val request = Request.Builder()
                    .url("$supabaseUrl/auth/v1/signup")
                    .addHeader("apikey", supabaseAnonKey)
                    .addHeader("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                val json = httpClient.newCall(request).execute().use { response ->
                    val rawBody = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IllegalStateException(signUpErrorMessage(rawBody, response.code))
                        )
                    }
                    JsonParser().parse(rawBody).asJsonObject
                }

                // With email confirmation on, Supabase returns only the user, no session.
                val accessToken = json.string("access_token")
                    ?: return@withContext Result.success(SignUpOutcome.ConfirmEmail(trimmedEmail))

                val user = json.objectValue("user")
                val authState = SupabaseAuthState(
                    accessToken = accessToken,
                    refreshToken = json.string("refresh_token"),
                    userId = user?.string("id"),
                    email = user?.string("email") ?: trimmedEmail
                )
                if (checkAccess(accessToken) == AccessCheck.APPROVED) {
                    persistState(authState)
                    _state.value = authState
                    Result.success(SignUpOutcome.SignedIn(authState))
                } else {
                    _pendingSession.value = authState
                    Result.success(SignUpOutcome.PendingApproval(authState.email ?: trimmedEmail))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /** Send the sign-up confirmation email again. */
    suspend fun resendConfirmation(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (supabaseUrl.isBlank() || supabaseAnonKey.isBlank()) {
                return@withContext Result.failure(
                    IllegalStateException("Supabase is not configured in app resources.")
                )
            }
            val payload = JsonObject().apply {
                addProperty("type", "signup")
                addProperty("email", email.trim())
            }
            val request = Request.Builder()
                .url("$supabaseUrl/auth/v1/resend")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val rawBody = response.body?.string().orEmpty()
                    return@withContext Result.failure(
                        IllegalStateException(errorMessage(rawBody, "Could not resend email (${response.code})"))
                    )
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Re-check that the stored session still belongs to an approved user, e.g. on app start.
     * Signs out only on an explicit "not approved"; network errors keep the session.
     */
    suspend fun verifyAccess(): AccessCheck = withContext(Dispatchers.IO) {
        val token = _state.value.accessToken ?: return@withContext AccessCheck.NOT_APPROVED
        // An expired token shows up as UNKNOWN; the data client's Authenticator refreshes it later.
        val result = checkAccess(token)
        if (result == AccessCheck.NOT_APPROVED) {
            // Subscription lapsed: drop the stored session but keep it in memory for re-subscribing.
            val lapsed = _state.value
            signOut()
            _pendingSession.value = lapsed
        }
        result
    }

    /**
     * Re-check an unpaid session, e.g. after the user returns from Stripe Checkout.
     * On success the session is stored and the app opens.
     */
    suspend fun recheckPendingAccess(): AccessCheck = withContext(Dispatchers.IO) {
        val pending = _pendingSession.value ?: return@withContext AccessCheck.NOT_APPROVED
        val result = checkAccess(pending.accessToken ?: return@withContext AccessCheck.NOT_APPROVED)
        if (result == AccessCheck.APPROVED) {
            persistState(pending)
            _state.value = pending
            _pendingSession.value = null
        }
        result
    }

    /** Start Stripe Checkout for the unpaid (or signed-in) account. */
    suspend fun startSubscriptionCheckout(): Result<CheckoutStart> = withContext(Dispatchers.IO) {
        runCatching {
            val json = callFunction(
                "create-subscription-checkout",
                JsonObject().apply { addProperty("return_path", BILLING_RETURN_PATH) }
            )
            if (json.get("already_subscribed")?.takeUnless { it.isJsonNull }?.asBoolean == true) {
                CheckoutStart.AlreadySubscribed
            } else {
                CheckoutStart.Redirect(
                    json.string("url") ?: throw IllegalStateException("Checkout did not return a link.")
                )
            }
        }
    }

    /** Link to the Stripe customer portal, where subscribers update payment details or cancel. */
    suspend fun billingPortalUrl(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            callFunction(
                "create-billing-portal",
                JsonObject().apply { addProperty("return_path", BILLING_RETURN_PATH) }
            ).string("url") ?: throw IllegalStateException("Billing did not return a link.")
        }
    }

    private fun callFunction(name: String, body: JsonObject): JsonObject {
        if (supabaseUrl.isBlank() || supabaseAnonKey.isBlank()) {
            throw IllegalStateException("Supabase is not configured in app resources.")
        }
        val token = _state.value.accessToken ?: _pendingSession.value?.accessToken
            ?: throw IllegalStateException("Sign in first.")
        val request = Request.Builder()
            .url("$supabaseUrl/functions/v1/$name")
            .addHeader("apikey", supabaseAnonKey)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()
        return httpClient.newCall(request).execute().use { response ->
            val rawBody = response.body?.string().orEmpty()
            if (response.code == 401) {
                throw IllegalStateException("Your session expired. Sign in again.")
            }
            if (!response.isSuccessful) {
                throw IllegalStateException(errorMessage(rawBody, "Request failed (${response.code})"))
            }
            JsonParser().parse(rawBody).asJsonObject
        }
    }

    /** Ask Supabase to email a password-reset link. */
    suspend fun requestPasswordReset(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (supabaseUrl.isBlank() || supabaseAnonKey.isBlank()) {
                return@withContext Result.failure(
                    IllegalStateException("Supabase is not configured in app resources.")
                )
            }
            val url = "$supabaseUrl/auth/v1/recover".toHttpUrlOrNull()!!.newBuilder().apply {
                if (passwordResetRedirectUrl.isNotBlank()) {
                    addQueryParameter("redirect_to", passwordResetRedirectUrl)
                }
            }.build()
            val payload = JsonObject().apply { addProperty("email", email.trim()) }
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val rawBody = response.body?.string().orEmpty()
                    return@withContext Result.failure(
                        IllegalStateException(errorMessage(rawBody, "Could not send reset email (${response.code})"))
                    )
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Refresh the access token using the stored refresh token.
     * Returns true if refresh was successful, false otherwise.
     * On failure, clears the auth state so the user is prompted to log in again.
     */
    suspend fun refreshAccessToken(): Boolean = withContext(Dispatchers.IO) {
        refreshAccessTokenSync()
    }

    /**
     * Blocking refresh, used directly by the OkHttp Authenticator.
     */
    @Synchronized
    fun refreshAccessTokenSync(): Boolean {
        val currentRefreshToken = _state.value.refreshToken
        if (currentRefreshToken.isNullOrBlank()) {
            signOut()
            return false
        }

        return try {
            val payload = JsonObject().apply {
                addProperty("refresh_token", currentRefreshToken)
            }

            val request = Request.Builder()
                .url("$supabaseUrl/auth/v1/token?grant_type=refresh_token")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    signOut()
                    return false
                }

                val json = JsonParser().parse(rawBody).asJsonObject
                val newAccessToken = json.string("access_token")
                if (newAccessToken == null) {
                    signOut()
                    return false
                }
                val user = json.objectValue("user")

                val authState = SupabaseAuthState(
                    accessToken = newAccessToken,
                    refreshToken = json.string("refresh_token") ?: currentRefreshToken,
                    userId = user?.string("id") ?: _state.value.userId,
                    email = user?.string("email") ?: _state.value.email
                )

                persistState(authState)
                _state.value = authState
                true
            }
        } catch (e: Exception) {
            signOut()
            false
        }
    }

    private fun checkAccess(accessToken: String): AccessCheck = try {
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/rpc/has_app_access")
            .addHeader("apikey", supabaseAnonKey)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .post("{}".toRequestBody(JSON_MEDIA_TYPE))
            .build()
        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty().trim()
            when {
                response.isSuccessful && body == "true" -> AccessCheck.APPROVED
                response.isSuccessful && body == "false" -> AccessCheck.NOT_APPROVED
                else -> AccessCheck.UNKNOWN
            }
        }
    } catch (e: Exception) {
        AccessCheck.UNKNOWN
    }

    private fun signUpErrorMessage(rawBody: String, code: Int): String {
        val errorBody = runCatching { JsonParser().parse(rawBody).asJsonObject }.getOrNull()
        return when (errorBody?.string("error_code") ?: errorBody?.string("code")) {
            "signup_disabled" -> "New sign-ups are turned off. Ask an admin for an invite."
            "user_already_exists", "email_exists" -> "That email already has an account. Sign in instead."
            "weak_password" -> errorBody?.string("msg") ?: "Choose a stronger password."
            else -> errorMessage(rawBody, "Sign up failed ($code)")
        }
    }

    private fun errorMessage(rawBody: String, fallback: String): String {
        val errorBody = runCatching { JsonParser().parse(rawBody).asJsonObject }.getOrNull()
        return errorBody?.string("msg")
            ?: errorBody?.string("error_description")
            ?: errorBody?.string("error")
            ?: fallback
    }

    private fun persistState(state: SupabaseAuthState) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, state.accessToken)
            .putString(KEY_REFRESH_TOKEN, state.refreshToken)
            .putString(KEY_USER_ID, state.userId)
            .putString(KEY_EMAIL, state.email)
            .apply()
    }

    private fun loadState(): SupabaseAuthState {
        return SupabaseAuthState(
            accessToken = prefs.getString(KEY_ACCESS_TOKEN, null),
            refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null),
            userId = prefs.getString(KEY_USER_ID, null),
            email = prefs.getString(KEY_EMAIL, null)
        )
    }

    companion object {
        // Excluded from backups in res/xml/backup_rules.xml and data_extraction_rules.xml.
        private const val PREFS_NAME = "supabase_auth"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_EMAIL = "email"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        const val NOT_APPROVED_MESSAGE = "You need an active subscription to use BARN Scout."
        /** Page on the web UI that Stripe sends people back to. */
        const val BILLING_RETURN_PATH = "/billing/done"
    }
}

private fun JsonObject.string(key: String): String? =
    get(key)?.takeUnless { it.isJsonNull }?.asString?.takeIf { it.isNotBlank() }

private fun JsonObject.objectValue(key: String): JsonObject? =
    get(key)?.takeUnless { it.isJsonNull }?.asJsonObject
