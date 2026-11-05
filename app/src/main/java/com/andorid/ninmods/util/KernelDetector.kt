package com.andorid.ninmods.util

import android.system.Os
import java.io.BufferedReader
import java.io.InputStreamReader

object KernelDetector {

    /**
     * APatch-identical kernel release detection:
     * 1. POSIX uname syscall via Os.uname().release
     * 2. System property 'os.version'
     * 3. 'uname -r' shell execution fallback
     */
    fun detectKernelRelease(): String {
        // Strategy 1: APatch official POSIX uname release
        try {
            val osRelease = Os.uname().release
            if (!osRelease.isNullOrBlank()) {
                return osRelease.trim()
            }
        } catch (_: Throwable) {}

        // Strategy 2: System property os.version
        val propVersion = System.getProperty("os.version")?.trim()
        if (!propVersion.isNullOrEmpty() && propVersion != "unknown") {
            return propVersion
        }

        // Strategy 3: Process execution uname -r
        try {
            val process = Runtime.getRuntime().exec("uname -r")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readLine()?.trim()
            reader.close()
            process.waitFor()
            if (!line.isNullOrEmpty()) {
                return line
            }
        } catch (_: Throwable) {}

        return "4.19.157"
    }

    /**
     * Extracts major.minor (e.g. 4.19, 5.10, 5.4, 4.14, 5.15)
     */
    fun getMajorMinor(version: String): String {
        val parts = version.split(".")
        return if (parts.size >= 2) {
            "${parts[0]}.${parts[1]}"
        } else {
            version
        }
    }
}
