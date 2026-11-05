package com.andorid.ninmods.ui.drivers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.andorid.ninmods.R
import com.andorid.ninmods.databinding.FragmentDriversBinding
import com.andorid.ninmods.databinding.ItemDriverCardBinding
import com.andorid.ninmods.ui.MainViewModel
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.io.File

class DriversFragment : Fragment() {

    private var _binding: FragmentDriversBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()
    private var currentMode = "QX"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDriversBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvDriversList.layoutManager = LinearLayoutManager(requireContext())

        setupTabs()
        loadDriversList()
    }

    private fun setupTabs() {
        binding.btnTabQx.setOnClickListener {
            currentMode = "QX"
            binding.btnTabQx.setBackgroundColor(0xFFD0BCFF.toInt())
            binding.btnTabQx.setTextColor(0xFF381E72.toInt())
            binding.btnTabRt.setBackgroundColor(0xFF24262A.toInt())
            binding.btnTabRt.setTextColor(0xFFE3E2E6.toInt())
            loadDriversList()
        }
        binding.btnTabRt.setOnClickListener {
            currentMode = "RT"
            binding.btnTabRt.setBackgroundColor(0xFFD0BCFF.toInt())
            binding.btnTabRt.setTextColor(0xFF381E72.toInt())
            binding.btnTabQx.setBackgroundColor(0xFF24262A.toInt())
            binding.btnTabQx.setTextColor(0xFFE3E2E6.toInt())
            loadDriversList()
        }
    }

    private fun loadDriversList() {
        viewLifecycleOwner.lifecycleScope.launch {
            val drivers = viewModel.getDriversForMode(currentMode)
            binding.tvDriversCount.text = "AVAILABLE MODULES (${drivers.size})"
            binding.rvDriversList.adapter = DriversAdapter(drivers, currentMode) { selectedFile ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Execute Driver Module")
                    .setMessage("Execute ${selectedFile.name} with superuser permissions?")
                    .setPositiveButton("Execute") { _, _ ->
                        viewModel.loadDriverScript(selectedFile)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class DriversAdapter(
        private val files: List<File>,
        private val mode: String,
        private val onExecute: (File) -> Unit
    ) : RecyclerView.Adapter<DriversAdapter.ViewHolder>() {

        class ViewHolder(val binding: ItemDriverCardBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemDriverCardBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val file = files[position]
            holder.binding.tvDriverName.text = file.name
            holder.binding.tvDriverMode.text = "Mode: $mode • ${file.parentFile?.name ?: ""}"
            holder.binding.btnExecuteDriver.setOnClickListener { onExecute(file) }
            holder.itemView.setOnClickListener { onExecute(file) }
        }

        override fun getItemCount() = files.size
    }
}
