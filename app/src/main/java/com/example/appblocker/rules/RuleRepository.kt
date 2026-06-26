package com.example.appblocker.rules

import kotlinx.coroutines.flow.Flow

/**
 * Reads and writes the single blocking rule. [rule] is a reactive stream for the
 * UI; [getRule] is a one-shot read for the accessibility path, which only needs
 * the current value when an app is opened.
 */
interface RuleRepository {
    val rule: Flow<BlockingRule>

    suspend fun getRule(): BlockingRule

    suspend fun setDailyLimitMinutes(minutes: Int)

    suspend fun setEnabled(enabled: Boolean)
}
