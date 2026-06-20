package com.casedhara.ui.screens.mapper

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.navOptions
import com.casedhara.ui.components.AshokaChakraAnimation
import com.casedhara.ui.components.ErrorCard
import com.casedhara.ui.components.MappingResultCard
import com.casedhara.ui.components.SplashThemeBackground
import com.casedhara.ui.components.design.GlassSurface
import com.casedhara.ui.navigation.NavRoutes
import com.casedhara.util.NetworkResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapperScreen(
    navController: NavController,
    viewModel: MapperViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val lawFilter by viewModel.lawFilter.collectAsStateWithLifecycle()
    LaunchedEffect(state) {
        if (!viewModel.shouldAutoOpenDetail()) return@LaunchedEffect
        val s = state
        if (s is NetworkResult.Success && s.data.results.isNotEmpty()) {
            viewModel.markAutoOpenDetailConsumed()
            val mapperEntryId = navController.currentBackStackEntry?.id
            navController.navigate(
                NavRoutes.sectionDetail(s.data.results.first().id),
                navOptions {
                    if (mapperEntryId != null) {
                        // Remove Mapper from the stack so Back returns to Case Summarizer.
                        popUpTo(mapperEntryId) { inclusive = true }
                    }
                },
            )
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Section Mapper") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        SplashThemeBackground {
            AshokaChakraAnimation(
                modifier = Modifier
                    .align(Alignment.Center)
                    .alpha(0.15f),
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
            ) {
                GlassSurface(modifier = Modifier.fillMaxWidth(), glowColor = MaterialTheme.colorScheme.primary) {
                    Column(Modifier.padding(14.dp)) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = viewModel::onQueryChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Section, keyword, or legal text") },
                            minLines = 2,
                            maxLines = 8,
                        )

                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    }
                }
                Spacer(Modifier.height(16.dp))

                when (val s = state) {
                    is NetworkResult.Loading -> CircularProgressIndicator()
                    is NetworkResult.Error -> ErrorCard(s.message)
                    is NetworkResult.Success -> {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(s.data.results, key = { it.id }) { record ->
                                MappingResultCard(
                                    record = record,
                                    lawFilter = lawFilter,   // ← pass filter so card shows only the right sections
                                    onClick = {
                                        navController.navigate(NavRoutes.sectionDetail(record.id))
                                    },
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
}
