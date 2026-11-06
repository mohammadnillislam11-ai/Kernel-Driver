package com.andorid.ninmods.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.andorid.ninmods.data.SessionManager
import com.andorid.ninmods.ui.MainActivity
import com.andorid.ninmods.ui.setup.InstallationSetupScreen

class LoginActivity : ComponentActivity() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)

        // If user already authenticated, remember session is on, and setup is completed: navigate to MainActivity
        if (sessionManager.isEnvironmentSetup && sessionManager.isLoggedIn && sessionManager.rememberLogin) {
            navigateToMain()
            return
        }

        setContent {
            var showSetup by remember { mutableStateOf(!sessionManager.isEnvironmentSetup) }

            if (showSetup) {
                InstallationSetupScreen(
                    onCompleteSetup = {
                        sessionManager.isEnvironmentSetup = true
                        if (sessionManager.isLoggedIn && sessionManager.rememberLogin) {
                            navigateToMain()
                        } else {
                            showSetup = false
                        }
                    },
                    onSkipSetup = {
                        sessionManager.isEnvironmentSetup = true
                        if (sessionManager.isLoggedIn && sessionManager.rememberLogin) {
                            navigateToMain()
                        } else {
                            showSetup = false
                        }
                    }
                )
            } else {
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
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
