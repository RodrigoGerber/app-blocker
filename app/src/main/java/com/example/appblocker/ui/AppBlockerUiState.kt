package com.example.appblocker.ui

/**
 * Immutable snapshot the screen renders from (spec §11). Derived values
 * ([remainingMinutes], [isReady], [canEnableBlocking]) live here so the
 * composables stay declarative.
 */
data class AppBlockerUiState(
    val isLoading: Boolean = true,
    val dailyLimitMinutes: Int = 30,
    val usedMinutesToday: Long = 0,
    val blockingEnabled: Boolean = false,
    val hasUsageAccess: Boolean = false,
    val hasAccessibilityAccess: Boolean = false,
    val errorMessage: String? = null,
) {
    val remainingMinutes: Long
        get() = (dailyLimitMinutes - usedMinutesToday).coerceAtLeast(0)

    /** True once both required permissions are granted. */
    val isReady: Boolean
        get() = hasUsageAccess && hasAccessibilityAccess

    /** Blocking may only be turned on with a positive limit and both grants. */
    val canEnableBlocking: Boolean
        get() = isReady && dailyLimitMinutes > 0
}
