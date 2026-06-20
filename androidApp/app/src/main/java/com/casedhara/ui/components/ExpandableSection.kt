package com.casedhara.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.casedhara.ui.components.design.DharaHaptic
import com.casedhara.ui.components.design.GlassSurface
import com.casedhara.ui.components.design.rememberDharaHaptic
import com.casedhara.ui.theme.DharaSpacing
import com.casedhara.ui.theme.GreenIndia

@Composable
fun ExpandableSection(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val haptic = rememberDharaHaptic()

    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(spring(stiffness = Spring.StiffnessLow)),
        glowColor = GreenIndia,
    ) {
        Column(Modifier.padding(DharaSpacing.md)) {
            Text(
                text = title + if (expanded) "" else " (tap to expand)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable {
                    haptic(DharaHaptic.Light)
                    expanded = !expanded
                },
            )
            if (expanded && body.isNotBlank()) {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = DharaSpacing.sm),
                )
            }
        }
    }
}
