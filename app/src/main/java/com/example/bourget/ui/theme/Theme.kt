package com.example.bourget.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Ink is the only "primary": switches, checkboxes and filled buttons are drawn in it.
private val LightColorScheme = lightColorScheme(
    primary = InkLight,
    onPrimary = OnInkLight,
    primaryContainer = TonalLight,
    onPrimaryContainer = InkLight,
    secondary = InkLight,
    onSecondary = OnInkLight,
    secondaryContainer = TonalLight,
    onSecondaryContainer = InkLight,
    background = PaperLight,
    onBackground = InkLight,
    surface = PaperLight,
    onSurface = InkLight,
    surfaceVariant = TonalLight,
    onSurfaceVariant = InkVariantLight,
    surfaceContainerHigh = RaisedLight,
    surfaceTint = Color.Transparent,
    outline = OutlineLight,
    outlineVariant = LineLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = InkDark,
    onPrimary = OnInkDark,
    primaryContainer = TonalDark,
    onPrimaryContainer = InkDark,
    secondary = InkDark,
    onSecondary = OnInkDark,
    secondaryContainer = TonalDark,
    onSecondaryContainer = InkDark,
    background = PaperDark,
    onBackground = InkDark,
    surface = PaperDark,
    onSurface = InkDark,
    surfaceVariant = TonalDark,
    onSurfaceVariant = InkVariantDark,
    surfaceContainerHigh = RaisedDark,
    surfaceTint = Color.Transparent,
    outline = OutlineDark,
    outlineVariant = LineDark,
)

@Composable
fun BourgetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
