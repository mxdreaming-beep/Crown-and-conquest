package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val CrownConquestColorScheme =
  darkColorScheme(
    primary = BurnishedGold,
    onPrimary = DarkOak,
    primaryContainer = CarvedWood,
    onPrimaryContainer = ParchmentCream,
    secondary = BronzeBorder,
    onSecondary = ParchmentCream,
    secondaryContainer = AgedTimber,
    onSecondaryContainer = AgedParchment,
    tertiary = ForestEmerald,
    onTertiary = ParchmentCream,
    background = DarkOak,
    onBackground = ParchmentCream,
    surface = AgedTimber,
    onSurface = ParchmentCream,
    surfaceVariant = CarvedWood,
    onSurfaceVariant = AgedParchment,
    outline = BronzeBorder,
    error = EmberRed,
    onError = ParchmentCream,
  )

private val CrownConquestShapes =
  Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
  )

@Composable
fun CrownConquestTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = CrownConquestColorScheme,
    typography = AppTypography,
    shapes = CrownConquestShapes,
    content = content,
  )
}
