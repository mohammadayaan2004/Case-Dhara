package com.casedhara.ui.screens.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.casedhara.R
import com.casedhara.ui.components.AshokaChakraAnimation
import com.casedhara.ui.components.SplashThemeBackground
import com.casedhara.ui.components.design.DharaHaptic
import com.casedhara.ui.components.design.ExpandableGlassFeatureCard
import com.casedhara.ui.components.design.FuturisticSearchBar
import com.casedhara.ui.components.design.GlassSurface
import com.casedhara.ui.components.design.rememberDharaHaptic
import com.casedhara.ui.components.isDharaDark
import com.casedhara.ui.navigation.ChatLaunchBridge
import com.casedhara.ui.navigation.NavRoutes
import com.casedhara.ui.theme.AshokaBlue
import com.casedhara.ui.theme.ChakraGlowBlue
import com.casedhara.ui.theme.DharaSpacing
import com.casedhara.ui.theme.GreenIndia
import com.casedhara.ui.theme.GreenNeon
import com.casedhara.ui.theme.NavyDeep
import com.casedhara.ui.theme.QuizViolet
import com.casedhara.ui.theme.SaffronBright
import com.casedhara.ui.theme.SaffronNeon
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun HomeScreen(
    navController: NavController,
) {
    var searchQuery by remember { mutableStateOf("") }
    val haptic = rememberDharaHaptic()
    val greeting = remember { greetingForTime() }
    val isDark = isDharaDark()

    fun launchChat(query: String? = null, voice: Boolean = false) {
        ChatLaunchBridge.pendingQuery = query?.trim()?.takeIf { it.isNotEmpty() }
        ChatLaunchBridge.startVoiceOnOpen = voice
        haptic(DharaHaptic.Confirm)
        navController.navigate(NavRoutes.CHAT)
    }

    Scaffold(containerColor = Color.Transparent) { padding ->
        SplashThemeBackground {
            AshokaChakraAnimation(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(280.dp)
                    .alpha(if (isDark) 0.08f else 0.10f),
                chakraColor = if (isDark) Color.White else AshokaBlue,
                glowColor = if (isDark) Color.White else ChakraGlowBlue,
                durationMillis = 14000,
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = DharaSpacing.lg),
            ) {
                Spacer(Modifier.height(DharaSpacing.md))
                HomeHero(
                    greeting = greeting,
                    isDark = isDark,
                    onSettings = {
                        haptic(DharaHaptic.Light)
                        navController.navigate(NavRoutes.SETTINGS)
                    },
                )

                Spacer(Modifier.height(DharaSpacing.lg))

                AiAssistantCard(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onSearch = { launchChat(searchQuery) },
                    onVoice = { launchChat(voice = true) },
                    isDark = isDark,
                )

                Spacer(Modifier.height(DharaSpacing.xl))

                Text(
                    text = "Legal Tools",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 2.dp, bottom = DharaSpacing.md),
                )

                HomeToolCard(
                    title = "Section Mapper",
                    subtitle = "IPC <> BNS lookup",
                    description = "Search and compare Indian Penal Code sections with Bharatiya Nyaya Sanhita mappings, headings, and descriptions.",
                    icon = Icons.Default.Search,
                    accentColor = SaffronBright,
                    actionLabel = "Open Mapper",
                    onOpen = {
                        haptic(DharaHaptic.Confirm)
                        navController.navigate(NavRoutes.MAPPER)
                    },
                )
                Spacer(Modifier.height(DharaSpacing.md))
                HomeToolCard(
                    title = "Case Summarizer",
                    subtitle = "PDF & judgment summaries",
                    description = "Upload a judgment PDF or paste text to generate structured case summaries with invoked sections.",
                    icon = Icons.Default.Description,
                    accentColor = if (isDark) GreenNeon else GreenIndia,
                    actionLabel = "Open Summarizer",
                    onOpen = {
                        haptic(DharaHaptic.Confirm)
                        navController.navigate(NavRoutes.SUMMARIZER)
                    },
                )
                Spacer(Modifier.height(DharaSpacing.md))
                HomeToolCard(
                    title = "Legal Quiz",
                    subtitle = "Test your knowledge",
                    description = "Practice IPC/BNS concepts with daily quizzes, track progress, and review wrong answers.",
                    icon = Icons.Default.Quiz,
                    accentColor = QuizViolet,
                    actionLabel = "Open Quiz",
                    onOpen = {
                        haptic(DharaHaptic.Confirm)
                        navController.navigate(NavRoutes.QUIZ)
                    },
                )

                Spacer(Modifier.height(DharaSpacing.xxxl))
            }
        }
    }
}

