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
                    // Hero Card Status
                    binding.tvStatusSubtitle.text = if (state.isDriverLoaded) {
                        "Driver Active & Bound to Kernel"
                    } else {
                        "No Active Character Device Node"
                    }

                    binding.tvHeroBadge.text = if (state.isDriverLoaded) {
                        getString(R.string.status_active)
                    } else {
                        getString(R.string.status_inactive)
                    }

                    // Details
                    binding.tvKernelDetail.text = "Linux Kernel: ${state.kernelRelease}"
                    binding.tvSuperuserDetail.text = if (state.isRootAvailable) {
                        "Superuser: Root Access Granted (uid=0)"
                    } else {
                        "Superuser: Root Access Denied / Required!"
                    }

                    binding.tvNodesDetail.text = if (state.activeNodes.isNotEmpty()) {
                        "Nodes: ${state.activeNodes.joinToString(", ")}"
                    } else {
                        "Nodes: None Active (/dev/BYNb missing)"
                    }

                    // Button States
                    binding.btnQuickLoadQx.isEnabled = !state.isWorking && state.isRootAvailable
                    binding.btnQuickLoadRt.isEnabled = !state.isWorking && state.isRootAvailable
                    binding.btnQuickUnload.isEnabled = !state.isWorking && state.isRootAvailable

                    // Live Output Log
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
