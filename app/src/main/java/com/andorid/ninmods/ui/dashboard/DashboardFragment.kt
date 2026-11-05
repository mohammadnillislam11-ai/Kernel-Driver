package com.andorid.ninmods.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.andorid.ninmods.databinding.FragmentDashboardBinding
import com.andorid.ninmods.ui.MainViewModel

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.appItem1.setOnClickListener {
            launchOrNotifyApp("com.dts.freefireth", "Free Fire")
        }
        binding.appItem2.setOnClickListener {
            launchOrNotifyApp("com.dts.freefiremax", "Free Fire MAX")
        }
        binding.appItem3.setOnClickListener {
            Toast.makeText(requireContext(), "Engine Ready", Toast.LENGTH_SHORT).show()
        }
        binding.appItem4.setOnClickListener {
            Toast.makeText(requireContext(), "Injector Ready", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchOrNotifyApp(packageName: String, appName: String) {
        val launchIntent = requireContext().packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            startActivity(launchIntent)
        } else {
            Toast.makeText(requireContext(), "$appName ready for launch", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
