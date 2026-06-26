package com.example.appblocker.accessibility

import com.example.appblocker.blocking.BlockingPolicy
import com.example.appblocker.blocking.HomeRedirector
import com.example.appblocker.rules.BlockingRule
import com.example.appblocker.rules.FakeRuleRepository
import com.example.appblocker.usage.DailyUsage
import com.example.appblocker.usage.DailyUsageProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ForegroundAppHandlerTest {

    private val pkg = "com.instagram.android"

    private class FakeHomeRedirector : HomeRedirector {
        var redirectCount = 0
        override fun redirectToHome(): Boolean {
            redirectCount++
            return true
        }
    }

    private class FakeUsageProvider(private val usedMillis: Long) : DailyUsageProvider {
        override suspend fun getUsageToday(packageName: String) =
            DailyUsage(packageName, usedMillis)
    }

    private fun policy(enabled: Boolean, limitMinutes: Int, usedMillis: Long) =
        BlockingPolicy(
            ruleRepository = FakeRuleRepository(
                listOf(
                    BlockingRule(
                        packageName = pkg,
                        dailyLimitMinutes = limitMinutes,
                        enabled = enabled,
                    ),
                ),
            ),
            dailyUsageProvider = FakeUsageProvider(usedMillis),
        )

    @Test
    fun `spike mode redirects on detection ignoring limit`() = runTest {
        val redirector = FakeHomeRedirector()
        val handler = ForegroundAppHandler(
            blockingPolicy = policy(enabled = false, limitMinutes = 30, usedMillis = 0),
            homeRedirector = redirector,
            spikeRedirectOnDetect = true,
            dryRun = false,
        )

        handler.onAppOpened(pkg)

        assertEquals(1, redirector.redirectCount)
    }

    @Test
    fun `spike mode with dry run does not redirect`() = runTest {
        val redirector = FakeHomeRedirector()
        val handler = ForegroundAppHandler(
            blockingPolicy = policy(enabled = false, limitMinutes = 30, usedMillis = 0),
            homeRedirector = redirector,
            spikeRedirectOnDetect = true,
            dryRun = true,
        )

        handler.onAppOpened(pkg)

        assertEquals(0, redirector.redirectCount)
    }

    @Test
    fun `normal mode below limit does not redirect`() = runTest {
        val redirector = FakeHomeRedirector()
        val handler = ForegroundAppHandler(
            blockingPolicy = policy(enabled = true, limitMinutes = 30, usedMillis = 5 * 60_000L),
            homeRedirector = redirector,
            spikeRedirectOnDetect = false,
            dryRun = false,
        )

        handler.onAppOpened(pkg)

        assertEquals(0, redirector.redirectCount)
    }

    @Test
    fun `normal mode over limit redirects`() = runTest {
        val redirector = FakeHomeRedirector()
        val handler = ForegroundAppHandler(
            blockingPolicy = policy(enabled = true, limitMinutes = 30, usedMillis = 45 * 60_000L),
            homeRedirector = redirector,
            spikeRedirectOnDetect = false,
            dryRun = false,
        )

        handler.onAppOpened(pkg)

        assertEquals(1, redirector.redirectCount)
    }

    @Test
    fun `normal mode over limit with dry run does not redirect`() = runTest {
        val redirector = FakeHomeRedirector()
        val handler = ForegroundAppHandler(
            blockingPolicy = policy(enabled = true, limitMinutes = 30, usedMillis = 45 * 60_000L),
            homeRedirector = redirector,
            spikeRedirectOnDetect = false,
            dryRun = true,
        )

        handler.onAppOpened(pkg)

        assertEquals(0, redirector.redirectCount)
    }
}
