package com.rork.nexa.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rork.nexa.ui.components.AuthPrimaryButton

private enum class IntroStep { Welcome, Protected, Control }

@Composable
fun OnboardingScreen(
    onSignUp: () -> Unit,
    onLogin: () -> Unit,
) {
    var step by remember { mutableStateOf(IntroStep.Welcome) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                (slideInHorizontally(tween(260)) { it / 4 } + fadeIn(tween(220))) togetherWith
                    (slideOutHorizontally(tween(220)) { -it / 4 } + fadeOut(tween(180)))
            },
            label = "onboarding",
            modifier = Modifier.fillMaxSize(),
        ) { current ->
            when (current) {
                IntroStep.Welcome -> IntroPage(
                    accent = Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary,
                        )
                    ),
                    icon = Icons.Outlined.Bolt,
                    eyebrow = "Welcome to",
                    title = "Nexa",
                    subtitle = "Private. Protected. Yours.",
                    body = "A messaging app built for real conversations — without the noise of public feeds.",
                    primaryLabel = "Get started",
                    indicator = 0,
                    onPrimary = { step = IntroStep.Protected },
                    onBack = null,
                    showAuthLinks = false,
                    onSignUp = onSignUp,
                    onLogin = onLogin,
                )
                IntroStep.Protected -> IntroPage(
                    accent = Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary,
                        )
                    ),
                    icon = Icons.Outlined.AutoAwesome,
                    eyebrow = "Built-in protection",
                    title = "Chat freely",
                    subtitle = "Shield has your back",
                    body = "Bullying or harmful messages get a gentle nudge before they reach anyone — for you and from you.",
                    primaryLabel = "Continue",
                    indicator = 1,
                    onPrimary = { step = IntroStep.Control },
                    onBack = { step = IntroStep.Welcome },
                    showAuthLinks = false,
                    onSignUp = onSignUp,
                    onLogin = onLogin,
                )
                IntroStep.Control -> IntroPage(
                    accent = Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.tertiary,
                            MaterialTheme.colorScheme.primary,
                        )
                    ),
                    icon = Icons.Outlined.Tune,
                    eyebrow = "You decide",
                    title = "You're in control",
                    subtitle = "Tune Shield to fit you",
                    body = "Pick how much help you want. Change it anytime in settings.",
                    primaryLabel = "Create account",
                    indicator = 2,
                    onPrimary = onSignUp,
                    onBack = { step = IntroStep.Protected },
                    showAuthLinks = true,
                    onSignUp = onSignUp,
                    onLogin = onLogin,
                )
            }
        }
    }
}

@Composable
private fun IntroPage(
    accent: Brush,
    icon: ImageVector,
    eyebrow: String,
    title: String,
    subtitle: String,
    body: String,
    primaryLabel: String,
    indicator: Int,
    onPrimary: () -> Unit,
    onBack: (() -> Unit)?,
    showAuthLinks: Boolean,
    onSignUp: () -> Unit,
    onLogin: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                CircleIcon(Icons.AutoMirrored.Filled.ArrowBack, onBack)
            } else {
                Spacer(Modifier.size(40.dp))
            }
            Spacer(Modifier.weight(1f))
            PageDots(count = 3, current = indicator)
            Spacer(Modifier.weight(1f))
            Text(
                "Log in",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onLogin() },
            )
        }
        Spacer(Modifier.weight(0.4f))

        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(34.dp))
                .background(accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(58.dp),
            )
        }
        Spacer(Modifier.height(34.dp))
        Text(
            eyebrow,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.4.sp,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            title,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 44.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            subtitle,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 15.sp,
            lineHeight = 22.sp,
        )
        Spacer(Modifier.weight(1f))
        AuthPrimaryButton(label = primaryLabel, onClick = onPrimary)
        if (showAuthLinks) {
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            ) {
                Text(
                    "Already have an account? ",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                )
                Text(
                    "Log in",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { onLogin() },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun PageDots(count: Int, current: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { i ->
            val active = i == current
            val width by animateFloatAsState(if (active) 24f else 8f, tween(220), label = "dot")
            Box(
                modifier = Modifier
                    .width(width.dp)
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    )
            )
            if (i < count - 1) Spacer(Modifier.width(6.dp))
        }
    }
}

@Composable
private fun CircleIcon(icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(18.dp),
        )
    }
}
