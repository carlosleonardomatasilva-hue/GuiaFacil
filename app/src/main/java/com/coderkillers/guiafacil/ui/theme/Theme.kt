package com.coderkillers.guiafacil.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val AccessibilityColorScheme = lightColorScheme(
    primary = InstitutionalBlue,
    onPrimary = SurfaceWhite,
    primaryContainer = InstitutionalBlueLight,
    secondary = HealthGreen,
    onSecondary = SurfaceWhite,
    secondaryContainer = HealthGreenLight,
    tertiary = AccentOrange,
    background = SoftCreamBackground,
    onBackground = DarkText,
    surface = SurfaceWhite,
    onSurface = DarkText
)

@Composable
fun GuiaFacilTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AccessibilityColorScheme,
        typography = HighAccessibilityTypography,
        content = content
    )
}
