package com.andorid.ninmods.util

import android.content.Context
import android.content.pm.PackageManager
import java.io.File

object RootDetector {

    data class RootInfo(
        val isRooted: Boolean,
        val managerName: String,
        val suBinaryPath: String?
    )

    fun detect(context: Context): RootInfo {
        val pm = context.packageManager

        // 1. Check Manager Packages and daemon sockets/paths
        val hasApatch = isPackageInstalled(pm, "me.bmax.apatch") ||
                isPackageInstalled(pm, "me.bmax.apatch.debug") ||
                File("/data/adb/ap").exists() ||
                File("/data/adb/apd").exists() ||
                File("/data/adb/ap/bin/su").exists()

        val hasKsu = isPackageInstalled(pm, "me.weishu.kernelsu") ||
                File("/data/adb/ksu").exists() ||
                File("/data/adb/ksud").exists() ||
                File("/data/adb/ksu/bin/su").exists()

        val hasMagisk = isPackageInstalled(pm, "com.topjohnwu.magisk") ||
                File("/data/adb/magisk").exists() ||
                File("/sbin/.magisk").exists()

        // 2. Check Standard SU Binaries in filesystem
        val suPaths = listOf(
            "/data/adb/ap/bin/su",
            "/data/adb/ksu/bin/su",
            "/data/adb/magisk/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/data/local/su"
        )
        val suFound = suPaths.firstOrNull { File(it).exists() }

        val isRooted = hasApatch || hasKsu || hasMagisk || suFound != null

        val managerName = when {
            hasApatch -> "APatch"
            hasKsu -> "KernelSU"
            hasMagisk -> "Magisk"
            suFound != null -> "SuperUser"
            else -> "Not Installed"
        }

        return RootInfo(
            isRooted = isRooted,
            managerName = managerName,
            suBinaryPath = suFound
        )
    }

    private fun isPackageInstalled(pm: PackageManager, packageName: String): Boolean {
        return try {
            pm.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }
}
