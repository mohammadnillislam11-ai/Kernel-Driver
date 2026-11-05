package com.andorid.ninmods.util

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.security.MessageDigest
import java.util.UUID

/**
 * Commercial Device Fingerprinting Utility.
 * Produces a stable, unique 32-character SHA-256 HWID for device locking.
 * Persists locally to survive app restarts and match server-side HWID limits.
 */
object DeviceIdentifier {

    private var cachedHwid: String? = null

    fun getHwid(context: Context): String {
        cachedHwid?.let { return it }

        val prefs = context.getSharedPreferences("ninmods_device_meta", Context.MODE_PRIVATE)
        val saved = prefs.getString("unique_device_hwid", null)
        if (!saved.isNullOrBlank()) {
            cachedHwid = saved
            return saved
        }

        val androidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
        } catch (_: Exception) {
            ""
        }

        val raw = StringBuilder().apply {
            append(Build.BOARD).append(";")
            append(Build.BRAND).append(";")
            append(Build.DEVICE).append(";")
            append(Build.MANUFACTURER).append(";")
            append(Build.MODEL).append(";")
            append(Build.PRODUCT).append(";")
            append(androidId)
        }.toString()

        val digest = try {
            val md = MessageDigest.getInstance("SHA-256")
            val bytes = md.digest(raw.toByteArray(Charsets.UTF_8))
            bytes.joinToString("") { "%02X".format(it) }.take(32)
        } catch (_: Exception) {
            UUID.randomUUID().toString().replace("-", "").uppercase()
        }

        prefs.edit().putString("unique_device_hwid", digest).apply()
        cachedHwid = digest
        return digest
    }
}
