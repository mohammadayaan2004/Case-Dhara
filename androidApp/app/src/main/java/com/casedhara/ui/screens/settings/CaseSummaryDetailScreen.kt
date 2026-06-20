package com.casedhara.ui.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.casedhara.data.local.entity.SavedCaseEntity
import com.casedhara.domain.model.CaseSummary
import com.casedhara.ui.components.AshokaChakraAnimation
import com.casedhara.ui.components.SectionNavEffects
import com.casedhara.ui.components.SplashThemeBackground
import com.casedhara.ui.components.SummaryView
import com.casedhara.ui.screens.mapper.SectionNavViewModel
import com.casedhara.util.CaseSummaryJson

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaseSummaryDetailScreen(
    navController: NavController,
    caseId: String,
    viewModel: SettingsViewModel = hiltViewModel(),
    sectionNavViewModel: SectionNavViewModel = hiltViewModel(),
) {
    var entity by remember { mutableStateOf<SavedCaseEntity?>(null) }
    var summary by remember { mutableStateOf<CaseSummary?>(null) }
    var loading by remember { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }
    val sectionLoading = SectionNavEffects(
        navController = navController,
        snackbarHostState = snackbarHostState,
        sectionNavViewModel = sectionNavViewModel,
    )

    LaunchedEffect(caseId) {
        loading = true
        val id = caseId.toLongOrNull()
        if (id != null) {
            entity = viewModel.getSavedCaseById(id)
            summary = entity?.summaryJson?.let { CaseSummaryJson.fromJson(it) }
        }
        loading = false
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(entity?.title ?: "Case Summary") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                when {
                    loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    summary != null -> {
                        SummaryView(
                            summary = summary!!,
                            onSectionClick = sectionNavViewModel::openSection,
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                        )
                        if (sectionLoading) {
                            CircularProgressIndicator(Modifier.align(Alignment.Center))
                        }
                    }
                    else -> Text(
                        text = "Summary not found.",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}
