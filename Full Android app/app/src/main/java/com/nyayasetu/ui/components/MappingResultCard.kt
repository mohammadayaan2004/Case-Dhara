package com.nyayasetu.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nyayasetu.domain.model.MappingRecord
import com.nyayasetu.ui.theme.BlueLink
import com.nyayasetu.ui.theme.GreenIndia

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MappingResultCard(
    record: MappingRecord,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StatusBadge(status = record.status)
                ConfidenceBar(score = record.confidence)
            }
            Spacer(Modifier.height(8.dp))
            SectionTextBlock(
                law = "IPC",
                section = record.ipcSection,
                heading = record.ipcHeading,
                description = record.ipcDescription,
                accent = BlueLink,
            )
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            SectionTextBlock(
                law = "BNS",
                section = record.bnsSection,
                heading = record.bnsHeading,
                description = record.bnsDescription,
                accent = GreenIndia,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Tap for full text · Long-press to bookmark",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
