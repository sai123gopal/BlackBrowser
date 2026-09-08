package com.saigopal.browser.ui.screens.settings

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.saigopal.browser.R
import com.saigopal.browser.data.repo.BackupCodec
import com.saigopal.browser.data.repo.WhitelistKind
import com.saigopal.browser.domain.NetscapeBookmarks
import com.saigopal.browser.domain.TabManager
import com.saigopal.browser.ui.design.components.SectionHeader
import com.saigopal.browser.ui.di.rememberContainer
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/*
 * Data & backup section (P10 10.3) — real SAF flows replace the legacy hardcoded
 * /storage/emulated/0/BlackBrowser_backup writes (defect B9).
 *
 * - Full profile: versioned JSON (BackupCodec v2) via CreateDocument/OpenDocument.
 * - Bookmarks: Netscape HTML byte-compatible with the old exporter (NetscapeBookmarks), so files
 *   round-trip with pre-rewrite releases in both directions.
 */
@Composable
fun DataSection(onBack: () -> Unit) {
    val context = LocalContext.current
    val container = rememberContainer()
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val exportedMessage = stringResource(R.string.bb_backup_exported)
    val failedMessage = stringResource(R.string.bb_backup_failed)
    val legacyNote = stringResource(R.string.bb_backup_legacy_note)
    val importedFormat = stringResource(R.string.bb_backup_imported_arg)
    val bookmarksImportedFormat = stringResource(R.string.bb_bookmarks_imported_arg)

    fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun writeText(uri: Uri, text: String): Boolean = runCatching {
        context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
            out.write(text.toByteArray(Charsets.UTF_8))
        } != null
    }.getOrDefault(false)

    fun readText(uri: Uri): String? = runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes().toString(Charsets.UTF_8)
        }
    }.getOrNull()

    // ---- full profile --------------------------------------------------------------------------

    val exportProfileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            val sp = PreferenceManager.getDefaultSharedPreferences(context)
            val whitelists = WhitelistKind.entries.associateWith { container.whitelists.listDomains(it) }
            val profile = BackupCodec.Profile(
                prefs = sp.all.orEmpty(),
                bookmarks = container.records.listBookmarks(),
                tiles = container.records.listTiles(),
                whitelists = whitelists,
                sessions = container.tabs.restoreSession(),
            )
            val ok = writeText(uri, BackupCodec.encode(profile))
            withContext(Dispatchers.Main) { toast(if (ok) exportedMessage else failedMessage) }
        }
    }

    val importProfileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            val text = readText(uri)
            val restored = runCatching {
                val profile = BackupCodec.decode(text.orEmpty())
                applyProfile(
                    context = context.applicationContext,
                    records = container.records,
                    whitelists = container.whitelists,
                    profile = profile,
                )
            }.getOrDefault(-1)
            withContext(Dispatchers.Main) {
                if (restored < 0) {
                    toast(failedMessage)
                } else {
                    Toast.makeText(context, String.format(importedFormat, restored), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ---- Netscape bookmarks ----------------------------------------------------------------------

    val exportBookmarksLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/html"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            val bookmarks = container.records.listBookmarks()
            val html = NetscapeBookmarks.buildExport(
                bookmarks.map { NetscapeBookmarks.entry(it.title, it.url, it.time) },
            )
            val ok = writeText(uri, html)
            withContext(Dispatchers.Main) { toast(if (ok) exportedMessage else failedMessage) }
        }
    }

    val importBookmarksLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            val entries = NetscapeBookmarks.parse(readText(uri).orEmpty())
            var count = 0
            for (entry in entries) {
                if (!container.records.bookmarkExists(entry.url)) {
                    container.records.addBookmark(entry.title, entry.url, time = System.currentTimeMillis())
                    count++
                }
            }
            val message = String.format(bookmarksImportedFormat, count)
            withContext(Dispatchers.Main) { toast(message) }
        }
    }

    SettingsScaffold(title = stringResource(R.string.setting_title_data), onBack = onBack) {
        SectionHeader(stringResource(R.string.title_appData))
        SettingsLinkRow(
            title = stringResource(R.string.setting_title_export_appData),
            subtitle = stringResource(R.string.setting_summary_export_appData),
            icon = null,
        ) {
            val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
            exportProfileLauncher.launch("BlackBrowser_backup_$stamp.json")
        }
        SettingsLinkRow(
            title = stringResource(R.string.setting_title_import_appData),
            subtitle = null,
            icon = null,
        ) { importProfileLauncher.launch(arrayOf("application/json")) }

        SectionHeader(stringResource(R.string.album_title_bookmarks))
        SettingsLinkRow(
            title = stringResource(R.string.setting_title_export_bookmarks),
            subtitle = null,
            icon = null,
        ) { exportBookmarksLauncher.launch("export_Bookmarks.html") }
        SettingsLinkRow(
            title = stringResource(R.string.setting_title_import_bookmarks),
            subtitle = null,
            icon = null,
        ) { importBookmarksLauncher.launch(arrayOf("text/html", "text/plain", "application/octet-stream")) }

        SectionHeader(stringResource(R.string.bb_legacy_data_link))
        SettingsLinkRow(
            title = stringResource(R.string.bb_legacy_data_note_title),
            subtitle = legacyNote,
            icon = null,
        ) {
            // Legacy folder-based tools only exist behind LEGACY_SHELL; SAF flows replaced them here.
            toast(legacyNote)
        }
    }
}

/** Merge-with-dedupe restore; returns items added, or -1 on any failure. */
private suspend fun applyProfile(
    context: Context,
    records: com.saigopal.browser.data.repo.RecordsRepository,
    whitelists: com.saigopal.browser.data.repo.WhitelistRepository,
    profile: BackupCodec.Profile,
): Int {
    return try {
        var count = 0

        // Prefs first — but never let a foreign snapshot clobber the local session store.
        val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
        val incomingPrefs = profile.prefs - TabManager.SESSION_KEY
        for ((key, value) in incomingPrefs) {
            when (value) {
                is String -> editor.putString(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Double -> editor.putFloat(key, value.toFloat()) // SharedPreferences has no double
                is Set<*> -> @Suppress("UNCHECKED_CAST") editor.putStringSet(key, value as Set<String>)
                else -> Unit // unknown JSON shape: skip rather than corrupt the store
            }
        }
        // Session rows are written explicitly AFTER prefs so they always win.
        val sessions = JSONArray()
        profile.sessions.forEach { (title, url) ->
            sessions.put(JSONObject().put("t", title).put("u", url))
        }
        editor.putString(TabManager.SESSION_KEY, sessions.toString())
        editor.apply()

        for (record in profile.bookmarks) {
            if (!records.bookmarkExists(record.url)) {
                records.addBookmark(record.title, record.url, record.colorCode, record.time)
                count++
            }
        }
        val tileUrls = records.listTiles().mapTo(mutableSetOf()) { it.url.lowercase() }
        for ((index, record) in profile.tiles.withIndex()) {
            if (record.url.isNotBlank() && record.url.lowercase() !in tileUrls) {
                records.addTile(record.title.ifBlank { record.url }, record.url, index)
                count++
                tileUrls.add(record.url.lowercase())
            }
        }
        for ((kind, domains) in profile.whitelists) {
            val existing = whitelists.listDomains(kind).toSet()
            for (domain in domains) {
                if (domain.isNotBlank() && domain !in existing) {
                    whitelists.addDomain(kind, domain)
                    count++
                }
            }
        }
        count
    } catch (t: Throwable) {
        -1
    }
}
