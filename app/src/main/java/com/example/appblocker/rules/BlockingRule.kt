package com.example.appblocker.rules

/**
 * A blocking rule for a single app. The MVP only ever has one rule (Instagram),
 * but carrying [packageName] in the model keeps the door open for multiple apps
 * later without reshaping the domain.
 */
data class BlockingRule(
    val packageName: String,
    val dailyLimitMinutes: Int,
    val enabled: Boolean,
)
