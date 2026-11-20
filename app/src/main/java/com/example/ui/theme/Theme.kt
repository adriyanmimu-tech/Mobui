package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val MuvioDarkColorScheme = darkColorScheme(
    primary = MuvioCrimson,
    onPrimary = Color.White,
    primaryContainer = MuvioCrimsonContainer,
    onPrimaryContainer = MuvioCrimsonGlow,
    secondary = MuvioChampagne,
    onSecondary = Color(0xFF140F05),
    secondaryContainer = MuvioSurfaceElevated,
    onSecondaryContainer = MuvioTextPrimary,
    tertiary = MuvioChampagneLight,
    background = MuvioBg,
    onBackground = MuvioTextPrimary,
    surface = MuvioSurface,
    onSurface = MuvioTextPrimary,
    surfaceVariant = MuvioSurfaceElevated,
    onSurfaceVariant = MuvioTextSecondary,
    outline = MuvioOutline,
    error = MuvioLiveRed,
    onError = Color.White
)

val MuvioShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

@Composable
fun MuvioTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MuvioDarkColorScheme,
        typography = MuvioTypography,
        shapes = MuvioShapes,
        content = content
    )
}
