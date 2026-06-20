package com.casedhara.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val DharaShapes = Shapes(
    extraSmall = RoundedCornerShape(DharaRadius.sm),
    small = RoundedCornerShape(DharaRadius.md),
    medium = RoundedCornerShape(DharaRadius.lg),
    large = RoundedCornerShape(DharaRadius.xl),
    extraLarge = RoundedCornerShape(28.dp),
)
