package com.saigopal.browser.domain

import android.content.Context
import androidx.preference.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicLong

/*
 * TabManager — source of truth for open-tab METADATA.
 *
 * P02 scope: metadata + session backup. WebView instances stay owned by the legacy
 * BrowserContainer until P05 flips ownership; MainActivity can adopt this class
 * incrementally. Session persistence uses a JSON blob in prefs (key below) because the
 * TAB table only has TITLE/URL/TIME columns — abusing TIME again would repeat the
 * bookmark-colour mistake (bug B6). Room migration in P10 moves this into the schema properly.
 */

class TabManager(private val appContext: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val idGen = AtomicLong(System.currentTimeMillis())

    private val _tabs = MutableStateFlow<List<TabState>>(emptyList())
    val tabs: StateFlow<List<TabState>> = _tabs

    private val _activeId = MutableStateFlow<Long?>(null)
    val activeId: StateFlow<Long?> = _activeId

    val active: TabState?
        get() = _tabs.value.firstOrNull { it.id == _activeId.value }

    // ---------------------------------------------------------------- lifecycle

    fun newTab(
        title: String,
        url: String?,
        foreground: Boolean,
        incognito: Boolean = false,
        desktopMode: Boolean = false,
    ): Long {
        val id = idGen.incrementAndGet()
        val tabsNow = _tabs.value.toMutableList()
        val insertAt = if (foreground) (_tabs.value.indexOfFirst { it.id == _activeId.value } + 1).coerceIn(0, tabsNow.size) else tabsNow.size
        val tab = TabState(
            id = id,
            title = title,
            url = url,
            position = insertAt,
            isForeground = foreground,
            isIncognito = incognito,
            isDesktopMode = desktopMode,
            progress = 100,
        )
        tabsNow.add(insertAt, tab)
        publish(tabsNow, if (foreground) id else _activeId.value)
        scheduleSessionSave()
        return id
    }

    /** P09 (9.3): per-tab desktop-site toggle; the caller applies the UA on the pooled instance. */
    fun setDesktop(id: Long, on: Boolean) {
        val updated = _tabs.value.map { t ->
            if (t.id != id) t else t.copy(isDesktopMode = on)
        }
        if (updated != _tabs.value) {
            _tabs.value = updated
            scheduleSessionSave()
        }
    }

    /** Dynamically toggles incognito mode on an existing open tab. */
    fun setIncognito(id: Long, on: Boolean) {
        val updated = _tabs.value.map { t ->
            if (t.id != id) t else t.copy(isIncognito = on)
        }
        if (updated != _tabs.value) {
            _tabs.value = updated
            scheduleSessionSave()
        }
    }

    fun close(id: Long): TabState? {
        val removed = _tabs.value.firstOrNull { it.id == id } ?: return null
        val remaining = _tabs.value.filterNot { it.id == id }.mapIndexed { i, t -> t.copy(position = i) }
        val nextActive = when {
            remaining.isEmpty() -> null
            id == _activeId.value -> remaining[(removed.position).coerceIn(0, remaining.size - 1)].id
            else -> _activeId.value
        }
        publish(remaining, nextActive)
        scheduleSessionSave()
        return removed
    }

    fun activate(id: Long) {
        if (_tabs.value.none { it.id == id }) return
        publish(_tabs.value, id)
        scheduleSessionSave()
    }

    /** Cycle for gestures NEXT_TAB/PREVIOUS_TAB; returns the new active id (null if empty). */
    fun cycle(delta: Int): Long? {
        val list = _tabs.value
        if (list.isEmpty()) return null
        val currentIdx = list.indexOfFirst { it.id == _activeId.value }.let { if (it < 0) 0 else it }
        val nextIdx = ((currentIdx + delta) % list.size + list.size) % list.size
        val target = list[nextIdx].id
        activate(target)
        return target
    }

    fun updateMeta(id: Long, title: String? = null, url: String? = null, progress: Int? = null) {
        val updated = _tabs.value.map { t ->
            if (t.id != id) t else t.copy(
                title = title ?: t.title,
                url = url ?: t.url,
                progress = progress ?: t.progress,
            )
        }
        if (updated != _tabs.value) {
            _tabs.value = updated
            scheduleSessionSave()
        }
    }

    private fun publish(list: List<TabState>, activeId: Long?) {
        _tabs.value = list.mapIndexed { i, t -> t.copy(position = i, isForeground = t.id == activeId) }
        _activeId.value = activeId
    }

    // ---------------------------------------------------------------- session backup

    private var saveJob: kotlinx.coroutines.Job? = null

    private fun scheduleSessionSave() {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(500)
            saveSessionNow()
        }
    }

    suspend fun saveSessionNow() = mutex.withLock {
        val prefs = PreferenceManager.getDefaultSharedPreferences(appContext)
        val arr = JSONArray()
        _tabs.value.filterNot { it.isIncognito }.forEach { t ->
            arr.put(JSONObject().put("t", t.title).put("u", t.url ?: ""))
        }
        prefs.edit().putString(SESSION_KEY, arr.toString()).apply()
    }

    /** Restored rows are placeholders; the caller creates real tabs from them on cold start. */
    fun restoreSession(): List<Pair<String, String>> {
        val prefs = PreferenceManager.getDefaultSharedPreferences(appContext)
        val raw = prefs.getString(SESSION_KEY, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                o.optString("t") to o.optString("u")
            }.filter { it.second.isNotBlank() || it.first.isNotBlank() }
        }.getOrDefault(emptyList())
    }

    fun clearSession() {
        PreferenceManager.getDefaultSharedPreferences(appContext).edit().remove(SESSION_KEY).apply()
    }

    companion object {
        const val SESSION_KEY = "p2_session_json_v1"
    }
}
