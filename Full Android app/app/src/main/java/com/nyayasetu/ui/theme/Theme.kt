package com.nyayasetu.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = NavyDeep,
    onPrimary = Color.White,
    secondary = SaffronBright,
    onSecondary = Color.Black,
    tertiary = GreenIndia,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFE8EAF0),
    onSurfaceVariant = TextSecondaryLight,
    error = RedRepeal,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5C6BC0),
    onPrimary = Color.White,
    secondary = SaffronBright,
    onSecondary = Color.Black,
    tertiary = GreenIndia,
    surface = SurfaceDark,
    onSurface = Color(0xFFE8EAF0),
    surfaceVariant = Color(0xFF2D3142),
    onSurfaceVariant = Color(0xFFB0B6C3),
    error = Color(0xFFFF8A80),
)

@Composable
fun NyayaSetuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content,
    )
}
