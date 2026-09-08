package com.saigopal.browser.ui.design.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saigopal.browser.ui.design.components.BBConfirmSheet
import com.saigopal.browser.ui.design.components.BBIconTile
import com.saigopal.browser.ui.design.components.BBListItem
import com.saigopal.browser.ui.design.components.BBOmniboxPill
import com.saigopal.browser.ui.design.components.BBSegmentedRow
import com.saigopal.browser.ui.design.components.BBTextFieldPill
import com.saigopal.browser.ui.design.components.ColorDotPicker
import com.saigopal.browser.ui.design.components.EmptyState
import com.saigopal.browser.ui.design.components.SectionHeader
import com.saigopal.browser.ui.design.components.SwitchRow
import com.saigopal.browser.ui.design.theme.BBThemeMode
import com.saigopal.browser.ui.design.theme.BBTheme
import com.saigopal.browser.ui.design.theme.BBSpace

/*
 * Debug-only gallery: every design-system component in all four themes.
 * Launch: adb shell am start -n com.saigopal.browser/.ui.design.gallery.DesignGalleryActivity
 */

@Composable
fun ComponentGalleryScreen() {
    var themeMode by remember { mutableStateOf(BBThemeMode.DARK) }
    var dynamic by remember { mutableStateOf(false) }

    BBTheme(mode = themeMode, dynamicColor = dynamic) {
        GalleryContent(
            mode = themeMode,
            dynamic = dynamic,
            onMode = { themeMode = it },
            onDynamic = { dynamic = it },
        )
    }
}

@Composable
private fun GalleryContent(
    mode: BBThemeMode,
    dynamic: Boolean,
    onMode: (BBThemeMode) -> Unit,
    onDynamic: (Boolean) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = BBSpace.xxl),
    ) {
        // ---- masthead -----------------------------------------------------
        Column(Modifier.padding(horizontal = BBSpace.md, vertical = BBSpace.lg)) {
            Text("BlackBrowser", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Obsidian & Aurora — component gallery",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionHeader("Themes")
        BBSegmentedRow(
            options = listOf("Sys", "Light", "Dark", "Amoled"),
            selectedIndex = when (mode) {
                BBThemeMode.SYSTEM -> 0; BBThemeMode.LIGHT -> 1; BBThemeMode.DARK -> 2; BBThemeMode.AMOLED -> 3
            },
            onSelect = { onMode(BBThemeMode.entries[it]) },
            modifier = Modifier.padding(horizontal = BBSpace.md),
        )
        SwitchRow(
            title = "Material You dynamic colour",
            subtitle = "Android 12+ wallpaper palette",
            checked = dynamic,
            onCheckedChange = onDynamic,
        )

        // ---- signature element --------------------------------------------
        SectionHeader("Signature · aurora edge")
        var progress by remember { mutableIntStateOf(100) }
        Row(
            Modifier.padding(horizontal = BBSpace.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BBSpace.sm),
        ) {
            Button(onClick = { progress = listOf(0, 35, 70, 100).random() }) {
                Text(if (progress in 1..99) "Loading ${progress}%" else "Simulate load")
            }
        }
        Spacer(Modifier.height(BBSpace.sm))
        BBOmniboxPill(
            text = "Wikipedia — The Free Encyclopedia",
            onClick = { },
            leadingIcon = Icons.Filled.Lock,
            progress = progress.takeIf { it in 0..99 },
            insecure = false,
            modifier = Modifier.padding(horizontal = BBSpace.md),
        )
        Spacer(Modifier.height(BBSpace.sm))
        BBOmniboxPill(
            text = "http://not-secure-site.example",
            onClick = { },
            leadingIcon = Icons.Filled.Warning,
            insecure = true,
            contentDescription = "Insecure site",
            modifier = Modifier.padding(horizontal = BBSpace.md),
        )

        // ---- inputs --------------------------------------------------------
        SectionHeader("Inputs")
        var query by remember { mutableStateOf("") }
        BBTextFieldPill(
            value = query,
            onValueChange = { query = it },
            hint = "Search or type address",
            leadingIcon = Icons.Filled.Search,
            trailing = {
                Icon(Icons.Filled.Add, contentDescription = null)
            },
            modifier = Modifier.padding(horizontal = BBSpace.md),
        )
        val dotCode = remember { mutableIntStateOf(6) }
        ColorDotPicker(
            selectedCode = dotCode.intValue,
            onSelect = { dotCode.intValue = it },
        )

        // ---- tiles ---------------------------------------------------------
        SectionHeader("Tiles — offline letter fallback + favicon")
        Row(Modifier.padding(horizontal = BBSpace.md), horizontalArrangement = Arrangement.spacedBy(BBSpace.xs)) {
            BBIconTile("Google", "https://www.google.com", { })
            BBIconTile("Zxvvq", null, { })   // nonsense label → letter tile always
            BBIconTile("Reddit", "https://reddit.com", { })
        }

        // ---- lists ----------------------------------------------------------
        SectionHeader("Lists & controls")
        SwitchRow(title = "Ad blocker", subtitle = "Blocks trackers and ads", checked = true, onCheckedChange = {})
        SwitchRow(title = "Save history", checked = false, onCheckedChange = {})
        BBListItem(
            title = "BlackBrowser website",
            subtitle = "blackbrowser-androidapp.web.app",
            leading = { Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            trailing = { Text("12 Apr") },
            onClick = { },
        )
        BBListItem(
            title = "Settings",
            leading = { Icon(Icons.Filled.Settings, contentDescription = null) },
            onClick = { },
        )

        // ---- states ----------------------------------------------------------
        SectionHeader("States")
        EmptyState(
            icon = Icons.Filled.Delete,
            title = "No bookmarks yet",
            hint = "Open the menu on any page and choose Add bookmark.",
        )
        var confirmShown by remember { mutableStateOf(false) }
        if (confirmShown) {
            BBConfirmSheet(
                title = "Clear history?",
                body = "All visited sites will be removed. This cannot be undone.",
                confirmLabel = "Clear",
                destructive = true,
                onConfirm = { },
                onDismiss = { confirmShown = false },
            )
        }
        Row(Modifier.padding(horizontal = BBSpace.md)) {
            Button(onClick = { confirmShown = true }) { Text("Show confirm sheet") }
        }
    }
}
