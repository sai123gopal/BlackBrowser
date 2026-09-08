package com.saigopal.browser.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.saigopal.browser.R
import com.saigopal.browser.data.repo.BBSettings
import com.saigopal.browser.ui.design.theme.BBDotColor
import com.saigopal.browser.ui.design.theme.BBSpace
import com.saigopal.browser.ui.di.rememberContainer

/*
 * Bookmark colour filters (legacy preference_filter.xml). The 11 codes are stored data:
 * code 11 = red … code 1 = grey (HelperUnit.addFilterItems parity). Toggling a colour hides
 * bookmarks wearing that dot from filtered views.
 */
@Composable
fun FilterSection(onBack: () -> Unit) {
    val container = rememberContainer()
    val settings by container.settings.state.collectAsState()

    fun update(transform: (BBSettings) -> BBSettings) {
        container.settings.update(transform)
    }

    SettingsScaffold(title = stringResource(R.string.setting_filter), onBack = onBack) {
        // Legacy intro copy, kept verbatim.
        Text(
            stringResource(R.string.dialogHelp_filterText),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = BBSpace.md, vertical = BBSpace.sm),
        )

        BBDotColor.entries.forEach { dot ->
            ColorFilterRow(
                dot = dot,
                checked = dot.code in settings.enabledFilterColors,
                onToggle = { v ->
                    update { s ->
                        s.copy(
                            enabledFilterColors = if (v) s.enabledFilterColors + dot.code
                            else s.enabledFilterColors - dot.code,
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun ColorFilterRow(
    dot: BBDotColor,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable { onToggle(!checked) }
            .padding(horizontal = BBSpace.md, vertical = BBSpace.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(dot.color),
        )
        Spacer(Modifier.width(BBSpace.md))
        Text(
            colorLabel(dot),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}

@Composable
private fun colorLabel(dot: BBDotColor): String = stringResource(legacyColorStringId(dot.code))

internal fun legacyColorStringId(code: Int): Int = when (code) {
    1 -> R.string.color_grey
    2 -> R.string.color_brown
    3 -> R.string.color_orange
    4 -> R.string.color_yellow
    5 -> R.string.color_lime
    6 -> R.string.color_green
    7 -> R.string.color_teal
    8 -> R.string.color_blue
    9 -> R.string.color_purple
    10 -> R.string.color_pink
    else -> R.string.color_red
}
