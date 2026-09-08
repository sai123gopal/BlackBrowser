package com.saigopal.browser.ui.screens.start

import com.saigopal.browser.domain.BBRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/*
 * Locks the one-time legacy quick-tile → GRID migration: same defaults, same slot order,
 * and strict idempotence so relaunching the start page never duplicates a tile.
 */
class StartPageMigrationTest {

    private fun record(url: String) = BBRecord(title = "t", url = url, time = 0L)

    private val defaults = listOf(
        "https://www.google.com/",
        "https://www.facebook.com",
        "https://www.instagram.com/",
        "https://twitter.com",
        "https://www.amazon.com/",
        "https://www.wikipedia.org/",
        "https://www.duckduckgo.com",
        "https://blackbrowser-androidapp.web.app/",
    )

    @Test
    fun `empty prefs yield all eight legacy defaults in slot order`() {
        val specs = StartPageMigration.migrateQuickTiles(emptyList(), emptyMap())
        assertEquals(8, specs.size)
        assertEquals(defaults, specs.map { it.url })
        assertEquals("Google", specs[0].title)
        assertEquals("BlackBrowser", specs[7].title)
    }

    @Test
    fun `prefs override names and urls per slot`() {
        val specs = StartPageMigration.migrateQuickTiles(
            emptyList(),
            mapOf("2_name" to "Kagi", "2_url" to "https://kagi.com"),
        )
        assertEquals("Kagi", specs[2].title)
        assertEquals("https://kagi.com", specs[2].url)
        assertEquals(defaults[3], specs[3].url) // untouched slots keep defaults
    }

    @Test
    fun `blank pref values fall back to defaults`() {
        val specs = StartPageMigration.migrateQuickTiles(
            emptyList(),
            mapOf("0_name" to "", "1_url" to "  "),
        )
        assertEquals("Google", specs[0].title)
        assertEquals(defaults[1], specs[1].url)
    }

    @Test
    fun `tiles already present are not migrated again - case insensitive`() {
        val existing = listOf(record("HTTPS://WWW.GOOGLE.COM/"), record("https://twitter.com"))
        val specs = StartPageMigration.migrateQuickTiles(existing, emptyMap())
        assertEquals(6, specs.size)
        assertTrue(specs.none { it.url.equals("https://www.google.com/", ignoreCase = true) })
    }

    @Test
    fun `duplicate urls inside prefs collapse to the first slot`() {
        val specs = StartPageMigration.migrateQuickTiles(
            emptyList(),
            mapOf("5_url" to "https://www.google.com/"),
        )
        // Slot 5 (Wikipedia) was overridden with Google's URL — slot 0 wins, slot 5 drops out.
        assertEquals(7, specs.size)
        assertEquals(1, specs.count { it.url == "https://www.google.com/" })
        assertTrue(specs.none { it.title == "Wikipedia" })
    }

    @Test
    fun `migration is idempotent across runs`() {
        val first = StartPageMigration.migrateQuickTiles(emptyList(), emptyMap())
        val applied = first.map { BBRecord(it.title, it.url, 0L) }
        val second = StartPageMigration.migrateQuickTiles(applied, emptyMap())
        assertTrue(second.isEmpty())
    }
}
