package com.example.appblocker.usage

/**
 * How long [packageName] has been in the foreground so far today.
 */
data class DailyUsage(
    val packageName: String,
    val usedMillis: Long,
) {
    val usedMinutes: Long
        get() = usedMillis / 60_000
}
