@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.saigopal.browser.ui.design.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.saigopal.browser.ui.design.theme.BBSpace

/**
 * The one confirmation pattern (replaces every legacy `dialog_action` bottom sheet).
 * [destructive] swaps confirm to error colours; copy states the consequence plainly.
 */
@Composable
fun BBConfirmSheet(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissLabel: String = "Cancel",
    destructive: Boolean = false,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val container = MaterialTheme.colorScheme.surfaceContainerLow

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = container,
        modifier = modifier,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = BBSpace.lg)
                .padding(bottom = BBSpace.lg),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(BBSpace.sm))
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(BBSpace.lg))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(BBSpace.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(dismissLabel)
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        onDismiss()
                        onConfirm()
                    },
                    colors = if (destructive)
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        )
                    else ButtonDefaults.buttonColors(),
                ) {
                    Text(confirmLabel)
                }
            }
        }
    }
}
