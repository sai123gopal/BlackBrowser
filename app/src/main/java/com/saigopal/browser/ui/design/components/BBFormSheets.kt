@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.saigopal.browser.ui.design.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.saigopal.browser.ui.design.theme.BBSpace

/*
 * Form sheets — the standard replacement for AlertDialog-based edit forms.
 * Matches the app's sheet language (OverviewActionSheet / BBConfirmSheet): rounded M3 sheet,
 * surfaceContainerLow, pill fields, primary confirm + quiet cancel. IME + nav-bar padded so the
 * keyboard never covers the fields. android.R strings are referenced fully-qualified so the
 * app R import stays unambiguous.
 */

/**
 * Generic field-editing sheet: large title, arbitrary field slots, confirm + cancel.
 *
 * @param confirmEnabled false keeps the confirm button disabled (e.g. blank required fields).
 */
@Composable
fun BBFormSheet(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    confirmLabel: String = "Save",
    cancelLabel: String = "Cancel",
    confirmEnabled: Boolean = true,
    icon: ImageVector? = null,
    fields: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = BBSpace.lg)
                .padding(bottom = BBSpace.lg),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(BBSpace.sm))
                }
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.height(BBSpace.md))
            fields()
            Spacer(Modifier.height(BBSpace.lg))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) {
                    Text(cancelLabel)
                }
                TextButton(
                    onClick = onConfirm,
                    enabled = confirmEnabled,
                ) {
                    Text(
                        confirmLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (confirmEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}
