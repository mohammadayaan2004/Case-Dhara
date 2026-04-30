package com.nyayasetu.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nyayasetu.domain.model.SectionReference

@Composable
fun SectionReferenceChip(
    section: SectionReference,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AssistChip(
        onClick = onClick,
        label = {
            Text(
                text = "IPC ${section.ipcSection} → BNS ${section.bnsSection}",
                style = MaterialTheme.typography.labelSmall,
            )
        },
        modifier = modifier.padding(end = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = AssistChipDefaults.assistChipColors(),
    )
}
