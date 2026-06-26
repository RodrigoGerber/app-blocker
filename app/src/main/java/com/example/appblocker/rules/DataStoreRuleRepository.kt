package com.example.appblocker.rules

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.example.appblocker.config.AppBlockerConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * [RuleRepository] backed by Preferences DataStore. The monitored package is a
 * fixed constant for the MVP, so only the limit and the enabled flag are
 * persisted (spec §9.7).
 */
class DataStoreRuleRepository(
    private val dataStore: DataStore<Preferences>,
    private val packageName: String = AppBlockerConfig.INSTAGRAM_PACKAGE,
) : RuleRepository {

    override val rule: Flow<BlockingRule> =
        dataStore.data.map { prefs -> prefs.toRule() }

    override suspend fun getRule(): BlockingRule = rule.first()

    override suspend fun setDailyLimitMinutes(minutes: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_DAILY_LIMIT_MINUTES] = minutes.coerceAtLeast(0)
        }
    }

    override suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_BLOCKING_ENABLED] = enabled
        }
    }

    private fun Preferences.toRule(): BlockingRule = BlockingRule(
        packageName = packageName,
        dailyLimitMinutes = this[KEY_DAILY_LIMIT_MINUTES]
            ?: AppBlockerConfig.DEFAULT_DAILY_LIMIT_MINUTES,
        enabled = this[KEY_BLOCKING_ENABLED] ?: false,
    )

    private companion object {
        val KEY_DAILY_LIMIT_MINUTES = intPreferencesKey("daily_limit_minutes")
        val KEY_BLOCKING_ENABLED = booleanPreferencesKey("blocking_enabled")
    }
}
