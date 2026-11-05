package com.andorid.ninmods.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

class DriverRepository(private val context: Context) {

    private val driversDir: File
        get() = File(context.filesDir, "drivers").also { if (!it.exists()) it.mkdirs() }

    /**
     * Extracts Kernel_Drivers.zip from assets to app private storage.
     */
    suspend fun extractDriversIfNeeded(onProgress: (String) -> Unit = {}): Boolean = withContext(Dispatchers.IO) {
        val qxDir = File(driversDir, "QX")
        val rtDir = File(driversDir, "RT")

        if (qxDir.exists() && qxDir.listFiles()?.isNotEmpty() == true &&
            rtDir.exists() && rtDir.listFiles()?.isNotEmpty() == true) {
            onProgress("NinMods drivers verified.")
            return@withContext true
        }

        try {
            onProgress("Extracting NinMods kernel driver archives...")
            extractZipAsset("Kernel_Drivers.zip")
            try {
                extractZipAsset("Kernel_Drivers_Master.zip")
            } catch (ignored: Exception) {
            }
            onProgress("Extraction complete.")
            true
        } catch (e: Exception) {
            onProgress("Extraction failed: ${e.message}")
            false
        }
    }

    private fun extractZipAsset(assetName: String) {
        try {
            val inputStream = context.assets.open(assetName)
            val zipInputStream = ZipInputStream(inputStream)
            var entry = zipInputStream.nextEntry

            while (entry != null) {
                val outFile = File(driversDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos ->
                        zipInputStream.copyTo(fos)
                    }
                    outFile.setExecutable(true, false)
                    outFile.setReadable(true, false)
                }
                zipInputStream.closeEntry()
                entry = zipInputStream.nextEntry
            }
            zipInputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Locates matching driver script files (.ko.sh or .sh) based on kernel version and mode (QX / RT).
     * If exact kernel match is found, returns them.
     * If major.minor match is found, returns them.
     * Otherwise returns all available scripts for the mode so the user can choose.
     */
    suspend fun findMatchingDrivers(kernelVersion: String, mode: String): List<File> = withContext(Dispatchers.IO) {
        var targetFolder = File(driversDir, mode)
        if (!targetFolder.exists() || targetFolder.listFiles()?.isEmpty() != false) {
            extractZipAsset("Kernel_Drivers.zip")
            targetFolder = File(driversDir, mode)
        }

        val fileArray = targetFolder.listFiles() ?: return@withContext emptyList()
        val allFiles = fileArray.filter { f -> f.isFile && (f.name.endsWith(".sh") || f.name.endsWith(".ko.sh")) }

        if (allFiles.isEmpty()) {
            return@withContext emptyList()
        }

        // 1. Exact match (e.g. starts with 4.19.157)
        if (kernelVersion.isNotBlank()) {
            val exactMatches = allFiles.filter { f -> f.name.startsWith(kernelVersion, ignoreCase = true) }
            if (exactMatches.isNotEmpty()) {
                return@withContext exactMatches.sortedBy { it.name }
            }

            // 2. Major.Minor match (e.g. starts with 4.19)
            val majorMinor = if (kernelVersion.contains(".")) {
                val parts = kernelVersion.split(".")
                if (parts.size >= 2) "${parts[0]}.${parts[1]}" else kernelVersion
            } else kernelVersion

            val mmMatches = allFiles.filter { f -> f.name.startsWith(majorMinor, ignoreCase = true) }
            if (mmMatches.isNotEmpty()) {
                return@withContext mmMatches.sortedBy { it.name }
            }
        }

        // 3. Fallback: Return all available drivers for this mode so the dialog ALWAYS presents options
        allFiles.sortedBy { it.name }
    }

    suspend fun getAllDrivers(mode: String): List<File> = withContext(Dispatchers.IO) {
        findMatchingDrivers("", mode)
    }
}
