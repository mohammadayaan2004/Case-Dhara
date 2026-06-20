package com.casedhara.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

import com.casedhara.ui.theme.AshokaBlue

@Composable
fun AshokaChakraAnimation(
    modifier: Modifier = Modifier,
    chakraColor: Color = AshokaBlue,
    glowColor: Color = chakraColor,
    durationMillis: Int = 9000,
    strokeScale: Float = 1f,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "chakra")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis, easing = LinearEasing)),
        label = "chakra_rotation",
    )
    Canvas(modifier = modifier.size(160.dp)) {
        val center = this.center
        val radius = size.minDimension / 2f
        drawCircle(
            color = glowColor.copy(alpha = 0.18f),
            radius = radius * 1.18f,
            center = center,
        )
        drawCircle(
            color = glowColor.copy(alpha = 0.10f),
            radius = radius * 1.42f,
            center = center,
        )
        rotate(angle, pivot = center) {
            drawCircle(
                color = chakraColor,
                radius = radius,
                center = center,
                style = Stroke(width = (3.6f * strokeScale).dp.toPx()),
            )
            drawCircle(
                color = chakraColor.copy(alpha = 0.74f),
                radius = radius * 0.86f,
                center = center,
                style = Stroke(width = (1.2f * strokeScale).dp.toPx()),
            )
            repeat(24) { i ->
                val spokeAngle = Math.toRadians((i * 15).toDouble())
                val innerR = radius * 0.13f
                val outerR = radius * 0.92f
                drawLine(
                    color = chakraColor,
                    start = Offset(
                        center.x + (innerR * cos(spokeAngle)).toFloat(),
                        center.y + (innerR * sin(spokeAngle)).toFloat(),
                    ),
                    end = Offset(
                        center.x + (outerR * cos(spokeAngle)).toFloat(),
                        center.y + (outerR * sin(spokeAngle)).toFloat(),
                    ),
                    strokeWidth = (1.55f * strokeScale).dp.toPx(),
                )
            }
            drawCircle(
                color = chakraColor,
                radius = radius * 0.12f,
                center = center,
                style = Stroke(width = (2.4f * strokeScale).dp.toPx()),
            )
        }
    }
}
