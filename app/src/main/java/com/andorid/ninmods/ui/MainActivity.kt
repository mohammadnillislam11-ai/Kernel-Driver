package com.andorid.ninmods.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.andorid.ninmods.R
import com.andorid.ninmods.databinding.ActivityMainBinding
import com.andorid.ninmods.ui.dashboard.DashboardFragment
import com.andorid.ninmods.ui.home.HomeFragment
import com.andorid.ninmods.ui.settings.SettingsFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupFloatingNavBar()

        // 1st page: Dashboard by default
        if (savedInstanceState == null) {
            selectTab(0)
        }
    }

    private fun setupFloatingNavBar() {
        binding.tabDashboard.setOnClickListener { selectTab(0) }
        binding.tabLoader.setOnClickListener { selectTab(1) }
        binding.tabSettings.setOnClickListener { selectTab(2) }
    }

    private fun selectTab(index: Int) {
        // Reset all tabs to unselected
        binding.tabDashboard.setBackgroundResource(0)
        binding.tabLoader.setBackgroundResource(0)
        binding.tabSettings.setBackgroundResource(0)

        binding.iconDashboard.setColorFilter(0xFF939AA5.toInt())
        binding.iconLoader.setColorFilter(0xFF939AA5.toInt())
        binding.iconSettings.setColorFilter(0xFF939AA5.toInt())

        val fragment: Fragment = when (index) {
            0 -> {
                // 1st page: Dashboard (4 Apps Side-by-Side)
                binding.tabDashboard.setBackgroundResource(R.drawable.apatch_pill_selected)
                binding.iconDashboard.setColorFilter(0xFFD0BCFF.toInt())
                DashboardFragment()
            }
            1 -> {
                // 2nd page: Driver Loader (APatch 2 Side-by-Side Cards Dashboard & Controls)
                binding.tabLoader.setBackgroundResource(R.drawable.apatch_pill_selected)
                binding.iconLoader.setColorFilter(0xFFD0BCFF.toInt())
                HomeFragment()
            }
            2 -> {
                // 3rd page: Settings
                binding.tabSettings.setBackgroundResource(R.drawable.apatch_pill_selected)
                binding.iconSettings.setColorFilter(0xFFD0BCFF.toInt())
                SettingsFragment()
            }
            else -> DashboardFragment()
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
