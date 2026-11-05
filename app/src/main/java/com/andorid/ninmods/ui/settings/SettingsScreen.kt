package com.andorid.ninmods.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.andorid.ninmods.data.SessionManager
import com.andorid.ninmods.ui.component.SwitchItem
import com.andorid.ninmods.ui.dialogs.AppSelectionBottomSheet
import com.andorid.ninmods.ui.dialogs.SheetItem
import com.andorid.ninmods.util.RootDetector
import com.andorid.ninmods.util.ShellExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    sessionManager: SessionManager,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var rootManagerName by remember { mutableStateOf("Detecting...") }
    var isSuGranted by remember { mutableStateOf(false) }
    var isStorageGranted by remember { mutableStateOf(false) }

    var globalNamespace by remember { mutableStateOf(true) }
    var legacySu by remember { mutableStateOf(false) }
    var showRebootSheet by remember { mutableStateOf(false) }

    val storageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        isStorageGranted = permissions.values.any { it } || permissions.isEmpty()
        Toast.makeText(context, "Storage permissions updated", Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val rootInfo = RootDetector.detect(context)
            rootManagerName = rootInfo.managerName

            val suTest = try {
                val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
                val out = p.inputStream.bufferedReader().readText()
                out.contains("uid=0")
            } catch (e: Exception) {
                false
            }
            withContext(Dispatchers.Main) {
                isSuGranted = suTest
            }
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

        // PERMISSIONS & SYSTEM ROOT (User can grant/check anytime)
        Text(
            text = "PERMISSIONS & ROOT ENVIRONMENT",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp)
        )

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column {
                // 1. Root Manager Item
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Root Manager",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Detected: $rootManagerName",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF81C784),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 2. SuperUser Access (su) Item - Tap to request
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch(Dispatchers.IO) {
                                val granted = try {
                                    val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
                                    val out = p.inputStream.bufferedReader().readText()
                                    out.contains("uid=0")
                                } catch (e: Exception) {
                                    false
                                }
                                withContext(Dispatchers.Main) {
                                    isSuGranted = granted
                                    val msg = if (granted) "SuperUser (uid=0) authorized!" else "SuperUser request denied."
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.VpnKey,
                        contentDescription = null,
                        tint = if (isSuGranted) Color(0xFF81C784) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SuperUser (su) Access",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isSuGranted) "Granted (uid=0)" else "Tap to Authorize Root Prompt",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSuGranted) Color(0xFF81C784) else Color(0xFFFFB4AB)
                        )
                    }
                    if (isSuGranted) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF81C784),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // 3. Storage Permission Item - Tap to request
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            storageLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                                )
                            )
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Folder,
                        contentDescription = null,
                        tint = if (isStorageGranted) Color(0xFF81C784) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Storage Permission",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isStorageGranted) "Granted" else "Tap to Grant Storage Permission",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isStorageGranted) Color(0xFF81C784) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // APATCH ENVIRONMENT SWITCHES
        Text(
            text = "KERNEL CONFIGURATION",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp)
        )

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column {
                SwitchItem(
                    icon = Icons.Filled.Layers,
                    title = "Global Namespace",
                    summary = "Mount namespace shared with system services",
                    checked = globalNamespace,
                    onCheckedChange = { globalNamespace = it }
                )

                SwitchItem(
                    icon = Icons.Filled.Terminal,
                    title = "Legacy su Compatibility",
                    summary = "Provide /system/bin/su symlink",
                    checked = legacySu,
                    onCheckedChange = { legacySu = it }
                )
            }
        }

        // DEVICE POWER & REBOOT (Using universal AppSelectionBottomSheet)
        Text(
            text = "DEVICE POWER",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp)
        )

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showRebootSheet = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Reboot Device",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.size(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Reboot Device",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "System reboot, recovery, bootloader or soft reboot",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // SESSION
        Text(
            text = "SESSION",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp)
        )

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.VerifiedUser,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Session: Active",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = sessionManager.userName ?: "NinMods User",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSignOut() }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.ExitToApp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    Text(
                        text = "Sign Out",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // COMMUNITY
        Text(
            text = "COMMUNITY",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp)
        )

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/ninmods"))
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Language,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.size(16.dp))
                Column {
                    Text(
                        text = "Telegram Channel",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "@ninmods",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Universal Reusable Selection Bottom Sheet for Device Reboot
    if (showRebootSheet) {
        val rebootOptions = listOf(
            SheetItem(
                value = "reboot",
                title = "Normal Reboot",
                subtitle = "Restart system cleanly",
                icon = Icons.Filled.Refresh
            ),
            SheetItem(
                value = "recovery",
                title = "Reboot to Recovery",
                subtitle = "Boot into TWRP or OrangeFox recovery",
                icon = Icons.Filled.Terminal
            ),
            SheetItem(
                value = "bootloader",
                title = "Reboot to Bootloader",
                subtitle = "Boot into Fastboot mode",
                icon = Icons.Filled.Security
            ),
            SheetItem(
                value = "soft",
                title = "Soft Reboot",
                subtitle = "Restart Android runtime (Zygote)",
                icon = Icons.Filled.Refresh
            )
        )

        AppSelectionBottomSheet(
            title = "Reboot Device",
            subtitle = "Select desired reboot mode",
            items = rebootOptions,
            actionButtonText = "Reboot",
            onDismissRequest = { showRebootSheet = false },
            onItemSelected = { mode ->
                showRebootSheet = false
                scope.launch(Dispatchers.IO) {
                    when (mode) {
                        "reboot" -> ShellExecutor.executeRoot("svc power reboot || reboot")
                        "recovery" -> ShellExecutor.executeRoot("reboot recovery")
                        "bootloader" -> ShellExecutor.executeRoot("reboot bootloader")
                        "soft" -> ShellExecutor.executeRoot("setprop ctl.restart zygote")
                    }
                }
            }
        )
    }
}
