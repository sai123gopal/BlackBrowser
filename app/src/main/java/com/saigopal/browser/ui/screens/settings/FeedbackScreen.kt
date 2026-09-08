package com.saigopal.browser.ui.screens.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.database.FirebaseDatabase
import com.saigopal.browser.R
import com.saigopal.browser.ui.design.components.BBTextFieldPill
import com.saigopal.browser.ui.design.theme.BBSpace
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/*
 * Modern Feedback Screen:
 * - Category selector chips with icons
 * - Elevated card for feedback input with live character counter
 * - Animated response toggle with email field
 * - High-polish submit button with loading state
 * - Strict Firebase RTDB schema and validator compatibility preserved
 */

object FeedbackValidator {
    /** Legacy regex from FeedbackActivity.isEmailValid (case-insensitive). */
    fun isEmailValid(email: String): Boolean =
        Regex("^[\\w.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$", RegexOption.IGNORE_CASE).matches(email)

    fun isFeedbackValid(text: String): Boolean = text.trim().length > 15
}

private data class FeedbackCategory(val name: String, val icon: ImageVector)

private val FEEDBACK_CATEGORIES = listOf(
    FeedbackCategory("Suggestion", Icons.Filled.Star),
    FeedbackCategory("Issue", Icons.Filled.Warning),
    FeedbackCategory("Bug", Icons.Filled.Build),
    FeedbackCategory("Other", Icons.Filled.Info),
)

@Composable
fun FeedbackScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var type by remember { mutableStateOf(FEEDBACK_CATEGORIES.first().name) }
    var message by remember { mutableStateOf("") }
    var wantsResponse by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }

    SettingsScaffold(title = "Feedback", onBack = onBack) {
        Column(
            modifier = Modifier
                .padding(horizontal = BBSpace.md)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(BBSpace.md),
        ) {
            // Header Info Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(BBSpace.md)) {
                    Text(
                        text = "Share Your Thoughts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Help us improve BlackBrowser by reporting bugs or sharing your suggestions.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Category Chips
            Text(
                text = "Select Category",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(BBSpace.xs),
            ) {
                FEEDBACK_CATEGORIES.forEach { category ->
                    val selected = type == category.name
                    FilterChip(
                        selected = selected,
                        onClick = { type = category.name },
                        leadingIcon = {
                            Icon(
                                category.icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        label = {
                            Text(
                                category.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }

            // Multiline Message Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(BBSpace.md)) {
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        placeholder = { Text(context.getString(R.string.bb_feedback_hint)) },
                        minLines = 5,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(BBSpace.xs))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val valid = FeedbackValidator.isFeedbackValid(message)
                        Text(
                            text = if (valid) "✓ Minimum length reached" else "At least 16 characters required",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (valid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        )
                        Text(
                            text = "${message.trim().length} chars",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Response Request Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(BBSpace.md)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.size(BBSpace.sm))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = context.getString(R.string.bb_feedback_response),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "We will reply back to you via email",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = wantsResponse,
                            onCheckedChange = { wantsResponse = it },
                        )
                    }

                    AnimatedVisibility(
                        visible = wantsResponse,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        Column {
                            Spacer(Modifier.height(BBSpace.sm))
                            BBTextFieldPill(
                                value = email,
                                onValueChange = { email = it },
                                hint = context.getString(R.string.bb_feedback_email_hint),
                                leadingIcon = Icons.Filled.Email,
                            )
                        }
                    }
                }
            }

            // Submit Button
            Button(
                onClick = {
                    when {
                        !FeedbackValidator.isFeedbackValid(message) ->
                            Toast.makeText(context, context.getString(R.string.bb_feedback_too_short), Toast.LENGTH_SHORT).show()
                        wantsResponse && email.isBlank() ->
                            Toast.makeText(context, "Enter Email", Toast.LENGTH_SHORT).show()
                        wantsResponse && !FeedbackValidator.isEmailValid(email.trim()) ->
                            Toast.makeText(context, "Enter valid Email", Toast.LENGTH_SHORT).show()
                        else -> {
                            sending = true
                            sendFeedback(type, message.trim(), email.trim().takeIf { wantsResponse }) { ok ->
                                sending = false
                                Toast.makeText(
                                    context,
                                    context.getString(if (ok) R.string.bb_feedback_thanks else R.string.bb_feedback_error),
                                    Toast.LENGTH_SHORT,
                                ).show()
                                if (ok) {
                                    message = ""
                                    email = ""
                                    wantsResponse = false
                                }
                            }
                        }
                    }
                },
                enabled = !sending,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                if (sending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = "Submit Feedback",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

/** Writes the exact legacy node shape: Feedback/{Type}/{push}{Time, Feedback[, Email]}. */
private fun sendFeedback(type: String, feedback: String, email: String?, onDone: (Boolean) -> Unit) {
    @Suppress("SimpleDateFormat")
    val df = SimpleDateFormat("dd MM yyyy, HH:mm", Locale.US)
    val time = df.format(Calendar.getInstance().time)
    val ref = FirebaseDatabase.getInstance().getReference().child("Feedback").child(type).push()
    ref.child("Time").setValue(time)
    ref.child("Feedback").setValue(feedback)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                if (email != null) {
                    ref.child("Email").setValue(email)
                        .addOnSuccessListener { onDone(true) }
                        .addOnFailureListener { onDone(true) }
                } else {
                    onDone(true)
                }
            } else {
                onDone(false)
            }
        }
}
