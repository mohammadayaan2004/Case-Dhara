package com.casedhara.ui.screens.mapper

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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.casedhara.ui.components.AshokaChakraAnimation
import com.casedhara.ui.components.RetrievalTierChip
import com.casedhara.ui.components.SectionTextBlock
import com.casedhara.ui.components.SplashThemeBackground
import com.casedhara.ui.components.StatusBadge
import com.casedhara.ui.navigation.NavRoutes
import com.casedhara.ui.theme.BlueLink
import com.casedhara.ui.theme.GreenIndia

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionDetailScreen(
    navController: NavController,
    viewModel: SectionDetailViewModel = hiltViewModel(),
) {
    val record = viewModel.record
    val isBookmarked by viewModel.isBookmarked.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.bookmarkEvent.collect { result ->
            val msg = when (result) {
                BookmarkResult.Added -> "Section bookmarked"
                BookmarkResult.AlreadyExists -> "Already bookmarked"
            }
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Section detail") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleBookmark() }) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark
                            else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        SplashThemeBackground {
            AshokaChakraAnimation(
                modifier = Modifier
                    .align(Alignment.Center)
                    .alpha(0.15f),
            )
            if (record == null) {
                Text(
                    text = "Record not found. Search again from the mapper.",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
                return@SplashThemeBackground
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
}
