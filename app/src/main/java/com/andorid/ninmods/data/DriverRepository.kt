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
    suspend fun extractDriversIfNeeded(onProgress: (String) -> Unit): Boolean = withContext(Dispatchers.IO) {
        val marker = File(driversDir, ".extracted")
        if (marker.exists()) {
            onProgress("Drivers already extracted.")
            return@withContext true
        }

        try {
            onProgress("Extracting kernel driver archives...")
            
            // Extract primary Kernel_Drivers.zip
            extractZipAsset("Kernel_Drivers.zip")
            
            // Extract master Kernel_Drivers_Master.zip if available
            try {
                extractZipAsset("Kernel_Drivers_Master.zip")
            } catch (me: Exception) {
                // Optional master zip failure is non-fatal
            }

            marker.createNewFile()
            onProgress("Extraction complete.")
            true
        } catch (e: Exception) {
            onProgress("Extraction failed: ${e.message}")
            false
        }
    }

    private fun extractZipAsset(assetName: String) {
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
    }
    }

    /**
     * Locates matching driver script files (.ko.sh or .sh) based on kernel version and mode (QX / RT).
     */
    suspend fun findMatchingDrivers(kernelVersion: String, mode: String): List<File> = withContext(Dispatchers.IO) {
        val targetFolder = File(driversDir, mode)
        if (!targetFolder.exists() || !targetFolder.isDirectory) {
            return@withContext emptyList()
        }

        val allFiles = targetFolder.listFiles()?.filter { it.isFile && (it.name.endsWith(".sh") || it.name.endsWith(".ko.sh")) }
            ?: return@withContext emptyList()

        // 1. Exact match (e.g. starts with 4.19.157)
        val exactMatches = allFiles.filter { it.name.startsWith(kernelVersion, ignoreCase = true) }
        if (exactMatches.isNotEmpty()) {
            return@withContext exactMatches
        }

        // 2. Major.Minor match (e.g. starts with 4.19)
        val majorMinor = if (kernelVersion.contains(".")) {
            val parts = kernelVersion.split(".")
            if (parts.size >= 2) "${parts[0]}.${parts[1]}" else kernelVersion
        } else kernelVersion

        val mmMatches = allFiles.filter { it.name.startsWith(majorMinor, ignoreCase = true) }
        if (mmMatches.isNotEmpty()) {
            return@withContext mmMatches
        }

        // 3. Fallback: Return all available in mode so user can pick
        return@withContext allFiles.sortedBy { it.name }
    }
}
