package com.example.appblocker.usage

import org.junit.Assert.assertEquals
import org.junit.Test

class UsageEventParserTest {

    private val parser = UsageEventParser()
    private val pkg = "com.instagram.android"

    private fun enter(ts: Long) = ForegroundEvent(pkg, ts, ForegroundTransition.ENTER)
    private fun exit(ts: Long) = ForegroundEvent(pkg, ts, ForegroundTransition.EXIT)

    @Test
    fun `no events yields zero`() {
        val result = parser.calculateForegroundTime(emptyList(), pkg, intervalEndMillis = 1_000)
        assertEquals(0L, result)
    }

    @Test
    fun `one complete session`() {
        val events = listOf(enter(1_000), exit(4_000))
        val result = parser.calculateForegroundTime(events, pkg, intervalEndMillis = 10_000)
        assertEquals(3_000L, result)
    }

    @Test
    fun `multiple sessions are summed`() {
        val events = listOf(
            enter(1_000), exit(3_000), // 2000
            enter(5_000), exit(6_000), // 1000
        )
        val result = parser.calculateForegroundTime(events, pkg, intervalEndMillis = 10_000)
        assertEquals(3_000L, result)
    }

    @Test
    fun `open session is closed at interval end`() {
        val events = listOf(enter(1_000))
        val result = parser.calculateForegroundTime(events, pkg, intervalEndMillis = 5_000)
        assertEquals(4_000L, result)
    }

    @Test
    fun `exit without enter is ignored`() {
        val events = listOf(exit(2_000), enter(3_000), exit(4_000))
        val result = parser.calculateForegroundTime(events, pkg, intervalEndMillis = 10_000)
        assertEquals(1_000L, result)
    }

    @Test
    fun `duplicate enter does not restart the session`() {
        val events = listOf(enter(1_000), enter(2_000), exit(4_000))
        val result = parser.calculateForegroundTime(events, pkg, intervalEndMillis = 10_000)
        assertEquals(3_000L, result)
    }

    @Test
    fun `events from other packages are ignored`() {
        val events = listOf(
            ForegroundEvent("com.other.app", 0, ForegroundTransition.ENTER),
            enter(1_000),
            ForegroundEvent("com.other.app", 2_000, ForegroundTransition.EXIT),
            exit(3_000),
        )
        val result = parser.calculateForegroundTime(events, pkg, intervalEndMillis = 10_000)
        assertEquals(2_000L, result)
    }

    @Test
    fun `out of order timestamps are sorted before parsing`() {
        val events = listOf(exit(4_000), enter(1_000))
        val result = parser.calculateForegroundTime(events, pkg, intervalEndMillis = 10_000)
        assertEquals(3_000L, result)
    }

    @Test
    fun `session crossing interval start counts only from first seen event`() {
        // First event in the queried window is already an EXIT (the ENTER was
        // before the window). With no open session, the EXIT is ignored.
        val events = listOf(exit(1_000), enter(2_000), exit(5_000))
        val result = parser.calculateForegroundTime(events, pkg, intervalEndMillis = 10_000)
        assertEquals(3_000L, result)
    }
}
