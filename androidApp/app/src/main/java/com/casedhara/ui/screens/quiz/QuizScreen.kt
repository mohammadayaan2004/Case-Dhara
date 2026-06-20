package com.casedhara.ui.screens.quiz

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.casedhara.ui.components.AshokaChakraAnimation
import com.casedhara.ui.components.SplashThemeBackground
import com.casedhara.ui.screens.quiz.QuizViewModel.QuizUiState
import com.casedhara.ui.theme.GreenNeon
import androidx.compose.ui.draw.alpha
import kotlinx.coroutines.delay

private const val QUESTION_TIMER_SECONDS = 30

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    navController: NavController,
    viewModel: QuizViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Legal Quiz") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                navigationIcon = {
                    IconButton(onClick = {
                        when (uiState) {
                            is QuizUiState.Review -> viewModel.backToResult()
                            else -> navController.popBackStack()
                        }
                    }) {
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

            // Use the state's type as the key for AnimatedContent.
            // Within Setup we suppress internal transitions so slider/chip changes
            // don't trigger a fade-in/out flicker.
            AnimatedContent(
                targetState = uiState,
                transitionSpec = {
                    // Only animate when switching between *different* state types
                    val entering = targetState::class
                    val leaving  = initialState::class
                    if (entering == leaving) {
                        // Same state type → no animation (prevents Setup flicker)
                        fadeIn(initialAlpha = 1f) togetherWith fadeOut(targetAlpha = 1f)
                    } else {
                        fadeIn() togetherWith fadeOut()
                    }
                },
                label = "quizState",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) { state ->

                when (state) {
                    is QuizUiState.Setup   -> QuizSetupContent(state, viewModel)
                    is QuizUiState.Loading -> QuizLoadingContent()
                    is QuizUiState.Active  -> QuizActiveContent(state, viewModel)
                    is QuizUiState.Result  -> QuizResultContent(state, viewModel)
                    is QuizUiState.Review  -> QuizReviewContent(state, viewModel)
                    is QuizUiState.Error   -> QuizErrorContent(state.message, viewModel)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// SETUP SCREEN
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuizSetupContent(
    state: QuizUiState.Setup,
    viewModel: QuizViewModel,
) {

    val topics = listOf(
        "Indian Constitution",
        "IPC",
        "BNS 2023",
        "CrPC / BNSS",
        "CPC",
        "Evidence Act",
        "Contract Act",
        "Transfer of Property Act",
        "Specific Relief Act",
        "Limitation Act",
        "Negotiable Instruments Act",
        "Company Law",
        "Family Law",
        "Hindu Law",
        "Muslim Personal Law",
        "Cyber Law (IT Act)",
        "Labour & Employment Law",
        "Land Acquisition Law",
        "Administrative Law",
        "Torts",
        "International Law",
        "Human Rights Law",
        "Environmental Law",
        "Intellectual Property Law",
        "Arbitration & ADR",
        "Banking Law",
        "Tax Law",
        "Legal Ethics & Bar Council Rules",
    )

    val difficulties = listOf("Easy", "Medium", "Hard")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {

        Text(
            text = "Customise Your Quiz",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Number of Questions: ${state.questionCount}",
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = state.questionCount.toFloat(),
                    onValueChange = { viewModel.onQuestionCountChange(it.toInt()) },
                    valueRange = 5f..20f,
                    steps = 14,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("5", style = MaterialTheme.typography.labelSmall)
                    Text("20", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Difficulty Level", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    difficulties.forEach { diff ->
                        val selected = state.difficulty == diff
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.onDifficultyChange(diff) },
                            label = { Text(diff) },
                            leadingIcon = if (selected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                        )
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Legal Topic", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                topics.chunked(2).forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        row.forEach { topic ->
                            val selected = state.topic == topic
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.onTopicChange(topic) },
                                label = { Text(topic, style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = if (selected) {
                                    { Icon(Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }

        if (state.streak > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                ),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Whatshot,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${state.streak}-day streak! Keep it up 🎯",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }
        }

        Button(
            onClick = { viewModel.startQuiz() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Start Quiz")
        }
    }
}

// ─────────────────────────────────────────────────────────────
// LOADING SCREEN
// ─────────────────────────────────────────────────────────────

@Composable
private fun QuizLoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Generating your personalised quiz…",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// ACTIVE QUIZ SCREEN — with countdown timer
// ─────────────────────────────────────────────────────────────

@Composable
private fun QuizActiveContent(
    state: QuizUiState.Active,
    viewModel: QuizViewModel,
) {
    val q = state.currentQuestion
    val answered = state.selectedAnswer != null

    var timeLeft by remember(state.questionIndex) { mutableIntStateOf(QUESTION_TIMER_SECONDS) }

    LaunchedEffect(state.questionIndex, answered) {
        if (answered) return@LaunchedEffect
        timeLeft = QUESTION_TIMER_SECONDS
        while (timeLeft > 0 && !answered) {
            delay(1000L)
            timeLeft--
        }
        if (!answered) viewModel.onTimeUp()
    }

    val timerFraction = timeLeft.toFloat() / QUESTION_TIMER_SECONDS
    val timerColor = when {
        timerFraction > 0.5f  -> Color(0xFF2E7D32)
        timerFraction > 0.25f -> Color(0xFFFF8F00)
        else                  -> MaterialTheme.colorScheme.error
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LinearProgressIndicator(
            progress = { (state.questionIndex + 1).toFloat() / state.totalQuestions },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Question ${state.questionIndex + 1} of ${state.totalQuestions}  •  Score: ${state.score}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.Timer, contentDescription = null, tint = timerColor, modifier = Modifier.size(16.dp))
                Text(
                    text = "${timeLeft}s",
                    style = MaterialTheme.typography.labelLarge,
                    color = timerColor,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { timerFraction },
                modifier = Modifier.size(48.dp),
                color = timerColor,
                strokeWidth = 4.dp,
            )
            Text(
                text = "$timeLeft",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = timerColor,
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Text(
                text = q.question,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }

        q.options.forEachIndexed { idx, option ->
            val label = "${'A' + idx}. $option"
            val isSelected = state.selectedAnswer == idx
            val isTimedOut = state.selectedAnswer == -1
            val isCorrect  = idx == q.correctIndex

            val borderColor = when {
                !answered                    -> MaterialTheme.colorScheme.outline
                isCorrect                    -> MaterialTheme.colorScheme.tertiary
                isSelected                   -> MaterialTheme.colorScheme.error
                isTimedOut && isCorrect      -> MaterialTheme.colorScheme.tertiary
                else                         -> MaterialTheme.colorScheme.outline
            }
            val containerColor = when {
                !answered  -> MaterialTheme.colorScheme.surface
                isCorrect  -> MaterialTheme.colorScheme.tertiaryContainer
                isSelected -> MaterialTheme.colorScheme.errorContainer
                else       -> MaterialTheme.colorScheme.surface
            }

            OutlinedCard(
                onClick = { if (!answered) viewModel.onAnswerSelected(idx) },
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(2.dp, borderColor),
                colors = CardDefaults.outlinedCardColors(containerColor = containerColor),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (answered) {
                        when {
                            isCorrect  -> Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                            isSelected -> Icon(Icons.Default.Cancel, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        if (state.selectedAnswer == -1) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text("Time's up!", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (answered) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Explanation",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = q.explanation, style = MaterialTheme.typography.bodySmall)
                }
            }

            Button(
                onClick = { viewModel.nextQuestion() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (state.questionIndex + 1 < state.totalQuestions) "Next Question →" else "See Results"
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// RESULT SCREEN
// ─────────────────────────────────────────────────────────────

@Composable
private fun QuizResultContent(
    state: QuizUiState.Result,
    viewModel: QuizViewModel,
) {
    val accuracy = if (state.totalQuestions > 0) (state.score * 100) / state.totalQuestions else 0

    val emoji = when {
        accuracy >= 80 -> "🏆"
        accuracy >= 60 -> "👍"
        accuracy >= 40 -> "📚"
        else -> "💪"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = emoji, style = MaterialTheme.typography.displayLarge)
        Text(
            text = "Quiz Complete!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCard(label = "Score", value = "${state.score}/${state.totalQuestions}", modifier = Modifier.weight(1f))
            StatCard(label = "Accuracy", value = "$accuracy%", modifier = Modifier.weight(1f))
            StatCard(label = "Streak", value = "${state.streak} days 🔥", modifier = Modifier.weight(1f))
        }

        Text(
            text = when {
                accuracy >= 80 -> "Excellent! You have strong legal knowledge."
                accuracy >= 60 -> "Good work! Keep practicing to improve."
                accuracy >= 40 -> "Fair attempt. Review the topics and try again."
                else -> "Keep studying! Every attempt makes you stronger."
            },
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedButton(
            onClick = { viewModel.showReview() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.RateReview, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Review Answers")
        }

        Button(
            onClick = { viewModel.resetToSetup() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Start New Quiz")
        }
    }
}

// ─────────────────────────────────────────────────────────────
// REVIEW SCREEN
// ─────────────────────────────────────────────────────────────

@Composable
private fun QuizReviewContent(
    state: QuizUiState.Review,
    viewModel: QuizViewModel,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Answer Review",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Score: ${state.score} / ${state.questions.size}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        state.questions.forEachIndexed { qIdx, q ->
            val userAnswer = state.answeredMap[qIdx]
            val isTimedOut = userAnswer == -1
            val isCorrect  = userAnswer == q.correctIndex
            val statusColor = if (isCorrect) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
            val statusBg    = if (isCorrect) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = statusBg,
                            modifier = Modifier.padding(end = 8.dp),
                        ) {
                            Text(
                                text = "Q${qIdx + 1}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Icon(
                            if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when {
                                isTimedOut -> "Time's Up"
                                isCorrect  -> "Correct"
                                else       -> "Incorrect"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                        )
                    }

                    Text(
                        text = q.question,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )

                    q.options.forEachIndexed { idx, option ->
                        val isUserSelected  = userAnswer == idx
                        val isCorrectOption = idx == q.correctIndex
                        val optBg = when {
                            isCorrectOption -> MaterialTheme.colorScheme.tertiaryContainer
                            isUserSelected && !isCorrectOption -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(optBg, MaterialTheme.shapes.small)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${'A' + idx}. $option",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                            )
                            when {
                                isCorrectOption -> Icon(Icons.Default.CheckCircle, contentDescription = "Correct", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                                isUserSelected  -> Icon(Icons.Default.Cancel, contentDescription = "Your answer", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    if (userAnswer != null && !isCorrect && !isTimedOut) {
                        Text(
                            text = "Your answer: ${'A' + userAnswer}  •  Correct: ${'A' + q.correctIndex}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    } else if (isTimedOut) {
                        Text(
                            text = "No answer given  •  Correct: ${'A' + q.correctIndex}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    HorizontalDivider()
                    Text(
                        text = "Explanation: ${q.explanation}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Button(
            onClick = { viewModel.backToResult() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Back to Results")
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// ERROR SCREEN
// ─────────────────────────────────────────────────────────────

@Composable
private fun QuizErrorContent(
    message: String,
    viewModel: QuizViewModel,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Quiz failed to load", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { viewModel.resetToSetup() }) { Text("Try Again") }
    }
}