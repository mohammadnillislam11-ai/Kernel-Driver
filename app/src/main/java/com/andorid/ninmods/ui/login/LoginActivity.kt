package com.andorid.ninmods.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.andorid.ninmods.data.SessionManager
import com.andorid.ninmods.ui.MainActivity

class LoginActivity : ComponentActivity() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)

        // If user already authenticated and remember session is on, navigate straight to MainActivity
        if (sessionManager.isLoggedIn && sessionManager.rememberLogin) {
            navigateToMain()
            return
        }

        setContent {
            LoginScreen { email, name, apiKey, licenseRemainingSec, role, balance, activeKey, remember ->
                sessionManager.isLoggedIn = true
                sessionManager.userEmail = email
                sessionManager.userName = name
                sessionManager.authToken = apiKey
                sessionManager.rememberLogin = remember
                sessionManager.userRole = role
                sessionManager.userBalance = balance
                if (!activeKey.isNullOrBlank()) {
                    sessionManager.activeLicenseKey = activeKey
                }

                if (licenseRemainingSec != null && licenseRemainingSec > 0) {
                    sessionManager.serverRemainingSeconds = licenseRemainingSec
                    sessionManager.licenseExpiryMillis = System.currentTimeMillis() + (licenseRemainingSec * 1000L)
                }

                Toast.makeText(this, "Welcome, $name", Toast.LENGTH_SHORT).show()
                navigateToMain()
            }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
