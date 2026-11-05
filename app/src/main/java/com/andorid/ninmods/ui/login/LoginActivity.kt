package com.andorid.ninmods.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.andorid.ninmods.data.SessionManager
import com.andorid.ninmods.ui.MainActivity
import com.andorid.ninmods.ui.compose.APatchLoginScreen

class LoginActivity : ComponentActivity() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)

        // If user already authenticated and remember session is on, navigate to MainActivity
        if (sessionManager.isLoggedIn && sessionManager.rememberLogin) {
            navigateToMain()
            return
        }

        // Render Pure APatch Jetpack Compose Login Screen
        setContent {
            APatchLoginScreen { email, name, remember ->
                sessionManager.isLoggedIn = true
                sessionManager.userEmail = email
                sessionManager.userName = name
                sessionManager.authToken = "nin_${System.currentTimeMillis()}"
                sessionManager.rememberLogin = remember

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
