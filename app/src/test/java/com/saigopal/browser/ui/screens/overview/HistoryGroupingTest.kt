package com.saigopal.browser.ui.screens.overview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class HistoryGroupingTest {

    private val utc = TimeZone.getTimeZone("UTC")

    private fun utcTime(year: Int, month: Int, day: Int, hour: Int = 12): Long {
        val cal = Calendar.getInstance(utc)
        cal.clear()
        cal.set(year, month - 1, day, hour, 0, 0)
        return cal.timeInMillis
    }

    @Test
    fun `day key is stable and date-derived`() {
        val a = historyDayKey(utcTime(2026, 8, 1), utc)
        val b = historyDayKey(utcTime(2026, 8, 1, 23), utc)
        val c = historyDayKey(utcTime(2026, 8, 2), utc)
        assertEquals(a, b)          // same day → same key regardless of hour
        assertTrue(a != c)          // next day → different key
    }

    @Test
    fun `same label in different years never merges`() {
        val key2025 = historyDayKey(utcTime(2025, 8, 1), utc)
        val key2026 = historyDayKey(utcTime(2026, 8, 1), utc)
        assertTrue(key2025 != key2026) // year prefix keeps sort-stable separation
    }

    @Test
    fun `groups are ordered newest first with newest-first items`() {
        val days = groupHistoryByDay(
            entries = listOf(
                HistoryEntry("old", "https://old.example", utcTime(2026, 7, 30)),
                HistoryEntry("mid", "https://mid.example", utcTime(2026, 7, 31)),
                HistoryEntry("new-late", "https://new.example/late", utcTime(2026, 8, 1, 20)),
                HistoryEntry("new-early", "https://new.example/early", utcTime(2026, 8, 1, 9)),
            ),
            timeZone = utc,
        )
        assertEquals(listOf("Aug 01", "Jul 31", "Jul 30"), days.map { it.label })
        assertEquals(
            listOf("new-late", "new-early"),
            days.first().items.map { it.title },
        )
    }

    @Test
    fun `empty input yields no groups`() {
        assertTrue(groupHistoryByDay(emptyList(), utc).isEmpty())
    }
}
