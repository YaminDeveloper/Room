package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun GameBadge(
  gameTitle: String,
  modifier: Modifier = Modifier
) {
  val (bgColor, borderColor, textColor) = when (gameTitle) {
    "PUBG Mobile" -> Triple(Color(0xFF261D12), NeonAmber.copy(alpha = 0.8f), NeonAmber)
    "Call of Duty Mobile" -> Triple(Color(0xFF0F1E29), NeonCyan.copy(alpha = 0.8f), NeonCyan)
    "Free Fire" -> Triple(Color(0xFF291118), NeonCoral.copy(alpha = 0.8f), NeonCoral)
    "Roblox" -> Triple(Color(0xFF1B132B), NeonPurple.copy(alpha = 0.8f), NeonPurple)
    "Brawl Stars" -> Triple(Color(0xFF10261A), NeonGreen.copy(alpha = 0.8f), NeonGreen)
    else -> Triple(CyberSurfaceElevated, CyberOutlineBright, TextPrimary)
  }

  Box(
    modifier = modifier
      .background(bgColor, RoundedCornerShape(6.dp))
      .border(1.dp, borderColor, RoundedCornerShape(6.dp))
      .padding(horizontal = 8.dp, vertical = 3.dp)
  ) {
    Text(
      text = gameTitle,
      color = textColor,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = 0.4.sp
    )
  }
}
