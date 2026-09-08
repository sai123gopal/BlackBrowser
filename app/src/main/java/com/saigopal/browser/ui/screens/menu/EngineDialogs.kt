package com.saigopal.browser.ui.screens.menu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.saigopal.browser.R
import com.saigopal.browser.ui.design.components.BBTextFieldPill
import com.saigopal.browser.ui.design.theme.BBSpace

/*
 * Engine dialog kit (§6.5): SSL error, HTTP auth, form resubmission, download confirm, save-as.
 * Copy and decision semantics are ported verbatim from SWebViewClient / HelperUnit; the shell
 * wires the callbacks to the real WebView handlers.
 */

/** Pure filename validation extracted from HelperUnit.save_as — unit-tested. */
object SaveAsValidator {

    /**
     * Legacy rule: title non-empty AND extension non-empty AND extension starts with "."
     * (HelperUnit.save_as line 187). Anything else is rejected with toast_input_empty.
     */
    fun isValid(title: String, extension: String): Boolean =
        title.isNotEmpty() && extension.isNotEmpty() && extension.startsWith(".")

    /** "photo.jpg" → ("photo", ".jpg"); no dot → (whole, ""). */
    fun splitFileName(fileName: String): Pair<String, String> {
        val dot = fileName.lastIndexOf('.')
        return if (dot <= 0) fileName to "" else fileName.substring(0, dot) to fileName.substring(dot)
    }

    /** Legacy prefill rule: suggested extension only reused when ≤ 8 chars incl. the dot. */
    fun suggestedExtension(fileName: String): String? {
        val (_, ext) = splitFileName(fileName)
        return ext.takeIf { it.isNotEmpty() && it.length <= 8 }
    }
}

/** 6-case reason builder ported verbatim from SWebViewClient.onReceivedSslError. */
fun sslReasonMessage(primaryErrorKind: String?): String = when (primaryErrorKind) {
    "SSL_UNTRUSTED" -> "\"Certificate authority is not trusted.\""
    "SSL_EXPIRED" -> "\"Certificate has expired.\""
    "SSL_IDMISMATCH" -> "\"Certificate Hostname mismatch.\""
    "SSL_NOTYETVALID" -> "\"Certificate is not yet valid.\""
    "SSL_DATE_INVALID" -> "\"Certificate date is invalid.\""
    "SSL_INVALID" -> "\"Certificate is invalid.\""
    else -> "\"SSL Certificate error.\""
}

@Composable
fun SslErrorDialog(
    host: String,
    primaryError: String,
    onProceed: () -> Unit,
    onBack: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onBack, // legacy parity: dismissing cancels the handler
        title = { Text(host) },
        text = { Text(sslReasonMessage(primaryError) + " - " + stringResource(R.string.dialog_content_ssl_error)) },
        confirmButton = {
            TextButton(onClick = onProceed) { Text(stringResource(R.string.app_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onBack) { Text(stringResource(R.string.bb_dialog_back)) }
        },
    )
}

@Composable
fun HttpAuthDialog(
    host: String,
    realm: String?,
    onCancel: () -> Unit,
    onSubmit: (user: String, pass: String) -> Unit,
) {
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(host + if (realm.isNullOrBlank()) "" else " ($realm)") },
        text = {
            Column {
                BBTextFieldPill(
                    value = user,
                    onValueChange = { user = it },
                    hint = stringResource(R.string.dialog_sign_in_username),
                )
                BBTextFieldPill(
                    value = pass,
                    onValueChange = { pass = it },
                    hint = stringResource(R.string.dialog_sign_in_password),
                    modifier = Modifier.padding(top = BBSpace.sm),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(user.trim(), pass) }) {
                Text(stringResource(R.string.app_ok))
            }
        },
        dismissButton = {
            // Legacy semantics: cancel == handler.cancel().
            TextButton(onClick = onCancel) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}

@Composable
fun ResubmitDialog(onResend: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.dialog_content_resubmission)) },
        confirmButton = {
            TextButton(onClick = onResend) { Text(stringResource(R.string.bb_dialog_resend)) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}

@Composable
fun DownloadConfirmDialog(
    filename: String,
    sizeText: String?,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDecline,
        title = { Text(stringResource(R.string.dialog_title_download)) },
        text = {
            Text(if (sizeText.isNullOrBlank()) filename else "$filename ($sizeText)")
        },
        confirmButton = {
            TextButton(onClick = onAccept) { Text(stringResource(R.string.app_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDecline) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}

/**
 * Port of HelperUnit.save_as dialog: name + extension fields with identical validation.
 * Extension prefills from the suggested name only when ≤ 8 chars (legacy prefill rule).
 */
@Composable
fun SaveAsDialog(
    suggestedName: String,
    onSave: (name: String) -> Unit,
    onCancel: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val (initialTitle, initialExt) = SaveAsValidator.splitFileName(suggestedName)
    var title by remember { mutableStateOf(initialTitle) }
    var extension by remember { mutableStateOf(SaveAsValidator.suggestedExtension(suggestedName) ?: "") }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.menu_edit)) },
        text = {
            Column {
                BBTextFieldPill(
                    value = title,
                    onValueChange = { title = it },
                    hint = stringResource(R.string.dialog_title_hint),
                )
                BBTextFieldPill(
                    value = extension,
                    onValueChange = { extension = it },
                    hint = stringResource(R.string.dialog_extension_hint),
                    modifier = Modifier.padding(top = BBSpace.sm),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (SaveAsValidator.isValid(title.trim(), extension.trim())) {
                    onSave(title.trim() + extension.trim())
                } else {
                    android.widget.Toast.makeText(
                        context,
                        context.getString(R.string.toast_input_empty),
                        android.widget.Toast.LENGTH_SHORT,
                    ).show()
                }
            }) { Text(stringResource(R.string.app_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}