@Composable
private fun HomeHero(
    greeting: String,
    isDark: Boolean,
    onSettings: () -> Unit,
) {
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .height(244.dp),
        cornerRadius = 28.dp,
        glowColor = if (isDark) ChakraGlowBlue else AshokaBlue,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val assistantSize = if (maxWidth < 380.dp) 170.dp else 205.dp
            ChakraWatermark(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 6.dp, y = (-26).dp)
                    .size(136.dp)
                    .alpha(if (isDark) 0.08f else 0.055f),
                isDark = isDark,
            )
            IndianFlagRibbon(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(y = 14.dp)
                    .fillMaxWidth(0.82f)
                    .height(96.dp),
                isDark = isDark,
            )
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 18.dp, top = 20.dp, end = 72.dp),
            ) {
                Text(
                    text = "Welcome to",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isDark) SaffronNeon else SaffronBright,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = brandText(isDark),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            IconButton(
                onClick = onSettings,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            LegalAssistantImage(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(assistantSize)
                    .offset(x = 16.dp, y = 8.dp),
            )
        }
    }
}

@Composable
private fun AiAssistantCard(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onVoice: () -> Unit,
    isDark: Boolean,
) {
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        glowColor = if (isDark) ChakraGlowBlue else AshokaBlue,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(DharaSpacing.md),
        ) {
            Text(
                text = "AI Legal Assistant",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            FuturisticSearchBar(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                onSearch = onSearch,
                onVoiceClick = onVoice,
                placeholder = "Ask anything about law...",
            )
            Text(
                text = "Legal Clarity, One Question Away",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun HomeToolCard(
    title: String,
    subtitle: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    actionLabel: String,
    onOpen: () -> Unit,
) {
    ExpandableGlassFeatureCard(
        title = title,
        subtitle = subtitle,
        description = description,
        icon = icon,
        accentColor = accentColor,
        actionLabel = actionLabel,
        onOpen = onOpen,
    )
}

@Composable
private fun LegalAssistantImage(modifier: Modifier = Modifier) {
    val progress by rememberInfiniteTransition(label = "assistantLoop").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(25000, easing = LinearEasing)),
        label = "assistantProgress",
    )
    val visibleAlpha = when {
        progress < 0.06f -> progress / 0.06f
        progress < 0.80f -> 1f
        progress < 0.88f -> 1f - ((progress - 0.80f) / 0.08f)
        else -> 0f
    }
    val xDrift = when {
        progress < 0.06f -> (1f - progress / 0.06f) * 34f
        progress < 0.80f -> 0f
        progress < 0.88f -> -36f * ((progress - 0.80f) / 0.08f)
        else -> -36f
    }
    val breathe = sin(progress * PI * 12.0).toFloat() * 4f

    Image(
        painter = painterResource(R.drawable.legal_assistant),
        contentDescription = "AI legal assistant",
        contentScale = ContentScale.Fit,
        modifier = modifier.graphicsLayer {
            alpha = visibleAlpha
            translationX = xDrift
            translationY = breathe
            scaleX = 0.985f + (breathe / 500f)
            scaleY = 1.0f - (breathe / 700f)
        },
    )
}

@Composable
private fun IndianFlagRibbon(modifier: Modifier, isDark: Boolean) {
    val phase by rememberInfiniteTransition(label = "flagRibbon").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(5200, easing = LinearEasing)),
        label = "flagPhase",
    )
    Canvas(modifier) {
        fun wavePath(baseY: Float, amplitude: Float): Path {
            return Path().apply {
                moveTo(0f, baseY)
                for (i in 0..40) {
                    val x = size.width * i / 40f
                    val y = baseY + sin(i / 40.0 * PI * 2.0 + phase * PI * 2.0).toFloat() * amplitude
                    lineTo(x, y)
                }
            }
        }
        val saffron = if (isDark) SaffronNeon else SaffronBright
        val green = if (isDark) GreenNeon else GreenIndia
        repeat(5) { layer ->
            val alpha = if (isDark) 0.58f / (layer + 1f) else 0.38f / (layer + 1f)
            drawPath(
                path = wavePath(size.height * (0.34f + layer * 0.018f), 12.dp.toPx() + layer),
                color = saffron.copy(alpha = alpha),
                style = Stroke(width = (8 - layer).coerceAtLeast(1).dp.toPx()),
            )
            drawPath(
                path = wavePath(size.height * (0.57f + layer * 0.018f), 10.dp.toPx() + layer),
                color = green.copy(alpha = alpha),
                style = Stroke(width = (8 - layer).coerceAtLeast(1).dp.toPx()),
            )
        }
    }
}

@Composable
private fun ChakraWatermark(modifier: Modifier, isDark: Boolean) {
    AshokaChakraAnimation(
        modifier = modifier,
        chakraColor = if (isDark) Color.White else AshokaBlue,
        glowColor = if (isDark) Color.White else ChakraGlowBlue,
        durationMillis = 18000,
        strokeScale = 0.7f,
    )
}

@Composable
private fun brandText(isDark: Boolean) = buildAnnotatedString {
    withStyle(SpanStyle(color = if (isDark) Color.White else NavyDeep)) { append("Case") }
    withStyle(SpanStyle(color = SaffronBright)) { append("D") }
    withStyle(SpanStyle(color = SaffronNeon)) { append("h") }
    withStyle(SpanStyle(color = GreenIndia)) { append("ara") }
}

private fun greetingForTime(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..20 -> "Good evening"
        else -> "Welcome"
    }
}
