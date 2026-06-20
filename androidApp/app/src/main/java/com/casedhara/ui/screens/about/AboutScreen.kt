package com.casedhara.ui.screens.about

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.casedhara.ui.components.AshokaChakraAnimation
import com.casedhara.ui.components.SplashThemeBackground
import com.casedhara.ui.components.design.GlassSurface
import com.casedhara.ui.theme.ChakraGlowBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    navController: NavController,
) {
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("About") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
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
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
            Text(
                text = "Case Dhara",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Map Laws • Summarize Cases • Chat with Law • Complete AI Legal Intelligence Platform",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            HorizontalDivider()

            Text(
                text = "Case Dhara is a modern legal intelligence platform created to simplify Indian legal research and learning. The application helps users explore laws, understand court judgments, and interact with legal information through an intelligent and user-friendly system.",
                style = MaterialTheme.typography.bodyMedium,
            )

            Text(
                text = "Designed for law students, judiciary aspirants, legal professionals, and general users, Case Dhara brings together legal research, AI-powered analysis, and interactive learning tools in one unified platform.",
                style = MaterialTheme.typography.bodyMedium,
            )

            HorizontalDivider()

            Text(
                text = "Core Features",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            FeatureSection(
                emoji = "⚖️",
                title = "IPC ↔ BNS Bidirectional Mapping",
                description = "Powered by sentence-transformers/all-mpnet-base-v2 and FAISS-based semantic search, the mapping system intelligently connects IPC sections with their corresponding BNS provisions. The model analyzes legal meaning, contextual similarity, and section relationships to provide fast, accurate, and direction-aware legal mapping between traditional and newly introduced criminal laws.",
            )

            FeatureSection(
                emoji = "📄",
                title = "AI Case Summarization",
                description = "Generate structured summaries of legal judgments within seconds. The summarization engine extracts important elements such as facts, legal issues, arguments, evidence, judgments, and ratio decidendi using the facebook/bart-large-cnn transformer model for concise and readable outputs.",
            )

            FeatureSection(
                emoji = "💬",
                title = "Legal Chatbot",
                description = "An interactive AI-based legal assistant that helps users ask legal questions, understand legal terminology, explore provisions, and receive contextual legal explanations through natural conversation.",
            )

            FeatureSection(
                emoji = "🧠",
                title = "Legal Quiz System",
                description = "Interactive quizzes designed for legal learning, judiciary preparation, and concept revision. Users can strengthen their understanding of legal topics through practice-based questions and instant feedback.",
            )

            Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun FeatureSection(emoji: String, title: String, description: String) {
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        glowColor = ChakraGlowBlue,
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "$emoji $title",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
