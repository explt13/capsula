package com.capsula.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColors = darkColorScheme(
    primary = StarGold,
    onPrimary = Color(0xFF2B1D00),
    primaryContainer = Color(0xFF4A3609),
    onPrimaryContainer = Color(0xFFFFDEA6),
    secondary = Periwinkle,
    onSecondary = Color(0xFF0E1A4D),
    secondaryContainer = Color(0xFF2A3466),
    onSecondaryContainer = Color(0xFFDCE1FF),
    tertiary = WaxRed,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF5E1F2C),
    onTertiaryContainer = Color(0xFFFFD9DF),
    background = NightInk,
    onBackground = Starlight,
    surface = NightInk,
    onSurface = Starlight,
    surfaceVariant = NightHigh,
    onSurfaceVariant = Mist,
    surfaceContainerLowest = NightDeep,
    surfaceContainerLow = Color(0xFF161933),
    surfaceContainer = NightRaised,
    surfaceContainerHigh = NightHigh,
    surfaceContainerHighest = NightHighest,
    outline = Color(0xFF6E7398),
    outlineVariant = Color(0xFF3A3F63),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

private val LightColors = lightColorScheme(
    primary = StarGoldDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDEA6),
    onPrimaryContainer = Color(0xFF2B1D00),
    secondary = Color(0xFF4458A8),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE1FF),
    onSecondaryContainer = Color(0xFF0E1A4D),
    tertiary = WaxRedDeep,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD9DF),
    onTertiaryContainer = Color(0xFF3E0614),
    background = Moonlight,
    onBackground = InkText,
    surface = Moonlight,
    onSurface = InkText,
    surfaceVariant = MoonlightHigh,
    onSurfaceVariant = InkMuted,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7F8FC),
    surfaceContainer = MoonlightRaised,
    surfaceContainerHigh = MoonlightHigh,
    surfaceContainerHighest = MoonlightHighest,
    outline = Color(0xFF7A7F9E),
    outlineVariant = Color(0xFFC3C7DA),
)

private val CapsulaShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

// Тема приложения Capsula, определяющая цветовую схему, типографику и формы для UI-компонентов.
@Composable
fun CapsulaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = CapsulaTypography,
        shapes = CapsulaShapes,
        content = content,
    )
}
