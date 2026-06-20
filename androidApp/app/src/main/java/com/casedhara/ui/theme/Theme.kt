package com.casedhara.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.map

private val LightColors = lightColorScheme(
    primary = NavyDeep,
    onPrimary = WhitePure,
    primaryContainer = Color(0xFFEAF2FF),
    onPrimaryContainer = NavyDeep,
    secondary = SaffronBright,
    onSecondary = WhitePure,
    secondaryContainer = Color(0xFFFFE6C8),
    onSecondaryContainer = Color(0xFF3D2404),
    tertiary = GreenIndia,
    onTertiary = WhitePure,
    tertiaryContainer = Color(0xFFE0F7E7),
    onTertiaryContainer = Color(0xFF053D18),
    background = PearlWhite,
    onBackground = TextPrimaryLight,
    surface = GlassLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFF2F6FB),
    onSurfaceVariant = TextSecondaryLight,
    outline = Color(0xFFDCE5F2),
    outlineVariant = Color(0xFFEAF0F7),
    error = RedRepeal,
)

private val DarkColors = darkColorScheme(
    primary = WhitePure,
    onPrimary = Color(0xFF0D1B2E),      // was NavyInk — now greyish-blue
    primaryContainer = Color(0xFF102A52),
    onPrimaryContainer = WhitePure,
    secondary = SaffronNeon,
    onSecondary = Color(0xFF1A1208),
    secondaryContainer = Color(0xFF3A260A),
    onSecondaryContainer = Color(0xFFFFE2BA),
    tertiary = GreenNeon,
    onTertiary = Color(0xFF052E12),
    tertiaryContainer = Color(0xFF0A3219),
    onTertiaryContainer = Color(0xFFC7FBD5),
    background = Color(0xFF0D1B2E),     // was NavyInk — greyish-blue
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = Color(0xFF111F30),  // was 0xFF101B2D — slightly warmer grey-blue
    onSurfaceVariant = TextSecondaryDark,
    outline = Color(0xFF25445F),         // was 0xFF23415F — slightly lifted
    outlineVariant = Color(0xFF152840),  // was 0xFF13263D
    error = Color(0xFFFF8A80),
)

private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
private val KEY_FONT_SIZE = floatPreferencesKey("font_size_scale")

/**
 * Hilt EntryPoint so that a @Composable (which cannot be injected directly)
 * can retrieve the singleton DataStore<Preferences> from the Hilt graph.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface SettingsDataStoreEntryPoint {
    fun settingsDataStore(): DataStore<Preferences>
}

/**
 * CompositionLocal so any composable can read the current font scale
 * without passing it down as a parameter.
 */
val LocalFontScale = compositionLocalOf { 1.0f }

@Composable
fun CaseDharaTheme(
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()

    // Retrieve the same singleton DataStore that SettingsViewModel uses —
    // no duplicate-delegate crash possible.
    val dataStore = remember(context) {
        EntryPoints
            .get(context.applicationContext, SettingsDataStoreEntryPoint::class.java)
            .settingsDataStore()
    }

    val darkTheme by remember(dataStore) {
        dataStore.data.map { it[KEY_DARK_MODE] ?: systemDark }
    }.collectAsState(initial = systemDark)

    val fontScale by remember(dataStore) {
        dataStore.data.map { it[KEY_FONT_SIZE] ?: 1.0f }
    }.collectAsState(initial = 1.0f)

    CompositionLocalProvider(LocalFontScale provides fontScale) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = scaledTypography(fontScale),
            shapes = DharaShapes,
            content = content,
        )
    }
}