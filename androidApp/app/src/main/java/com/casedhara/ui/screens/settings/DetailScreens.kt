package com.casedhara.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.casedhara.data.local.entity.QuizProgressEntity
import com.casedhara.data.local.entity.WrongAnswerEntity
import com.casedhara.ui.components.AshokaChakraAnimation
import com.casedhara.ui.components.SplashThemeBackground
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WrongAnswerDetailScreen(
    navController: NavController,
    answerId: String,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    var entity by remember { mutableStateOf<WrongAnswerEntity?>(null) }

    LaunchedEffect(answerId) {
        val id = answerId.toLongOrNull() ?: return@LaunchedEffect
        entity = viewModel.getWrongAnswerById(id)
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Wrong Answer") },
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
            val item = entity
            if (item == null) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(padding),
                )
            } else {
                val opts = listOf(item.optionA, item.optionB, item.optionC, item.optionD)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = item.topic,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                    Text(
                        text = item.question,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    opts.forEachIndexed { index, option ->
                        val isCorrect = index == item.correctIndex
                        val isUserWrong = index == item.userAnswerIndex && !isCorrect
                        val bg = when {
                            isCorrect -> Color(0xFFE8F5E9)
                            isUserWrong -> Color(0xFFFFEBEE)
                            else -> MaterialTheme.colorScheme.surface
                        }
                        val textColor = when {
                            isCorrect -> Color(0xFF2E7D32)
                            isUserWrong -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = bg),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                if (isCorrect) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF2E7D32))
                                } else if (isUserWrong) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                }
                                Text(
                                    text = "${'A' + index}. $option",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textColor,
                                )
                            }
                        }
                    }
                    Text(
                        text = "Explanation",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = item.explanation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

data class SavedQuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val userAnswer: Int,
    val explanation: String,
)

fun parseQuizQuestions(json: String): List<SavedQuizQuestion> {
    val arr = JSONArray(json)
    return (0 until arr.length()).map { i ->
        val obj = arr.getJSONObject(i)
        val optionsArr = obj.getJSONArray("options")
        val options = (0 until optionsArr.length()).map { j -> optionsArr.getString(j) }
        SavedQuizQuestion(
            question = obj.getString("question"),
            options = options,
            correctIndex = obj.getInt("correctIndex"),
            userAnswer = obj.optInt("userAnswer", -1),
            explanation = obj.optString("explanation", ""),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizProgressDetailScreen(
    navController: NavController,
    progressId: String,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    var entity by remember { mutableStateOf<QuizProgressEntity?>(null) }

    LaunchedEffect(progressId) {
        val id = progressId.toLongOrNull() ?: return@LaunchedEffect
        entity = viewModel.getQuizProgressById(id)
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(entity?.topic ?: "Quiz Details") },
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
            val item = entity
            if (item == null) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(padding),
                )
            } else {
                val questions = remember(item.questionsJson) { parseQuizQuestions(item.questionsJson) }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "Score: ${item.correctAnswers} / ${item.totalQuestions}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    questions.forEachIndexed { qIndex, q ->
                        Text(
                            text = "Q${qIndex + 1}. ${q.question}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        q.options.forEachIndexed { index, option ->
                            val isCorrect = index == q.correctIndex
                            val isUser = index == q.userAnswer
                            val isUserCorrect = isUser && isCorrect
                            val isUserWrong = isUser && !isCorrect
                            val bg = when {
                                isCorrect -> Color(0xFFE8F5E9)
                                isUserWrong -> Color(0xFFFFEBEE)
                                else -> MaterialTheme.colorScheme.surface
                            }
                            val textColor = when {
                                isCorrect || isUserCorrect -> Color(0xFF2E7D32)
                                isUserWrong -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                            Card(
                                colors = CardDefaults.cardColors(containerColor = bg),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    if (isCorrect || isUserCorrect) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF2E7D32))
                                    } else if (isUserWrong) {
                                        Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                    }
                                    Text(
                                        text = "${'A' + index}. $option",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = textColor,
                                    )
                                }
                            }
                        }
                        if (q.explanation.isNotBlank()) {
                            Text(
                                text = q.explanation,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
