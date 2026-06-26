package com.example.appblocker.di

import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.example.appblocker.accessibility.AccessibilityPermissionChecker
import com.example.appblocker.blocking.BlockingPolicy
import com.example.appblocker.rules.DataStoreRuleRepository
import com.example.appblocker.rules.RuleRepository
import com.example.appblocker.system.ClockProvider
import com.example.appblocker.system.InstalledAppProvider
import com.example.appblocker.system.SettingsNavigator
import com.example.appblocker.usage.AndroidDailyUsageProvider
import com.example.appblocker.usage.DailyUsageProvider
import com.example.appblocker.usage.UsageAccessPermissionChecker
import com.example.appblocker.usage.UsageEventParser

/** Single DataStore instance for the whole process. */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_blocker_prefs",
)

/**
 * Hand-rolled dependency container (spec §4.6). The project is small enough that
 * a single object wiring everything by hand is clearer than Hilt. All members
 * are process-wide singletons, created lazily.
 */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    private val clock = ClockProvider.systemClock()

    val ruleRepository: RuleRepository by lazy {
        DataStoreRuleRepository(appContext.dataStore)
    }

    private val usageStatsManager: UsageStatsManager by lazy {
        appContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    }

    val dailyUsageProvider: DailyUsageProvider by lazy {
        AndroidDailyUsageProvider(
            usageStatsManager = usageStatsManager,
            clock = clock,
            eventParser = UsageEventParser(),
        )
    }

    val blockingPolicy: BlockingPolicy by lazy {
        BlockingPolicy(
            ruleRepository = ruleRepository,
            dailyUsageProvider = dailyUsageProvider,
        )
    }

    val usageAccessChecker: UsageAccessPermissionChecker by lazy {
        UsageAccessPermissionChecker(appContext)
    }

    val accessibilityChecker: AccessibilityPermissionChecker by lazy {
        AccessibilityPermissionChecker(appContext)
    }

    val settingsNavigator: SettingsNavigator by lazy {
        SettingsNavigator(appContext)
    }

    val installedAppProvider: InstalledAppProvider by lazy {
        InstalledAppProvider(appContext)
    }
}
