package com.nyayasetu.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun SectionTextBlock(
    law: String,
    section: String,
    heading: String,
    description: String,
    accent: Color,
    modifier: Modifier = Modifier,
    previewMaxLines: Int? = 12,
) {
    Column(modifier = modifier) {
        Text(
            text = "$law $section — $heading",
            style = MaterialTheme.typography.titleMedium,
            color = accent,
        )
        if (previewMaxLines == null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 6.dp),
            )
        } else {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 6.dp),
                maxLines = previewMaxLines,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
