package com.example.appblocker.rules

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.appblocker.config.AppBlockerConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * [RuleRepository] backed by Preferences DataStore. The rule list is stored as a
 * single JSON string (one entry per app); the global pause is a boolean. Room is
 * deferred until a later history phase (spec §4.3).
 *
 * Legacy migration: the previous single-rule keys (`daily_limit_minutes`,
 * `blocking_enabled`) are read as a fallback into a one-element Instagram rule
 * whenever the new JSON key is absent, and are cleared on the next write.
 */
class DataStoreRuleRepository(
    private val dataStore: DataStore<Preferences>,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : RuleRepository {

    override val rules: Flow<List<BlockingRule>> =
        dataStore.data.map { prefs -> prefs.readRules() }

    override val paused: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[KEY_PAUSED] ?: false }

    override suspend fun getRules(): List<BlockingRule> = rules.first()

    override suspend fun getRule(packageName: String): BlockingRule? =
        rules.first().firstOrNull { it.packageName == packageName }

    override suspend fun isPaused(): Boolean = paused.first()

    override suspend fun upsertRule(rule: BlockingRule) = mutateRules { current ->
        current.filterNot { it.packageName == rule.packageName } +
            rule.copy(dailyLimitMinutes = rule.dailyLimitMinutes.coerceAtLeast(0))
    }

    override suspend fun deleteRule(packageName: String) = mutateRules { current ->
        current.filterNot { it.packageName == packageName }
    }

    override suspend fun setEnabled(packageName: String, enabled: Boolean) =
        mutateRules { current ->
            current.map { if (it.packageName == packageName) it.copy(enabled = enabled) else it }
        }

    override suspend fun setDailyLimitMinutes(packageName: String, minutes: Int) =
        mutateRules { current ->
            val clamped = minutes.coerceAtLeast(0)
            current.map {
                if (it.packageName == packageName) it.copy(dailyLimitMinutes = clamped) else it
            }
        }

    override suspend fun setPaused(paused: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_PAUSED] = paused }
    }

    /**
     * Reads the current list, applies [transform], writes the result as JSON, and
     * clears the legacy keys so the migration only matters until the first write.
     */
    private suspend fun mutateRules(transform: (List<BlockingRule>) -> List<BlockingRule>) {
        dataStore.edit { prefs ->
            val updated = transform(prefs.readRules())
            prefs[KEY_RULES_JSON] = json.encodeToString(updated)
            prefs.remove(LEGACY_DAILY_LIMIT_MINUTES)
            prefs.remove(LEGACY_BLOCKING_ENABLED)
        }
    }

    private fun Preferences.readRules(): List<BlockingRule> {
        this[KEY_RULES_JSON]?.let { stored ->
            return runCatching { json.decodeFromString<List<BlockingRule>>(stored) }
                .getOrDefault(emptyList())
        }
        // Fallback: migrate the old single-rule shape if present.
        val legacyLimit = this[LEGACY_DAILY_LIMIT_MINUTES]
        val legacyEnabled = this[LEGACY_BLOCKING_ENABLED]
        if (legacyLimit != null || legacyEnabled != null) {
            return listOf(
                BlockingRule(
                    packageName = AppBlockerConfig.INSTAGRAM_PACKAGE,
                    dailyLimitMinutes = legacyLimit ?: AppBlockerConfig.DEFAULT_DAILY_LIMIT_MINUTES,
                    enabled = legacyEnabled ?: false,
                ),
            )
        }
        return emptyList()
    }

    private companion object {
        val KEY_RULES_JSON = stringPreferencesKey("rules_json")
        val KEY_PAUSED = booleanPreferencesKey("blocking_paused")

        val LEGACY_DAILY_LIMIT_MINUTES = intPreferencesKey("daily_limit_minutes")
        val LEGACY_BLOCKING_ENABLED = booleanPreferencesKey("blocking_enabled")
    }
}
