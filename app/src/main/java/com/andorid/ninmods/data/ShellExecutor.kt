package com.andorid.ninmods.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

object ShellExecutor {

    sealed class ShellOutput {
        data class Line(val text: String) : ShellOutput()
        data class Complete(val exitCode: Int, val success: Boolean) : ShellOutput()
        data class Error(val message: String) : ShellOutput()
    }

    /**
     * Executes commands in a genuine root 'su' shell session.
     * Streams stdout and stderr line-by-line in real time.
     */
    fun executeRootCommand(commands: List<String>): Flow<ShellOutput> = flow {
        var process: Process? = null
        try {
            process = ProcessBuilder("su")
                .redirectErrorStream(false)
                .start()

            val outputStream = DataOutputStream(process.outputStream)
            for (cmd in commands) {
                outputStream.write((cmd + "\n").toByteArray(Charsets.UTF_8))
            }
            outputStream.write("exit $?\n".toByteArray(Charsets.UTF_8))
            outputStream.flush()

            val inputStream = BufferedReader(InputStreamReader(process.inputStream))
            val errorStream = BufferedReader(InputStreamReader(process.errorStream))

            var line: String?
            while (inputStream.readLine().also { line = it } != null) {
                emit(ShellOutput.Line(line!!))
            }

            while (errorStream.readLine().also { line = it } != null) {
                emit(ShellOutput.Line("[STDERR] ${line!!}"))
            }

            val exitCode = process.waitFor()
            emit(ShellOutput.Complete(exitCode = exitCode, success = exitCode == 0))

        } catch (e: Exception) {
            // Attempt fallback through /system/bin/su or /data/adb/ap/bin/su if generic 'su' isn't in default PATH
            try {
                val suBin = listOf(
                    "/system/bin/su",
                    "/system/xbin/su",
                    "/data/adb/ap/bin/su",
                    "/data/adb/magisk/su"
                ).firstOrNull { java.io.File(it).exists() } ?: "su"

                val joinedCmd = commands.joinToString(" && ")
                val fbProc = ProcessBuilder(suBin, "-c", joinedCmd).start()
                val reader = BufferedReader(InputStreamReader(fbProc.inputStream))
                var fbLine: String?
                while (reader.readLine().also { fbLine = it } != null) {
                    emit(ShellOutput.Line(fbLine!!))
                }
                val code = fbProc.waitFor()
                emit(ShellOutput.Complete(exitCode = code, success = code == 0))
            } catch (fe: Exception) {
                emit(ShellOutput.Error("Root execution error: ${e.message}"))
            }
        } finally {
            process?.destroy()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Executes a single command as root synchronously on the calling thread (or dispatcher).
     */
    fun executeRoot(command: String): Boolean {
        return try {
            val suBin = listOf(
                "/system/bin/su",
                "/system/xbin/su",
                "/data/adb/ap/bin/su",
                "/data/adb/magisk/su"
            ).firstOrNull { java.io.File(it).exists() } ?: "su"

            val proc = ProcessBuilder(suBin, "-c", command).start()
            val exitCode = proc.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            false
        }
    }
}
