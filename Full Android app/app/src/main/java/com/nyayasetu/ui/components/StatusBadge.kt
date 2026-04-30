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
import com.nyayasetu.ui.theme.RedRepeal
import com.nyayasetu.ui.theme.SaffronBright

@Composable
fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    val (label, color) = when (status) {
        "mapped" -> "Mapped" to BlueLink
        "repealed" -> "Repealed" to RedRepeal
        "new_in_bns" -> "New in BNS" to GreenIndia
        "merged" -> "Merged" to SaffronBright
        "split" -> "Split" to AmberSplit
        else -> status to Color(0xFF424242)
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = color,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
