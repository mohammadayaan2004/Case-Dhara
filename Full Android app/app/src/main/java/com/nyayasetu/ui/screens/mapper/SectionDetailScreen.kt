package com.nyayasetu.ui.screens.mapper

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.nyayasetu.ui.components.RetrievalTierChip
import com.nyayasetu.ui.components.SectionTextBlock
import com.nyayasetu.ui.components.StatusBadge
import com.nyayasetu.ui.navigation.NavRoutes
import com.nyayasetu.ui.theme.BlueLink
import com.nyayasetu.ui.theme.GreenIndia

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionDetailScreen(
    navController: NavController,
    viewModel: SectionDetailViewModel = hiltViewModel(),
) {
    val record = viewModel.record

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Section detail") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (record != null) {
                        androidx.compose.material3.TextButton(
                            onClick = {
                                navController.navigate(NavRoutes.compare(record.id))
                            },
                        ) {
                            Text("Compare")
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (record == null) {
            Text(
                text = "Record not found. Search again from the mapper.",
                modifier = Modifier.padding(padding).padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            StatusBadge(status = record.status)
            Spacer(Modifier.height(8.dp))
            RetrievalTierChip(tier = record.retrievalTier)
            Spacer(Modifier.height(16.dp))
            SectionTextBlock(
                law = "IPC",
                section = record.ipcSection,
                heading = record.ipcHeading,
                description = record.ipcDescription,
                accent = BlueLink,
                previewMaxLines = null,
            )
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            SectionTextBlock(
                law = "BNS",
                section = record.bnsSection,
                heading = record.bnsHeading,
                description = record.bnsDescription,
                accent = GreenIndia,
                modifier = Modifier.fillMaxWidth(),
                previewMaxLines = null,
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { navController.navigate(NavRoutes.compare(record.id)) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Compare side by side")
            }
        }
    }
}
