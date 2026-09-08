package com.saigopal.browser.ui.design.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.saigopal.browser.ui.design.theme.BBDotColor
import com.saigopal.browser.ui.design.theme.BBSpace

/** The 11 bookmark colour dots (legacy codes preserved) as a selectable row. */
@Composable
fun ColorDotPicker(
    selectedCode: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BBSpace.sm),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = BBSpace.md),
    ) {
        items(BBDotColor.entries.toList()) { dot ->
            val selected = dot.code == selectedCode
            Box(
                Modifier
                    .padding(vertical = BBSpace.xs)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(dot.color)
                    .border(
                        BorderStroke(
                            width = if (selected) 3.dp else 0.dp,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        CircleShape,
                    )
                    .clickable { onSelect(dot.code) },
                contentAlignment = Alignment.Center,
            ) { }
        }
    }
}
