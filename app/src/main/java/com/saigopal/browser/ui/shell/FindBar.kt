package com.saigopal.browser.ui.shell

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.saigopal.browser.R
import com.saigopal.browser.ui.design.components.BBTextFieldPill
import com.saigopal.browser.ui.design.theme.BBSpace

/*
 * Find-in-page bar (P05 §5.5) — live query, match count, prev/next/cancel.
 * Engine wiring (findAllAsync / findNext / setFindListener) lives in BrowserScreen; this is UI only.
 */

@Composable
fun FindBar(
    query: String,
    onQueryChange: (String) -> Unit,
    matchCount: Int,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = BBSpace.sm, vertical = BBSpace.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BBTextFieldPill(
                value = query,
                onValueChange = onQueryChange,
                hint = stringResource(R.string.menu_other_searchSite),
                modifier = Modifier.weight(1f),
                autoFocus = true,
            )
            Spacer(Modifier.width(BBSpace.xs))
            if (query.isNotEmpty()) {
                Text(
                    text = "$matchCount",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(BBSpace.xxs))
            }
            IconButton(onClick = onPrevious) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null)
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null)
            }
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(android.R.string.cancel),
                )
            }
        }
    }
}
