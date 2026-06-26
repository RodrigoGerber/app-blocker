package com.example.appblocker.system

import android.content.Context
import android.content.pm.PackageManager

/**
 * Thin wrapper over [PackageManager] for app metadata. The MVP only needs the
 * monitored app's display name; this is kept as a seam for the future
 * "choose an app" feature (spec §21.3).
 */
class InstalledAppProvider(
    private val context: Context,
) {
    /** Human-readable label for [packageName], or null if it is not installed. */
    fun getAppLabel(packageName: String): String? {
        val pm = context.packageManager
        return try {
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    fun isInstalled(packageName: String): Boolean = getAppLabel(packageName) != null
}
