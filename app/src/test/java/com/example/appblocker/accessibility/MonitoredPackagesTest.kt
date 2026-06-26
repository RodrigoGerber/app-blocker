package com.example.appblocker.accessibility

import com.example.appblocker.rules.BlockingRule
import org.junit.Assert.assertEquals
import org.junit.Test

class MonitoredPackagesTest {

    private fun rule(pkg: String, enabled: Boolean) =
        BlockingRule(packageName = pkg, dailyLimitMinutes = 30, enabled = enabled)

    @Test
    fun `no rules yields empty set`() {
        assertEquals(emptySet<String>(), MonitoredPackages.from(emptyList(), paused = false))
    }

    @Test
    fun `only enabled rules are monitored`() {
        val rules = listOf(
            rule("com.instagram.android", enabled = true),
            rule("com.zhiliaoapp.musically", enabled = false),
            rule("com.twitter.android", enabled = true),
        )
        assertEquals(
            setOf("com.instagram.android", "com.twitter.android"),
            MonitoredPackages.from(rules, paused = false),
        )
    }

    @Test
    fun `paused yields empty set even with enabled rules`() {
        val rules = listOf(
            rule("com.instagram.android", enabled = true),
            rule("com.twitter.android", enabled = true),
        )
        assertEquals(emptySet<String>(), MonitoredPackages.from(rules, paused = true))
    }

    @Test
    fun `all disabled yields empty set`() {
        val rules = listOf(
            rule("com.instagram.android", enabled = false),
            rule("com.twitter.android", enabled = false),
        )
        assertEquals(emptySet<String>(), MonitoredPackages.from(rules, paused = false))
    }
}
