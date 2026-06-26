package com.example.appblocker.usage

/**
 * Abstraction over "how much has this app been used today". The Android
 * implementation is backed by UsageStatsManager; tests can substitute a fake.
 */
interface DailyUsageProvider {
    suspend fun getUsageToday(packageName: String): DailyUsage
}
