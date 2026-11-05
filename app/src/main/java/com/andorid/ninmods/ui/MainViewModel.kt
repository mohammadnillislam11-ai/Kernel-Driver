package com.andorid.ninmods.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.andorid.ninmods.data.DriverRepository
import com.andorid.ninmods.data.ShellExecutor
import com.andorid.ninmods.util.DeviceNodeManager
import com.andorid.ninmods.util.KernelDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class LoaderUiState(
    val kernelRelease: String = "",
    val detectedMajorMinor: String = "",
    val isRootAvailable: Boolean = false,
    val isDriverLoaded: Boolean = false,
    val activeNodes: List<String> = emptyList(),
    val logMessages: List<String> = emptyList(),
    val isWorking: Boolean = false,
    val matchingDrivers: List<File> = emptyList()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DriverRepository(application)
    private val _uiState = MutableStateFlow(LoaderUiState())
    val uiState: StateFlow<LoaderUiState> = _uiState.asStateFlow()

    init {
        initEnvironment()
    }

    private fun initEnvironment() {
        val kernel = KernelDetector.detectKernelRelease()
        val mm = KernelDetector.getMajorMinor(kernel)
        val nodes = DeviceNodeManager.checkActiveNodes()

        _uiState.value = _uiState.value.copy(
            kernelRelease = kernel,
            detectedMajorMinor = mm,
            isDriverLoaded = nodes.isNotEmpty(),
            activeNodes = nodes
        )
        appendLog("[SYSTEM] Kernel Detected: $kernel (Base: $mm)")
        if (nodes.isNotEmpty()) {
            appendLog("[SYSTEM] Driver Nodes Found: ${nodes.joinToString(", ")}")
        }

        checkRootAndExtract()
    }

    private fun checkRootAndExtract() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isWorking = true)
            appendLog("[ROOT] Verifying superuser privileges...")

            var rootGranted = false
            ShellExecutor.executeRootCommand(listOf("id", "which su")).collect { out ->
                when (out) {
                    is ShellExecutor.ShellOutput.Line -> {
                        appendLog(out.text)
                        if (out.text.contains("uid=0")) rootGranted = true
                    }
                    is ShellExecutor.ShellOutput.Complete -> {
                        if (rootGranted || out.success) {
                            appendLog("[ROOT] Superuser access granted (uid=0).")
                            _uiState.value = _uiState.value.copy(isRootAvailable = true)
                        } else {
                            appendLog("[ERROR] Root permission denied. Root is required!")
                        }
                    }
                    is ShellExecutor.ShellOutput.Error -> appendLog("[ERROR] ${out.message}")
                }
            }

            if (_uiState.value.isRootAvailable) {
                repository.extractDriversIfNeeded { log ->
                    appendLog(log)
                }
            }
            _uiState.value = _uiState.value.copy(isWorking = false)
        }
    }

    fun prepareDrivers(mode: String, onSelectionRequired: (List<File>) -> Unit) {
        viewModelScope.launch {
            val kernel = _uiState.value.kernelRelease
            appendLog("[LOADER] Finding drivers for $mode mode matching kernel $kernel...")
            val matches = repository.findMatchingDrivers(kernel, mode)
            appendLog("[SELECT] ${matches.size} drivers found. Presenting selection dialog.")
            onSelectionRequired(matches)
        }
    }

    suspend fun getDriversForMode(mode: String): List<File> {
        return repository.getAllDrivers(mode)
    }

    suspend fun getMatchingDrivers(mode: String): List<File> {
        val kernel = _uiState.value.kernelRelease
        return repository.findMatchingDrivers(kernel, mode)
    }

    fun loadDriverScript(file: File) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isWorking = true)
            val path = file.absolutePath
            appendLog("────────────────────────────────────────")
            appendLog("[EXEC] Preparing script: ${file.name}")

            val commands = listOf(
                "chmod 777 '$path'",
                "sh '$path'",
                "for s in /sys/dev/char/*/uevent; do if [ -e \"\$s\" ]; then eval \"\$(grep -E '^(MAJOR|MINOR|DEVNAME)=' \"\$s\")\"; if [ -n \"\$DEVNAME\" ] && [ -n \"\$MAJOR\" ] && [ -n \"\$MINOR\" ]; then [ ! -e \"/dev/\$DEVNAME\" ] && mknod \"/dev/\$DEVNAME\" c \"\$MAJOR\" \"\$MINOR\" 2>/dev/null; case \"\$DEVNAME\" in BYNb|mem_tool|nh|wanbai) chmod 666 \"/dev/\$DEVNAME\" 2>/dev/null;; esac; fi; fi; done",
                "for n in /dev/BYNb /dev/mem_tool /dev/nh /dev/wanbai; do if [ -e \"\$n\" ]; then chmod 666 \"\$n\" 2>/dev/null; fi; done"
            )

            ShellExecutor.executeRootCommand(commands).collect { out ->
                when (out) {
                    is ShellExecutor.ShellOutput.Line -> appendLog(out.text)
                    is ShellExecutor.ShellOutput.Complete -> {
                        refreshStatus()
                        if (_uiState.value.isDriverLoaded) {
                            appendLog("[SUCCESS] Driver successfully loaded and device nodes are active!")
                        } else {
                            appendLog("[WARN] Script execution completed with code ${out.exitCode}.")
                        }
                    }
                    is ShellExecutor.ShellOutput.Error -> appendLog("[ERROR] ${out.message}")
                }
            }
            _uiState.value = _uiState.value.copy(isWorking = false)
        }
    }

    fun unloadDrivers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isWorking = true)
            appendLog("[UNLOAD] Removing kernel modules and stopping drivers...")
            val commands = listOf(
                "rmmod BYNb 2>/dev/null",
                "rmmod mem_tool 2>/dev/null",
                "rmmod nh 2>/dev/null",
                "rmmod wanbai 2>/dev/null",
                "for m in \$(cat /proc/modules 2>/dev/null | awk '{print \$1}'); do case \"\$m\" in *BYNb*|*mem_tool*|*nh*|*wanbai*|*5.10*|*4.19*|*4.14*) rmmod \"\$m\" 2>/dev/null;; esac; done",
                "pkill -f 'Driver.sh' 2>/dev/null",
                "pkill -f 'QX' 2>/dev/null",
                "pkill -f 'RT' 2>/dev/null"
            )

            ShellExecutor.executeRootCommand(commands).collect { out ->
                when (out) {
                    is ShellExecutor.ShellOutput.Line -> appendLog(out.text)
                    is ShellExecutor.ShellOutput.Complete -> {
                        refreshStatus()
                        appendLog("[SUCCESS] Unload routines completed.")
                    }
                    is ShellExecutor.ShellOutput.Error -> appendLog("[ERROR] ${out.message}")
                }
            }
            _uiState.value = _uiState.value.copy(isWorking = false)
        }
    }

    fun refreshStatus() {
        val nodes = DeviceNodeManager.checkActiveNodes()
        _uiState.value = _uiState.value.copy(
            isDriverLoaded = nodes.isNotEmpty(),
            activeNodes = nodes
        )
    }

    fun clearLog() {
        _uiState.value = _uiState.value.copy(logMessages = emptyList())
    }

    private fun appendLog(msg: String) {
        val current = _uiState.value.logMessages.toMutableList()
        current.add(msg)
        _uiState.value = _uiState.value.copy(logMessages = current)
    }
}
