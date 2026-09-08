package com.saigopal.browser.ui.screens.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/*
 * The settings search filter must stay pure JVM so it can be tested without Robolectric.
 * Entries here are synthetic; only filter() semantics are under test.
 */
class SettingsIndexTest {

    private val entries = listOf(
        SettingsEntry(SettingsRoute.PRIVACY, "Start control", "privacy adblock cookies"),
        SettingsEntry(SettingsRoute.GESTURES, "Gestures", "swipe toolbar"),
        SettingsEntry(SettingsRoute.FEEDBACK, "Feedback", "feedback report bug suggestion issue"),
    )

    @Test
    fun `blank query returns everything`() {
        assertEquals(entries, SettingsIndex.filter(entries, ""))
        assertEquals(entries, SettingsIndex.filter(entries, "   "))
    }

    @Test
    fun `title match is case-insensitive`() {
        val result = SettingsIndex.filter(entries, "GESTURES")
        assertEquals(listOf(entries[1]), result)
    }

    @Test
    fun `keyword match hits synonyms not in the title`() {
        val result = SettingsIndex.filter(entries, "adblock")
        assertEquals(listOf(entries[0]), result)
    }

    @Test
    fun `query with surrounding whitespace still matches`() {
        val result = SettingsIndex.filter(entries, "  bug ")
        assertEquals(listOf(entries[2]), result)
    }

    @Test
    fun `no match returns empty list`() {
        assertTrue(SettingsIndex.filter(entries, "zzz-not-a-setting").isEmpty())
    }

    @Test
    fun `partial words match - mirrors legacy contains behaviour`() {
        val result = SettingsIndex.filter(entries, "gest")
        assertEquals(listOf(entries[1]), result)
    }
}
