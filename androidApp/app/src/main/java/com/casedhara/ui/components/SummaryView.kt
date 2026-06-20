package com.casedhara.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.casedhara.domain.model.CaseSummary
import com.casedhara.domain.model.SectionReference
import com.casedhara.domain.model.toDisplayFields

@Composable
fun SummaryView(
    summary: CaseSummary,
    onSectionClick: (SectionReference) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fields = summary.toDisplayFields()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (summary.sectionsInvoked.isNotEmpty()) {
            ExpandableSectionsInvoked(
                sections = summary.sectionsInvoked,
                onSectionClick = onSectionClick,
            )
        }

        fields.forEach { field ->
            ExpandableSection(title = field.label, body = field.value)
        }
    }
}
