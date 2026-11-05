package com.andorid.ninmods.ui.compose

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.andorid.ninmods.data.SessionManager
import com.andorid.ninmods.ui.MainViewModel
import com.andorid.ninmods.ui.dashboard.DashboardScreen
import com.andorid.ninmods.ui.loader.DriverLoaderScreen
import com.andorid.ninmods.ui.profile.ProfileScreen
import com.andorid.ninmods.ui.settings.SettingsScreen

enum class MainDestination(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DASHBOARD("Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    LOADER("Driver Loader", Icons.Filled.Build, Icons.Outlined.Build),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
    PROFILE("Profile", Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: MainViewModel,
    sessionManager: SessionManager,
    onSignOut: () -> Unit
) {
    var currentDestination by remember { mutableStateOf(MainDestination.LOADER) }
    var previousDestination by remember { mutableStateOf(MainDestination.LOADER) }

    val bottomBarDestinations = listOf(
        MainDestination.DASHBOARD,
        MainDestination.LOADER,
        MainDestination.SETTINGS
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentDestination.label,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (currentDestination == MainDestination.PROFILE) {
                        IconButton(onClick = { currentDestination = previousDestination }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    if (currentDestination != MainDestination.PROFILE) {
                        IconButton(onClick = {
                            previousDestination = currentDestination
                            currentDestination = MainDestination.PROFILE
                        }) {
                            Icon(
                                imageVector = Icons.Filled.AccountCircle,
                                contentDescription = "Profile",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (currentDestination in bottomBarDestinations) {
                NavigationBar(tonalElevation = 8.dp) {
                    bottomBarDestinations.forEach { dest ->
                        val isSelected = currentDestination == dest
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentDestination = dest },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) dest.selectedIcon else dest.unselectedIcon,
                                    contentDescription = dest.label
                                )
                            },
                            label = {
                                Text(
                                    text = dest.label,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            alwaysShowLabel = false
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Crossfade(
            targetState = currentDestination,
            label = "ScreenTransition",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { destination ->
            when (destination) {
                MainDestination.DASHBOARD -> DashboardScreen(sessionManager = sessionManager)
                MainDestination.LOADER -> DriverLoaderScreen(
                    viewModel = viewModel,
                    sessionManager = sessionManager
                )
                MainDestination.SETTINGS -> SettingsScreen(
                    sessionManager = sessionManager,
                    onSignOut = onSignOut
                )
                MainDestination.PROFILE -> ProfileScreen(
                    sessionManager = sessionManager,
                    onSignOut = onSignOut
                )
            }
        }
    }
}
