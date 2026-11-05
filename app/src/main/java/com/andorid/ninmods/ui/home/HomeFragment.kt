package com.andorid.ninmods.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.andorid.ninmods.R
import com.andorid.ninmods.databinding.FragmentHomeBinding
import com.andorid.ninmods.ui.MainViewModel
import kotlinx.coroutines.launch

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
            viewModel.prepareDrivers("QX") {}
        }

        binding.btnQuickLoadRt.setOnClickListener {
            viewModel.prepareDrivers("RT") {}
        }

        binding.btnQuickUnload.setOnClickListener {
            viewModel.unloadDrivers()
        }

        binding.btnHomeClearLog.setOnClickListener {
            viewModel.clearLog()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Card 1: Kernel Driver
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

                    // Card 2: SuperUser
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

                    // Button States
                    binding.btnQuickLoadQx.isEnabled = !state.isWorking && state.isRootAvailable
                    binding.btnQuickLoadRt.isEnabled = !state.isWorking && state.isRootAvailable
                    binding.btnQuickUnload.isEnabled = !state.isWorking && state.isRootAvailable

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
