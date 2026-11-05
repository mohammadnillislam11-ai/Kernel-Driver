package com.andorid.ninmods.data

import android.content.Context
import android.os.Environment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class ModItem(
    val id: String,
    val name: String,
    val fileName: String,
    val folder: String,
    val file: File,
    val icon: ImageVector,
    val isRunning: Boolean = false
)

/**
 * Official Mod Repository managing two distinct modular environments:
 * - kernel_mods: Kernel-level hooks, bypasses, and drivers
 * - external_mods: External overlays, injectors, and floating controls
 */
class ModRepository(private val context: Context) {

    val kernelModsDir: File
        get() = File(context.filesDir, "kernel_mods").also { if (!it.exists()) it.mkdirs() }

    val externalModsDir: File
        get() = File(context.filesDir, "external_mods").also { if (!it.exists()) it.mkdirs() }

    val sdcardKernelModsDir: File?
        get() = try {
            File(Environment.getExternalStorageDirectory(), "NinMods/kernel_mods").also {
                if (!it.exists()) it.mkdirs()
            }
        } catch (_: Exception) {
            null
        }

    val sdcardExternalModsDir: File?
        get() = try {
            File(Environment.getExternalStorageDirectory(), "NinMods/external_mods").also {
                if (!it.exists()) it.mkdirs()
            }
        } catch (_: Exception) {
            null
        }

    init {
        initializeFolders()
    }

    fun initializeFolders() {
        try {
            kernelModsDir.mkdirs()
            externalModsDir.mkdirs()
            sdcardKernelModsDir?.mkdirs()
            sdcardExternalModsDir?.mkdirs()

            // Seed starter kernel mods if directory has no files
            val currentKernelFiles = kernelModsDir.listFiles()?.filter { !it.name.startsWith(".") }
            if (currentKernelFiles.isNullOrEmpty()) {
                seedDefaultKernelMods()
            }

            // Seed starter external mods if directory has no files
            val currentExternalFiles = externalModsDir.listFiles()?.filter { !it.name.startsWith(".") }
            if (currentExternalFiles.isNullOrEmpty()) {
                seedDefaultExternalMods()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun seedDefaultKernelMods() {
        createScript(
            folder = kernelModsDir,
            fileName = "engine.sh",
            content = "#!/system/bin/sh\n# NinMods Kernel Engine Service\necho \"[Kernel Engine] Initializing memory hooks...\"\nexit 0\n"
        )
        createScript(
            folder = kernelModsDir,
            fileName = "bypass.sh",
            content = "#!/system/bin/sh\n# NinMods Kernel Bypass Service\necho \"[Kernel Bypass] Applying SELinux and integrity bypass...\"\nexit 0\n"
        )
        createScript(
            folder = kernelModsDir,
            fileName = "hook.sh",
            content = "#!/system/bin/sh\n# NinMods Syscall Hook Service\necho \"[Syscall Hook] Routing system hooks...\"\nexit 0\n"
        )
    }

    private fun seedDefaultExternalMods() {
        createScript(
            folder = externalModsDir,
            fileName = "injector.sh",
            content = "#!/system/bin/sh\n# NinMods Overlay Injector\necho \"[Injector] Injecting visual overlay...\"\nexit 0\n"
        )
        createScript(
            folder = externalModsDir,
            fileName = "floating_menu.sh",
            content = "#!/system/bin/sh\n# NinMods Floating Menu\necho \"[Floating Menu] Activating floating window...\"\nexit 0\n"
        )
        createScript(
            folder = externalModsDir,
            fileName = "aim_assist.sh",
            content = "#!/system/bin/sh\n# NinMods Aim Assist\necho \"[Aim Assist] Calibrating touch sensors...\"\nexit 0\n"
        )
    }

    private fun createScript(folder: File, fileName: String, content: String) {
        try {
            val file = File(folder, fileName)
            if (!file.exists()) {
                FileOutputStream(file).use { it.write(content.toByteArray()) }
                file.setExecutable(true, false)
                file.setReadable(true, false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun loadKernelMods(): List<ModItem> = withContext(Dispatchers.IO) {
        scanFolder(kernelModsDir, "kernel_mods")
    }

    suspend fun loadExternalMods(): List<ModItem> = withContext(Dispatchers.IO) {
        scanFolder(externalModsDir, "external_mods")
    }

    private fun scanFolder(folder: File, folderName: String): List<ModItem> {
        val files = folder.listFiles()?.filter { !it.name.startsWith(".") } ?: emptyList()

        val sdcardFolder = if (folderName == "kernel_mods") sdcardKernelModsDir else sdcardExternalModsDir
        val sdcardFiles = sdcardFolder?.listFiles()?.filter { !it.name.startsWith(".") } ?: emptyList()

        val allFiles = (files + sdcardFiles).distinctBy { it.name }

        return allFiles.map { file ->
            ModItem(
                id = "${folderName}_${file.name}",
                name = formatModName(file.name),
                fileName = file.name,
                folder = folderName,
                file = file,
                icon = resolveIconForMod(file.name, folderName)
            )
        }.sortedBy { it.name }
    }

    private fun formatModName(rawName: String): String {
        val base = rawName
            .removeSuffix(".sh")
            .removeSuffix(".ko.sh")
            .removeSuffix(".apk")
            .removeSuffix(".dex")
            .removeSuffix(".so")
            .replace("_", " ")
            .replace("-", " ")

        return base.split(" ").filter { it.isNotBlank() }.joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.uppercase() }
        }
    }

    private fun resolveIconForMod(fileName: String, folderName: String): ImageVector {
        val lower = fileName.lowercase()
        return when {
            lower.contains("engine") -> Icons.Filled.Memory
            lower.contains("bypass") || lower.contains("shield") -> Icons.Filled.Security
            lower.contains("hook") -> Icons.Filled.Code
            lower.contains("injector") || lower.contains("rocket") -> Icons.Filled.RocketLaunch
            lower.contains("floating") || lower.contains("menu") -> Icons.Filled.Layers
            lower.contains("aim") || lower.contains("tune") -> Icons.Filled.Tune
            lower.contains("build") || lower.contains("kernel") -> Icons.Filled.Build
            folderName == "kernel_mods" -> Icons.Filled.Terminal
            else -> Icons.Filled.Extension
        }
    }

    suspend fun executeMod(mod: ModItem): Result<String> = withContext(Dispatchers.IO) {
        try {
            mod.file.setExecutable(true, false)
            val path = mod.file.absolutePath
            val success = ShellExecutor.executeRoot("sh \"$path\"")
            if (success) {
                Result.success("Mod ${mod.name} executed successfully.")
            } else {
                Result.failure(Exception("Execution failed or superuser access denied."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
