package com.example.appblocker.blocking

import com.example.appblocker.rules.BlockingRule
import com.example.appblocker.rules.FakeRuleRepository
import com.example.appblocker.usage.DailyUsage
import com.example.appblocker.usage.DailyUsageProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockingPolicyTest {

    private val pkg = "com.instagram.android"

    private class FakeUsageProvider(
        private val usedMillis: Long,
        private val throwError: Boolean = false,
    ) : DailyUsageProvider {
        override suspend fun getUsageToday(packageName: String): DailyUsage {
            if (throwError) throw IllegalStateException("boom")
            return DailyUsage(packageName, usedMillis)
        }
    }

    private fun policy(rules: List<BlockingRule>, usedMillis: Long, error: Boolean = false) =
        BlockingPolicy(
            ruleRepository = FakeRuleRepository(rules),
            dailyUsageProvider = FakeUsageProvider(usedMillis, error),
        )

    private fun rule(enabled: Boolean = true, limit: Int = 30, pkg: String = this.pkg) =
        BlockingRule(packageName = pkg, dailyLimitMinutes = limit, enabled = enabled)

    @Test
    fun `no rule for app allows`() = runTest {
        val decision = policy(rules = emptyList(), usedMillis = 999_999_999).evaluate(pkg)
        assertEquals(BlockingDecision.Allow, decision)
    }

    @Test
    fun `disabled rule allows`() = runTest {
        val decision = policy(listOf(rule(enabled = false)), usedMillis = 999_999_999).evaluate(pkg)
        assertEquals(BlockingDecision.Allow, decision)
    }

    @Test
    fun `package without a rule allows`() = runTest {
        val decision = policy(listOf(rule()), usedMillis = 999_999_999).evaluate("com.other.app")
        assertEquals(BlockingDecision.Allow, decision)
    }

    @Test
    fun `usage below limit allows`() = runTest {
        val decision = policy(listOf(rule(limit = 30)), usedMillis = 10 * 60_000L).evaluate(pkg)
        assertEquals(BlockingDecision.Allow, decision)
    }

    @Test
    fun `usage exactly at limit blocks`() = runTest {
        val decision = policy(listOf(rule(limit = 30)), usedMillis = 30 * 60_000L).evaluate(pkg)
        assertTrue(decision is BlockingDecision.Block)
    }

    @Test
    fun `usage above limit blocks`() = runTest {
        val decision = policy(listOf(rule(limit = 30)), usedMillis = 45 * 60_000L).evaluate(pkg)
        assertTrue(decision is BlockingDecision.Block)
    }

    @Test
    fun `zero limit blocks immediately`() = runTest {
        val decision = policy(listOf(rule(limit = 0)), usedMillis = 0).evaluate(pkg)
        assertTrue(decision is BlockingDecision.Block)
    }

    @Test
    fun `provider error fails open and allows`() = runTest {
        val decision = policy(listOf(rule(limit = 30)), usedMillis = 0, error = true).evaluate(pkg)
        assertEquals(BlockingDecision.Allow, decision)
    }

    @Test
    fun `evaluates the rule matching the opened package`() = runTest {
        // Two rules; opening the second app should use its (zero) limit.
        val rules = listOf(
            rule(limit = 60, pkg = "com.instagram.android"),
            rule(limit = 0, pkg = "com.zhiliaoapp.musically"),
        )
        val decision = policy(rules, usedMillis = 0).evaluate("com.zhiliaoapp.musically")
        assertTrue(decision is BlockingDecision.Block)
    }
}
