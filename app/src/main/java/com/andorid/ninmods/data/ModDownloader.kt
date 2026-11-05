package com.andorid.ninmods.data

import android.content.Context
import com.andorid.ninmods.config.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

object ModDownloader {

    sealed class DownloadState {
        object Idle : DownloadState()
        data class Progress(val percentage: Int, val bytesDownloaded: Long, val totalBytes: Long) : DownloadState()
        data class Success(val destinationDir: File) : DownloadState()
        data class Error(val message: String) : DownloadState()
    }

    /**
     * Download a mod package (zip or binary) from backend server to local directory.
     */
    suspend fun downloadMod(
        context: Context,
        downloadUrl: String,
        targetDir: File,
        fileName: String,
        onProgress: (Int) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null

        try {
            if (!targetDir.exists()) targetDir.mkdirs()

            val effectiveUrl = if (downloadUrl.startsWith("http")) {
                downloadUrl
            } else {
                "${AppConfig.BASE_URL.trimEnd('/')}/downloads/${downloadUrl.trimStart('/')}"
            }

            val url = URL(effectiveUrl)
            conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = AppConfig.TIMEOUT_MILLIS
                readTimeout = AppConfig.TIMEOUT_MILLIS
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "NINMODS-DriverLoader-Client")
            }

            val code = conn.responseCode
            if (code !in 200..299) {
                return@withContext Result.failure(Exception("Server returned HTTP $code while downloading."))
            }

            val contentLength = conn.contentLength.toLong()
            inputStream = conn.inputStream

            val tempFile = File(targetDir, "tmp_${System.currentTimeMillis()}_$fileName")
            outputStream = FileOutputStream(tempFile)

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalBytesRead = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                if (contentLength > 0) {
                    val progress = ((totalBytesRead * 100) / contentLength).toInt().coerceIn(0, 100)
                    onProgress(progress)
                }
            }
            outputStream.flush()
            outputStream.close()
            outputStream = null

            // If the downloaded file is a ZIP archive, extract its contents
            val finalTargetFile: File = if (fileName.endsWith(".zip", ignoreCase = true) || isZipFile(tempFile)) {
                unzip(tempFile, targetDir)
                tempFile.delete()
                File(targetDir, fileName.removeSuffix(".zip") + ".sh").let {
                    if (it.exists()) it else targetDir
                }
            } else {
                val destFile = File(targetDir, fileName)
                if (destFile.exists()) destFile.delete()
                tempFile.renameTo(destFile)
                destFile
            }

            // Ensure executable permissions
            try {
                finalTargetFile.setExecutable(true, false)
                ShellExecutor.executeRoot("chmod 755 '${targetDir.absolutePath}'/*")
            } catch (_: Exception) {}

            onProgress(100)
            Result.success(finalTargetFile)

        } catch (e: Exception) {
            val msg = when {
                e.message?.contains("Unable to resolve host") == true -> "Server unreachable. Check domain configuration in AppConfig."
                e.message?.contains("timed out") == true -> "Download connection timed out. Please try again."
                else -> e.message ?: "Download failed."
            }
            Result.failure(Exception(msg))
        } finally {
            try { inputStream?.close() } catch (_: Exception) {}
            try { outputStream?.close() } catch (_: Exception) {}
            conn?.disconnect()
        }
    }

    private fun isZipFile(file: File): Boolean {
        if (!file.exists() || file.length() < 4) return false
        return try {
            file.inputStream().use { stream ->
                val b = ByteArray(4)
                stream.read(b) == 4 && b[0] == 0x50.toByte() && b[1] == 0x4B.toByte() && b[2] == 0x03.toByte() && b[3] == 0x04.toByte()
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun unzip(zipFile: File, targetDir: File) {
        val canonicalDest = targetDir.canonicalPath
        ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val newFile = File(targetDir, entry.name)
                val canonicalEntry = newFile.canonicalPath
                if (!canonicalEntry.startsWith(canonicalDest + File.separator) && canonicalEntry != canonicalDest) {
                    zis.closeEntry()
                    entry = zis.nextEntry
                    continue // Skip malicious or escaped zip entry (Zip Slip protection)
                }

                if (entry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile?.mkdirs()
                    FileOutputStream(newFile).use { fos ->
                        zis.copyTo(fos)
                    }
                    newFile.setExecutable(true, false)
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
}
