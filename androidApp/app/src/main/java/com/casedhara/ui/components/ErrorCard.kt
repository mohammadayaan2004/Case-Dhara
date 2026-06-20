package com.casedhara.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.casedhara.ui.components.design.GlassSurface
import com.casedhara.ui.theme.DharaSpacing
import com.casedhara.ui.theme.RedRepeal

@Composable
fun ErrorCard(
    message: String,
    modifier: Modifier = Modifier,
) {
    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        glowColor = RedRepeal,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(DharaSpacing.lg),
        )
    }
}
