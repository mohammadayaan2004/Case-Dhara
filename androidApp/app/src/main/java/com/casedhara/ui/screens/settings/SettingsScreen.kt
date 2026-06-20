package com.casedhara.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.casedhara.ui.components.AshokaChakraAnimation
import com.casedhara.ui.components.SplashThemeBackground
import com.casedhara.ui.components.design.DharaHaptic
import com.casedhara.ui.components.design.GlassSurface
import com.casedhara.ui.components.design.rememberDharaHaptic
import com.casedhara.ui.components.isDharaDark
import com.casedhara.ui.navigation.NavRoutes
import com.casedhara.ui.theme.ChakraGlowBlue
import com.casedhara.ui.theme.DharaSpacing
import com.casedhara.ui.theme.GreenIndia
import com.casedhara.ui.theme.GreenNeon
import com.casedhara.ui.theme.QuizViolet
import com.casedhara.ui.theme.SaffronBright
import com.google.firebase.auth.FirebaseAuth
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val fontScale by viewModel.fontSizeScale.collectAsStateWithLifecycle()
    val wrongAnswers by viewModel.wrongAnswers.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val quizProgress by viewModel.quizProgress.collectAsStateWithLifecycle()
    val savedCases by viewModel.savedCases.collectAsStateWithLifecycle()
    val profileImagePath by viewModel.profileImagePath.collectAsStateWithLifecycle()
    val isDark = isDharaDark()

    val haptic = rememberDharaHaptic()
    val user = FirebaseAuth.getInstance().currentUser
    val userEmail = user?.email ?: "Not logged in"
    val displayName = user?.displayName?.takeIf { it.isNotBlank() } ?: userEmail.substringBefore("@")

    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    var showTerms by remember { mutableStateOf(false) }
    var showEmptyWrongAnswers by remember { mutableStateOf(false) }
    var showEmptyQuizProgress by remember { mutableStateOf(false) }
    var showEmptyBookmarks by remember { mutableStateOf(false) }
    var showEmptyCaseSummaries by remember { mutableStateOf(false) }

    fun dismissAllDialogs() {
        showPrivacyPolicy = false
        showTerms = false
        showDeleteAccountDialog = false
        showClearCacheDialog = false
        showEmptyWrongAnswers = false
        showEmptyQuizProgress = false
        showEmptyBookmarks = false
        showEmptyCaseSummaries = false
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.ExtraBold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                navigationIcon = {
                    IconButton(onClick = {
                        haptic(DharaHaptic.Light)
                        navController.popBackStack()
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
                    .size(250.dp)
                    .padding(16.dp),
                chakraColor = if (isDark) Color.White else ChakraGlowBlue,
                glowColor = if (isDark) Color.White else ChakraGlowBlue,
                durationMillis = 15000,
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ProfileGlassCard(
                    displayName = displayName,
                    email = userEmail,
                    profileImagePath = profileImagePath,
                    onProfile = {
                        dismissAllDialogs()
                        haptic(DharaHaptic.Light)
                        navController.navigate(NavRoutes.PROFILE)
                    },
                    onLogout = {
                        dismissAllDialogs()
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate(NavRoutes.LOGIN) {
                            popUpTo(navController.graph.id) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )

                SettingsGroup("Learning Data", if (isDark) ChakraGlowBlue else MaterialTheme.colorScheme.primary) {
                    SettingsItem(
                        icon = Icons.Default.ErrorOutline,
                        iconTint = MaterialTheme.colorScheme.error,
                        title = "Saved Wrong Answers",
                        subtitle = if (wrongAnswers.isEmpty()) "No data yet" else "${wrongAnswers.size} saved",
                        onClick = {
                            dismissAllDialogs()
                            if (wrongAnswers.isEmpty()) showEmptyWrongAnswers = true else navController.navigate(NavRoutes.WRONG_ANSWERS)
                        },
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Default.BarChart,
                        iconTint = QuizViolet,
                        title = "Quiz Progress",
                        subtitle = if (quizProgress.isEmpty()) "No quizzes attempted" else "${quizProgress.size} quizzes completed",
                        onClick = {
                            dismissAllDialogs()
                            if (quizProgress.isEmpty()) showEmptyQuizProgress = true else navController.navigate(NavRoutes.QUIZ_PROGRESS)
                        },
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Default.Star,
                        iconTint = SaffronBright,
                        title = "Bookmarks",
                        subtitle = if (bookmarks.isEmpty()) "No bookmarks saved yet" else "${bookmarks.size} saved",
                        onClick = {
                            dismissAllDialogs()
                            if (bookmarks.isEmpty()) showEmptyBookmarks = true else navController.navigate(NavRoutes.BOOKMARKS)
                        },
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Default.Description,
                        iconTint = if (isDark) GreenNeon else GreenIndia,
                        title = "Saved Case Summaries",
                        subtitle = if (savedCases.isEmpty()) "No summaries saved yet" else "${savedCases.size} case summaries saved",
                        onClick = {
                            dismissAllDialogs()
                            if (savedCases.isEmpty()) showEmptyCaseSummaries = true else navController.navigate(NavRoutes.SAVED_CASE_SUMMARIES)
                        },
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Default.Info,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "About App",
                        subtitle = "Platform details & features",
                        onClick = {
                            dismissAllDialogs()
                            navController.navigate(NavRoutes.ABOUT)
                        },
                    )
                }

                SettingsGroup("Appearance", if (isDarkMode) ChakraGlowBlue else SaffronBright) {
                    AppearanceToggle(
                        isDarkMode = isDarkMode,
                        onToggle = viewModel::setDarkMode,
                    )
                    SettingsDivider()
                    FontScaleControl(
                        fontScale = fontScale,
                        onChange = viewModel::setFontSizeScale,
                    )
                }

                SettingsGroup("Support & Feedback", if (isDark) GreenNeon else GreenIndia) {
                    SettingsItem(Icons.Default.Email, MaterialTheme.colorScheme.primary, "Contact Support", "Get help from our team") {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:support@casedhara.com")
                            putExtra(Intent.EXTRA_SUBJECT, "CaseDhara Support Request")
                        }
                        context.startActivity(Intent.createChooser(intent, "Send Email"))
                    }
                    SettingsDivider()
                    SettingsItem(Icons.Default.BugReport, MaterialTheme.colorScheme.error, "Report a Bug", "Help us improve") {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:bugs@casedhara.com")
                            putExtra(Intent.EXTRA_SUBJECT, "Bug Report - CaseDhara")
                        }
                        context.startActivity(Intent.createChooser(intent, "Report Bug"))
                    }
                    SettingsDivider()
                    SettingsItem(Icons.Default.Lightbulb, SaffronBright, "Request Feature", "Suggest an improvement") {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:features@casedhara.com")
                            putExtra(Intent.EXTRA_SUBJECT, "Feature Request - CaseDhara")
                        }
                        context.startActivity(Intent.createChooser(intent, "Request Feature"))
                    }
                    SettingsDivider()
                    SettingsItem(Icons.Default.Star, Color(0xFFFFD600), "Rate App", "Love CaseDhara? Leave a review!") {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.casedhara"))
                        try {
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.casedhara")),
                            )
                        }
                    }
                    SettingsDivider()
                    SettingsItem(Icons.Default.Share, if (isDark) GreenNeon else GreenIndia, "Share App", "Tell friends about CaseDhara") {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Check out CaseDhara - the AI Legal Intelligence Platform for Indian law!\nhttps://play.google.com/store/apps/details?id=com.casedhara",
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share CaseDhara"))
                    }
                }

                SettingsGroup("Privacy & Security", MaterialTheme.colorScheme.error) {
                    SettingsItem(Icons.Default.Policy, MaterialTheme.colorScheme.primary, "Privacy Policy", "Read our privacy practices") {
                        dismissAllDialogs()
                        showPrivacyPolicy = true
                    }
                    SettingsDivider()
                    SettingsItem(Icons.Default.Gavel, MaterialTheme.colorScheme.primary, "Terms & Conditions", "Read our terms of use") {
                        dismissAllDialogs()
                        showTerms = true
                    }
                    SettingsDivider()
                    SettingsItem(Icons.Default.CleaningServices, if (isDark) GreenNeon else GreenIndia, "Clear Cache", "Free up storage space") {
                        dismissAllDialogs()
                        showClearCacheDialog = true
                    }
                    SettingsDivider()
                    SettingsItem(Icons.Default.DeleteForever, MaterialTheme.colorScheme.error, "Delete Account", "Permanently delete your account") {
                        dismissAllDialogs()
                        showDeleteAccountDialog = true
                    }
                }

                Spacer(Modifier.height(20.dp))
            }
        }

        SettingsDialogs(
            showDeleteAccountDialog = showDeleteAccountDialog,
            showClearCacheDialog = showClearCacheDialog,
            showPrivacyPolicy = showPrivacyPolicy,
            showTerms = showTerms,
            showEmptyWrongAnswers = showEmptyWrongAnswers,
            showEmptyQuizProgress = showEmptyQuizProgress,
            showEmptyBookmarks = showEmptyBookmarks,
            showEmptyCaseSummaries = showEmptyCaseSummaries,
            onDeleteDismiss = { showDeleteAccountDialog = false },
            onClearDismiss = { showClearCacheDialog = false },
            onPrivacyDismiss = { showPrivacyPolicy = false },
            onTermsDismiss = { showTerms = false },
            onEmptyWrongAnswersDismiss = { showEmptyWrongAnswers = false },
            onEmptyQuizProgressDismiss = { showEmptyQuizProgress = false },
            onEmptyBookmarksDismiss = { showEmptyBookmarks = false },
            onEmptyCaseSummariesDismiss = { showEmptyCaseSummaries = false },
            onClearCache = {
                viewModel.clearCache()
                showClearCacheDialog = false
            },
            onDeleteAccount = {
                user?.delete()
                FirebaseAuth.getInstance().signOut()
                navController.navigate(NavRoutes.LOGIN) {
                    popUpTo(navController.graph.id) { inclusive = true }
                }
                showDeleteAccountDialog = false
            },
        )
    }
}

