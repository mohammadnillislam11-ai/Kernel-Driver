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
     * Executes commands in a root 'su' shell session.
     * Streams stdout and stderr line-by-line without artificial timeout.
     */
    fun executeRootCommand(commands: List<String>): Flow<ShellOutput> = flow {
        var process: Process? = null
        try {
            process = Runtime.getRuntime().exec("su")
            val outputStream = DataOutputStream(process.outputStream)
            val inputStream = BufferedReader(InputStreamReader(process.inputStream))
            val errorStream = BufferedReader(InputStreamReader(process.errorStream))

            for (cmd in commands) {
                outputStream.writeBytes("$cmd\n")
            }
            outputStream.writeBytes("echo \"__EXIT_CODE__=$?\"\n")
            outputStream.writeBytes("exit\n")
            outputStream.flush()

            var exitCode = -1
            var line: String?

            // Read standard output
            while (inputStream.readLine().also { line = it } != null) {
                val l = line!!
                if (l.startsWith("__EXIT_CODE__=")) {
                    exitCode = l.substringAfter("__EXIT_CODE__=").trim().toIntOrNull() ?: 0
                } else {
                    emit(ShellOutput.Line(l))
                }
            }

            // Read error output
            while (errorStream.readLine().also { line = it } != null) {
                emit(ShellOutput.Line("[STDERR] ${line!!}"))
            }

            val procExit = process.waitFor()
            if (exitCode == -1) exitCode = procExit

            emit(ShellOutput.Complete(exitCode = exitCode, success = exitCode == 0))

        } catch (e: Exception) {
            emit(ShellOutput.Error("Execution failed: ${e.message}"))
        } finally {
            process?.destroy()
        }
    }.flowOn(Dispatchers.IO)
}
