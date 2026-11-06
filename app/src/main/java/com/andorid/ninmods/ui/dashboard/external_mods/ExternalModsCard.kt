package com.andorid.ninmods.ui.dashboard.external_mods

import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andorid.ninmods.R
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

data class ExternalModItem(
    val id: String,
    val name: String,
    val fileName: String,
    val downloadPackage: String,
    val file: File,
    val isDownloaded: Boolean
)

@Composable
fun ExternalModsCard(
    sessionManager: SessionManager? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val processBridge = remember { InteractiveProcessBridge(scope) }
    val isProcessRunning by processBridge.isRunning.collectAsState()
    val isLicensePromptTriggered by processBridge.licensePromptTriggered.collectAsState()
    val isReadyToStart by processBridge.isReadyToStart.collectAsState()

    var mods by remember { mutableStateOf<List<ExternalModItem>>(emptyList()) }
    var activeModId by remember { mutableStateOf<String?>(null) }

    // Dialog state holders
    var confirmDownloadMod by remember { mutableStateOf<ExternalModItem?>(null) }
    var downloadingMod by remember { mutableStateOf<ExternalModItem?>(null) }
    var downloadProgress by remember { mutableIntStateOf(0) }

    var stopConfirmMod by remember { mutableStateOf<ExternalModItem?>(null) }

    var showLicenseDialog by remember { mutableStateOf(false) }
    var isSubmittingLicense by remember { mutableStateOf(false) }
    var licenseErrorMessage by remember { mutableStateOf<String?>(null) }

    // Automatically trigger license pop-up dialog when stdin prompt is detected in running binary
    LaunchedEffect(isLicensePromptTriggered) {
        if (isLicensePromptTriggered) {
            showLicenseDialog = true
        }
    }

    LaunchedEffect(isReadyToStart) {
        if (isReadyToStart) {
            isSubmittingLicense = false
        }
    }

    fun refreshMods() {
        scope.launch {
            mods = withContext(Dispatchers.IO) {
                val internalDir = File(context.filesDir, "external_mods").also { if (!it.exists()) it.mkdirs() }

                // The single official external mod built from refer/nin
                val officialFileName = "gemini-cli.sh"
                val officialZipName = "gemini-cli.zip"
                val officialFile = File(internalDir, officialFileName)

                // Self-healing: if gemini-cli.zip is present locally but .sh is missing, unpack it automatically
                val internalZip = File(internalDir, officialZipName)
                if (internalZip.exists() && (!officialFile.exists() || officialFile.length() == 0L)) {
                    try {
                        ModDownloader.unzip(internalZip, internalDir)
                        officialFile.setExecutable(true, false)
                    } catch (_: Exception) {}
                }

                // If user downloaded via browser / MT Manager to Download folder, unpack to internalDir as well
                try {
                    val publicDownloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val publicZip = File(publicDownloadDir, officialZipName)
                    if (publicZip.exists() && (!officialFile.exists() || officialFile.length() == 0L)) {
                        ModDownloader.unzip(publicZip, internalDir)
                        officialFile.setExecutable(true, false)
                    }
                } catch (_: Exception) {}

                val officialMod = ExternalModItem(
                    id = officialFileName,
                    name = "NINMODS External",
                    fileName = officialZipName,
                    downloadPackage = "external/$officialZipName",
                    file = officialFile,
                    isDownloaded = officialFile.exists() && officialFile.length() > 0L
                )

                // Optional custom external scripts from sdcard
                val sdcardDir = try {
                    File(Environment.getExternalStorageDirectory(), "NinMods/external_mods").also {
                        if (!it.exists()) it.mkdirs()
                    }
                } catch (_: Exception) { null }

                val customFiles = (sdcardDir?.listFiles()?.toList() ?: emptyList())
                    .filter { it.isFile && !it.name.startsWith(".") && it.name.endsWith(".sh") && it.name != officialFileName }
                    .distinctBy { it.name }
                    .map { file ->
                        ExternalModItem(
                            id = file.name,
                            name = formatModName(file.name),
                            fileName = file.name,
                            downloadPackage = file.name,
                            file = file,
                            isDownloaded = true
                        )
                    }

                listOf(officialMod) + customFiles
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
                        targetDir = mod.file.parentFile ?: File(context.filesDir, "external_mods"),
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

    // ─── 2. APATCH STOP CONFIRMATION DIALOG (Cancel & Stop buttons) ───
    stopConfirmMod?.let { mod ->
        ApatchWarningDialog(
            modName = mod.name,
            title = "Stop Mod",
            message = "Do you want to stop \"${mod.name}\" and unload it from memory?",
            confirmButtonText = "Stop",
            onDismiss = { stopConfirmMod = null },
            onConfirmLaunch = {
                stopConfirmMod = null
                processBridge.stop()
                activeModId = null
                Toast.makeText(context, "${mod.name} stopped.", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // ─── 3. APATCH MATERIAL 3 LICENSE KEY POP-UP DIALOG (Verify -> Start buttons) ───
    if (showLicenseDialog) {
        val initialKey = sessionManager?.activeLicenseKey?.ifBlank { sessionManager?.userPassword ?: "" } ?: (sessionManager?.userPassword ?: "")
        ApatchLicenseKeyDialog(
            initialKey = initialKey,
            isSubmitting = isSubmittingLicense,
            errorMessage = licenseErrorMessage,
            isVerified = isReadyToStart,
            onDismiss = {
                showLicenseDialog = false
                isSubmittingLicense = false
                licenseErrorMessage = null
                processBridge.stop()
                activeModId = null
            },
            onSubmitKey = { submittedKey ->
                isSubmittingLicense = true
                licenseErrorMessage = null
                scope.launch {
                    val deviceHwid = com.andorid.ninmods.util.DeviceIdentifier.getHwid(context)
                    // Synchronize and verify with server
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

                        // Feed submitted license into running process stdin
                        val sent = processBridge.sendInput(submittedKey)
                        if (!sent) {
                            isSubmittingLicense = false
                            licenseErrorMessage = "Failed to communicate with running background process."
                        }
                        // Binary will verify and print readiness, setting isReadyToStart = true
                    } else {
                        isSubmittingLicense = false
                        licenseErrorMessage = activationResult.exceptionOrNull()?.message 
                            ?: "License verification failed on server."
                    }
                }
            },
            onStartMod = {
                scope.launch {
                    processBridge.sendInput("START")
                    showLicenseDialog = false
                    Toast.makeText(context, "Loading floating engine into RAM...", Toast.LENGTH_SHORT).show()
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
                    imageVector = Icons.Filled.Terminal,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "EXTERNAL MOD",
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
                        text = "No external mod configured",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 18.dp, horizontal = 12.dp)
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    mods.forEach { mod ->
                        val isRunning = isProcessRunning && activeModId == mod.id

                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (!mod.isDownloaded) {
                                        // 1. Not downloaded -> Show download confirmation dialog
                                        confirmDownloadMod = mod
                                    } else if (isRunning) {
                                        // 2. Already active -> Ask to stop
                                        stopConfirmMod = mod
                                    } else {
                                        // 3. Downloaded -> Launch directly in background; binary stdin prompt triggers ApatchLicenseKeyDialog
                                        activeModId = mod.id
                                        licenseErrorMessage = null
                                        Toast.makeText(context, "Initializing ${mod.name}...", Toast.LENGTH_SHORT).show()
                                        val deviceHwid = com.andorid.ninmods.util.DeviceIdentifier.getHwid(context)
                                        processBridge.start(
                                            executableFile = mod.file,
                                            deviceHwid = deviceHwid,
                                            licenseKey = "", // Force prompt detection so terminal opens ApatchLicenseKeyDialog
                                            onLicensePrompt = {
                                                showLicenseDialog = true
                                            }
                                        )
                                    }
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 14.dp, horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Official App Logo container
                                Surface(
                                    shape = CircleShape,
                                    color = when {
                                        isRunning -> MaterialTheme.colorScheme.primaryContainer
                                        !mod.isDownloaded -> MaterialTheme.colorScheme.surfaceVariant
                                        else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isRunning) {
                                            Icon(
                                                imageVector = Icons.Filled.PlayArrow,
                                                contentDescription = "Running",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        } else {
                                            // Official Software Logo
                                            Image(
                                                painter = painterResource(id = R.drawable.logo_n),
                                                contentDescription = mod.name,
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = mod.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = if (isRunning) "Process active & running in RAM" else "Official in-memory client engine",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Status badge
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
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
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            isRunning -> MaterialTheme.colorScheme.primary
                                            !mod.isDownloaded -> MaterialTheme.colorScheme.onSurfaceVariant
                                            else -> MaterialTheme.colorScheme.secondary
                                        },
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatModName(fileName: String): String {
    val base = fileName.substringBeforeLast(".")
    return base.replace("_", " ").replace("-", " ")
        .split(" ")
        .filter { it.isNotBlank() }
        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
}
