package com.nyayasetu.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.nyayasetu.ui.navigation.NavRoutes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val recent by viewModel.recentQueries.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NyayaSetu") },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Legal Bridge — IPC ↔ BNS",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Choose a tool:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FeatureCard(
                title = "Section mapper",
                subtitle = "Search sections, keywords, or paste FIR text",
                icon = { Icon(Icons.Default.Search, contentDescription = null) },
                onClick = { navController.navigate(NavRoutes.MAPPER) },
            )
            FeatureCard(
                title = "Judgment summarizer",
                subtitle = "Upload PDF or paste judgment text",
                icon = { Icon(Icons.Default.Description, contentDescription = null) },
                onClick = { navController.navigate(NavRoutes.SUMMARIZER) },
            )
            FeatureCard(
                title = "Legal chat",
                subtitle = "Ask about IPC & BNS",
                icon = { Icon(Icons.Default.Chat, contentDescription = null) },
                onClick = { navController.navigate(NavRoutes.CHAT) },
            )
            FeatureCard(
                title = "Bookmarks",
                subtitle = "Saved section mappings",
                icon = { Icon(Icons.Default.Star, contentDescription = null) },
                onClick = { navController.navigate(NavRoutes.BOOKMARKS) },
            )
            Spacer(Modifier.height(8.dp))
            Text("Recent searches", style = MaterialTheme.typography.labelLarge)
            if (recent.isEmpty()) {
                Text(
                    text = "No history yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(recent, key = { it.id }) { entry ->
                        Text(
                            text = entry.query,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val encodedQuery = android.net.Uri.encode(entry.query)
                                    navController.navigate("${NavRoutes.MAPPER}?query=$encodedQuery")
                                }
                                .padding(vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeatureCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            androidx.compose.foundation.layout.Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                icon()
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
