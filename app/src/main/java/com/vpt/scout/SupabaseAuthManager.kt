package com.vpt.scout

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

data class SupabaseAuthState(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val userId: String? = null,
    val email: String? = null
) {
    val isAuthenticated: Boolean
        get() = !accessToken.isNullOrBlank()
}

class SupabaseAuthManager(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val httpClient = OkHttpClient()
    private val supabaseUrl = appContext.getString(R.string.supabase_url).trimEnd('/')
    private val supabaseAnonKey = appContext.getString(R.string.supabase_anon_key)

    private val _state = MutableStateFlow(loadState())
    val state: StateFlow<SupabaseAuthState> = _state.asStateFlow()

    val projectUrl: String
        get() = supabaseUrl

    val anonKey: String
        get() = supabaseAnonKey

    fun getAccessToken(): String? = _state.value.accessToken

    fun signOut() {
        prefs.edit().clear().apply()
        _state.value = SupabaseAuthState()
    }

    suspend fun signIn(email: String, password: String): Result<SupabaseAuthState> =
        withContext(Dispatchers.IO) {
            try {
                if (supabaseUrl.isBlank() || supabaseAnonKey.isBlank()) {
                    return@withContext Result.failure(
                        IllegalStateException("Supabase is not configured in app resources.")
                    )
                }

                val payload = JSONObject()
                    .put("email", email.trim())
                    .put("password", password)

                val request = Request.Builder()
                    .url("$supabaseUrl/auth/v1/token?grant_type=password")
                    .addHeader("apikey", supabaseAnonKey)
                    .addHeader("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    val rawBody = response.body?.string().orEmpty()

                    if (!response.isSuccessful) {
                        val errorBody = runCatching { JSONObject(rawBody) }.getOrNull()
                        val message = errorBody?.optString("msg")
                            ?.takeIf { it.isNotBlank() }
                            ?: errorBody?.optString("error_description")
                                ?.takeIf { it.isNotBlank() }
                            ?: errorBody?.optString("error")
                                ?.takeIf { it.isNotBlank() }
                            ?: "Login failed (${response.code})"
                        return@withContext Result.failure(IllegalStateException(message))
                    }

                    val json = JSONObject(rawBody)
                    val accessToken = json.optString("access_token").takeIf { it.isNotBlank() }
                        ?: return@withContext Result.failure(
                            IllegalStateException("Supabase login did not return an access token.")
                        )
                    val refreshToken = json.optString("refresh_token").takeIf { it.isNotBlank() }
                    val user = json.optJSONObject("user")
                    val userId = user?.optString("id")?.takeIf { it.isNotBlank() }
                    val userEmail = user?.optString("email")?.takeIf { it.isNotBlank() } ?: email.trim()

                    val authState = SupabaseAuthState(
                        accessToken = accessToken,
                        refreshToken = refreshToken,
                        userId = userId,
                        email = userEmail
                    )

                    persistState(authState)
                    _state.value = authState
                    Result.success(authState)
                }
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
        val currentRefreshToken = _state.value.refreshToken
        if (currentRefreshToken.isNullOrBlank()) {
            signOut()
            return@withContext false
        }

        try {
            val payload = JSONObject()
                .put("refresh_token", currentRefreshToken)

            val request = Request.Builder()
                .url("$supabaseUrl/auth/v1/token?grant_type=refresh_token")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    // Refresh token is invalid or expired, clear state
                    signOut()
                    return@withContext false
                }

                val json = JSONObject(rawBody)
                val newAccessToken = json.optString("access_token").takeIf { it.isNotBlank() }
                val newRefreshToken = json.optString("refresh_token").takeIf { it.isNotBlank() }
                    ?: currentRefreshToken
                val user = json.optJSONObject("user")
                val userId = user?.optString("id")?.takeIf { it.isNotBlank() } ?: _state.value.userId
                val userEmail = user?.optString("email")?.takeIf { it.isNotBlank() } ?: _state.value.email

                if (newAccessToken == null) {
                    signOut()
                    return@withContext false
                }

                val authState = SupabaseAuthState(
                    accessToken = newAccessToken,
                    refreshToken = newRefreshToken,
                    userId = userId,
                    email = userEmail
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

    /**
     * Synchronous version of refreshAccessToken for use in OkHttp Authenticator.
     * This blocks the calling thread.
     */
    @Synchronized
    fun refreshAccessTokenSync(): Boolean {
        val currentRefreshToken = _state.value.refreshToken
        if (currentRefreshToken.isNullOrBlank()) {
            signOut()
            return false
        }

        return try {
            val payload = JSONObject()
                .put("refresh_token", currentRefreshToken)

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

                val json = JSONObject(rawBody)
                val newAccessToken = json.optString("access_token").takeIf { it.isNotBlank() }
                val newRefreshToken = json.optString("refresh_token").takeIf { it.isNotBlank() }
                    ?: currentRefreshToken
                val user = json.optJSONObject("user")
                val userId = user?.optString("id")?.takeIf { it.isNotBlank() } ?: _state.value.userId
                val userEmail = user?.optString("email")?.takeIf { it.isNotBlank() } ?: _state.value.email

                if (newAccessToken == null) {
                    signOut()
                    return false
                }

                val authState = SupabaseAuthState(
                    accessToken = newAccessToken,
                    refreshToken = newRefreshToken,
                    userId = userId,
                    email = userEmail
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
        private const val PREFS_NAME = "supabase_auth"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_EMAIL = "email"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
