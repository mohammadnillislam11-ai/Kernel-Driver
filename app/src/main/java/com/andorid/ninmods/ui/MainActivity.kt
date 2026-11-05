package com.andorid.ninmods.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.andorid.ninmods.R
import com.andorid.ninmods.databinding.ActivityMainBinding
import com.andorid.ninmods.ui.dashboard.DashboardFragment
import com.andorid.ninmods.ui.home.HomeFragment
import com.andorid.ninmods.ui.settings.SettingsFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private var currentTabIndex = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupFloatingNavBar()

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
        if (currentTabIndex == index) return
        currentTabIndex = index

        updateTabState(binding.tabDashboard, binding.iconDashboard, index == 0)
        updateTabState(binding.tabLoader, binding.iconLoader, index == 1)
        updateTabState(binding.tabSettings, binding.iconSettings, index == 2)

        val fragment: Fragment = when (index) {
            0 -> DashboardFragment()
            1 -> HomeFragment()
            2 -> SettingsFragment()
            else -> DashboardFragment()
        }

        supportFragmentManager.beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun updateTabState(tabView: FrameLayout, iconView: ImageView, isSelected: Boolean) {
        if (isSelected) {
            tabView.setBackgroundResource(R.drawable.apatch_pill_selected)
            ImageViewCompat.setImageTintList(iconView, ColorStateList.valueOf(0xFFD0BCFF.toInt()))
        } else {
            tabView.setBackgroundResource(0)
            ImageViewCompat.setImageTintList(iconView, ColorStateList.valueOf(0xFF939AA5.toInt()))
        }
    }
}
