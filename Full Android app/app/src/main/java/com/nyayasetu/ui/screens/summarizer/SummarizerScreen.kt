package com.nyayasetu.ui.screens.summarizer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.nyayasetu.ui.components.ErrorCard
import com.nyayasetu.ui.components.SummaryView
import com.nyayasetu.util.NetworkResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummarizerScreen(
    navController: NavController,
    viewModel: SummarizerViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pastedText by viewModel.pastedText.collectAsStateWithLifecycle()
    val fileName by viewModel.selectedFileName.collectAsStateWithLifecycle()

    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        uri?.let { viewModel.summarizePdf(context, it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Judgment summarizer") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Button(
                onClick = { pdfPicker.launch("application/pdf") },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Upload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Upload PDF")
                }
            }
            fileName?.let {
                Text(
                    text = "Selected: $it",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text("Or paste judgment text:", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                value = pastedText,
                onValueChange = viewModel::onPastedTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                placeholder = { Text("Minimum 100 characters…") },
            )
            Button(
                onClick = { viewModel.summarizeText() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Text("Summarize text")
            }
            Spacer(Modifier.height(16.dp))

            when (val s = state) {
                is NetworkResult.Loading -> CircularProgressIndicator()
                is NetworkResult.Error -> ErrorCard(s.message)
                is NetworkResult.Success -> SummaryView(
                    summary = s.data,
                    onSectionClick = { sec ->
                        val queryStr = if (sec.ipcSection.isNotEmpty()) "IPC ${sec.ipcSection}" else "BNS ${sec.bnsSection}"
                        val encoded = android.net.Uri.encode(queryStr)
                        navController.navigate("${com.nyayasetu.ui.navigation.NavRoutes.MAPPER}?query=$encoded")
                    }
                )
                is NetworkResult.Idle -> Text(
                    text = "Upload a PDF or paste text (100+ chars). Summaries may take up to ~60s.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
