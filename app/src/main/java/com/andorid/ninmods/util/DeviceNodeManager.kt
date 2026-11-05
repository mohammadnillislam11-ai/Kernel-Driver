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
        return found
    }

    fun isDriverActive(): Boolean {
        return checkActiveNodes().isNotEmpty()
    }
}
