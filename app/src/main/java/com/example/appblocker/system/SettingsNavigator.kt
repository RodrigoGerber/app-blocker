package com.example.appblocker.system

import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * Opens the system Settings screens where the user grants the two permissions
 * this app needs. Both are granted manually, not via runtime dialogs
 * (spec §13).
 */
class SettingsNavigator(
    private val context: Context,
) {
    fun openUsageAccessSettings() {
        startSettings(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    }

    fun openAccessibilitySettings() {
        startSettings(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    }

    private fun startSettings(action: String) {
        val intent = Intent(action).apply {
            // Required because we may launch from a non-Activity context.
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
