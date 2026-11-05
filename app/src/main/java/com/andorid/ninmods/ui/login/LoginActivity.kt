package com.andorid.ninmods.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.andorid.ninmods.data.SessionManager
import com.andorid.ninmods.databinding.ActivityLoginBinding
import com.andorid.ninmods.ui.MainActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)

        // If user already authenticated and remember session is on, navigate to MainActivity
        if (sessionManager.isLoggedIn && sessionManager.rememberLogin) {
            navigateToMain()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.cbRemember.isChecked = sessionManager.rememberLogin

        binding.btnLogin.setOnClickListener {
            val key = binding.etAccessKey.text?.toString()?.trim() ?: ""

            if (key.isEmpty()) {
                binding.tilAccessKey.error = "Please enter an access key"
                return@setOnClickListener
            }

            binding.tilAccessKey.error = null

            // Save session
            sessionManager.isLoggedIn = true
            sessionManager.authToken = key
            sessionManager.rememberLogin = binding.cbRemember.isChecked

            Toast.makeText(this, "Authentication verified. Access granted.", Toast.LENGTH_SHORT).show()
            navigateToMain()
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
