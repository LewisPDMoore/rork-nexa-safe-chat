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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Shield
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rork.nexa.data.AppState
import com.rork.nexa.data.MockData
import com.rork.nexa.models.SuggestedFriend
import com.rork.nexa.ui.components.Avatar

private enum class Step { Welcome, Protected, Control, SignUp, Username, Friends }

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    var step by remember { mutableStateOf(Step.Welcome) }
    var contact by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    val selectedFriends = remember { mutableStateOf(setOf<String>()) }

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
                Step.Welcome -> WelcomePage(
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
                    onPrimary = { step = Step.Protected },
                    onBack = null,
                )
                Step.Protected -> WelcomePage(
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
                    onPrimary = { step = Step.Control },
                    onBack = { step = Step.Welcome },
                )
                Step.Control -> WelcomePage(
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
                    primaryLabel = "Set up account",
                    indicator = 2,
                    onPrimary = { step = Step.SignUp },
                    onBack = { step = Step.Protected },
                )
                Step.SignUp -> SignUpPage(
                    contact = contact,
                    onContact = { contact = it },
                    onContinue = { step = Step.Username },
                    onBack = { step = Step.Control },
                )
                Step.Username -> UsernamePage(
                    username = username,
                    onUsername = { username = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '_' }.take(20) },
                    onContinue = { step = Step.Friends },
                    onBack = { step = Step.SignUp },
                )
                Step.Friends -> FriendsPage(
                    selected = selectedFriends.value,
                    onToggle = { name ->
                        selectedFriends.value = if (selectedFriends.value.contains(name))
                            selectedFriends.value - name else selectedFriends.value + name
                    },
                    onDone = {
                        AppState.username = username.ifBlank { "you" }
                        AppState.displayName = username.ifBlank { "You" }.replaceFirstChar { it.uppercase() }
                        MockData.suggestedFriends
                            .filter { selectedFriends.value.contains(it.name) }
                            .forEach { AppState.startChat(it.name, it.initials, it.color) }
                        AppState.hasOnboarded = true
                        onDone()
                    },
                    onBack = { step = Step.Username },
                )
            }
        }
    }
}

@Composable
private fun WelcomePage(
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
        PrimaryButton(primaryLabel, onPrimary)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SignUpPage(
    contact: String,
    onContact: (String) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
) {
    val isEmail = contact.contains("@")
    val canContinue = contact.length >= 5

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircleIcon(Icons.AutoMirrored.Filled.ArrowBack, onBack)
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Create your account",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Use your phone or email. We'll keep it private.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(28.dp))

        InputField(
            value = contact,
            onChange = onContact,
            placeholder = "Phone or email",
            leadingIcon = if (isEmail) Icons.Outlined.Mail else Icons.Outlined.Phone,
        )
        Spacer(Modifier.height(14.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                .padding(14.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "End-to-end encrypted by default",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(40.dp))
        PrimaryButton("Continue", onContinue, enabled = canContinue)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun UsernamePage(
    username: String,
    onUsername: (String) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
) {
    val canContinue = username.length >= 3
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 20.dp),
    ) {
        Row { CircleIcon(Icons.AutoMirrored.Filled.ArrowBack, onBack) }
        Spacer(Modifier.height(24.dp))
        Text(
            "Pick a username",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "This is how friends will find you on Nexa.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(28.dp))
        InputField(
            value = username,
            onChange = onUsername,
            placeholder = "yourname",
            leadingIcon = Icons.Outlined.AlternateEmail,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            if (username.isNotBlank()) "nexa.app/@$username" else "Letters, numbers, underscores",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
        )
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(40.dp))
        PrimaryButton("Continue", onContinue, enabled = canContinue)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun FriendsPage(
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onDone: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 0.dp, vertical = 20.dp),
    ) {
        Row(modifier = Modifier.padding(horizontal = 28.dp)) {
            CircleIcon(Icons.AutoMirrored.Filled.ArrowBack, onBack)
        }
        Spacer(Modifier.height(24.dp))
        Column(modifier = Modifier.padding(horizontal = 28.dp)) {
            Text(
                "Add a few friends",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Pick people you actually talk to. You can skip and invite later.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
            )
        }
        Spacer(Modifier.height(20.dp))
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(MockData.suggestedFriends, key = { it.name }) { f ->
                FriendChip(f, selected.contains(f.name)) { onToggle(f.name) }
            }
        }
        Spacer(Modifier.weight(1f))
        Column(modifier = Modifier.padding(horizontal = 28.dp)) {
            PrimaryButton(
                if (selected.isEmpty()) "Skip for now" else "Add ${selected.size} & continue",
                onDone,
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FriendChip(friend: SuggestedFriend, selected: Boolean, onClick: () -> Unit) {
    val border = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(96.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.5.dp, border, RoundedCornerShape(22.dp))
            .clickable { onClick() }
            .padding(vertical = 16.dp, horizontal = 8.dp),
    ) {
        Box {
            Avatar(
                initials = friend.initials,
                color = Color(friend.color),
                size = 60.dp,
            )
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            friend.name,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
        )
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

@Composable
private fun PrimaryButton(label: String, onClick: () -> Unit, enabled: Boolean = true) {
    val gradient = Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary,
        )
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (enabled) gradient
                else Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.surfaceVariant,
                    )
                )
            )
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                color = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
            Spacer(Modifier.width(10.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun InputField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Icon(
            imageVector = leadingIcon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            if (value.isEmpty()) {
                Text(
                    placeholder,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
