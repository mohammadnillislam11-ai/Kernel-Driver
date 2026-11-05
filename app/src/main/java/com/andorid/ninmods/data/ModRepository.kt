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
 * Pure Mod Scanner & Executor for User Scripts (.sh):
 * Discovers user mods from dedicated kernel_mods and external_mods directories.
 * Zero dummy/fake files generated.
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

    val rootTmpKernelModsDir: File
        get() = File("/data/local/tmp/ninmods/kernel_mods")

    val rootTmpExternalModsDir: File
        get() = File("/data/local/tmp/ninmods/external_mods")

    init {
        ensureFoldersExist()
    }

    fun ensureFoldersExist() {
        try {
            kernelModsDir.mkdirs()
            externalModsDir.mkdirs()
            sdcardKernelModsDir?.mkdirs()
            sdcardExternalModsDir?.mkdirs()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun loadKernelMods(): List<ModItem> = withContext(Dispatchers.IO) {
        scanFolder("kernel_mods", listOfNotNull(kernelModsDir, sdcardKernelModsDir, rootTmpKernelModsDir))
    }

    suspend fun loadExternalMods(): List<ModItem> = withContext(Dispatchers.IO) {
        scanFolder("external_mods", listOfNotNull(externalModsDir, sdcardExternalModsDir, rootTmpExternalModsDir))
    }

    private fun scanFolder(folderName: String, directories: List<File>): List<ModItem> {
        val collectedFiles = mutableListOf<File>()

        for (dir in directories) {
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.filter { f ->
                    f.isFile && !f.name.startsWith(".")
                }?.let { collectedFiles.addAll(it) }
            }
        }

        return collectedFiles.distinctBy { it.name }.map { file ->
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
