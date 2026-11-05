package com.andorid.ninmods.network

import com.andorid.ninmods.config.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class ActiveLicenseInfo(
    val licenseKey: String,
    val durationType: String,
    val expiresAt: String,
    val remainingSeconds: Long,
    val maxDevices: Int,
    val devicesUsed: Int
)

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val userId: Int,
    val username: String,
    val email: String,
    val role: String,
    val apiKey: String,
    val sessionToken: String,
    val activeLicense: ActiveLicenseInfo? = null
)

data class SignUpResponse(
    val success: Boolean,
    val message: String,
    val userId: Int,
    val username: String,
    val email: String,
    val apiKey: String
)

data class BasicResponse(
    val success: Boolean,
    val message: String
)

/**
 * Robust REST API Client for Authentication & User operations.
 * Endpoints are dynamically powered by AppConfig (single .env equivalent).
 */
object AuthApiClient {

    /**
     * POST /api/v2/auth/login
     */
    suspend fun login(
        usernameOrEmail: String,
        password: String,
        hwid: String = ""
    ): Result<LoginResponse> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("username", usernameOrEmail.trim())
                put("password", password.trim())
                if (hwid.isNotBlank()) put("hwid", hwid.trim())
            }

            val (code, responseBody) = sendPostRequest(AppConfig.LOGIN_URL, payload.toString())
            val json = JSONObject(responseBody)

            if (code in 200..299 && (json.optBoolean("success", false) || json.optBoolean("status", false))) {
                val activeLicJson = json.optJSONObject("active_license")
                val activeLic = if (activeLicJson != null) {
                    ActiveLicenseInfo(
                        licenseKey = activeLicJson.optString("license_key", ""),
                        durationType = activeLicJson.optString("duration_type", ""),
                        expiresAt = activeLicJson.optString("expires_at", ""),
                        remainingSeconds = activeLicJson.optLong("remaining_seconds", 0L),
                        maxDevices = activeLicJson.optInt("max_devices", 1),
                        devicesUsed = activeLicJson.optInt("devices_used", 1)
                    )
                } else null

                val result = LoginResponse(
                    success = true,
                    message = json.optString("message", "Login successful"),
                    userId = json.optInt("user_id", 0),
                    username = json.optString("username", usernameOrEmail),
                    email = json.optString("email", ""),
                    role = json.optString("role", "user"),
                    apiKey = json.optString("api_key", ""),
                    sessionToken = json.optString("session_token", ""),
                    activeLicense = activeLic
                )
                Result.success(result)
            } else {
                val errorMsg = json.optString("error", json.optString("message", "Invalid login credentials (HTTP $code)"))
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            val cleanError = when {
                e.message?.contains("Unable to resolve host") == true -> "Server unreachable. Check internet connection or domain configuration in AppConfig."
                e.message?.contains("failed to connect") == true -> "Could not connect to backend server."
                else -> e.message ?: "Authentication network error"
            }
            Result.failure(Exception(cleanError))
        }
    }

    /**
     * POST /api/v2/auth/signup
     */
    suspend fun signup(
        username: String,
        email: String,
        password: String,
        passwordAgain: String = password,
        telephone: String = ""
    ): Result<SignUpResponse> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("username", username.trim())
                put("email", email.trim())
                put("password", password.trim())
                put("password_again", passwordAgain.trim())
                if (telephone.isNotBlank()) put("telephone", telephone.trim())
            }

            val (code, responseBody) = sendPostRequest(AppConfig.SIGNUP_URL, payload.toString())
            val json = JSONObject(responseBody)

            if (code in 200..299 && (json.optBoolean("success", false) || json.optBoolean("status", false))) {
                val result = SignUpResponse(
                    success = true,
                    message = json.optString("message", "Account created successfully!"),
                    userId = json.optInt("user_id", 0),
                    username = json.optString("username", username),
                    email = json.optString("email", email),
                    apiKey = json.optString("api_key", "")
                )
                Result.success(result)
            } else {
                val errorMsg = json.optString("error", json.optString("message", "Registration failed (HTTP $code)"))
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            val cleanError = when {
                e.message?.contains("Unable to resolve host") == true -> "Server unreachable. Check domain in AppConfig."
                else -> e.message ?: "Signup network error"
            }
            Result.failure(Exception(cleanError))
        }
    }

    /**
     * POST /api/v2/auth/reset-password
     */
    suspend fun resetPassword(
        usernameOrEmail: String,
        newPassword: String = ""
    ): Result<BasicResponse> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                if (newPassword.isNotBlank()) {
                    put("username", usernameOrEmail.trim())
                    put("newPassword", newPassword.trim())
                } else {
                    put("email", usernameOrEmail.trim())
                }
            }

            val (code, responseBody) = sendPostRequest(AppConfig.RESET_PASSWORD_URL, payload.toString())
            val json = JSONObject(responseBody)

            if (code in 200..299 && (json.optBoolean("success", false) || json.optBoolean("status", false))) {
                val msg = json.optString("message", "Password reset successful!")
                Result.success(BasicResponse(success = true, message = msg))
            } else {
                val errorMsg = json.optString("error", json.optString("message", "Password reset failed (HTTP $code)"))
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Password reset error"))
        }
    }

    /**
     * POST /api/v2/profile/update (Authenticated password update)
     */
    suspend fun updatePassword(
        apiKey: String,
        newPassword: String
    ): Result<BasicResponse> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("password", newPassword.trim())
            }

            val headers = mapOf(
                "Authorization" to "Bearer ${apiKey.trim()}",
                "api-key" to apiKey.trim()
            )

            val (code, responseBody) = sendPostRequest(AppConfig.PROFILE_UPDATE_URL, payload.toString(), headers)
            val json = JSONObject(responseBody)

            if (code in 200..299 && (json.optBoolean("success", false) || json.optBoolean("status", false))) {
                val msg = json.optString("message", "Password updated successfully!")
                Result.success(BasicResponse(success = true, message = msg))
            } else {
                val errorMsg = json.optString("error", json.optString("message", "Failed to update password (HTTP $code)"))
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Profile update network error"))
        }
    }

    private fun sendPostRequest(
        urlString: String,
        jsonBody: String,
        extraHeaders: Map<String, String> = emptyMap()
    ): Pair<Int, String> {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = AppConfig.TIMEOUT_MILLIS
        conn.readTimeout = AppConfig.TIMEOUT_MILLIS
        conn.doInput = true
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        conn.setRequestProperty("Accept", "application/json")

        for ((k, v) in extraHeaders) {
            conn.setRequestProperty(k, v)
        }

        OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
            writer.write(jsonBody)
            writer.flush()
        }

        val code = conn.responseCode
        val inputStream = if (code in 200..299) conn.inputStream else conn.errorStream

        val responseBody = if (inputStream != null) {
            BufferedReader(InputStreamReader(inputStream, "UTF-8")).use { reader ->
                reader.readText()
            }
        } else {
            "{}"
        }

        conn.disconnect()
        return Pair(code, responseBody)
    }
}
