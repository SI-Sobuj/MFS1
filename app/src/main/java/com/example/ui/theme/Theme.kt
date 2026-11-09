package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = TakaMagentaLight,
  onPrimary = Color(0xFF540028),
  primaryContainer = TakaMagentaDark,
  onPrimaryContainer = TakaMagentaContainer,
  secondary = TakaGold,
  onSecondary = Color(0xFF3E2700),
  secondaryContainer = Color(0xFF5B3B00),
  onSecondaryContainer = TakaGoldContainer,
  tertiary = TakaEmerald,
  onTertiary = Color.White,
  tertiaryContainer = Color(0xFF065F46),
  onTertiaryContainer = TakaEmeraldContainer,
  background = SurfaceDark,
  onBackground = OnSurfaceDark,
  surface = SurfaceDark,
  onSurface = OnSurfaceDark,
  surfaceVariant = SurfaceContainerDark,
  onSurfaceVariant = OnSurfaceVariantDark,
)

private val LightColorScheme = lightColorScheme(
  primary = TakaMagenta,
  onPrimary = Color.White,
  primaryContainer = TakaMagentaContainer,
  onPrimaryContainer = OnTakaMagentaContainer,
  secondary = TakaGoldDark,
  onSecondary = Color.White,
  secondaryContainer = TakaGoldContainer,
  onSecondaryContainer = OnTakaGoldContainer,
  tertiary = TakaEmerald,
  onTertiary = Color.White,
  tertiaryContainer = TakaEmeraldContainer,
  onTertiaryContainer = OnTakaEmeraldContainer,
  background = SurfaceLight,
  onBackground = OnSurfaceLight,
  surface = SurfaceLight,
  onSurface = OnSurfaceLight,
  surfaceVariant = SurfaceContainerLight,
  onSurfaceVariant = OnSurfaceVariantLight,
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content,
  )
}
