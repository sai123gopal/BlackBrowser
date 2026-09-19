package com.saigopal.browser.ui.screens.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.saigopal.browser.R
import com.saigopal.browser.data.repo.BBSettings
import com.saigopal.browser.domain.GestureAction
import com.saigopal.browser.domain.GestureArea
import com.saigopal.browser.domain.GestureDirection
import com.saigopal.browser.ui.di.rememberContainer

/*
 * Gestures section: Modern Jetpack Compose screen for configuring swipe actions on the
 * Address Bar and New Tab (+) button, along with an overview of built-in touch gestures.
 */
@Composable
fun GesturesSection(onBack: () -> Unit) {
    val container = rememberContainer()
    val settings by container.settings.state.collectAsState()
    var picking by remember { mutableStateOf<GesturePick?>(null) }

    fun update(transform: (BBSettings) -> BBSettings) {
        container.settings.update(transform)
    }

    SettingsScaffold(title = stringResource(R.string.setting_gestures), onBack = onBack) {
        // Master switch
        SettingsCardGroup(title = "Controls") {
            ModernSettingsSwitchRow(
                title = stringResource(R.string.setting_gestures_use_title),
                subtitle = if (settings.gesturesEnabled) {
                    "Swipe gestures active on address bar & New Tab button"
                } else {
                    "Gestures are currently disabled"
                },
                icon = Icons.Filled.TouchApp,
                iconContainerColor = Color(0xFFF3E5F5),
                iconColor = Color(0xFF9C27B0),
                checked = settings.gesturesEnabled,
                onCheckedChange = { v -> update { s -> s.copy(gesturesEnabled = v) } },
            )
        }

        // Built-in touch shortcuts
        SettingsCardGroup(title = "Built-in Touch Shortcuts") {
            ModernSettingsRow(
                title = "Swipe Pill to Switch Tabs",
                subtitle = "Swipe horizontally along the bottom navigation bar to swiftly slide between adjacent open tabs.",
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                iconContainerColor = Color(0xFFE8F5E9),
                iconColor = Color(0xFF2E7D32),
                onClick = {},
            )
            SettingsDivider()
            ModernSettingsRow(
                title = "Auto-Collapsing Toolbar",
                subtitle = "Scroll down on any page to tuck away the navigation bar into a minimalist mini chip. Tap or scroll up to expand.",
                icon = Icons.Filled.ArrowDownward,
                iconContainerColor = Color(0xFFE3F2FD),
                iconColor = Color(0xFF1976D2),
                onClick = {},
            )
        }

        // Address Bar Gestures
        SettingsCardGroup(title = stringResource(R.string.setting_title_toolbar)) {
            DirectionRow(
                title = stringResource(R.string.setting_gesture_up),
                currentAction = settings.gestureToolbarUp,
                icon = Icons.Filled.ArrowUpward,
                iconContainerColor = Color(0xFFEDE7F6),
                iconColor = Color(0xFF673AB7),
                onClick = { picking = GesturePick(GestureArea.TOOLBAR, GestureDirection.UP, settings.gestureToolbarUp) },
            )
            SettingsDivider()
            DirectionRow(
                title = stringResource(R.string.setting_gesture_down),
                currentAction = settings.gestureToolbarDown,
                icon = Icons.Filled.ArrowDownward,
                iconContainerColor = Color(0xFFEDE7F6),
                iconColor = Color(0xFF673AB7),
                onClick = { picking = GesturePick(GestureArea.TOOLBAR, GestureDirection.DOWN, settings.gestureToolbarDown) },
            )
            SettingsDivider()
            DirectionRow(
                title = stringResource(R.string.setting_gesture_left),
                currentAction = settings.gestureToolbarLeft,
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                iconContainerColor = Color(0xFFEDE7F6),
                iconColor = Color(0xFF673AB7),
                onClick = { picking = GesturePick(GestureArea.TOOLBAR, GestureDirection.LEFT, settings.gestureToolbarLeft) },
            )
            SettingsDivider()
            DirectionRow(
                title = stringResource(R.string.setting_gesture_right),
                currentAction = settings.gestureToolbarRight,
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                iconContainerColor = Color(0xFFEDE7F6),
                iconColor = Color(0xFF673AB7),
                onClick = { picking = GesturePick(GestureArea.TOOLBAR, GestureDirection.RIGHT, settings.gestureToolbarRight) },
            )
        }

        // New Tab Button Gestures
        SettingsCardGroup(title = "New Tab Button Gestures") {
            DirectionRow(
                title = stringResource(R.string.setting_gesture_up),
                currentAction = settings.gestureNavUp,
                icon = Icons.Filled.ArrowUpward,
                iconContainerColor = Color(0xFFE0F2F1),
                iconColor = Color(0xFF00897B),
                onClick = { picking = GesturePick(GestureArea.NAV_BUTTON, GestureDirection.UP, settings.gestureNavUp) },
            )
            SettingsDivider()
            DirectionRow(
                title = stringResource(R.string.setting_gesture_down),
                currentAction = settings.gestureNavDown,
                icon = Icons.Filled.ArrowDownward,
                iconContainerColor = Color(0xFFE0F2F1),
                iconColor = Color(0xFF00897B),
                onClick = { picking = GesturePick(GestureArea.NAV_BUTTON, GestureDirection.DOWN, settings.gestureNavDown) },
            )
            SettingsDivider()
            DirectionRow(
                title = stringResource(R.string.setting_gesture_left),
                currentAction = settings.gestureNavLeft,
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                iconContainerColor = Color(0xFFE0F2F1),
                iconColor = Color(0xFF00897B),
                onClick = { picking = GesturePick(GestureArea.NAV_BUTTON, GestureDirection.LEFT, settings.gestureNavLeft) },
            )
            SettingsDivider()
            DirectionRow(
                title = stringResource(R.string.setting_gesture_right),
                currentAction = settings.gestureNavRight,
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                iconContainerColor = Color(0xFFE0F2F1),
                iconColor = Color(0xFF00897B),
                onClick = { picking = GesturePick(GestureArea.NAV_BUTTON, GestureDirection.RIGHT, settings.gestureNavRight) },
            )
        }
    }

    picking?.let { pick ->
        val actionNames = GestureAction.entries.map { gestureActionLabel(it) }
        val areaLabel = if (pick.area == GestureArea.TOOLBAR) {
            stringResource(R.string.setting_title_toolbar)
        } else {
            "New Tab Button"
        }
        val dirLabel = when (pick.direction) {
            GestureDirection.UP -> stringResource(R.string.setting_gesture_up)
            GestureDirection.DOWN -> stringResource(R.string.setting_gesture_down)
            GestureDirection.LEFT -> stringResource(R.string.setting_gesture_left)
            GestureDirection.RIGHT -> stringResource(R.string.setting_gesture_right)
        }

        SettingsChoiceDialog(
            title = "$areaLabel • $dirLabel",
            options = actionNames,
            selectedIndex = GestureAction.entries.indexOf(pick.current),
            label = { it },
            onSelect = { index ->
                val action = GestureAction.entries[index]
                update { s ->
                    when (pick.area to pick.direction) {
                        GestureArea.TOOLBAR to GestureDirection.UP -> s.copy(gestureToolbarUp = action)
                        GestureArea.TOOLBAR to GestureDirection.DOWN -> s.copy(gestureToolbarDown = action)
                        GestureArea.TOOLBAR to GestureDirection.LEFT -> s.copy(gestureToolbarLeft = action)
                        GestureArea.TOOLBAR to GestureDirection.RIGHT -> s.copy(gestureToolbarRight = action)
                        GestureArea.NAV_BUTTON to GestureDirection.UP -> s.copy(gestureNavUp = action)
                        GestureArea.NAV_BUTTON to GestureDirection.DOWN -> s.copy(gestureNavDown = action)
                        GestureArea.NAV_BUTTON to GestureDirection.LEFT -> s.copy(gestureNavLeft = action)
                        else -> s.copy(gestureNavRight = action)
                    }
                }
                picking = null
            },
            onDismiss = { picking = null },
        )
    }
}

