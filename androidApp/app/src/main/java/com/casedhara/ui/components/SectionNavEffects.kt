package com.casedhara.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.casedhara.ui.navigation.NavRoutes
import com.casedhara.ui.screens.mapper.SectionNavViewModel
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SectionNavEffects(
    navController: NavController,
    snackbarHostState: SnackbarHostState,
    sectionNavViewModel: SectionNavViewModel = hiltViewModel(),
): Boolean {
    val sectionLoading by sectionNavViewModel.loading.collectAsStateWithLifecycle()

    LaunchedEffect(sectionNavViewModel) {
        sectionNavViewModel.navigateToRecord.collectLatest { recordId ->
            navController.navigate(NavRoutes.sectionDetail(recordId))
        }
    }

    LaunchedEffect(sectionNavViewModel) {
        sectionNavViewModel.error.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    return sectionLoading
}
