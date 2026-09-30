package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SquadPingDarkColorScheme = darkColorScheme(
  primary = NeonCyan,
  onPrimary = CyberBackground,
  primaryContainer = NeonCyanDark,
  onPrimaryContainer = TextPrimary,
  secondary = NeonPurple,
  onSecondary = CyberBackground,
  secondaryContainer = NeonPurpleDark,
  onSecondaryContainer = TextPrimary,
  tertiary = NeonCoral,
  onTertiary = CyberBackground,
  background = CyberBackground,
  onBackground = TextPrimary,
  surface = CyberSurface,
  onSurface = TextPrimary,
  surfaceVariant = CyberSurfaceVariant,
  onSurfaceVariant = TextSecondary,
  outline = CyberOutline,
  outlineVariant = CyberOutlineBright,
  error = NeonCoral,
  onError = CyberBackground
)

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit
) {
  MaterialTheme(
    colorScheme = SquadPingDarkColorScheme,
    typography = Typography,
    content = content
  )
}
