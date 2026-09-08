package com.saigopal.browser.ui.screens.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saigopal.browser.BuildConfig
import com.saigopal.browser.R
import com.saigopal.browser.ui.ads.AdsController
import com.saigopal.browser.ui.design.components.SectionHeader
import com.saigopal.browser.ui.design.theme.BBSpace

/*
 * Modern About & Tips Screen:
 * - App hero header card with version and badge
 * - Organized cards for Gestures & Pro Tips (Tab swipe, Collapsible bar, Fast toggles)
 * - Community links & Google UMP privacy options
 */

@Composable
fun AboutSheet(onBack: () -> Unit) {
    val context = LocalContext.current
    var consentTick by remember { mutableIntStateOf(0) }

    SettingsScaffold(title = stringResource(R.string.menu_other_info), onBack = onBack) {
        Column(
            modifier = Modifier.padding(horizontal = BBSpace.md),
            verticalArrangement = Arrangement.spacedBy(BBSpace.md),
        ) {
            // ---- Hero Branding Card -----------------------------------------------------------------
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(BBSpace.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(64.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(BBSpace.sm))
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Text(
                            text = "Version ${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                    Spacer(Modifier.height(BBSpace.xs))
                    Text(
                        text = "Fast, private, and customizable Android browser.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ---- Tips & Gestures --------------------------------------------------------------------
            SectionHeader(stringResource(R.string.dialogHelp_tipTitle))

            TipCard(
                icon = Icons.Filled.List,
                title = "Swipe Between Tabs",
                description = "Swipe horizontally left or right on the bottom bar or floating chip to cycle through open tabs effortlessly.",
            )

            TipCard(
                icon = Icons.Filled.KeyboardArrowUp,
                title = "Collapsible Bottom Bar",
                description = "Scroll down while reading to shrink the bottom bar into a compact chip for full-screen focus. Tap or scroll up to expand.",
            )

            TipCard(
                icon = Icons.Filled.Build,
                title = "Fast Privacy Toggles",
                description = "Long press the menu button to instantly toggle JavaScript, images, cookies, or ad-blocking for the current site.",
            )

            TipCard(
                icon = Icons.Filled.Star,
                title = "Bookmark Color Tagging",
                description = "Long press any bookmark to assign custom color tags and organize your saved pages into categories.",
            )

            // ---- Community & Links ------------------------------------------------------------------
            SectionHeader("Community & Web")

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    LinkRowItem(
                        icon = Icons.Filled.PlayArrow,
                        title = "YouTube Channel",
                        subtitle = "Tutorials and updates",
                        onClick = {
                            openUrl(context, "https://www.youtube.com/channel/UCvLJAVIXT_AVIj1gXq4P5xw")
                        },
                    )
                    LinkRowItem(
                        icon = Icons.Filled.Info,
                        title = "Official Website",
                        subtitle = "blackbrowser-androidapp.web.app",
                        onClick = {
                            openUrl(context, "https://blackbrowser-androidapp.web.app/")
                        },
                    )
                }
            }

            // ---- Privacy Options (GDPR / UMP) -------------------------------------------------------
            if (consentTick >= 0 && BuildConfig.ADS_ENABLED && AdsController.privacyOptionsRequired(context)) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    SettingsLinkRow(
                        title = stringResource(R.string.bb_privacy_options),
                        subtitle = stringResource(R.string.bb_privacy_options_summary),
                        icon = null,
                    ) {
                        (context as? Activity)?.let { activity ->
                            AdsController.showPrivacyOptionsForm(activity) {
                                Toast.makeText(
                                    activity,
                                    R.string.bb_privacy_options_saved,
                                    Toast.LENGTH_SHORT,
                                ).show()
                                consentTick++
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(BBSpace.lg))
        }
    }
}

@Composable
private fun TipCard(
    icon: ImageVector,
    title: String,
    description: String,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(BBSpace.md),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(36.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(Modifier.size(BBSpace.sm))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LinkRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = BBSpace.md, vertical = BBSpace.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.size(BBSpace.sm))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun openUrl(context: Context, url: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }
}