@Composable
private fun ProfileGlassCard(
    displayName: String,
    email: String,
    profileImagePath: String?,
    onProfile: () -> Unit,
    onLogout: () -> Unit,
) {
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onProfile),
        cornerRadius = 28.dp,
        glowColor = ChakraGlowBlue,
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                val imagePath = profileImagePath
                if (!imagePath.isNullOrBlank() && File(imagePath).exists()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imagePath)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Profile photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                    )
                } else {
                    GlassSurface(
                        modifier = Modifier.fillMaxSize(),
                        cornerRadius = 999.dp,
                        glowColor = ChakraGlowBlue,
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(30.dp),
                            )
                        }
                    }
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Profile and account",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            OutlinedButton(onClick = onLogout, colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Logout", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    accent: Color,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(width = 28.dp, height = 3.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(accent.copy(alpha = 0.9f)),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = accent,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 22.dp,
            glowColor = accent,
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        GlassSurface(
            modifier = Modifier.size(42.dp),
            cornerRadius = 12.dp,
            glowColor = iconTint,
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(21.dp))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AppearanceToggle(isDarkMode: Boolean, onToggle: (Boolean) -> Unit) {
    val iconTint by animateColorAsState(
        targetValue = if (isDarkMode) ChakraGlowBlue else SaffronBright,
        label = "themeIconTint",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        GlassSurface(Modifier.size(42.dp), cornerRadius = 12.dp, glowColor = iconTint) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Column(Modifier.weight(1f)) {
            Text(if (isDarkMode) "Dark Mode" else "Light Mode", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(
                if (isDarkMode) "Deep navy neon interface" else "White premium interface",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = isDarkMode, onCheckedChange = onToggle)
    }
}

@Composable
private fun FontScaleControl(fontScale: Float, onChange: (Float) -> Unit) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            GlassSurface(Modifier.size(42.dp), cornerRadius = 12.dp, glowColor = MaterialTheme.colorScheme.primary) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.FormatSize, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Column(Modifier.weight(1f)) {
                Text("Font Size", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    text = when {
                        fontScale <= 0.85f -> "Small"
                        fontScale <= 1.0f -> "Default"
                        fontScale <= 1.15f -> "Large"
                        else -> "Extra Large"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Slider(
            value = fontScale,
            onValueChange = onChange,
            valueRange = 0.8f..1.3f,
            steps = 3,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("A", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("A", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun SettingsDialogs(
    showDeleteAccountDialog: Boolean,
    showClearCacheDialog: Boolean,
    showPrivacyPolicy: Boolean,
    showTerms: Boolean,
    showEmptyWrongAnswers: Boolean,
    showEmptyQuizProgress: Boolean,
    showEmptyBookmarks: Boolean,
    showEmptyCaseSummaries: Boolean,
    onDeleteDismiss: () -> Unit,
    onClearDismiss: () -> Unit,
    onPrivacyDismiss: () -> Unit,
    onTermsDismiss: () -> Unit,
    onEmptyWrongAnswersDismiss: () -> Unit,
    onEmptyQuizProgressDismiss: () -> Unit,
    onEmptyBookmarksDismiss: () -> Unit,
    onEmptyCaseSummariesDismiss: () -> Unit,
    onClearCache: () -> Unit,
    onDeleteAccount: () -> Unit,
) {
    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = onDeleteDismiss,
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Account") },
            text = { Text("This will permanently delete your account and all associated data. This action cannot be undone.") },
            confirmButton = {
                Button(onClick = onDeleteAccount, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Delete")
                }
            },
            dismissButton = { TextButton(onClick = onDeleteDismiss) { Text("Cancel") } },
        )
    }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = onClearDismiss,
            icon = { Icon(Icons.Default.CleaningServices, contentDescription = null) },
            title = { Text("Clear Cache") },
            text = { Text("This will clear temporary files and free up storage. Your data and bookmarks will not be affected.") },
            confirmButton = { Button(onClick = onClearCache) { Text("Clear") } },
            dismissButton = { TextButton(onClick = onClearDismiss) { Text("Cancel") } },
        )
    }

    if (showPrivacyPolicy) {
        AlertDialog(
            onDismissRequest = onPrivacyDismiss,
            title = { Text("Privacy Policy") },
            text = {
                Text("CaseDhara collects minimal data necessary to provide legal intelligence services. Your queries and quiz responses are processed to improve your learning experience. We do not sell personal data to third parties. Data is stored securely using Firebase. For full details, visit casedhara.com/privacy.")
            },
            confirmButton = { TextButton(onClick = onPrivacyDismiss) { Text("Close") } },
        )
    }

    if (showTerms) {
        AlertDialog(
            onDismissRequest = onTermsDismiss,
            title = { Text("Terms & Conditions") },
            text = {
                Text("By using CaseDhara, you agree that the app provides legal information for educational purposes only and does not constitute legal advice. You will not misuse AI-generated content for unlawful purposes. CaseDhara is not liable for decisions made based on app content. For full terms, visit casedhara.com/terms.")
            },
            confirmButton = { TextButton(onClick = onTermsDismiss) { Text("Close") } },
        )
    }

    EmptyInfoDialog(showEmptyWrongAnswers, Icons.Default.ErrorOutline, MaterialTheme.colorScheme.error, "Saved Wrong Answers", "No saved wrong answers yet. Incorrect quiz answers will appear here.", onEmptyWrongAnswersDismiss)
    EmptyInfoDialog(showEmptyQuizProgress, Icons.Default.BarChart, QuizViolet, "Quiz Progress", "No quiz results yet. Complete a quiz to see your progress and scores here.", onEmptyQuizProgressDismiss)
    EmptyInfoDialog(showEmptyBookmarks, Icons.Default.Star, SaffronBright, "Bookmarks", "No bookmarks saved yet. Bookmark IPC <> BNS section mappings to access them quickly here.", onEmptyBookmarksDismiss)
    EmptyInfoDialog(showEmptyCaseSummaries, Icons.Default.Description, GreenIndia, "Saved Case Summaries", "No saved case summaries yet. Summarize a PDF case to save it here.", onEmptyCaseSummariesDismiss)
}

@Composable
private fun EmptyInfoDialog(
    visible: Boolean,
    icon: ImageVector,
    tint: Color,
    title: String,
    message: String,
    onDismiss: () -> Unit,
) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(icon, contentDescription = null, tint = tint) },
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