/** Which (area, direction) pair a picker is currently editing. */
private data class GesturePick(val area: GestureArea, val direction: GestureDirection, val current: GestureAction)

@Composable
private fun DirectionRow(
    title: String,
    currentAction: GestureAction,
    icon: ImageVector,
    iconContainerColor: Color,
    iconColor: Color,
    onClick: () -> Unit,
) {
    ModernSettingsRow(
        title = title,
        trailingText = gestureActionLabel(currentAction),
        icon = icon,
        iconContainerColor = iconContainerColor,
        iconColor = iconColor,
        onClick = onClick,
    )
}

/**
 * Action labels reuse the legacy string array entries (setting_gesture_01..10), so the picker
 * reads identically to the old ListPreference in every locale.
 */
@Composable
internal fun gestureActionLabel(action: GestureAction): String =
    stringResource(legacyGestureStringId(action.code))

internal fun legacyGestureStringId(code: String): Int = when (code) {
    "01" -> R.string.setting_gesture_01
    "02" -> R.string.setting_gesture_02
    "03" -> R.string.setting_gesture_03
    "04" -> R.string.setting_gesture_04
    "05" -> R.string.setting_gesture_05
    "06" -> R.string.setting_gesture_06
    "07" -> R.string.setting_gesture_07
    "08" -> R.string.setting_gesture_08
    "09" -> R.string.setting_gesture_09
    else -> R.string.setting_gesture_10
}
