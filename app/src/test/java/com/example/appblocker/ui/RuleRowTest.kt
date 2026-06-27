package com.example.appblocker.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class RuleRowTest {

    private fun row(limit: Int, used: Long) = RuleRow(
        packageName = "com.example",
        label = "Example",
        icon = null,
        limitMinutes = limit,
        usedMinutesToday = used,
        enabled = true,
    )

    @Test
    fun `remaining is limit minus used`() {
        assertEquals(20L, row(limit = 30, used = 10).remainingMinutes)
    }

    @Test
    fun `remaining never goes negative`() {
        assertEquals(0L, row(limit = 30, used = 45).remainingMinutes)
    }

    @Test
    fun `progress is the used fraction of the limit`() {
        assertEquals(0.5f, row(limit = 30, used = 15).progress, 0.0001f)
    }

    @Test
    fun `progress is clamped to one when over the limit`() {
        assertEquals(1f, row(limit = 30, used = 45).progress, 0.0001f)
    }

    @Test
    fun `zero limit reads as full progress`() {
        assertEquals(1f, row(limit = 0, used = 0).progress, 0.0001f)
    }
}
