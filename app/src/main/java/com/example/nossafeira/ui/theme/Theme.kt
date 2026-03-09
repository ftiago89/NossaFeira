package com.example.nossafeira.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NossaFeiraDarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = Primary,
    secondary = PrimaryDim,
    onSecondary = TextPrimary,
    secondaryContainer = Surface2,
    onSecondaryContainer = TextSecondary,
    tertiary = Green,
    onTertiary = OnPrimary,
    tertiaryContainer = GreenDim,
    onTertiaryContainer = Green,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextSecondary,
    outline = Border,
    outlineVariant = Surface3,
    error = Pink,
    onError = PinkDim,
    errorContainer = PinkDim,
    onErrorContainer = Pink,
    inverseSurface = TextPrimary,
    inverseOnSurface = Background,
    inversePrimary = PrimaryDim,
    scrim = Background,
)

@Composable
fun NossaFeiraTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NossaFeiraDarkColorScheme,
        typography = Typography,
        content = content
    )
}
