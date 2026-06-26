package com.example.appblocker.accessibility

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.text.TextUtils

/**
 * Reports whether our [AppBlockerAccessibilityService] is currently enabled.
 * There is no API to query our own service instance reliably from outside it,
 * so we read the system's enabled-services setting (spec §15.2).
 */
class AccessibilityPermissionChecker(
    private val context: Context,
) {
    fun isServiceEnabled(): Boolean {
        val expected = ComponentName(
            context,
            AppBlockerAccessibilityService::class.java,
        ).flattenToString()

        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false

        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)
        for (service in splitter) {
            if (service.equals(expected, ignoreCase = true)) {
                return true
            }
        }
        return false
    }
}
