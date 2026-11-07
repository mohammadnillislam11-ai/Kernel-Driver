package com.andorid.ninmods.ui.dashboard.kernel_mods

import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andorid.ninmods.data.InteractiveProcessBridge
import com.andorid.ninmods.data.ModDownloader
import com.andorid.ninmods.data.SessionManager
import com.andorid.ninmods.ui.dialogs.ApatchConfirmDownloadDialog
import com.andorid.ninmods.ui.dialogs.ApatchDownloadDialog
import com.andorid.ninmods.ui.dialogs.ApatchLicenseKeyDialog
import com.andorid.ninmods.ui.dialogs.ApatchWarningDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class KernelModItem(
    val id: String,
    val name: String,
    val fileName: String,
    val downloadPackage: String,
    val file: File,
    val isDownloaded: Boolean,
    val icon: ImageVector
)

@Composable
fun KernelModsCard(
    sessionManager: SessionManager? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val processBridge = remember { InteractiveProcessBridge(scope) }
    val isProcessRunning by processBridge.isRunning.collectAsState()
    val isLicensePromptTriggered by processBridge.licensePromptTriggered.collectAsState()

    var mods by remember { mutableStateOf<List<KernelModItem>>(emptyList()) }
    var activeModId by remember { mutableStateOf<String?>(null) }

    // Dialog state holders
    var confirmDownloadMod by remember { mutableStateOf<KernelModItem?>(null) }
    var downloadingMod by remember { mutableStateOf<KernelModItem?>(null) }
    var downloadProgress by remember { mutableIntStateOf(0) }

    var warningMod by remember { mutableStateOf<KernelModItem?>(null) }

    var showLicenseDialog by remember { mutableStateOf(false) }
    var isSubmittingLicense by remember { mutableStateOf(false) }
    var licenseErrorMessage by remember { mutableStateOf<String?>(null) }

    // Automatically trigger license pop-up dialog when stdin prompt is detected
    LaunchedEffect(isLicensePromptTriggered) {
        if (isLicensePromptTriggered) {
            showLicenseDialog = true
        }
    }

    fun refreshMods() {
        scope.launch {
            mods = withContext(Dispatchers.IO) {
                val internalDir = File(context.filesDir, "kernel_mods").also { if (!it.exists()) it.mkdirs() }

                // 1. Core Built-In Official Kernel Mods (downloaded directly from backend server /downloads/kernel/)
                val defaultSpecs = listOf(
                    Triple("Kernel Core Engine", "run_ram.sh", "kernel/run_ram.sh"),
                    Triple("Driver Node Loader", "driver_loader.sh", "kernel/driver_loader.sh"),
                    Triple("Kernel Security Bypass", "sec_bypass.sh", "kernel/sec_bypass.sh")
                )

                val defaultItems = defaultSpecs.map { (name, fileName, downloadPkg) ->
                    val file = File(internalDir, fileName)
                    KernelModItem(
                        id = fileName,
                        name = name,
                        fileName = fileName,
                        downloadPackage = downloadPkg,
                        file = file,
                        isDownloaded = file.exists(),
                        icon = resolveKernelIcon(fileName)
                    )
                }

                // 2. Custom User Mods (from sdcard / internal)
                val sdcardDir = try {
                    File(Environment.getExternalStorageDirectory(), "NinMods/kernel_mods").also {
                        if (!it.exists()) it.mkdirs()
                    }
                } catch (_: Exception) { null }

                val customFiles = (internalDir.listFiles()?.toList() ?: emptyList()) +
                        (sdcardDir?.listFiles()?.toList() ?: emptyList())

                val userItems = customFiles
                    .filter { it.isFile && !it.name.startsWith(".") && it.name.endsWith(".sh") }
                    .filter { file -> defaultSpecs.none { it.second == file.name } }
                    .distinctBy { it.name }
                    .map { file ->
                        KernelModItem(
                            id = "custom_${file.name}",
                            name = formatModName(file.name),
                            fileName = file.name,
                            downloadPackage = file.name,
                            file = file,
                            isDownloaded = true,
                            icon = resolveKernelIcon(file.name)
                        )
                    }

                defaultItems + userItems
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshMods()
    }

    // ─── 0. APATCH CONFIRM DOWNLOAD POP-UP DIALOG ───
    confirmDownloadMod?.let { mod ->
        ApatchConfirmDownloadDialog(
            modName = mod.name,
            onDismiss = { confirmDownloadMod = null },
            onConfirmDownload = {
                confirmDownloadMod = null
                downloadingMod = mod
                downloadProgress = 0
                scope.launch {
                    val result = ModDownloader.downloadMod(
                        context = context,
                        downloadUrl = mod.downloadPackage,
                        targetDir = mod.file.parentFile ?: File(context.filesDir, "kernel_mods"),
                        fileName = mod.fileName,
                        onProgress = { p -> downloadProgress = p }
                    )
                    downloadingMod = null
                    if (result.isSuccess) {
                        Toast.makeText(context, "${mod.name} downloaded successfully!", Toast.LENGTH_SHORT).show()
                        refreshMods()
                    } else {
                        Toast.makeText(context, result.exceptionOrNull()?.message ?: "Download failed", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    // ─── 1. APATCH ANIMATED DOWNLOAD DIALOG ───
    downloadingMod?.let { mod ->
        ApatchDownloadDialog(
            modName = mod.name,
            progress = downloadProgress,
            onCancel = {
                downloadingMod = null
                downloadProgress = 0
            }
        )
    }

    // ─── 2. APATCH WARNING CONFIRMATION DIALOG ───
    warningMod?.let { mod ->
        ApatchWarningDialog(
            modName = mod.name,
            onDismiss = { warningMod = null },
            onConfirmLaunch = {
                val targetMod = mod
                warningMod = null
                activeModId = targetMod.id
                licenseErrorMessage = null
                Toast.makeText(context, "Launching ${targetMod.name} into memory...", Toast.LENGTH_SHORT).show()
                val deviceHwid = com.andorid.ninmods.util.DeviceIdentifier.getHwid(context)
                val initialKey = sessionManager?.activeLicenseKey?.ifBlank { sessionManager?.userPassword ?: "" } ?: (sessionManager?.userPassword ?: "")
                processBridge.start(
                    executableFile = targetMod.file,
                    deviceHwid = deviceHwid,
                    licenseKey = initialKey,
                    onLicensePrompt = {
                        showLicenseDialog = true
                    }
                )
            }
        )
    }

    // ─── 3. APATCH MATERIAL 3 LICENSE KEY POP-UP DIALOG ───
    if (showLicenseDialog) {
        val initialKey = sessionManager?.activeLicenseKey?.ifBlank { sessionManager?.userPassword ?: "" } ?: (sessionManager?.userPassword ?: "")
        ApatchLicenseKeyDialog(
            initialKey = initialKey,
            isSubmitting = isSubmittingLicense,
            errorMessage = licenseErrorMessage,
            onDismiss = {
                showLicenseDialog = false
                isSubmittingLicense = false
                licenseErrorMessage = null
            },
            onSubmitKey = { submittedKey ->
                isSubmittingLicense = true
                licenseErrorMessage = null
                scope.launch {
                    val deviceHwid = com.andorid.ninmods.util.DeviceIdentifier.getHwid(context)
                    val activationResult = com.andorid.ninmods.network.AuthApiClient.activateLicense(
                        licenseKey = submittedKey,
                        hwid = deviceHwid
                    )
                    if (activationResult.isSuccess) {
                        val licInfo = activationResult.getOrNull()
                        sessionManager?.let { sm ->
                            sm.activeLicenseKey = submittedKey
                            sm.serverRemainingSeconds = licInfo?.remainingSeconds ?: 86400L
                        }
                    }

                    val sent = processBridge.sendInput(submittedKey)
                    isSubmittingLicense = false
                    if (sent) {
                        showLicenseDialog = false
                        Toast.makeText(context, "License dispatched to core engine.", Toast.LENGTH_SHORT).show()
                    } else {
                        val runningMod = mods.find { it.id == activeModId }
                        if (runningMod != null) {
                            processBridge.stop()
                            processBridge.start(
                                executableFile = runningMod.file,
                                deviceHwid = deviceHwid,
                                licenseKey = submittedKey,
                                onLicensePrompt = { showLicenseDialog = true }
                            )
                            showLicenseDialog = false
                            Toast.makeText(context, "Engine reloaded with active license.", Toast.LENGTH_SHORT).show()
                        } else {
                            licenseErrorMessage = "Failed to communicate with running background process."
                        }
                    }
                }
            }
        )
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Memory,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "KERNEL MODS",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = { refreshMods() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Refresh",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (mods.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = "No kernel mods configured",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 18.dp, horizontal = 12.dp)
                    )
                }
            } else {
                val rows = mods.chunked(3)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rows.forEach { rowMods ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowMods.forEach { mod ->
                                val isRunning = isProcessRunning && activeModId == mod.id

                                ElevatedCard(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            if (!mod.isDownloaded) {
                                                // Trigger APatch Official Download Confirmation Dialog
                                                confirmDownloadMod = mod
                                            } else {
                                                // Trigger APatch Official Warning Dialog
                                                warningMod = mod
                                            }
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.elevatedCardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 14.dp, horizontal = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = when {
                                                isRunning -> MaterialTheme.colorScheme.primaryContainer
                                                !mod.isDownloaded -> MaterialTheme.colorScheme.surfaceVariant
                                                else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                            },
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                // Always render official mod icon (PlayArrow if active)
                                                Icon(
                                                    imageVector = if (isRunning) Icons.Filled.PlayArrow else mod.icon,
                                                    contentDescription = mod.name,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = mod.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = when {
                                                isRunning -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                !mod.isDownloaded -> MaterialTheme.colorScheme.surfaceVariant
                                                else -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                            }
                                        ) {
                                            Text(
                                                text = when {
                                                    isRunning -> "ACTIVE"
                                                    !mod.isDownloaded -> "AVAILABLE"
                                                    else -> "READY"
                                                },
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when {
                                                    isRunning -> MaterialTheme.colorScheme.primary
                                                    !mod.isDownloaded -> MaterialTheme.colorScheme.onSurfaceVariant
                                                    else -> MaterialTheme.colorScheme.secondary
                                                },
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            repeat(3 - rowMods.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun resolveKernelIcon(fileName: String): ImageVector {
    val lower = fileName.lowercase()
    return when {
        lower.contains("driver") || lower.contains("node") -> Icons.Filled.Tune
        lower.contains("sec") || lower.contains("bypass") -> Icons.Filled.Security
        else -> Icons.Filled.Memory
    }
}

private fun formatModName(fileName: String): String {
    val base = fileName.substringBeforeLast(".")
    return base.replace("_", " ").replace("-", " ")
        .split(" ")
        .filter { it.isNotBlank() }
        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
}
