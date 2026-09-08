package com.saigopal.browser.data.repo

import com.saigopal.browser.domain.BBRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/*
 * P10 10.3: the versioned backup format round-trips every section; unknown/foreign versions are
 * rejected rather than half-imported.
 */
class BackupCodecTest {

    @Test
    fun `encode then decode round-trips a full profile`() {
        val profile = BackupCodec.Profile(
            prefs = mapOf(
                "sp_theme" to "2",
                "sp_invert" to false,
                "font_size" to 3,
                "big_time" to 1700000000123L,
                "ratio" to 0.75,
                "saved_set" to setOf("x", "y"),
            ),
            bookmarks = listOf(BBRecord("Alpha", "https://a.example/", time = 111L, colorCode = 4)),
            tiles = listOf(BBRecord("Tile", "https://t.example/", time = 222L)),
            whitelists = mapOf(
                WhitelistKind.AD_BLOCK to listOf("a.com", "b.com"),
                WhitelistKind.COOKIE to listOf("c.com"),
            ),
            sessions = listOf(("Title" to "https://open.example/")),
        )

        val decoded = BackupCodec.decode(BackupCodec.encode(profile))

        assertEquals("2", decoded.prefs["sp_theme"])
        assertEquals(false, decoded.prefs["sp_invert"])
        assertEquals(3, decoded.prefs["font_size"])
        assertEquals(1700000000123L, decoded.prefs["big_time"])
        assertEquals(0.75, decoded.prefs["ratio"] as Double, 0.0)
        assertEquals(setOf("x", "y"), decoded.prefs["saved_set"])
        assertEquals(profile.bookmarks, decoded.bookmarks)
        assertEquals(profile.tiles, decoded.tiles)
        assertEquals(listOf("a.com", "b.com"), decoded.whitelists[WhitelistKind.AD_BLOCK])
        assertEquals(listOf("c.com"), decoded.whitelists[WhitelistKind.COOKIE])
        assertTrue(decoded.whitelists[WhitelistKind.JAVASCRIPT].isNullOrEmpty())
        assertEquals(profile.sessions, decoded.sessions)
    }

    @Test
    fun `empty profile encodes and decodes to defaults`() {
        val decoded = BackupCodec.decode(BackupCodec.encode(BackupCodec.Profile()))
        assertTrue(decoded.prefs.isEmpty())
        assertTrue(decoded.bookmarks.isEmpty())
        assertTrue(decoded.tiles.isEmpty())
        assertTrue(decoded.whitelists.isEmpty())
        assertTrue(decoded.sessions.isEmpty())
    }

    @Test
    fun `foreign format versions are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            BackupCodec.decode("""{"format": 99}""")
        }
    }
}
