package com.example.appblocker.ui

import com.example.appblocker.system.InstalledApp

/**
 * Filters [apps] by a case-insensitive label match on [query]. A blank query
 * returns the list unchanged. Pure function so it can be unit tested directly.
 */
internal fun filterApps(apps: List<InstalledApp>, query: String): List<InstalledApp> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return apps
    return apps.filter { it.label.contains(trimmed, ignoreCase = true) }
}
