package com.nyayasetu.ui.screens.mapper

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.nyayasetu.ui.components.ErrorCard
import com.nyayasetu.ui.components.MappingResultCard
import com.nyayasetu.ui.components.RetrievalTierChip
import com.nyayasetu.ui.navigation.NavRoutes
import com.nyayasetu.util.NetworkResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapperScreen(
    navController: NavController,
    viewModel: MapperViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val lawFilter by viewModel.lawFilter.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("IPC ↔ BNS Mapper") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Section, keyword, or legal text") },
                minLines = 2,
                maxLines = 8,
            )
            Spacer(Modifier.height(8.dp))
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = lawFilter == null,
                    onClick = { viewModel.onLawFilterChange(null) },
                    label = { Text("All") },
                )
                FilterChip(
                    selected = lawFilter == "ipc",
                    onClick = { viewModel.onLawFilterChange("ipc") },
                    label = { Text("IPC") },
                )
                FilterChip(
                    selected = lawFilter == "bns",
                    onClick = { viewModel.onLawFilterChange("bns") },
                    label = { Text("BNS") },
                )
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { viewModel.search() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Search")
            }
            Spacer(Modifier.height(16.dp))

            when (val s = state) {
                is NetworkResult.Loading -> CircularProgressIndicator()
                is NetworkResult.Error -> ErrorCard(s.message)
                is NetworkResult.Success -> {
                    RetrievalTierChip(tier = s.data.retrievalTier)
                    Text(
                        text = s.data.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(s.data.results, key = { it.id }) { record ->
                            MappingResultCard(
                                record = record,
                                onClick = {
                                    navController.navigate(NavRoutes.sectionDetail(record.id))
                                },
                                onLongClick = { viewModel.addBookmark(record) },
                            )
                        }
                    }
                }
                is NetworkResult.Idle -> Text(
                    text = "Enter a query and tap Search.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
