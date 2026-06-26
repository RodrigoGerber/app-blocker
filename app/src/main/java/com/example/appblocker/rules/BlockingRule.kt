package com.example.appblocker.rules

import kotlinx.serialization.Serializable

/**
 * A blocking rule for a single app. [packageName] is the rule's identity: there
 * is at most one rule per app (independent daily limit per app). Serialized as
 * part of the rule list persisted in DataStore.
 */
@Serializable
data class BlockingRule(
    val packageName: String,
    val dailyLimitMinutes: Int,
    val enabled: Boolean,
)
