package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val MuvioDarkColorScheme = darkColorScheme(
    primary = MuvioChampagne,
    onPrimary = Color(0xFF140F05),
    primaryContainer = MuvioEmeraldContainer,
    onPrimaryContainer = MuvioChampagneLight,
    secondary = MuvioBronze,
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
    error = MuvioHotRed,
    onError = Color.White
)

val MuvioShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
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
