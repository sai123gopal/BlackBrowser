package com.saigopal.browser.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.saigopal.browser.R
import com.saigopal.browser.data.repo.WhitelistKind
import com.saigopal.browser.ui.design.components.BBConfirmSheet
import com.saigopal.browser.ui.design.components.BBIconButton
import com.saigopal.browser.ui.design.components.BBListItem
import com.saigopal.browser.ui.design.components.BBTextFieldPill
import com.saigopal.browser.ui.design.components.EmptyState
import com.saigopal.browser.ui.design.theme.BBSpace
import com.saigopal.browser.ui.di.rememberContainer

/*
 * One whitelist screen for the four kinds (legacy WhitelistAdBlock/Javascript/Cookie/Remote).
 * Reads and writes go through WhitelistRepository, which delegates to the legacy adapter classes.
 */
@Composable
fun WhitelistScreen(kind: WhitelistKind, onBack: () -> Unit) {
    val container = rememberContainer()
    val context = LocalContext.current
    var domains by remember { mutableStateOf<List<String>>(emptyList()) }
    var newDomain by remember { mutableStateOf("") }
    var confirmClear by remember { mutableStateOf(false) }
    var refreshTick by remember { mutableStateOf(0) }

    LaunchedEffect(kind, refreshTick) {
        domains = container.whitelists.listDomains(kind)
    }

    SettingsScaffold(title = kind.label, onBack = onBack) {
        Row(
            Modifier.padding(horizontal = BBSpace.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BBTextFieldPill(
                value = newDomain,
                onValueChange = { newDomain = it },
                hint = stringResource(R.string.whitelist_add_hint),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(BBSpace.sm))
            BBIconButton(
                icon = Icons.Filled.Add,
                contentDescription = stringResource(R.string.whitelist_add),
                onClick = {
                    val domain = newDomain.trim().lowercase()
                    if (isValidDomainEntry(domain)) {
                        container.whitelists.addDomain(kind, domain)
                        newDomain = ""
                        refreshTick++
                    } else {
                        Toast.makeText(context, context.getString(R.string.whitelist_invalid), Toast.LENGTH_SHORT).show()
                    }
                },
            )
        }

        if (domains.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Close,
                title = stringResource(R.string.whitelist_empty_title),
                hint = stringResource(R.string.whitelist_empty_hint),
            )
        } else {
            Column {
                domains.forEach { domain ->
                    BBListItem(
                        title = domain,
                        onClick = { },
                        trailing = {
                            BBIconButton(
                                icon = Icons.Filled.Close,
                                contentDescription = context.getString(R.string.whitelist_remove),
                                onClick = {
                                    container.whitelists.removeDomain(kind, domain)
                                    refreshTick++
                                },
                            )
                        },
                    )
                }
            }
        }

        Row(Modifier.padding(BBSpace.md)) {
            SettingsLinkRow(
                title = stringResource(R.string.whitelist_clear_all),
                subtitle = null,
                icon = null,
                onClick = { confirmClear = true },
            )
        }
        Row(Modifier.padding(horizontal = BBSpace.md)) {
            SettingsLinkRow(
                title = stringResource(R.string.whitelist_export),
                subtitle = null,
                icon = null,
                onClick = {
                    // SAF-based export lands with the P10 backup rework.
                    Toast.makeText(context, context.getString(R.string.bb_backup_coming_soon), Toast.LENGTH_SHORT).show()
                },
            )
        }
    }

    if (confirmClear) {
        BBConfirmSheet(
            title = stringResource(R.string.whitelist_clear_all),
            body = stringResource(R.string.whitelist_clear_confirm),
            confirmLabel = stringResource(android.R.string.ok),
            destructive = true,
            onConfirm = {
                container.whitelists.clearDomains(kind)
                refreshTick++
            },
            onDismiss = { confirmClear = false },
        )
    }
}

/**
 * Domain entry validation. Legacy accepted raw strings; we keep it permissive but reject the
 * clearly broken entries (blank, whitespace, scheme prefixes) that could never match a host.
 */
internal fun isValidDomainEntry(domain: String): Boolean {
    if (domain.isEmpty() || domain.length > 253) return false
    if (domain.any { it.isWhitespace() }) return false
    if (domain.contains("://")) return false
    return domain.all { it.isLetterOrDigit() || it == '.' || it == '-' || it == '_' }
}
