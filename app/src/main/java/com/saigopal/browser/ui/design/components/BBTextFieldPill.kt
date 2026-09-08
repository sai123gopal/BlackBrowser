package com.saigopal.browser.ui.design.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.saigopal.browser.ui.design.theme.BBSpace
import com.saigopal.browser.ui.design.theme.PillShape

/**
 * The system's only text field shape: a surface pill with optional leading icon and trailing slot.
 * Used by the omnibox edit mode, overview search, whitelist add, feedback — everywhere text goes in.
 */
@Composable
fun BBTextFieldPill(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: String = "",
    leadingIcon: ImageVector? = null,
    trailing: @Composable () -> Unit = {},
    singleLine: Boolean = true,
    autoFocus: Boolean = false,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    onSearch: (() -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val focusRequester = remember { FocusRequester() }
    val focusModifier = if (autoFocus) Modifier.focusRequester(focusRequester) else Modifier

    LaunchedEffect(autoFocus) {
        if (autoFocus) focusRequester.requestFocus()
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = PillShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .height(52.dp)
                .padding(start = BBSpace.md, end = BBSpace.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                Icon(
                    leadingIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(Modifier.padding(horizontal = BBSpace.xs))
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty() && hint.isNotEmpty()) {
                    Text(
                        hint,
                        style = textStyle,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = singleLine,
                    textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    interactionSource = interaction,
                    visualTransformation = VisualTransformation.None,
                    keyboardOptions = KeyboardOptions(
                        imeAction = if (onSearch != null) ImeAction.Search else ImeAction.Default,
                    ),
                    keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(focusModifier),
                )
            }
            trailing()
        }
    }
}
