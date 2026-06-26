package com.example.appblocker.blocking

import android.util.Log
import com.example.appblocker.rules.RuleRepository
import com.example.appblocker.usage.DailyUsageProvider

/**
 * The single place that decides whether an app should be blocked. Keeping this
 * logic out of the accessibility service makes it trivially unit testable
 * (spec §5.1, §9.3).
 *
 * Fail-open: if usage cannot be computed, the decision is [BlockingDecision.Allow]
 * so a query failure never produces a confusing, unexplained block (spec §15.3).
 */
class BlockingPolicy(
    private val ruleRepository: RuleRepository,
    private val dailyUsageProvider: DailyUsageProvider,
) {
    suspend fun evaluate(packageName: String): BlockingDecision {
        val rule = ruleRepository.getRule()

        if (!rule.enabled || rule.packageName != packageName) {
            return BlockingDecision.Allow
        }

        val limitMillis = rule.dailyLimitMinutes * 60_000L

        val usedMillis = try {
            dailyUsageProvider.getUsageToday(packageName).usedMillis
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read usage for $packageName; allowing.", e)
            return BlockingDecision.Allow
        }

        return if (usedMillis >= limitMillis) {
            BlockingDecision.Block(
                packageName = packageName,
                usedMillis = usedMillis,
                limitMillis = limitMillis,
            )
        } else {
            BlockingDecision.Allow
        }
    }

    private companion object {
        const val TAG = "BlockingPolicy"
    }
}
