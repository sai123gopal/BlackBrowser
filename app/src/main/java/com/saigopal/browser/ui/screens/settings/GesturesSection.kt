package com.saigopal.browser.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.saigopal.browser.R
import com.saigopal.browser.data.repo.BBSettings
import com.saigopal.browser.domain.GestureAction
import com.saigopal.browser.domain.GestureArea
import com.saigopal.browser.domain.GestureDirection
import com.saigopal.browser.ui.design.components.SectionHeader
import com.saigopal.browser.ui.design.components.SwitchRow
import com.saigopal.browser.ui.di.rememberContainer

/*
 * Gestures section (legacy preference_gesture.xml). The stored values are the legacy codes
 * "01".."10" — they ride in GestureAction, never renumbered. Each direction row opens the same
 * 10-action picker the old ListPreference offered.
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
        SwitchRow(
            title = stringResource(R.string.setting_gestures_use_title),
            subtitle = stringResource(R.string.setting_gestures_use_summary),
            checked = settings.gesturesEnabled,
            onCheckedChange = { v -> update { s -> s.copy(gesturesEnabled = v) } },
        )

        SectionHeader(stringResource(R.string.setting_title_toolbar))
        DirectionRow(
            settings = settings,
            area = GestureArea.TOOLBAR,
            direction = GestureDirection.UP,
            label = stringResource(R.string.setting_gesture_up),
            onPick = { picking = it },
        )
        DirectionRow(
            settings = settings,
            area = GestureArea.TOOLBAR,
            direction = GestureDirection.DOWN,
            label = stringResource(R.string.setting_gesture_down),
            onPick = { picking = it },
        )
        DirectionRow(
            settings = settings,
            area = GestureArea.TOOLBAR,
            direction = GestureDirection.LEFT,
            label = stringResource(R.string.setting_gesture_left),
            onPick = { picking = it },
        )
        DirectionRow(
            settings = settings,
            area = GestureArea.TOOLBAR,
            direction = GestureDirection.RIGHT,
            label = stringResource(R.string.setting_gesture_right),
            onPick = { picking = it },
        )

        SectionHeader(stringResource(R.string.setting_title_nav))
        DirectionRow(
            settings = settings,
            area = GestureArea.NAV_BUTTON,
            direction = GestureDirection.UP,
            label = stringResource(R.string.setting_gesture_up),
            onPick = { picking = it },
        )
        DirectionRow(
            settings = settings,
            area = GestureArea.NAV_BUTTON,
            direction = GestureDirection.DOWN,
            label = stringResource(R.string.setting_gesture_down),
            onPick = { picking = it },
        )
        DirectionRow(
            settings = settings,
            area = GestureArea.NAV_BUTTON,
            direction = GestureDirection.LEFT,
            label = stringResource(R.string.setting_gesture_left),
            onPick = { picking = it },
        )
        DirectionRow(
            settings = settings,
            area = GestureArea.NAV_BUTTON,
            direction = GestureDirection.RIGHT,
            label = stringResource(R.string.setting_gesture_right),
            onPick = { picking = it },
        )
    }

    picking?.let { pick ->
        // gestureActionLabel is @Composable: resolve labels here in composition (inline map),
        // pass plain strings, and map the chosen row back to its enum by index.
        val actionNames = GestureAction.entries.map { gestureActionLabel(it) }
        SettingsChoiceDialog(
            title = pick.direction.name.lowercase().replaceFirstChar { it.uppercase() },
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
    settings: BBSettings,
    area: GestureArea,
    direction: GestureDirection,
    label: String,
    onPick: (GesturePick) -> Unit,
) {
    val current = currentAction(settings, area, direction)
    SettingsLinkRow(
        title = label,
        subtitle = gestureActionLabel(current),
        icon = null,
    ) {
        onPick(GesturePick(area, direction, current))
    }
}

private fun currentAction(s: BBSettings, area: GestureArea, direction: GestureDirection): GestureAction =
    when (area to direction) {
        GestureArea.TOOLBAR to GestureDirection.UP -> s.gestureToolbarUp
        GestureArea.TOOLBAR to GestureDirection.DOWN -> s.gestureToolbarDown
        GestureArea.TOOLBAR to GestureDirection.LEFT -> s.gestureToolbarLeft
        GestureArea.TOOLBAR to GestureDirection.RIGHT -> s.gestureToolbarRight
        GestureArea.NAV_BUTTON to GestureDirection.UP -> s.gestureNavUp
        GestureArea.NAV_BUTTON to GestureDirection.DOWN -> s.gestureNavDown
        GestureArea.NAV_BUTTON to GestureDirection.LEFT -> s.gestureNavLeft
        GestureArea.NAV_BUTTON to GestureDirection.RIGHT -> s.gestureNavRight
        else -> GestureAction.NONE
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
