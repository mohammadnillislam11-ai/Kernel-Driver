package com.andorid.ninmods.ui.loader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andorid.ninmods.data.SessionManager
import com.andorid.ninmods.ui.MainViewModel
import com.andorid.ninmods.ui.clock.LicenseClockCard
import java.io.File

@Composable
fun DriverLoaderScreen(
    viewModel: MainViewModel,
    sessionManager: SessionManager,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    var showDriverDialog by remember { mutableStateOf(false) }
    var dialogMode by remember { mutableStateOf("") }
    var dialogDrivers by remember { mutableStateOf<List<File>>(emptyList()) }

    val logListState = rememberLazyListState()

    LaunchedEffect(state.logMessages.size) {
        if (state.logMessages.isNotEmpty()) {
            logListState.animateScrollToItem(state.logMessages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // 1. Digital License Expiration Clock (Hours - Minutes - Seconds)
        LicenseClockCard(sessionManager = sessionManager)

        // 2. APatch 2 Side-by-Side Status Cards (Simple: 1 line status, no extra text clutter)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card 1: Kernel Driver (Left)
            ElevatedCard(
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.refreshStatus() },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (state.isDriverLoaded) Icons.Filled.CheckCircle else Icons.Filled.Build,
                        contentDescription = "Kernel Driver",
                        tint = if (state.isDriverLoaded) Color(0xFF81C784) else Color(0xFFFFB4AB),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (state.isDriverLoaded) "Working" else "Not Loaded",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (state.isDriverLoaded) Color(0xFF81C784) else Color(0xFFFFB4AB)
                    )
                }
            }

            // Card 2: SuperUser (Right)
            ElevatedCard(
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.requestRootAndCheck() },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (state.isRootAvailable) Icons.Filled.CheckCircle else Icons.Filled.Security,
                        contentDescription = "SuperUser",
                        tint = if (state.isRootAvailable) Color(0xFF81C784) else Color(0xFFFFB4AB),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (state.isRootAvailable) "Granted" else "Denied",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (state.isRootAvailable) Color(0xFF81C784) else Color(0xFFFFB4AB)
                    )
                }
            }
        }

        // 3. Simple Action Buttons: QX and RT (Borderless Material 3)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    viewModel.prepareDrivers("QX") { matches ->
                        dialogMode = "QX"
                        dialogDrivers = matches
                        showDriverDialog = true
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "QX",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = {
                    viewModel.prepareDrivers("RT") { matches ->
                        dialogMode = "RT"
                        dialogDrivers = matches
                        showDriverDialog = true
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "RT",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 4. Stop Driver Button (Enabled strictly when driver is loaded)
        Button(
            onClick = { viewModel.unloadDrivers() },
            enabled = state.isDriverLoaded,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
            )
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Stop Driver",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = "Stop Driver",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // 5. Embedded Real-Time Live Console Terminal Card
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = Color(0xFF111418)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TERMINAL LOG",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = { viewModel.clearLog() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Clear",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                LazyColumn(
                    state = logListState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.logMessages) { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            ),
                            color = when {
                                line.contains("[ERROR]") || line.contains("failed") -> Color(0xFFFFB4AB)
                                line.contains("[SUCCESS]") -> Color(0xFF81C784)
                                line.contains("[WARN]") -> Color(0xFFFFCC80)
                                line.contains("[ROOT]") || line.contains("[SELECT]") -> Color(0xFF90CAF9)
                                else -> Color(0xFFE0E0E0)
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // 6. Official Android / Material 3 Dialog for Kernel Driver Selection
    if (showDriverDialog) {
        AlertDialog(
            onDismissRequest = { showDriverDialog = false },
            title = {
                Text(
                    text = "Select $dialogMode Driver",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                if (dialogDrivers.isEmpty()) {
                    Text(
                        text = "No matching drivers found for kernel ${state.kernelRelease}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        dialogDrivers.forEach { file ->
                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showDriverDialog = false
                                        viewModel.loadDriverScript(file)
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                elevation = CardDefaults.cardElevation(0.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Build,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.size(12.dp))
                                    Text(
                                        text = file.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDriverDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}
