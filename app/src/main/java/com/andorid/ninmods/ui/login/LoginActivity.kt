package com.andorid.ninmods.ui.login

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.andorid.ninmods.data.SessionManager
import com.andorid.ninmods.ui.MainActivity
import com.andorid.ninmods.ui.setup.InstallationSetupScreen
import com.andorid.ninmods.ui.theme.APatchTheme

class LoginActivity : ComponentActivity() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)

        // If user already authenticated, remember session is on: navigate to MainActivity
        if (sessionManager.isLoggedIn && sessionManager.rememberLogin) {
            navigateToMain()
            return
        }

        setContent {
            APatchTheme {
                val context = this@LoginActivity
                val isOverlayMissing = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)
                val isStorageMissing = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()

                var showSetup by remember {
                    mutableStateOf(!sessionManager.isEnvironmentSetup || isOverlayMissing || isStorageMissing)
                }

                AnimatedContent(
                    targetState = showSetup,
                    transitionSpec = {
                        if (targetState) {
                            (slideInHorizontally(initialOffsetX = { -it / 4 }, animationSpec = tween(340, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween(340)))
                                .togetherWith(slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(340, easing = FastOutSlowInEasing)) + fadeOut(animationSpec = tween(340)))
                        } else {
                            (slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(340, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween(340)))
                                .togetherWith(slideOutHorizontally(targetOffsetX = { -it / 4 }, animationSpec = tween(340, easing = FastOutSlowInEasing)) + fadeOut(animationSpec = tween(340)))
                        }
                    },
                    label = "SetupToLoginTransition"
                ) { isSetupVisible ->
                    if (isSetupVisible) {
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
                        LoginScreen(
                            onOpenSetup = {
                                showSetup = true
                            },
                            onLoginSuccess = { email, name, apiKey, licenseRemainingSec, role, balance, activeKey, remember ->
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

                            Toast.makeText(this@LoginActivity, "Welcome, $name", Toast.LENGTH_SHORT).show()
                            navigateToMain()
                        }
                    )
                }
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
