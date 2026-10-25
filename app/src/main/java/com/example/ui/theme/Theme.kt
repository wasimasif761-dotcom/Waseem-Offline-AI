package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val WaseemDarkColorScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = OnCyanPrimary,
    primaryContainer = CyanPrimaryContainer,
    onPrimaryContainer = TextPrimaryDark,
    secondary = VioletSecondary,
    onSecondary = OnVioletSecondary,
    secondaryContainer = VioletSecondaryContainer,
    onSecondaryContainer = TextPrimaryDark,
    tertiary = EmeraldTertiary,
    onTertiary = TextPrimaryDark,
    tertiaryContainer = EmeraldTertiaryContainer,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainer = SurfaceVariantDark,
    surfaceContainerHigh = SurfaceHighDark,
    outline = CardBorderDark,
    error = ErrorDark,
    onError = TextPrimaryDark
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = WaseemDarkColorScheme,
        typography = Typography,
        content = content
    )
}
