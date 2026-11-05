package com.andorid.ninmods.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.andorid.ninmods.R
import com.andorid.ninmods.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        observeState()
    }

    private fun setupListeners() {
        binding.btnLoadQx.setOnClickListener {
            viewModel.prepareDrivers("QX") { matches ->
                showDriverSelectionDialog(matches)
            }
        }

        binding.btnLoadRt.setOnClickListener {
            viewModel.prepareDrivers("RT") { matches ->
                showDriverSelectionDialog(matches)
            }
        }

        binding.btnUnload.setOnClickListener {
            viewModel.unloadDrivers()
        }

        binding.btnCheckStatus.setOnClickListener {
            viewModel.refreshStatus()
            val state = viewModel.uiState.value
            val msg = if (state.isDriverLoaded) {
                "Active Nodes: ${state.activeNodes.joinToString(", ")}"
            } else {
                "No driver active (/dev/BYNb not found)"
            }
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        binding.btnClearLog.setOnClickListener {
            viewModel.clearLog()
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // System info bar
                    binding.tvKernelInfo.text = "Kernel: ${state.kernelRelease}"
                    binding.tvStatusBadge.text = if (state.isDriverLoaded) "ACTIVE" else "NOT LOADED"
                    binding.tvStatusBadge.setBackgroundColor(
                        if (state.isDriverLoaded) 0xFF00C853.toInt() else 0xFFD50000.toInt()
                    )

                    // Button controls
                    binding.btnLoadQx.isEnabled = !state.isWorking && state.isRootAvailable
                    binding.btnLoadRt.isEnabled = !state.isWorking && state.isRootAvailable
                    binding.btnUnload.isEnabled = !state.isWorking && state.isRootAvailable

                    // Live Terminal Output
                    binding.tvConsoleLog.text = state.logMessages.joinToString("\n")
                    binding.scrollLog.post {
                        binding.scrollLog.fullScroll(android.view.View.FOCUS_DOWN)
                    }
                }
            }
        }
    }

    private fun showDriverSelectionDialog(files: List<File>) {
        val names = files.map { it.name }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Select Compatible Driver")
            .setItems(names) { _, which ->
                viewModel.loadDriverScript(files[which])
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
