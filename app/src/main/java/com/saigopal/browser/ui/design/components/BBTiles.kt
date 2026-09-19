package com.saigopal.browser.ui.design.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.saigopal.browser.ui.design.theme.BBSpace
import kotlin.math.absoluteValue

/*
 * BBIconTile — the favicon tile used on start page, tab cards and quick links.
 * Favicon via Coil when it loads; until then (and offline) a letter tile whose hue is derived
 * deterministically from the host so a site always wears the same colour.
 * Completely resolves the bug where the letter tile and transparent favicon overlap.
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

data class BrandPreset(
    val bgColor: Color,
)

fun getBrandPreset(domain: String, label: String): BrandPreset? {
    val d = domain.lowercase()
    val l = label.lowercase()
    return when {
        d.contains("google") || l == "google" -> BrandPreset(Color(0xFFFFFFFF))
        d.contains("facebook") || l.contains("facebook") -> BrandPreset(Color(0xFF1877F2))
        d.contains("instagram") || l.contains("instagram") -> BrandPreset(Color(0xFF2B2B2B))
        d.contains("twitter") || d == "x.com" || l == "twitter" || l == "x" -> BrandPreset(Color(0xFF000000))
        d.contains("amazon") || l.contains("amazon") -> BrandPreset(Color(0xFF232F3E))
        d.contains("wikipedia") || l.contains("wikipedia") -> BrandPreset(Color(0xFFFFFFFF))
        d.contains("duckduckgo") || l.contains("duckduckgo") -> BrandPreset(Color(0xFFDE5833))
        d.contains("reddit") || l.contains("reddit") -> BrandPreset(Color(0xFFFF4500))
        d.contains("youtube") || l.contains("youtube") -> BrandPreset(Color(0xFFCC0000))
        else -> null
    }
}

/**
 * Clean favicon renderer that strictly separates image and letter fallback states.
 * Guarantees zero overlapping between letter and logo.
 */
@Composable
fun BBFavicon(
    label: String,
    url: String?,
    size: Dp = 52.dp,
    shape: Shape = CircleShape,
    modifier: Modifier = Modifier,
    backgroundColor: Color? = null,
) {
    val cleanUrl = url?.trim().orEmpty()
    val domain = remember(cleanUrl) {
        if (cleanUrl.isNotEmpty() && !cleanUrl.startsWith("about:") && !cleanUrl.startsWith("file:")) {
            cleanUrl
                .removePrefix("https://").removePrefix("http://")
                .substringBefore('/')
                .removePrefix("www.")
        } else ""
    }

    val faviconUrl = remember(domain) {
        if (domain.isNotEmpty()) {
            "https://www.google.com/s2/favicons?sz=128&domain_url=$domain"
        } else null
    }

    val brandPreset = remember(domain, label) {
        getBrandPreset(domain, label)
    }

    val defaultBg = backgroundColor
        ?: brandPreset?.bgColor
        ?: MaterialTheme.colorScheme.surfaceContainerHigh

    Surface(
        shape = shape,
        color = defaultBg,
        modifier = modifier.size(size),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            if (faviconUrl != null) {
                SubcomposeAsyncImage(
                    model = faviconUrl,
                    contentDescription = label,
                    modifier = Modifier.size(size * 0.65f),
                    loading = {
                        LetterTile(label, cleanUrl, size, shape = shape)
                    },
                    error = {
                        LetterTile(label, cleanUrl, size, shape = shape)
                    },
                )
            } else {
                LetterTile(label, cleanUrl, size, shape = shape)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BBIconTile(
    label: String,
    url: String?,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    tileSize: Dp = 52.dp,
    shape: Shape = CircleShape,
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
            .padding(vertical = 6.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BBFavicon(
            label = label,
            url = url,
            size = tileSize,
            shape = shape,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
fun LetterTile(
    label: String,
    url: String?,
    tileSize: Dp,
    shape: Shape = CircleShape,
    modifier: Modifier = Modifier,
) {
    val (bg, letter) = letterTileColorsFor(if (url.isNullOrEmpty()) label else url)
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(shape)
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter,
            style = if (tileSize <= 42.dp) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}
