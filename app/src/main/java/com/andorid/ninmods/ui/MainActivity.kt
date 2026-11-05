package com.andorid.ninmods.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.andorid.ninmods.data.SessionManager
import com.andorid.ninmods.ui.compose.MainAppScreen
import com.andorid.ninmods.ui.login.LoginActivity
import com.andorid.ninmods.ui.theme.APatchTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)

        setContent {
            APatchTheme {
                MainAppScreen(
                    viewModel = viewModel,
                    sessionManager = sessionManager,
                    onSignOut = {
                        sessionManager.logout()
                        val intent = Intent(this@MainActivity, LoginActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}
