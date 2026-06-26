package com.example.appblocker.rules

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * In-memory [RuleRepository] for unit tests. Mirrors the real semantics:
 * upsert replaces by package name, and the reactive flows update on every write.
 */
class FakeRuleRepository(
    initialRules: List<BlockingRule> = emptyList(),
    initialPaused: Boolean = false,
) : RuleRepository {

    private val rulesState = MutableStateFlow(initialRules)
    private val pausedState = MutableStateFlow(initialPaused)

    override val rules: StateFlow<List<BlockingRule>> = rulesState
    override val paused: StateFlow<Boolean> = pausedState

    override suspend fun getRules(): List<BlockingRule> = rulesState.value

    override suspend fun getRule(packageName: String): BlockingRule? =
        rulesState.value.firstOrNull { it.packageName == packageName }

    override suspend fun isPaused(): Boolean = pausedState.value

    override suspend fun upsertRule(rule: BlockingRule) {
        rulesState.value = rulesState.value.filterNot { it.packageName == rule.packageName } + rule
    }

    override suspend fun deleteRule(packageName: String) {
        rulesState.value = rulesState.value.filterNot { it.packageName == packageName }
    }

    override suspend fun setEnabled(packageName: String, enabled: Boolean) {
        rulesState.value = rulesState.value.map {
            if (it.packageName == packageName) it.copy(enabled = enabled) else it
        }
    }

    override suspend fun setDailyLimitMinutes(packageName: String, minutes: Int) {
        rulesState.value = rulesState.value.map {
            if (it.packageName == packageName) it.copy(dailyLimitMinutes = minutes) else it
        }
    }

    override suspend fun setPaused(paused: Boolean) {
        pausedState.value = paused
    }
}
