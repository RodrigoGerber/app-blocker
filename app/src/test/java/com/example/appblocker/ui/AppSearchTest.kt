package com.example.appblocker.ui

import com.example.appblocker.system.InstalledApp
import org.junit.Assert.assertEquals
import org.junit.Test

class AppSearchTest {

    private fun app(label: String, pkg: String = label) =
        InstalledApp(packageName = pkg, label = label, icon = null)

    private val apps = listOf(app("Instagram"), app("TikTok"), app("Telegram"))

    @Test
    fun `blank query returns all apps`() {
        assertEquals(apps, filterApps(apps, ""))
    }

    @Test
    fun `whitespace-only query is treated as blank`() {
        assertEquals(apps, filterApps(apps, "   "))
    }

    @Test
    fun `match is case-insensitive`() {
        assertEquals(listOf(app("Instagram")), filterApps(apps, "INSTA"))
    }

    @Test
    fun `substring matches every label containing it`() {
        assertEquals(listOf(app("Instagram"), app("Telegram")), filterApps(apps, "gram"))
    }

    @Test
    fun `no match returns empty`() {
        assertEquals(emptyList<InstalledApp>(), filterApps(apps, "zzz"))
    }
}
