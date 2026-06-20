package com.casedhara.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val DharaFont = FontFamily.SansSerif

val Typography = Typography(
    displaySmall = TextStyle(
        fontFamily = DharaFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = DharaFont,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = DharaFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = DharaFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = DharaFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = DharaFont,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = DharaFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = DharaFont,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = DharaFont,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = DharaFont,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = DharaFont,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.sp,
    ),
)

fun scaledTypography(scale: Float): Typography {
    fun Float.s() = (this * scale).sp
    return Typography(
        displayLarge = Typography.displayLarge.copy(fontSize = Typography.displayLarge.fontSize.value.s()),
        displayMedium = Typography.displayMedium.copy(fontSize = Typography.displayMedium.fontSize.value.s()),
        displaySmall = Typography.displaySmall.copy(fontSize = Typography.displaySmall.fontSize.value.s()),
        headlineLarge = Typography.headlineLarge.copy(fontSize = Typography.headlineLarge.fontSize.value.s()),
        headlineMedium = Typography.headlineMedium.copy(fontSize = Typography.headlineMedium.fontSize.value.s()),
        headlineSmall = Typography.headlineSmall.copy(fontSize = Typography.headlineSmall.fontSize.value.s()),
        titleLarge = Typography.titleLarge.copy(fontSize = Typography.titleLarge.fontSize.value.s()),
        titleMedium = Typography.titleMedium.copy(fontSize = Typography.titleMedium.fontSize.value.s()),
        titleSmall = Typography.titleSmall.copy(fontSize = Typography.titleSmall.fontSize.value.s()),
        bodyLarge = Typography.bodyLarge.copy(fontSize = Typography.bodyLarge.fontSize.value.s()),
        bodyMedium = Typography.bodyMedium.copy(fontSize = Typography.bodyMedium.fontSize.value.s()),
        bodySmall = Typography.bodySmall.copy(fontSize = Typography.bodySmall.fontSize.value.s()),
        labelLarge = Typography.labelLarge.copy(fontSize = Typography.labelLarge.fontSize.value.s()),
        labelMedium = Typography.labelMedium.copy(fontSize = Typography.labelMedium.fontSize.value.s()),
        labelSmall = Typography.labelSmall.copy(fontSize = Typography.labelSmall.fontSize.value.s()),
    )
}
