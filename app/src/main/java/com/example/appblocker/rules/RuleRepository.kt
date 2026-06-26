package com.example.appblocker.rules

import kotlinx.coroutines.flow.Flow

/**
 * Reads and writes the set of blocking rules (one per app) plus a global pause
 * flag. The reactive [rules]/[paused] streams drive the UI; the one-shot reads
 * serve the accessibility path, which only needs current values when an app is
 * opened.
 */
interface RuleRepository {

    /** All rules, in no particular order. */
    val rules: Flow<List<BlockingRule>>

    /** Global "pause all" — when true, nothing is blocked regardless of rules. */
    val paused: Flow<Boolean>

    suspend fun getRules(): List<BlockingRule>

    /** The rule for [packageName], or null if the app is not monitored. */
    suspend fun getRule(packageName: String): BlockingRule?

    suspend fun isPaused(): Boolean

    /** Insert or replace the rule for [rule.packageName]. */
    suspend fun upsertRule(rule: BlockingRule)

    suspend fun deleteRule(packageName: String)

    suspend fun setEnabled(packageName: String, enabled: Boolean)

    suspend fun setDailyLimitMinutes(packageName: String, minutes: Int)

    suspend fun setPaused(paused: Boolean)
}
