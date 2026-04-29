package com.rork.nexa.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class ThemeMode(val label: String) {
    System("System"),
    Light("Light"),
    Dark("Dark"),
    Dim("Dim"),
}

val NexaViolet = Color(0xFF7C5CFF)
val NexaVioletDeep = Color(0xFF5333E0)
val NexaIndigoDeep = Color(0xFF0E0C28)
val NexaTeal = Color(0xFF34E5C8)
val NexaPink = Color(0xFFFF6BA8)
val NexaCoral = Color(0xFFFF8A8A)
val NexaMint = Color(0xFF53D593)

private val DarkColors = darkColorScheme(
    primary = NexaViolet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2A1F6B),
    onPrimaryContainer = Color(0xFFE3DCFF),
    secondary = NexaTeal,
    onSecondary = Color(0xFF003830),
    secondaryContainer = Color(0xFF11433A),
    onSecondaryContainer = Color(0xFFB6F4E5),
    tertiary = NexaPink,
    onTertiary = Color(0xFF3A0F22),
    background = NexaIndigoDeep,
    onBackground = Color(0xFFEDEAFF),
    surface = Color(0xFF15123A),
    onSurface = Color(0xFFEDEAFF),
    surfaceVariant = Color(0xFF1F1B4D),
    onSurfaceVariant = Color(0xFFC8C2EA),
    outline = Color(0xFF4A4280),
    outlineVariant = Color(0xFF2C2860),
    error = NexaCoral,
    onError = Color.White,
)

private val DimColors = darkColorScheme(
    primary = NexaViolet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF332B66),
    onPrimaryContainer = Color(0xFFE3DCFF),
    secondary = NexaTeal,
    onSecondary = Color(0xFF003830),
    secondaryContainer = Color(0xFF1A3F39),
    onSecondaryContainer = Color(0xFFB6F4E5),
    tertiary = NexaPink,
    onTertiary = Color(0xFF3A0F22),
    background = Color(0xFF1F2030),
    onBackground = Color(0xFFE9E8F2),
    surface = Color(0xFF282A3A),
    onSurface = Color(0xFFE9E8F2),
    surfaceVariant = Color(0xFF323446),
    onSurfaceVariant = Color(0xFFB8B8CE),
    outline = Color(0xFF4A4D63),
    outlineVariant = Color(0xFF393B50),
    error = NexaCoral,
    onError = Color.White,
)

private val LightColors = lightColorScheme(
    primary = NexaVioletDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6DFFF),
    onPrimaryContainer = Color(0xFF1B0A6B),
    secondary = Color(0xFF00897B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB6F4E5),
    onSecondaryContainer = Color(0xFF002A24),
    tertiary = Color(0xFFC2185B),
    onTertiary = Color.White,
    background = Color(0xFFFBFAFF),
    onBackground = Color(0xFF14123A),
    surface = Color.White,
    onSurface = Color(0xFF14123A),
    surfaceVariant = Color(0xFFEEEAFB),
    onSurfaceVariant = Color(0xFF4A4280),
    outline = Color(0xFFBCB4DC),
    outlineVariant = Color(0xFFE0DAF3),
    error = Color(0xFFD23B3B),
    onError = Color.White,
)

@Composable
fun AppTheme(
    mode: ThemeMode = ThemeMode.System,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val scheme = when (mode) {
        ThemeMode.Light -> LightColors
        ThemeMode.Dark -> DarkColors
        ThemeMode.Dim -> DimColors
        ThemeMode.System -> if (systemDark) DarkColors else LightColors
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
