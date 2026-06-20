package com.casedhara.ui.screens.splash

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.casedhara.ui.components.AshokaChakraAnimation
import com.casedhara.ui.components.SplashThemeBackground
import com.casedhara.ui.components.design.GlassSurface
import com.casedhara.ui.components.isDharaDark
import com.casedhara.ui.navigation.NavRoutes
import com.casedhara.ui.theme.AshokaBlue
import com.casedhara.ui.theme.ChakraGlowBlue
import com.casedhara.ui.theme.GreenIndia
import com.casedhara.ui.theme.GreenNeon
import com.casedhara.ui.theme.NavyDeep
import com.casedhara.ui.theme.SaffronBright
import com.casedhara.ui.theme.SaffronNeon
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import androidx.compose.ui.text.style.TextAlign
@Composable
fun SplashScreen(
    navController: NavController,
) {
    val isDark = isDharaDark()
    var exiting by remember { mutableStateOf(false) }
    val intro by animateFloatAsState(
        targetValue = if (exiting) 0f else 1f,
        animationSpec = tween(520, easing = FastOutSlowInEasing),
        label = "splashFade",
    )
    val pulse by rememberInfiniteTransition(label = "splashPulse").animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "splashPulseValue",
    )

    LaunchedEffect(Unit) {
        delay(2400)
        exiting = true
        delay(360)
        val next =
            if (FirebaseAuth.getInstance().currentUser != null) NavRoutes.HOME
            else NavRoutes.LOGIN

        navController.navigate(next) {
            popUpTo(NavRoutes.SPLASH) { inclusive = true }
            launchSingleTop = true
        }
    }

    SplashThemeBackground(showChakraGhosts = true) {
        PremiumParticles(
            modifier = Modifier.fillMaxSize(),
            isDark = isDark,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .alpha(intro)
                .scale(0.96f + intro * 0.04f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            GlassSurface(
                modifier = Modifier.size(186.dp).scale(pulse),
                cornerRadius = 40.dp,
                glowColor = if (isDark) Color.White else ChakraGlowBlue,
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AshokaChakraAnimation(
                        modifier = Modifier.size(118.dp),
                        chakraColor = if (isDark) Color.White else AshokaBlue,
                        glowColor = if (isDark) Color.White else ChakraGlowBlue,
                        durationMillis = 4200,
                    )
                }
            }

            Spacer(Modifier.height(30.dp))
            Text(
                text = caseDharaLogoText(isDark),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Maps Laws\n Summarize Cases\n Chat with Law \n Compleete AI  Legal Intelligence Platform",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(34.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                LoadingDash(SaffronBright)
                LoadingDash(if (isDark) GreenNeon else GreenIndia)
            }
        }
    }
}

@Composable
private fun LoadingDash(color: Color) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .width(44.dp)
            .height(5.dp)
            .then(
                Modifier
                    .alpha(0.92f)
                    .padding(0.dp),
            ),
    ) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            drawRoundRect(
                color = color,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(40f, 40f),
            )
        }
    }
}

@Composable
private fun caseDharaLogoText(isDark: Boolean) = buildAnnotatedString {
    withStyle(SpanStyle(color = if (isDark) Color.White else NavyDeep)) { append("Case") }
    withStyle(SpanStyle(color = SaffronNeon)) { append("D") }
    withStyle(SpanStyle(color = SaffronBright)) { append("h") }
    withStyle(SpanStyle(color = GreenIndia)) { append("ara") }
}

@Composable
private fun PremiumParticles(modifier: Modifier, isDark: Boolean) {
    val phase by rememberInfiniteTransition(label = "splashParticles").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200)),
        label = "particlePhase",
    )
    Canvas(modifier) {
        val colors = listOf(
            if (isDark) SaffronNeon else SaffronBright,
            if (isDark) Color.White else AshokaBlue,
            if (isDark) GreenNeon else GreenIndia,
        )
        repeat(18) { index ->
            val row = index / 6
            val col = index % 6
            val alpha = 0.18f + ((phase + index * 0.07f) % 1f) * 0.28f
            drawCircle(
                color = colors[row].copy(alpha = alpha),
                radius = 2.2.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(
                    x = size.width * 0.78f + col * 10.dp.toPx(),
                    y = size.height * 0.12f + row * 11.dp.toPx(),
                ),
            )
            drawCircle(
                color = colors[row].copy(alpha = alpha * 0.7f),
                radius = 1.8.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(
                    x = size.width * 0.11f + col * 9.dp.toPx(),
                    y = size.height * 0.86f + row * 10.dp.toPx(),
                ),
            )
        }
        drawCircle(
            color = (if (isDark) Color.White else AshokaBlue).copy(alpha = if (isDark) 0.04f else 0.05f),
            radius = size.width * 0.23f,
            center = center.copy(x = size.width * 0.77f, y = size.height * 0.28f),
            style = Stroke(1.dp.toPx()),
        )
    }
}
