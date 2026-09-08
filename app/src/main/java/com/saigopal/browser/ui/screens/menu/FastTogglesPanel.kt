@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.saigopal.browser.ui.screens.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.saigopal.browser.R
import com.saigopal.browser.data.repo.WhitelistKind
import com.saigopal.browser.ui.design.components.SwitchRow
import com.saigopal.browser.ui.design.theme.BBSpace
import com.saigopal.browser.ui.di.rememberContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/*
 * Fast toggles (§6.4) — port of dialog_toggle. Master switches write settings immediately;
 * each engine card also carries a this-domain whitelist chip backed by WhitelistRepository.
 */

/** Host sheet so the shell can present the panel with one call. */
@Composable
fun FastTogglesSheet(
    currentDomain: String?,
    onApplyReload: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        FastTogglesPanel(
            currentDomain = currentDomain,
            onApplyReload = {
                onDismiss()
                onApplyReload()
            },
        )
    }
}

/**
 * @param currentDomain host of the active tab; null/blank hides whitelist chips (e.g. start page).
 */
@Composable
fun FastTogglesPanel(
    currentDomain: String?,
    onApplyReload: () -> Unit,
) {
    val container = rememberContainer()
    val settings by container.settings.state.collectAsState()

    Column(Modifier.padding(bottom = BBSpace.lg)) {
        EngineToggleCard(
            title = stringResource(R.string.setting_title_javascript),
            checked = settings.javaScriptEnabled,
            onToggle = { v -> container.settings.update { it.copy(javaScriptEnabled = v) } },
            kind = WhitelistKind.JAVASCRIPT,
            currentDomain = currentDomain,
        )
        EngineToggleCard(
            title = stringResource(R.string.setting_title_adblock),
            checked = settings.adBlockEnabled,
            onToggle = { v -> container.settings.update { it.copy(adBlockEnabled = v) } },
            kind = WhitelistKind.AD_BLOCK,
            currentDomain = currentDomain,
        )
        EngineToggleCard(
            title = stringResource(R.string.setting_title_cookie),
            checked = settings.cookiesEnabled,
            onToggle = { v -> container.settings.update { it.copy(cookiesEnabled = v) } },
            kind = WhitelistKind.COOKIE,
            currentDomain = currentDomain,
        )
        EngineToggleCard(
            title = stringResource(R.string.setting_title_remote),
            checked = settings.remoteContentEnabled,
            onToggle = { v -> container.settings.update { it.copy(remoteContentEnabled = v) } },
            kind = WhitelistKind.REMOTE_CONTENT,
            currentDomain = currentDomain,
        )

        SwitchRow(
            title = stringResource(R.string.setting_title_history),
            subtitle = stringResource(R.string.setting_summary_history),
            checked = settings.saveHistory,
            onCheckedChange = { v -> container.settings.update { it.copy(saveHistory = v) } },
        )
        SwitchRow(
            title = stringResource(R.string.setting_title_images),
            checked = settings.images,
            onCheckedChange = { v -> container.settings.update { it.copy(images = v) } },
        )
        SwitchRow(
            title = stringResource(R.string.setting_title_inverted),
            subtitle = stringResource(R.string.setting_summary_inverted),
            checked = settings.invertWebContent,
            onCheckedChange = { v -> container.settings.update { it.copy(invertWebContent = v) } },
        )

        // Port of ib_reload: recreate/reload after preference changes.
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onApplyReload)
                .padding(horizontal = BBSpace.lg, vertical = BBSpace.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.bb_apply_reload),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/**
 * One engine card: master switch plus a domain chip that toggles membership in that engine's
 * whitelist table (green check / red cross — semantic parity with legacy dialog_toggle icons).
 */
@Composable
private fun EngineToggleCard(
    title: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    kind: WhitelistKind,
    currentDomain: String?,
) {
    val container = rememberContainer()

    val scope = androidx.compose.runtime.rememberCoroutineScope()

    Column(Modifier.fillMaxWidth()) {
        SwitchRow(title = title, checked = checked, onCheckedChange = onToggle)
        if (!currentDomain.isNullOrBlank()) {
            var whitelisted by remember(currentDomain, kind) {
                mutableStateOf(container.whitelists.isWhitelisted(kind, currentDomain))
            }
            Row(
                Modifier
                    .clickable {
                        val next = !whitelisted
                        whitelisted = next
                        scope.launch(Dispatchers.IO) {
                            val success = runCatching {
                                if (next) {
                                    container.whitelists.addDomain(kind, currentDomain)
                                } else {
                                    container.whitelists.removeDomain(kind, currentDomain)
                                }
                            }.isSuccess
                            if (!success) {
                                withContext(Dispatchers.Main) {
                                    whitelisted = !next
                                }
                            }
                        }
                    }
                    .padding(start = BBSpace.xl, bottom = BBSpace.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (whitelisted) Icons.Filled.CheckCircle else Icons.Filled.Clear,
                    contentDescription = stringResource(
                        if (whitelisted) R.string.bb_whitelisted_for_domain
                        else R.string.bb_not_whitelisted_for_domain,
                    ),
                    tint = if (whitelisted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.padding(BBSpace.sm))
                Text(
                    currentDomain,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
