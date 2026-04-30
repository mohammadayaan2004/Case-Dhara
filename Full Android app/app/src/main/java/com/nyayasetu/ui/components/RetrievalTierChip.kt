package com.nyayasetu.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nyayasetu.ui.theme.AmberSplit
import com.nyayasetu.ui.theme.BlueLink
import com.nyayasetu.ui.theme.GreenIndia

@Composable
fun RetrievalTierChip(tier: Int, modifier: Modifier = Modifier) {
    val (label, color) = when (tier) {
        1 -> "Exact match" to GreenIndia
        2 -> "Keyword match" to BlueLink
        3 -> "Semantic match" to AmberSplit
        else -> "Unknown" to Color(0xFF757575)
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.12f),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}
