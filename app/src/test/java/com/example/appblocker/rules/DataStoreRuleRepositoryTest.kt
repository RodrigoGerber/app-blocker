package com.example.appblocker.rules

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.example.appblocker.config.AppBlockerConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DataStoreRuleRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val scope = TestScope(UnconfinedTestDispatcher())
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repo: DataStoreRuleRepository

    private val instagram = "com.instagram.android"
    private val tiktok = "com.zhiliaoapp.musically"

    @Before
    fun setUp() {
        dataStore = PreferenceDataStoreFactory.create(scope = scope.backgroundScope) {
            File(tmp.root, "rules.preferences_pb")
        }
        repo = DataStoreRuleRepository(dataStore)
    }

    @Test
    fun `defaults are empty and not paused`() = scope.runTest {
        assertEquals(emptyList<BlockingRule>(), repo.getRules())
        assertFalse(repo.isPaused())
        assertNull(repo.getRule(instagram))
    }

    @Test
    fun `upsert adds a rule`() = scope.runTest {
        repo.upsertRule(BlockingRule(instagram, 30, enabled = true))
        assertEquals(listOf(BlockingRule(instagram, 30, true)), repo.getRules())
        assertEquals(BlockingRule(instagram, 30, true), repo.getRule(instagram))
    }

    @Test
    fun `upsert replaces by package name rather than duplicating`() = scope.runTest {
        repo.upsertRule(BlockingRule(instagram, 30, enabled = true))
        repo.upsertRule(BlockingRule(instagram, 45, enabled = false))
        assertEquals(listOf(BlockingRule(instagram, 45, false)), repo.getRules())
    }

    @Test
    fun `multiple apps coexist with independent values`() = scope.runTest {
        repo.upsertRule(BlockingRule(instagram, 30, enabled = true))
        repo.upsertRule(BlockingRule(tiktok, 15, enabled = true))
        repo.setDailyLimitMinutes(tiktok, 20)
        repo.setEnabled(instagram, false)

        assertEquals(BlockingRule(instagram, 30, false), repo.getRule(instagram))
        assertEquals(BlockingRule(tiktok, 20, true), repo.getRule(tiktok))
    }

    @Test
    fun `delete removes only the targeted rule`() = scope.runTest {
        repo.upsertRule(BlockingRule(instagram, 30, enabled = true))
        repo.upsertRule(BlockingRule(tiktok, 15, enabled = true))
        repo.deleteRule(instagram)
        assertEquals(listOf(BlockingRule(tiktok, 15, true)), repo.getRules())
    }

    @Test
    fun `negative limit is clamped to zero`() = scope.runTest {
        repo.upsertRule(BlockingRule(instagram, -10, enabled = true))
        assertEquals(0, repo.getRule(instagram)?.dailyLimitMinutes)
    }

    @Test
    fun `pause flag persists`() = scope.runTest {
        assertFalse(repo.isPaused())
        repo.setPaused(true)
        assertTrue(repo.isPaused())
        repo.setPaused(false)
        assertFalse(repo.isPaused())
    }

    @Test
    fun `legacy single-rule keys migrate to an Instagram rule`() = scope.runTest {
        // Simulate a pre-update install: only the old keys exist.
        dataStore.edit { prefs ->
            prefs[intPreferencesKey("daily_limit_minutes")] = 25
            prefs[booleanPreferencesKey("blocking_enabled")] = true
        }

        assertEquals(
            listOf(
                BlockingRule(
                    packageName = AppBlockerConfig.INSTAGRAM_PACKAGE,
                    dailyLimitMinutes = 25,
                    enabled = true,
                ),
            ),
            repo.getRules(),
        )
    }

    @Test
    fun `first write clears legacy keys`() = scope.runTest {
        dataStore.edit { prefs ->
            prefs[intPreferencesKey("daily_limit_minutes")] = 25
            prefs[booleanPreferencesKey("blocking_enabled")] = true
        }

        // Any rule mutation should persist JSON and drop the legacy keys.
        repo.setDailyLimitMinutes(AppBlockerConfig.INSTAGRAM_PACKAGE, 40)

        val prefs = dataStore.data.first()
        assertNull(prefs[intPreferencesKey("daily_limit_minutes")])
        assertNull(prefs[booleanPreferencesKey("blocking_enabled")])
        assertEquals(40, repo.getRule(AppBlockerConfig.INSTAGRAM_PACKAGE)?.dailyLimitMinutes)
    }
}
