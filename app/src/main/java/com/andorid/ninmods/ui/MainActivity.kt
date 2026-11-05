package com.andorid.ninmods.ui

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.andorid.ninmods.R
import com.andorid.ninmods.databinding.ActivityMainBinding
import com.andorid.ninmods.ui.dashboard.DashboardFragment
import com.andorid.ninmods.ui.home.HomeFragment
import com.andorid.ninmods.ui.drivers.DriversFragment
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
        binding.tabHome.setOnClickListener { selectTab(0) }
        binding.tabDrivers.setOnClickListener { selectTab(1) }
        binding.tabTuning.setOnClickListener { selectTab(2) }
        binding.tabSettings.setOnClickListener { selectTab(3) }
    }

    private fun selectTab(index: Int) {
        // Reset all tabs to unselected icon buttons
        binding.tabHome.setBackgroundResource(0)
        binding.tabDrivers.setBackgroundResource(0)
        binding.tabTuning.setBackgroundResource(0)
        binding.tabSettings.setBackgroundResource(0)

        binding.textHome.visibility = View.GONE
        binding.textDrivers.visibility = View.GONE
        binding.textTuning.visibility = View.GONE
        binding.textSettings.visibility = View.GONE

        binding.iconHome.setColorFilter(0xFF939AA5.toInt())
        binding.iconDrivers.setColorFilter(0xFF939AA5.toInt())
        binding.iconTuning.setColorFilter(0xFF939AA5.toInt())
        binding.iconSettings.setColorFilter(0xFF939AA5.toInt())

        val fragment: Fragment = when (index) {
            0 -> {
                // 1st page: Dashboard (Available mods logo & status)
                binding.tabHome.setBackgroundResource(R.drawable.apatch_pill_selected)
                binding.textHome.visibility = View.VISIBLE
                binding.iconHome.setColorFilter(0xFFD0BCFF.toInt())
                DashboardFragment()
            }
            1 -> {
                // 2nd page: Driver Loader (Core kernel loader & controls)
                binding.tabDrivers.setBackgroundResource(R.drawable.apatch_pill_selected)
                binding.textDrivers.visibility = View.VISIBLE
                binding.iconDrivers.setColorFilter(0xFFD0BCFF.toInt())
                HomeFragment()
            }
            2 -> {
                // 3rd page: Drivers Module Archive
                binding.tabTuning.setBackgroundResource(R.drawable.apatch_pill_selected)
                binding.textTuning.visibility = View.VISIBLE
                binding.iconTuning.setColorFilter(0xFFD0BCFF.toInt())
                DriversFragment()
            }
            3 -> {
                // 4th page: Settings
                binding.tabSettings.setBackgroundResource(R.drawable.apatch_pill_selected)
                binding.textSettings.visibility = View.VISIBLE
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
