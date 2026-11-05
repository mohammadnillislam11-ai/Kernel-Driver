package com.andorid.ninmods.ui.drivers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.andorid.ninmods.R
import com.andorid.ninmods.databinding.FragmentDriversBinding
import com.andorid.ninmods.ui.MainViewModel
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
            loadDriversList()
        }
        binding.btnTabRt.setOnClickListener {
            currentMode = "RT"
            loadDriversList()
        }
    }

    private fun loadDriversList() {
        viewModel.prepareDrivers(currentMode) { matches ->
            binding.rvDriversList.adapter = DriversAdapter(matches) { selectedFile ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Execute Driver Module")
                    .setMessage("Do you want to execute ${selectedFile.name} as root?")
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
        private val onClick: (File) -> Unit
    ) : RecyclerView.Adapter<DriversAdapter.ViewHolder>() {

        class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(android.R.id.text1)
            val tvPath: TextView = view.findViewById(android.R.id.text2)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val file = files[position]
            holder.tvName.text = file.name
            holder.tvName.setTextColor(0xFFE3E2E6.toInt())
            holder.tvPath.text = file.parentFile?.name ?: ""
            holder.tvPath.setTextColor(0xFF8B949E.toInt())
            holder.view.setOnClickListener { onClick(file) }
        }

        override fun getItemCount() = files.size
    }
}
