package com.casedhara.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.casedhara.domain.model.SectionReference
import com.casedhara.ui.components.design.GlassSurface
import com.casedhara.ui.theme.SaffronBright

@Composable
fun ExpandableSectionsInvoked(
    sections: List<SectionReference>,
    onSectionClick: (SectionReference) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (sections.isEmpty()) return

    var expanded by remember { mutableStateOf(false) }
    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        glowColor = SaffronBright,
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = "Sections Invoked" + if (expanded) "" else " (tap to expand)",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.clickable { expanded = !expanded },
            )
            if (expanded) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp),
                ) {
                    items(sections, key = { "${it.ipcSection}|${it.bnsSection}|${it.rawRef}" }) { section ->
                        SectionReferenceChip(
                            section = section,
                            onClick = { onSectionClick(section) },
                        )
                    }
                }
            }
        }
    }
}
