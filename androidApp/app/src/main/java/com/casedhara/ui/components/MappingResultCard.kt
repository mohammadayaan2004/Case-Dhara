package com.casedhara.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.casedhara.domain.model.MappingRecord
import com.casedhara.ui.components.design.GlassSurface
import com.casedhara.ui.theme.BlueLink
import com.casedhara.ui.theme.GreenIndia

@Composable
fun MappingResultCard(
    record: MappingRecord,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    lawFilter: String? = null,
) {
    val showIpc = lawFilter == null || lawFilter == "ipc"
    val showBns = lawFilter == null || lawFilter == "bns"

    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        glowColor = if (showIpc) BlueLink else GreenIndia,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StatusBadge(status = record.status)
                ConfidenceBar(
                    score = record.confidence,
                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                )
            }
            Spacer(Modifier.height(8.dp))

            if (showIpc) {
                SectionTextBlock(
                    law = "IPC",
                    section = record.ipcSection,
                    heading = record.ipcHeading,
                    description = record.ipcDescription,
                    accent = BlueLink,
                    previewMaxLines = 2,
                )
            }

            if (showIpc && showBns) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
            }

            if (showBns) {
                SectionTextBlock(
                    law = "BNS",
                    section = record.bnsSection,
                    heading = record.bnsHeading,
                    description = record.bnsDescription,
                    accent = GreenIndia,
                    previewMaxLines = 2,
                )
            }

            Spacer(Modifier.height(6.dp))
        }
    }
}
