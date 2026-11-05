package com.andorid.ninmods.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.InputStreamReader

/**
 * Interactive Root Process Bridge
 * Manages background root execution of native binaries/scripts without raw terminal windows.
 * Exposes real-time stdout listening, license key prompt detection, and stdin piping.
 */
class InteractiveProcessBridge(
    private val scope: CoroutineScope
) {
    private var activeProcess: Process? = null
    private var outputStream: DataOutputStream? = null
    private var readerJob: Job? = null

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _licensePromptTriggered = MutableStateFlow(false)
    val licensePromptTriggered: StateFlow<Boolean> = _licensePromptTriggered.asStateFlow()

    private val _outputLines = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val outputLines: SharedFlow<String> = _outputLines.asSharedFlow()

    private val _executionStatus = MutableStateFlow<String?>(null)
    val executionStatus: StateFlow<String?> = _executionStatus.asStateFlow()

    /**
     * Start root execution of a script or binary file.
     */
    fun start(
        executableFile: File,
        workingDir: File = executableFile.parentFile ?: executableFile,
        onLicensePrompt: () -> Unit = {}
    ) {
        if (_isRunning.value) return

        scope.launch(Dispatchers.IO) {
            try {
                _executionStatus.value = "Starting execution..."
                _licensePromptTriggered.value = false

                val suBin = listOf(
                    "/system/bin/su",
                    "/system/xbin/su",
                    "/data/adb/ap/bin/su",
                    "/data/adb/magisk/su"
                ).firstOrNull { File(it).exists() } ?: "su"

                val proc = ProcessBuilder(suBin)
                    .directory(workingDir)
                    .redirectErrorStream(true)
                    .start()

                activeProcess = proc
                val out = DataOutputStream(proc.outputStream)
                outputStream = out
                _isRunning.value = true

                // Grant execution rights and launch
                out.writeBytes("chmod 755 '${executableFile.absolutePath}'\n")
                out.writeBytes("cd '${workingDir.absolutePath}'\n")
                if (executableFile.name.endsWith(".sh")) {
                    out.writeBytes("sh '${executableFile.absolutePath}'\n")
                } else {
                    out.writeBytes("'${executableFile.absolutePath}'\n")
                }
                out.flush()

                val reader = BufferedReader(InputStreamReader(proc.inputStream, Charsets.UTF_8))
                readerJob = launch(Dispatchers.IO) {
                    val buffer = CharArray(1024)
                    val accumulated = StringBuilder()
                    var charsRead: Int
                    try {
                        while (reader.read(buffer).also { charsRead = it } != -1) {
                            val chunk = String(buffer, 0, charsRead)
                            accumulated.append(chunk)

                            // Emit lines or chunks to output stream
                            _outputLines.tryEmit(chunk)

                            val window = accumulated.takeLast(512).toString()

                            // Detect license prompt in terminal output even without trailing newline
                            if (window.contains("Enter API Access Key", ignoreCase = true) ||
                                window.contains("Enter License Key", ignoreCase = true) ||
                                window.contains("[?] Enter", ignoreCase = true) ||
                                (window.contains("Authentication", ignoreCase = true) && window.contains("Key", ignoreCase = true))
                            ) {
                                if (!_licensePromptTriggered.value) {
                                    _licensePromptTriggered.value = true
                                    launch(Dispatchers.Main) {
                                        onLicensePrompt()
                                    }
                                }
                            }

                            // Detect license success
                            if (window.contains("validated successfully", ignoreCase = true) ||
                                window.contains("Core engine authorized", ignoreCase = true) ||
                                (window.contains("Active", ignoreCase = true) && window.contains("Runtime", ignoreCase = true))
                            ) {
                                _executionStatus.value = "Active & Running in RAM"
                            }
                        }
                    } catch (_: Exception) {}
                }

                val exitCode = proc.waitFor()
                _isRunning.value = false
                _licensePromptTriggered.value = false
                _executionStatus.value = if (exitCode == 0) "Completed" else "Exited (Code $exitCode)"

            } catch (e: Exception) {
                _isRunning.value = false
                _executionStatus.value = "Execution failed: ${e.message}"
            } finally {
                cleanup()
            }
        }
    }

    /**
     * Feeds the license key or user input directly into the process's stdin pipe.
     */
    suspend fun sendInput(text: String): Boolean = withContext(Dispatchers.IO) {
        val out = outputStream ?: return@withContext false
        try {
            out.writeBytes("${text.trim()}\n")
            out.flush()
            _licensePromptTriggered.value = false
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Stop and terminate background process.
     */
    fun stop() {
        cleanup()
    }

    private fun cleanup() {
        try {
            outputStream?.writeBytes("exit\n")
            outputStream?.flush()
        } catch (_: Exception) {}
        try {
            activeProcess?.destroy()
        } catch (_: Exception) {}
        readerJob?.cancel()
        activeProcess = null
        outputStream = null
        _isRunning.value = false
        _licensePromptTriggered.value = false
    }
}
