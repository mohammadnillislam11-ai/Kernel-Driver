package com.andorid.ninmods.util

import java.io.File

object DeviceNodeManager {

    val TARGET_DEVICE_NODES = listOf(
        "/dev/BYNb",
        "/dev/mem_tool",
        "/dev/nh",
        "/dev/wanbai"
    )

    fun checkActiveNodes(): List<String> {
        val found = mutableListOf<String>()
        for (path in TARGET_DEVICE_NODES) {
            val file = File(path)
            if (file.exists()) {
                found.add(path)
            }
        }

        // If direct file access was constrained by SELinux, inspect loaded kernel modules
        if (found.isEmpty()) {
            try {
                val procModules = File("/proc/modules")
                if (procModules.exists() && procModules.canRead()) {
                    val text = procModules.readText()
                    for (mod in listOf("BYNb", "mem_tool", "nh", "wanbai")) {
                        if (text.contains(mod)) {
                            found.add("/dev/$mod")
                        }
                    }
                }
            } catch (_: Throwable) {}
        }

        return found
    }

    fun isDriverActive(): Boolean {
        return checkActiveNodes().isNotEmpty()
    }
}
