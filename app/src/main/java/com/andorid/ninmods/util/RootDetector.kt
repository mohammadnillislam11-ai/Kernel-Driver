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

        // 1. Check Manager Packages
        val hasApatch = isPackageInstalled(pm, "me.bmax.apatch") || File("/data/adb/apd").exists()
        val hasKsu = isPackageInstalled(pm, "me.weishu.kernelsu") || File("/data/adb/ksud").exists()
        val hasMagisk = isPackageInstalled(pm, "com.topjohnwu.magisk") || File("/data/adb/magisk").exists()

        // 2. Check SU Binary
        val suPaths = listOf(
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
            else -> "Standard Su"
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
