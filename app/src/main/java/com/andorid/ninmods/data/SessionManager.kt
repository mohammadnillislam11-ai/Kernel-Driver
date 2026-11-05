package com.andorid.ninmods.data

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "ninmods_auth_session"
        private const val KEY_IS_SETUP = "is_environment_setup"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_REMEMBER = "remember_login"
        private const val KEY_LICENSE_EXPIRY = "license_expiry_time"
    }

    var licenseExpiryMillis: Long
        get() {
            var exp = prefs.getLong(KEY_LICENSE_EXPIRY, 0L)
            if (exp == 0L) {
                // Default 24 hours license expiration from first session initialization
                exp = System.currentTimeMillis() + (24 * 60 * 60 * 1000L)
                prefs.edit().putLong(KEY_LICENSE_EXPIRY, exp).apply()
            }
            return exp
        }
        set(value) = prefs.edit().putLong(KEY_LICENSE_EXPIRY, value).apply()

    var isEnvironmentSetup: Boolean
        get() = prefs.getBoolean(KEY_IS_SETUP, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_SETUP, value).apply()

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply()

    var authToken: String?
        get() = prefs.getString(KEY_AUTH_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_AUTH_TOKEN, value).apply()

    var userEmail: String?
        get() = prefs.getString(KEY_USER_EMAIL, "user@ninmods.dev")
        set(value) = prefs.edit().putString(KEY_USER_EMAIL, value).apply()

    var userName: String?
        get() = prefs.getString(KEY_USER_NAME, "NinMods User")
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var rememberLogin: Boolean
        get() = prefs.getBoolean(KEY_REMEMBER, true)
        set(value) = prefs.edit().putBoolean(KEY_REMEMBER, value).apply()

    fun logout() {
        // Keep environment setup status, clear user session
        val isSetup = isEnvironmentSetup
        prefs.edit().clear().apply()
        isEnvironmentSetup = isSetup
    }
}
