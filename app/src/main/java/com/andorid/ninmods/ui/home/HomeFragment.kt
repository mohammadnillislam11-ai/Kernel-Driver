package com.andorid.ninmods.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.andorid.ninmods.databinding.FragmentHomeBinding
import com.andorid.ninmods.ui.MainViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.io.File

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupActions()
        observeViewModel()
    }

    private fun setupActions() {
        binding.btnQuickLoadQx.setOnClickListener {
            showDriverSelectionDialog("QX")
        }

        binding.btnQuickLoadRt.setOnClickListener {
            showDriverSelectionDialog("RT")
        }

        binding.btnQuickUnload.setOnClickListener {
            viewModel.unloadDrivers()
        }

        binding.btnHomeClearLog.setOnClickListener {
            viewModel.clearLog()
        }
    }

    private fun showDriverSelectionDialog(mode: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val drivers = viewModel.getMatchingDrivers(mode)
            val kernel = viewModel.uiState.value.kernelRelease

            if (drivers.isEmpty()) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Select $mode Driver")
                    .setMessage("No matching drivers found for kernel $kernel.")
                    .setPositiveButton("OK", null)
                    .show()
                return@launch
            }

            val fileNames = drivers.map { it.name }.toTypedArray()

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select $mode Driver")
                .setItems(fileNames) { _, index ->
                    val selected = drivers[index]
                    viewModel.loadDriverScript(selected)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Card 1: Kernel Driver Status
                    if (state.isDriverLoaded) {
                        binding.tvDriverState.text = "Working"
                        binding.tvDriverState.setTextColor(0xFF81C784.toInt())
                    } else {
                        binding.tvDriverState.text = "Not Loaded"
                        binding.tvDriverState.setTextColor(0xFFFFB4AB.toInt())
                    }
                    binding.tvKernelVersion.text = if (state.kernelRelease.isNotEmpty()) {
                        "Linux ${state.kernelRelease}"
                    } else {
                        "Linux Detecting..."
                    }

                    // Card 2: SuperUser Status
                    if (state.isRootAvailable) {
                        binding.tvSuperuserState.text = "Granted (uid=0)"
                        binding.tvSuperuserState.setTextColor(0xFF81C784.toInt())
                    } else {
                        binding.tvSuperuserState.text = "Denied / Required"
                        binding.tvSuperuserState.setTextColor(0xFFFFB4AB.toInt())
                    }
                    binding.tvNodesSummary.text = if (state.activeNodes.isNotEmpty()) {
                        state.activeNodes.joinToString(", ")
                    } else {
                        "No Node Active"
                    }

                    // Button States: QX and RT require root and not busy
                    binding.btnQuickLoadQx.isEnabled = !state.isWorking && state.isRootAvailable
                    binding.btnQuickLoadRt.isEnabled = !state.isWorking && state.isRootAvailable

                    // Stop Driver button is ONLY enabled when a driver is actively loaded!
                    binding.btnQuickUnload.isEnabled = state.isDriverLoaded && !state.isWorking && state.isRootAvailable

                    // Live Log Output
                    binding.tvHomeConsoleLog.text = state.logMessages.joinToString("\n")
                    binding.scrollHomeLog.post {
                        binding.scrollHomeLog.fullScroll(View.FOCUS_DOWN)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
