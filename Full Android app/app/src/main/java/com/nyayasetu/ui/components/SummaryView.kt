package com.nyayasetu.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nyayasetu.domain.model.CaseSummary

@Composable
fun SummaryView(
    summary: CaseSummary,
    onSectionClick: (com.nyayasetu.domain.model.SectionReference) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(colors = CardDefaults.cardColors()) {
            Column(Modifier.padding(16.dp)) {
                Text(summary.caseTitle, style = MaterialTheme.typography.titleMedium)
                summary.citation?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
                Text(summary.court, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Text(summary.summaryShort, style = MaterialTheme.typography.bodyMedium)
                summary.outcome?.let {
                    Text("Outcome: $it", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        if (summary.sectionsInvoked.isNotEmpty()) {
            Text("Sections invoked", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(summary.sectionsInvoked) { sec ->
                    SectionReferenceChip(
                        section = sec,
                        onClick = { onSectionClick(sec) }
                    )
                }
            }
        }
        ExpandableSection(title = "Facts", body = summary.facts)
        ExpandableSection(title = "Issues", body = summary.issues.joinToString("\n• ", "• "))
        ExpandableSection(
            title = "Arguments — Prosecution",
            body = summary.arguments["prosecution"].orEmpty(),
        )
        ExpandableSection(
            title = "Arguments — Defense",
            body = summary.arguments["defense"].orEmpty(),
        )
        ExpandableSection(title = "Evidence", body = summary.evidence)
        ExpandableSection(title = "Held", body = summary.held)
        ExpandableSection(title = "Ratio decidendi", body = summary.ratioDecidendi)
        ExpandableSection(title = "Order", body = summary.order)
    }
}
