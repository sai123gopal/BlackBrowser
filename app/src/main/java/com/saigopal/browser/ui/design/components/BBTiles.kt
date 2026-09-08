package com.saigopal.browser.ui.design.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.saigopal.browser.ui.design.theme.BBSpace
import kotlin.math.absoluteValue

/*
 * BBIconTile — the favicon tile used on start page, tab cards and quick links.
 * Favicon via Coil when it loads; until then (and offline) a letter tile whose hue is derived
 * deterministically from the host so a site always wears the same colour. Looking good must
 * never depend on the network.
 */

private val tilePalette = listOf(
    Color(0xFF00685A), Color(0xFF4A635D), Color(0xFF685BA6), Color(0xFF825500),
    Color(0xFF8A5443), Color(0xFF2E5B8C), Color(0xFF5B6E1F), Color(0xFF7B4467),
)

fun letterTileColorsFor(seed: String): Pair<Color, String> {
    val host = seed
        .removePrefix("https://").removePrefix("http://")
        .substringBefore('/')
        .removePrefix("www.")
    val letter = host.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "?"
    val bg = tilePalette[host.hashCode().absoluteValue % tilePalette.size]
    return bg to letter
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BBIconTile(
    label: String,
    url: String?,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    tileSize: Dp = 56.dp,
) {
    val clickModifier = if (onLongClick != null) {
        Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
    } else {
        Modifier.clickable(onClick = onClick)
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(clickModifier)
            .padding(BBSpace.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(tileSize).clip(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            LetterTile(label, url, tileSize)
            if (!url.isNullOrEmpty()) {
                SubcomposeAsyncImage(
                    model = "https://www.google.com/s2/favicons?sz=64&domain_url=$url",
                    contentDescription = null,
                    modifier = Modifier.size(tileSize),
                    loading = { },
                    error = { },
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = BBSpace.xxs),
        )
    }
}

@Composable
fun LetterTile(label: String, url: String?, tileSize: Dp) {
    val (bg, letter) = letterTileColorsFor(if (url.isNullOrEmpty()) label else url)
    Box(
        Modifier
            .size(tileSize)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
        )
    }
}
