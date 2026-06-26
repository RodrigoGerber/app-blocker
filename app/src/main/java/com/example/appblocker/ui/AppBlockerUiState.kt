package com.example.appblocker.ui

import androidx.compose.ui.graphics.ImageBitmap
import com.example.appblocker.system.InstalledApp

/** One row in the rules list: a monitored app plus its current numbers. */
data class RuleRow(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?,
    val limitMinutes: Int,
    val usedMinutesToday: Long,
    val enabled: Boolean,
) {
    val remainingMinutes: Long
        get() = (limitMinutes - usedMinutesToday).coerceAtLeast(0)

    val progress: Float
        get() = if (limitMinutes > 0) {
            (usedMinutesToday.toFloat() / limitMinutes).coerceIn(0f, 1f)
        } else {
            1f
        }
}

/** Draft shown in the add/edit bottom sheet. */
data class EditorState(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?,
    val limitMinutes: Int,
    val enabled: Boolean,
    val isNew: Boolean,
)

/** State for the main rules screen. */
data class AppBlockerUiState(
    val isLoading: Boolean = true,
    val paused: Boolean = false,
    val rules: List<RuleRow> = emptyList(),
    val hasUsageAccess: Boolean = false,
    val hasAccessibilityAccess: Boolean = false,
    val editor: EditorState? = null,
) {
    val hasAllPermissions: Boolean
        get() = hasUsageAccess && hasAccessibilityAccess
}

/** State for the app-picker screen. */
data class PickerState(
    val isLoading: Boolean = true,
    val apps: List<InstalledApp> = emptyList(),
    val query: String = "",
)
