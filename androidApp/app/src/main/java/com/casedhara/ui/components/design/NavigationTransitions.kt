package com.casedhara.ui.components.design

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavBackStackEntry

private val springSpec = spring<IntOffset>(
    stiffness = Spring.StiffnessMediumLow,
    dampingRatio = 0.86f,
)

fun AnimatedContentTransitionScope<NavBackStackEntry>.dharaEnter(): EnterTransition =
    slideInHorizontally(
        initialOffsetX = { it / 4 },
        animationSpec =    springSpec,
    ) + fadeIn(animationSpec = tween(280))

fun AnimatedContentTransitionScope<NavBackStackEntry>.dharaExit(): ExitTransition =
    slideOutHorizontally(
        targetOffsetX = { -it / 6 },
        animationSpec = springSpec,
    ) + fadeOut(animationSpec = tween(220))

fun AnimatedContentTransitionScope<NavBackStackEntry>.dharaPopEnter(): EnterTransition =
    slideInHorizontally(
        initialOffsetX = { -it / 6 },
        animationSpec = springSpec,
    ) + fadeIn(animationSpec = tween(280))

fun AnimatedContentTransitionScope<NavBackStackEntry>.dharaPopExit(): ExitTransition =
    slideOutHorizontally(
        targetOffsetX = { it / 4 },
        animationSpec = springSpec,
    ) + fadeOut(animationSpec = tween(220))
