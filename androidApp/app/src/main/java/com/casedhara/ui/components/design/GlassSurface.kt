package com.casedhara.ui.components.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.casedhara.ui.components.isDharaDark
import com.casedhara.ui.theme.GlassDark
import com.casedhara.ui.theme.GlassDarkBorder
import com.casedhara.ui.theme.GlassWhite
import com.casedhara.ui.theme.DharaRadius

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = DharaRadius.lg,
    glowColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    val isDark = isDharaDark()

    val fill = if (isDark) GlassDark else GlassWhite
    val border = if (isDark) GlassDarkBorder else Color.White.copy(alpha = 0.74f)
    val shadow = if (isDark) glowColor.copy(alpha = 0.16f) else Color(0xFF1D3557).copy(alpha = 0.10f)

    Box(
        modifier = modifier
            .shadow(
                elevation = if (isDark) 18.dp else 14.dp,
                shape = shape,
                ambientColor = shadow,
                spotColor = glowColor.copy(alpha = if (isDark) 0.18f else 0.08f),
            )
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        fill.copy(alpha = if (isDark) 0.80f else 0.92f),
                        fill.copy(alpha = if (isDark) 0.58f else 0.74f),
                    ),
                ),
            )
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    listOf(
                        border,
                        glowColor.copy(alpha = if (isDark) 0.34f else 0.15f),
                        Color.White.copy(alpha = if (isDark) 0.05f else 0.36f),
                    ),
                ),
                shape = shape,
            ),
        content = content,
    )
}
