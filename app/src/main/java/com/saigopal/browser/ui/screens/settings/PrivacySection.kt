package com.saigopal.browser.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import com.saigopal.browser.R
import com.saigopal.browser.data.repo.WhitelistKind
import com.saigopal.browser.ui.design.components.SectionHeader
import com.saigopal.browser.ui.design.components.SwitchRow
import com.saigopal.browser.ui.di.rememberContainer

/*
 * Privacy & security section (legacy preference_start.xml / "Start Control").
 * Each engine group pairs its master switch with an entry into the unified whitelist screen.
 */
@Composable
fun PrivacySection(onBack: () -> Unit, onOpenRoute: (String) -> Unit) {
    val container = rememberContainer()
    val settings by container.settings.state.collectAsState()

    fun update(transform: (com.saigopal.browser.data.repo.BBSettings) -> com.saigopal.browser.data.repo.BBSettings) {
        container.settings.update(transform)
    }

    SettingsScaffold(title = stringResource(R.string.setting_title_start_control), onBack = onBack) {
        SwitchRow(
            title = stringResource(R.string.setting_title_images),
            subtitle = stringResource(R.string.setting_summary_images),
            checked = settings.images,
            onCheckedChange = { v -> update { s -> s.copy(images = v) } },
        )
        SwitchRow(
            title = stringResource(R.string.setting_title_save_data),
            subtitle = stringResource(R.string.setting_summary_save_data),
            checked = settings.saveDataHeader,
            onCheckedChange = { v -> update { s -> s.copy(saveDataHeader = v) } },
        )
        SwitchRow(
            title = stringResource(R.string.setting_title_history),
            subtitle = stringResource(R.string.setting_summary_history),
            checked = settings.saveHistory,
            onCheckedChange = { v -> update { s -> s.copy(saveHistory = v) } },
        )
        SwitchRow(
            title = stringResource(R.string.setting_title_location),
            subtitle = stringResource(R.string.setting_summary_location),
            checked = settings.locationAllowed,
            onCheckedChange = { v -> update { s -> s.copy(locationAllowed = v) } },
        )
        SettingsLinkRow(
            title = stringResource(R.string.bb_privacy_reset_permissions),
            subtitle = stringResource(R.string.bb_privacy_reset_permissions_summary),
            icon = null,
        ) {
            // P09 (9.1): forget the session cache AND the WebView-side geolocation state.
            com.saigopal.browser.ui.shell.WebsitePermissionStore.reset()
            android.webkit.GeolocationPermissions.getInstance().clearAll()
        }

        SectionHeader(stringResource(R.string.setting_title_adblock))
        SwitchRow(
            title = stringResource(R.string.setting_title_adblock),
            subtitle = stringResource(R.string.setting_summary_adblock),
            checked = settings.adBlockEnabled,
            onCheckedChange = { v -> update { s -> s.copy(adBlockEnabled = v) } },
        )
        WhitelistGroupRow(stringResource(R.string.setting_title_whitelist)) {
            onOpenRoute(SettingsRoute.WHITELIST_ADBLOCK.id)
        }

        SectionHeader(stringResource(R.string.setting_title_javascript))
        SwitchRow(
            title = stringResource(R.string.setting_title_javascript),
            subtitle = stringResource(R.string.setting_summary_javascript),
            checked = settings.javaScriptEnabled,
            onCheckedChange = { v -> update { s -> s.copy(javaScriptEnabled = v) } },
        )
        WhitelistGroupRow(stringResource(R.string.setting_title_whitelistJS)) {
            onOpenRoute(SettingsRoute.WHITELIST_JAVASCRIPT.id)
        }

        SectionHeader(stringResource(R.string.setting_title_cookie))
        SwitchRow(
            title = stringResource(R.string.setting_title_cookie),
            subtitle = stringResource(R.string.setting_summary_cookie),
            checked = settings.cookiesEnabled,
            onCheckedChange = { v -> update { s -> s.copy(cookiesEnabled = v) } },
        )
        WhitelistGroupRow(stringResource(R.string.setting_title_whitelistCookie)) {
            onOpenRoute(SettingsRoute.WHITELIST_COOKIE.id)
        }

        SectionHeader(stringResource(R.string.setting_title_remote))
        SwitchRow(
            title = stringResource(R.string.setting_title_remote),
            subtitle = stringResource(R.string.setting_summary_remote),
            checked = settings.remoteContentEnabled,
            onCheckedChange = { v -> update { s -> s.copy(remoteContentEnabled = v) } },
        )
        WhitelistGroupRow(stringResource(R.string.setting_title_whitelistRemote)) {
            onOpenRoute(SettingsRoute.WHITELIST_REMOTE.id)
        }
    }
}

@Composable
private fun WhitelistGroupRow(title: String, onClick: () -> Unit) {
    SettingsLinkRow(title = title, subtitle = null, icon = null, onClick = onClick)
}

/** Route-id → kind for the four whitelist screens; kept beside the privacy UI that links to them. */
internal fun whitelistKindFor(routeId: String): WhitelistKind? = when (routeId) {
    SettingsRoute.WHITELIST_ADBLOCK.id -> WhitelistKind.AD_BLOCK
    SettingsRoute.WHITELIST_JAVASCRIPT.id -> WhitelistKind.JAVASCRIPT
    SettingsRoute.WHITELIST_COOKIE.id -> WhitelistKind.COOKIE
    SettingsRoute.WHITELIST_REMOTE.id -> WhitelistKind.REMOTE_CONTENT
    else -> null
}
