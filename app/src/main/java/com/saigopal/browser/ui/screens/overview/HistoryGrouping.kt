package com.saigopal.browser.ui.screens.overview

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/*
 * Pure history grouping (P04 §4.6). Extracted from UI so it can be unit-tested on the JVM.
 * Label format "MMM dd" matches the legacy RecordAdapter day headers (spec parity).
 */

/** One grouped section of the history list. */
internal data class HistoryDay(
    val label: String,
    val items: List<HistoryEntry>,
)

internal data class HistoryEntry(
    val title: String,
    val url: String,
    val time: Long,
)

/**
 * Day key for a timestamp in [timeZone] — "MMM dd" label plus a sort-stable prefix so
 * different months with equal labels never merge.
 */
internal fun historyDayKey(timeMs: Long, timeZone: TimeZone = TimeZone.getDefault()): String {
    val cal = Calendar.getInstance(timeZone).apply { timeInMillis = timeMs }
    val df = SimpleDateFormat("yyyy|MMM dd", Locale.US)
    df.timeZone = timeZone
    return df.format(Date(cal.timeInMillis))
}

internal fun groupHistoryByDay(
    entries: List<HistoryEntry>,
    timeZone: TimeZone = TimeZone.getDefault(),
): List<HistoryDay> =
    entries
        .sortedByDescending { it.time }
        .groupBy { historyDayKey(it.time, timeZone) }
        .map { (_, items) -> HistoryDay(label = historyDayLabel(items.first().time, timeZone), items = items) }

private fun historyDayLabel(timeMs: Long, timeZone: TimeZone): String {
    val df = SimpleDateFormat("MMM dd", Locale.US)
    df.timeZone = timeZone
    return df.format(Date(timeMs))
}
