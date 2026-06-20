package com.casedhara.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.casedhara.data.local.entity.QuizProgressEntity
import com.casedhara.data.local.entity.WrongAnswerEntity
import com.casedhara.ui.components.AshokaChakraAnimation
import com.casedhara.ui.components.EmptyStateBox
import com.casedhara.ui.components.SplashThemeBackground
import com.casedhara.ui.components.design.GlassSurface
import com.casedhara.ui.navigation.NavRoutes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WrongAnswersScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val items by viewModel.wrongAnswers.collectAsStateWithLifecycle()
    val fmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Saved Wrong Answers") },
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                if (items.isEmpty()) {
                    EmptyStateBox(
                        message = "No saved wrong answers yet. Incorrect quiz answers will appear here.",
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                    ) {
                        items(items, key = { it.id }) { item ->
                            WrongAnswerListCard(
                                item = item,
                                dateFmt = fmt,
                                onClick = { navController.navigate(NavRoutes.wrongAnswerDetail(item.id.toString())) },
                                onDelete = { viewModel.deleteWrongAnswer(item) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WrongAnswerListCard(
    item: WrongAnswerEntity,
    dateFmt: SimpleDateFormat,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val opts = listOf(item.optionA, item.optionB, item.optionC, item.optionD)
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        glowColor = MaterialTheme.colorScheme.error,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = item.topic,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                }
            }
            Text(
                text = item.question,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            opts.forEachIndexed { index, option ->
                Text(
                    text = "${'A' + index}. $option",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = dateFmt.format(Date(item.savedAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizProgressScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val items by viewModel.quizProgress.collectAsStateWithLifecycle()
    val fmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Quiz Progress") },
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                if (items.isEmpty()) {
                    EmptyStateBox(
                        message = "No quiz results yet. Complete a quiz to see your progress here.",
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                    ) {
                        items(items, key = { it.id }) { item ->
                            QuizProgressListCard(
                                item = item,
                                dateFmt = fmt,
                                onClick = { navController.navigate(NavRoutes.quizProgressDetail(item.id.toString())) },
                                onDelete = { viewModel.deleteQuizProgress(item) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizProgressListCard(
    item: QuizProgressEntity,
    dateFmt: SimpleDateFormat,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        glowColor = MaterialTheme.colorScheme.primary,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = item.topic,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Score: ${item.correctAnswers} / ${item.totalQuestions}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = dateFmt.format(Date(item.completedAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Delete", modifier = Modifier.size(16.dp))
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
