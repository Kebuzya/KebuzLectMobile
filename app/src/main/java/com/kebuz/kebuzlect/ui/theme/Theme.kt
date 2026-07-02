package com.kebuz.kebuzlect.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp

private val SharpShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp),
)

private fun schemeFrom(c: KebuzColors) = if (c.isDark) {
    darkColorScheme(
        primary = c.magenta,
        onPrimary = c.background,
        secondary = c.cyan,
        onSecondary = c.background,
        tertiary = c.amber,
        background = c.background,
        onBackground = c.foreground,
        surface = c.surface,
        onSurface = c.foreground,
        surfaceVariant = c.dateBg,
        onSurfaceVariant = c.muted,
        outline = c.divider,
        outlineVariant = c.divider,
        error = c.red,
        onError = c.background,
    )
} else {
    lightColorScheme(
        primary = c.magenta,
        onPrimary = c.surface,
        secondary = c.cyan,
        onSecondary = c.surface,
        tertiary = c.amber,
        background = c.background,
        onBackground = c.foreground,
        surface = c.surface,
        onSurface = c.foreground,
        surfaceVariant = c.dateBg,
        onSurfaceVariant = c.muted,
        outline = c.divider,
        outlineVariant = c.divider,
        error = c.red,
        onError = c.surface,
    )
}

@Composable
fun KebuzLectMobileTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val kebuzColors = if (darkTheme) DarkKebuzColors else LightKebuzColors
    CompositionLocalProvider(LocalKebuzColors provides kebuzColors) {
        MaterialTheme(
            colorScheme = schemeFrom(kebuzColors),
            typography = Typography,
            shapes = SharpShapes,
            content = content,
        )
    }
}
