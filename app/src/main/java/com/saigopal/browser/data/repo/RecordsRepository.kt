package com.saigopal.browser.data.repo

import android.app.Application
import com.saigopal.browser.database.Record
import com.saigopal.browser.database.RecordAction
import com.saigopal.browser.domain.BBRecord
import com.saigopal.browser.unit.RecordUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/*
 * RecordsRepository — suspend wrappers around the legacy RecordAction SQLite helper.
 * All DB work runs on Dispatchers.IO under one mutex (RecordAction is not thread-safe).
 * Room replaces the storage in P10 without changing this API.
 */

class RecordsRepository(private val app: Application) {

    private val mutex = Mutex()

    // ---------------------------------------------------------------- history

    suspend fun addHistory(record: BBRecord): Unit = db {
        it.addHistory(Record(record.title, record.url, record.time, 0))
    }

    suspend fun listHistory(): List<BBRecord> = db { action ->
        action.listHistory().map(::fromLegacy)
    }

    suspend fun listHistoryPage(limit: Int, offset: Int): List<BBRecord> = db { action ->
        action.listHistory(limit, offset).map(::fromLegacy)
    }

    /**
     * PERF: bounded newest-first window for the start page's "Continue Browsing" strip.
     * Semantics after the caller's distinct-by-url + take match listHistory() exactly; the
     * difference is the query now stops at a small window instead of materializing every row.
     */
    suspend fun listRecentHistory(limit: Int): List<BBRecord> = db { action ->
        action.listRecentHistory(limit * 4).map(::fromLegacy) // extra headroom under the SQL window
    }

    suspend fun deleteUrl(url: String, table: String): Unit = db { it.deleteURL(url, table) }

    suspend fun clearHistory(): Unit = db { it.clearTable(RecordUnit.TABLE_HISTORY) }

    // ---------------------------------------------------------------- bookmarks

    /** Legacy colour codes ride in TIME until the P10 migration repairs the schema. */
    suspend fun addBookmark(
        title: String,
        url: String,
        colorCode: Int = 0,
        time: Long = 0L,
    ): Unit = db {
        val effectiveTime = when {
            time > 0L -> time
            colorCode in 1..11 -> colorCode.toLong()   // colour-only rows keep the legacy encoding
            else -> System.currentTimeMillis()
        }
        it.addBookmark(Record(title.trim(), url.trim(), effectiveTime, 0))
    }

    suspend fun listBookmarks(filter: Boolean = false, filterBy: Long = 0): List<BBRecord> = db { action ->
        action.listBookmark(app.applicationContext, filter, filterBy).map(::fromLegacy)
    }

    suspend fun editBookmark(oldUrl: String, newTitle: String, newColorCode: Int): Unit = db { action ->
        val existing = action.listBookmark(app.applicationContext, false, 0)
            .firstOrNull { r -> r.getURL() == oldUrl } ?: return@db
        action.deleteURL(oldUrl, RecordUnit.TABLE_BOOKMARK)
        val encoded = if (newColorCode in 1..11) newColorCode.toLong() else existing.getTime()
        action.addBookmark(Record(newTitle.trim(), oldUrl, encoded, 0))
    }

    suspend fun bookmarkExists(url: String): Boolean = db { it.checkUrl(url, RecordUnit.TABLE_BOOKMARK) }

    suspend fun deleteBookmark(url: String): Unit = db { it.deleteURL(url, RecordUnit.TABLE_BOOKMARK) }

    suspend fun clearBookmarks(): Unit = db { it.clearTable(RecordUnit.TABLE_BOOKMARK) }

    // ---------------------------------------------------------------- start tiles (GRID)

    suspend fun addTile(title: String, url: String, ordinal: Int): Boolean = db {
        it.addGridItem(Record(title.trim(), url.trim(), 0, ordinal))
    }

    suspend fun listTiles(): List<BBRecord> = db { action ->
        // listStartSite(Context) overload added in P02 — sorts by the legacy sort_startSite pref.
        action.listStartSite(app.applicationContext).map(::fromLegacy)
    }

    suspend fun deleteTile(url: String): Unit = db { it.deleteURL(url, RecordUnit.TABLE_GRID) }

    suspend fun clearTiles(): Unit = db { it.clearTable(RecordUnit.TABLE_GRID) }

    // ---------------------------------------------------------------- autocomplete

    /** Legacy parity: GRID + HISTORY + BOOKMARK concatenated (CompleteAdapter's source). */
    suspend fun autocomplete(): List<BBRecord> = db { action ->
        action.listEntries(app.applicationContext).map(::fromLegacy)
    }

    // ---------------------------------------------------------------- raw domain tables

    suspend fun listDomains(table: String): List<String> = db { it.listDomains(table) }

    suspend fun addDomain(domain: String, table: String): Unit = db { it.addDomain(domain, table) }

    suspend fun removeDomain(domain: String, table: String): Unit = db { it.deleteDomain(domain, table) }

    suspend fun domainExists(domain: String, table: String): Boolean = db { it.checkDomain(domain, table) }

    // ---------------------------------------------------------------- helpers

    private suspend fun <T> db(block: (RecordAction) -> T): T =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val action = RecordAction(app.applicationContext)
                try {
                    action.open(true)
                    block(action)
                } finally {
                    runCatching { action.close() }
                }
            }
        }

    private fun fromLegacy(r: Record): BBRecord {
        val t = r.getTime()
        val color = if (t in 1..11) t.toInt() else 0
        return BBRecord(
            title = r.getTitle() ?: "",
            url = r.getURL() ?: "",
            time = if (color > 0) 0L else t,
            colorCode = color,
        )
    }
}
