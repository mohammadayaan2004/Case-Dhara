package com.casedhara.ui.screens.chatbot

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.casedhara.ui.components.AshokaChakraAnimation
import com.casedhara.ui.components.ChatBubble
import com.casedhara.ui.components.SplashThemeBackground
import com.casedhara.ui.components.TypingIndicator
import com.casedhara.ui.components.design.DharaHaptic
import com.casedhara.ui.components.design.FuturisticSearchBar
import com.casedhara.ui.components.design.GlassSurface
import com.casedhara.ui.components.design.rememberDharaHaptic
import com.casedhara.ui.navigation.ChatLaunchBridge
import com.casedhara.ui.theme.DharaSpacing

private val suggestedQuestions = listOf(
    "What is Section 302 IPC?",
    "Difference between IPC and BNS",
    "Explain Section 420 cheating",
    "Bail provisions under BNS",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val inputText by viewModel.inputText.collectAsStateWithLifecycle()
    val voiceState by viewModel.voiceState.collectAsStateWithLifecycle()
    val recognisedText by viewModel.recognisedText.collectAsStateWithLifecycle()
    val haptic = rememberDharaHaptic()

    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val hasConversation = messages.isNotEmpty() || isLoading

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) viewModel.startVoiceInput() }

    LaunchedEffect(Unit) {
        ChatLaunchBridge.consumeQuery()?.let { q ->
            viewModel.onInputChange(q)
            viewModel.sendMessage()
        }
        if (ChatLaunchBridge.consumeVoice()) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) viewModel.startVoiceInput()
            else permLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(recognisedText) {
        val text = recognisedText ?: return@LaunchedEffect
        viewModel.sendVoiceMessage(text)
        viewModel.consumeRecognisedText()
    }

    LaunchedEffect(Unit) {
        viewModel.errorEvents.collect { snackbarHostState.showSnackbar(it) }
    }
    LaunchedEffect(voiceState) {
        if (voiceState is VoiceAssistantManager.VoiceState.Error) {
            snackbarHostState.showSnackbar((voiceState as VoiceAssistantManager.VoiceState.Error).message)
        }
    }

    LaunchedEffect(messages.size, isLoading) {
        if (!hasConversation) return@LaunchedEffect
        val lastIndex = when {
            isLoading -> messages.size
            messages.isNotEmpty() -> messages.lastIndex
            else -> return@LaunchedEffect
        }
        listState.animateScrollToItem(lastIndex.coerceAtLeast(0))
    }

    LaunchedEffect(isLoading) {
        if (!isLoading && messages.isNotEmpty()) {
            haptic(DharaHaptic.Confirm)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (hasConversation) "Legal Answer" else "Legal Assistant",
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
                navigationIcon = {
                    IconButton(onClick = {
                        haptic(DharaHaptic.Light)
                        navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (voiceState == VoiceAssistantManager.VoiceState.Speaking) {
                        IconButton(onClick = { viewModel.stopSpeaking() }) {
                            Icon(Icons.Default.VolumeOff, contentDescription = "Stop speaking")
                        }
                    }
                    if (hasConversation) {
                        IconButton(onClick = { viewModel.clearHistory() }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear chat")
                        }
                    }
                },
            )
        },
    ) { padding ->
        SplashThemeBackground {
            AshokaChakraAnimation(
                modifier = Modifier
                    .align(Alignment.Center)
                    .alpha(0.1f),
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding(),
            ) {
                if (!hasConversation) {
                    ChatEmptyHero(
                        inputText = inputText,
                        onInputChange = viewModel::onInputChange,
                        onSearch = {
                            haptic(DharaHaptic.Search)
                            viewModel.sendMessage()
                        },
                        onVoiceClick = {
                            when {
                                voiceState == VoiceAssistantManager.VoiceState.Listening ->
                                    viewModel.stopVoiceInput()
                                ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.RECORD_AUDIO,
                                ) == PackageManager.PERMISSION_GRANTED -> viewModel.startVoiceInput()
                                else -> permLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        onSuggestionClick = { q ->
                            viewModel.onInputChange(q)
                            viewModel.sendMessage()
                        },
                        enabled = !isLoading,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(DharaSpacing.md),
                        contentPadding = PaddingValues(
                            horizontal = DharaSpacing.lg,
                            vertical = DharaSpacing.md,
                        ),
                    ) {
                        itemsIndexed(messages, key = { index, _ -> index }) { _, msg ->
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn() + slideInVertically { it / 4 },
                            ) {
                                ChatBubble(message = msg)
                            }
                        }

                        if (isLoading) {
                            item(key = "typing") {
                                GlassSurface(glowColor = MaterialTheme.colorScheme.tertiary) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(DharaSpacing.lg),
                                    ) {
                                        TypingIndicator()
                                        Spacer(Modifier.width(DharaSpacing.sm))
                                        Text(
                                            text = "AI is thinking…",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (
                        voiceState == VoiceAssistantManager.VoiceState.Listening ||
                        voiceState == VoiceAssistantManager.VoiceState.Processing
                    ) {
                        VoiceListeningBanner(voiceState = voiceState) {
                            viewModel.stopVoiceInput()
                        }
                    }

                    ChatFollowUpBar(
                        inputText = inputText,
                        onInputChange = viewModel::onInputChange,
                        onSend = {
                            haptic(DharaHaptic.Search)
                            viewModel.sendMessage()
                        },
                        onVoiceClick = {
                            when {
                                voiceState == VoiceAssistantManager.VoiceState.Listening ->
                                    viewModel.stopVoiceInput()
                                ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.RECORD_AUDIO,
                                ) == PackageManager.PERMISSION_GRANTED -> viewModel.startVoiceInput()
                                else -> permLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        onSuggestionClick = { q ->
                            viewModel.onInputChange(q)
                            viewModel.sendMessage()
                        },
                        isLoading = isLoading,
                        voiceState = voiceState,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatEmptyHero(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSearch: () -> Unit,
    onVoiceClick: () -> Unit,
    onSuggestionClick: (String) -> Unit,
    enabled: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(DharaSpacing.lg),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Ask anything about law",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(DharaSpacing.sm))
        Text(
            text = "IPC, BNS, case law & legal concepts",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(DharaSpacing.xxl))
        FuturisticSearchBar(
            value = inputText,
            onValueChange = onInputChange,
            onSearch = onSearch,
            onVoiceClick = onVoiceClick,
            enabled = enabled,
        )
        Spacer(Modifier.height(DharaSpacing.lg))
        SuggestionChipsRow(onSuggestionClick = onSuggestionClick, enabled = enabled)
    }
}

@Composable
private fun ChatFollowUpBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceClick: () -> Unit,
    onSuggestionClick: (String) -> Unit,
    isLoading: Boolean,
    voiceState: VoiceAssistantManager.VoiceState,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DharaSpacing.md, vertical = DharaSpacing.sm),
    ) {
        SuggestionChipsRow(onSuggestionClick = onSuggestionClick, enabled = !isLoading)
        Spacer(Modifier.height(DharaSpacing.sm))
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth(),
        ) {
            FuturisticSearchBar(
                value = inputText,
                onValueChange = onInputChange,
                onSearch = onSend,
                onVoiceClick = onVoiceClick,
                modifier = Modifier.weight(1f),
                placeholder = "Ask a follow-up…",
                enabled = !isLoading &&
                    voiceState != VoiceAssistantManager.VoiceState.Speaking &&
                    voiceState != VoiceAssistantManager.VoiceState.Processing,
            )
            IconButton(
                onClick = onSend,
                enabled = inputText.isNotBlank() && !isLoading,
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
        }
    }
}

@Composable
private fun SuggestionChipsRow(
    onSuggestionClick: (String) -> Unit,
    enabled: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(DharaSpacing.sm),
    ) {
        suggestedQuestions.forEach { q ->
            FilterChip(
                selected = false,
                onClick = { if (enabled) onSuggestionClick(q) },
                enabled = enabled,
                label = { Text(q, style = MaterialTheme.typography.labelMedium) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f),
                ),
            )
        }
    }
}

@Composable
private fun VoiceListeningBanner(
    voiceState: VoiceAssistantManager.VoiceState,
    onCancel: () -> Unit,
) {
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        glowColor = MaterialTheme.colorScheme.error,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = DharaSpacing.lg, vertical = DharaSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Mic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.width(DharaSpacing.sm))
            Text(
                text = if (voiceState == VoiceAssistantManager.VoiceState.Processing)
                    "Processing speech…" else "Listening… tap mic to stop",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.MicOff, contentDescription = "Cancel")
            }
        }
    }
}
