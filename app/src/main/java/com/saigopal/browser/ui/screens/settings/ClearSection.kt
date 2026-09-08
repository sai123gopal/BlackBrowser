package com.saigopal.browser.ui.screens.settings

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.saigopal.browser.R
import com.saigopal.browser.data.repo.BBSettings
import com.saigopal.browser.ui.design.components.SectionHeader
import com.saigopal.browser.ui.design.components.SwitchRow
import com.saigopal.browser.ui.design.components.BBConfirmSheet
import com.saigopal.browser.ui.di.rememberContainer

/*
 * Clear-controls section (legacy preference_clear.xml). The four flags only MARK what the
 * quit path should wipe (consumed by the legacy ClearService until P09 reworks it); the
 * delete-database action is immediate and destructive, hence the ConfirmSheet.
 */
@Composable
fun ClearSection(onBack: () -> Unit) {
    val container = rememberContainer()
    val settings by container.settings.state.collectAsState()
    var confirmingDelete by remember { mutableStateOf(false) }
    val context = LocalContext.current

    fun update(transform: (BBSettings) -> BBSettings) {
        container.settings.update(transform)
    }

    SettingsScaffold(title = stringResource(R.string.setting_title_clear_control), onBack = onBack) {
        SwitchRow(
            title = stringResource(R.string.clear_title_cache),
            checked = settings.clearFlags.cache,
            onCheckedChange = { v -> update { s -> s.copy(clearFlags = s.clearFlags.copy(cache = v)) } },
        )
        SwitchRow(
            title = stringResource(R.string.clear_title_history),
            checked = settings.clearFlags.history,
            onCheckedChange = { v -> update { s -> s.copy(clearFlags = s.clearFlags.copy(history = v)) } },
        )
        SwitchRow(
            title = stringResource(R.string.clear_title_indexedDB),
            checked = settings.clearFlags.indexedDb,
            onCheckedChange = { v -> update { s -> s.copy(clearFlags = s.clearFlags.copy(indexedDb = v)) } },
        )
        SwitchRow(
            title = stringResource(R.string.clear_title_cookie),
            subtitle = stringResource(R.string.setting_summary_cookie_delete),
            checked = settings.clearFlags.cookies,
            onCheckedChange = { v -> update { s -> s.copy(clearFlags = s.clearFlags.copy(cookies = v)) } },
        )
        SwitchRow(
            title = stringResource(R.string.clear_title_quit),
            subtitle = stringResource(R.string.clear_summary_quit),
            checked = settings.clearFlags.onQuit,
            onCheckedChange = { v -> update { s -> s.copy(clearFlags = s.clearFlags.copy(onQuit = v)) } },
        )

        SectionHeader(stringResource(R.string.clear_title_deleteDatabase))
        SettingsLinkRow(
            title = stringResource(R.string.clear_title_deleteDatabase),
            subtitle = stringResource(R.string.clear_summary_deleteDatabase),
            icon = null,
            onClick = { confirmingDelete = true },
        )
    }

    if (confirmingDelete) {
        BBConfirmSheet(
            title = stringResource(R.string.clear_title_deleteDatabase),
            body = stringResource(R.string.clear_summary_deleteDatabase),
            confirmLabel = stringResource(android.R.string.ok),
            destructive = true,
            onConfirm = {
                // Same file the legacy SQLiteOpenHelper owns; deleting it resets all tables.
                val ok = context.deleteDatabase("data.db")
                Toast.makeText(
                    context,
                    if (ok) "Deleted" else "Error",
                    Toast.LENGTH_SHORT,
                ).show()
            },
            onDismiss = { confirmingDelete = false },
        )
    }
}
