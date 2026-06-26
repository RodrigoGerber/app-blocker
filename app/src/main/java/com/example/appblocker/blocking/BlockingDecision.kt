package com.example.appblocker.blocking

/**
 * Outcome of evaluating whether an app should be allowed to stay in the
 * foreground. [Block] carries the numbers behind the decision so callers can
 * log or display them.
 */
sealed interface BlockingDecision {
    data object Allow : BlockingDecision

    data class Block(
        val packageName: String,
        val usedMillis: Long,
        val limitMillis: Long,
    ) : BlockingDecision
}
