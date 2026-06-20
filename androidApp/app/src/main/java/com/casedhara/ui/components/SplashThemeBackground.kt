package com.casedhara.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.casedhara.ui.theme.AshokaBlue
import com.casedhara.ui.theme.ChakraGlowBlue
import com.casedhara.ui.theme.GreenIndia
import com.casedhara.ui.theme.GreenNeon
import com.casedhara.ui.theme.MistGray
import com.casedhara.ui.theme.NavyDeep
import com.casedhara.ui.theme.NavyInk
import com.casedhara.ui.theme.SaffronBright
import com.casedhara.ui.theme.SaffronNeon
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun SplashThemeBackground(
    modifier: Modifier = Modifier,
    showChakraGhosts: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val isDark = isDharaDark()
    val transition = rememberInfiniteTransition(label = "dhara_background")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing)),
        label = "wave_phase",
    )

    val bg = if (isDark) {
        Brush.verticalGradient(
            listOf(
                NavyInk,
                Color(0xFF0A1828),  // was 0xFF041124 — greyish-blue
                Color(0xFF081422),  // was 0xFF020711 — greyish-blue
            ),
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White,
                MistGray,
                Color(0xFFF9FBFE),
            ),
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bg),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val glowAlpha = if (isDark) 0.32f else 0.12f

            drawCircle(
                color = ChakraGlowBlue.copy(alpha = if (isDark) 0.12f else 0.05f),
                radius = w * 0.75f,
                center = Offset(w * 0.92f, h * 0.12f),
            )
            drawCircle(
                color = SaffronBright.copy(alpha = glowAlpha * 0.72f),
                radius = w * 0.58f,
                center = Offset(w * 0.03f, -h * 0.04f),
            )
            drawCircle(
                color = GreenIndia.copy(alpha = glowAlpha * 0.68f),
                radius = w * 0.62f,
                center = Offset(w * 0.95f, h * 1.02f),
            )

            drawWaveBand(
                color = if (isDark) SaffronNeon else SaffronBright,
                alpha = if (isDark) 0.48f else 0.22f,
                phase = phase,
                fromTop = true,
            )
            drawWaveBand(
                color = if (isDark) GreenNeon else GreenIndia,
                alpha = if (isDark) 0.42f else 0.18f,
                phase = 1f - phase,
                fromTop = false,
            )

            if (showChakraGhosts) {
                drawGhostChakra(
                    center = Offset(w * 0.83f, h * 0.22f),
                    radius = w * 0.14f,
                    color = if (isDark) Color.White else AshokaBlue,
                    alpha = if (isDark) 0.035f else 0.045f,
                )
                drawGhostChakra(
                    center = Offset(w * 0.16f, h * 0.82f),
                    radius = w * 0.17f,
                    color = if (isDark) Color.White else NavyDeep,
                    alpha = if (isDark) 0.03f else 0.035f,
                )
            }
        }
        content()
    }
}

@Composable
fun isDharaDark(): Boolean {
    val bg = MaterialTheme.colorScheme.background
    return bg.red + bg.green + bg.blue < 1.05f
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWaveBand(
    color: Color,
    alpha: Float,
    phase: Float,
    fromTop: Boolean,
) {
    val w = size.width
    val h = size.height
    val baseY = if (fromTop) h * 0.12f else h * 0.78f
    val amplitude = h * 0.035f
    val bandHeight = h * 0.13f
    val startY = if (fromTop) baseY - bandHeight else baseY + bandHeight

    repeat(7) { layer ->
        val path = Path().apply {
            moveTo(0f, startY + layer * 9.dp.toPx())
            for (i in 0..56) {
                val x = w * i / 56f
                val y = baseY +
                        sin((i / 56f * PI * 2.0) + phase * PI * 2.0 + layer * 0.42).toFloat() *
                        (amplitude + layer * 1.2.dp.toPx())
                lineTo(x, y + layer * 4.dp.toPx())
            }
            lineTo(w, if (fromTop) -bandHeight else h + bandHeight)
            lineTo(0f, if (fromTop) -bandHeight else h + bandHeight)
            close()
        }
        drawPath(path = path, color = color.copy(alpha = alpha / (layer + 1.1f)))
        drawPath(
            path = path,
            color = color.copy(alpha = alpha * 0.36f),
            style = Stroke(width = 0.8.dp.toPx()),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGhostChakra(
    center: Offset,
    radius: Float,
    color: Color,
    alpha: Float,
) {
    drawCircle(color = color.copy(alpha = alpha), center = center, radius = radius, style = Stroke(2.dp.toPx()))
    repeat(24) { i ->
        val angle = (i * 15f) * PI / 180.0
        val start = Offset(
            center.x + (radius * 0.16f * kotlin.math.cos(angle)).toFloat(),
            center.y + (radius * 0.16f * kotlin.math.sin(angle)).toFloat(),
        )
        val end = Offset(
            center.x + (radius * 0.92f * kotlin.math.cos(angle)).toFloat(),
            center.y + (radius * 0.92f * kotlin.math.sin(angle)).toFloat(),
        )
        drawLine(color = color.copy(alpha = alpha), start = start, end = end, strokeWidth = 1.dp.toPx())
    }
}