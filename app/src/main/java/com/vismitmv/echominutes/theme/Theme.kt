package com.vismitmv.echominutes.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = IndigoPrimary,
    onPrimary = TextPrimary,
    primaryContainer = IndigoDark,
    onPrimaryContainer = IndigoLight,
    secondary = IndigoLight,
    onSecondary = NavyDark,
    secondaryContainer = NavySurface,
    onSecondaryContainer = TextSecondary,
    tertiary = GreenSuccess,
    background = NavyDark,
    onBackground = TextPrimary,
    surface = NavyMedium,
    onSurface = TextPrimary,
    surfaceVariant = NavyCard,
    onSurfaceVariant = TextSecondary,
    error = CoralRecord,
    onError = TextPrimary,
    outline = TextMuted
)

@Composable
fun EchoMinutesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = EchoTypography,
        content = content
    )
}
