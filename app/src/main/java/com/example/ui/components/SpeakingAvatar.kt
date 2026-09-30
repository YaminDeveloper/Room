package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MemberRole
import com.example.ui.theme.*

@Composable
fun SpeakingAvatar(
  name: String,
  isSpeaking: Boolean,
  audioLevel: Float,
  isMuted: Boolean,
  role: MemberRole,
  volume: Float = 1.0f,
  size: Dp = 60.dp,
  onClick: () -> Unit = {}
) {
  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1.0f,
    targetValue = if (isSpeaking) 1.18f else 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 500, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "scale"
  )

  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.6f,
    targetValue = if (isSpeaking) 0.15f else 0.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 500, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "alpha"
  )

  // Stable avatar color palette based on name hash
  val avatarColors = listOf(
    Pair(Color(0xFF00E5FF), Color(0xFF005B66)),
    Pair(Color(0xFFA855F7), Color(0xFF4A1D75)),
    Pair(Color(0xFFFF3366), Color(0xFF75152B)),
    Pair(Color(0xFF00E676), Color(0xFF00522B)),
    Pair(Color(0xFFFFB300), Color(0xFF6E4C00))
  )
  val colorPair = avatarColors[Math.abs(name.hashCode()) % avatarColors.size]

  Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier
      .size(size + 16.dp)
      .clickable { onClick() }
  ) {
    // Speaking Outer Wave Ring
    if (isSpeaking) {
      Box(
        modifier = Modifier
          .size(size + 14.dp)
          .scale(pulseScale)
          .clip(CircleShape)
          .background(colorPair.first.copy(alpha = pulseAlpha))
      )
    }

    // Avatar Core Circle
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .size(size)
        .clip(CircleShape)
        .background(
          brush = Brush.radialGradient(
            colors = listOf(colorPair.first, colorPair.second)
          )
        )
        .border(
          width = if (isSpeaking) 2.5.dp else 1.5.dp,
          color = if (isSpeaking) NeonCyan else CyberOutlineBright,
          shape = CircleShape
        )
    ) {
      Text(
        text = name.take(2).uppercase(),
        color = Color.White,
        fontWeight = FontWeight.ExtraBold,
        fontSize = (size.value * 0.38f).sp,
        letterSpacing = 1.sp
      )
    }

    // Status Badges
    // 1. Role (Owner crown)
    if (role == MemberRole.OWNER) {
      Box(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .offset(x = 2.dp, y = (-2).dp)
          .size(18.dp)
          .clip(CircleShape)
          .background(NeonAmber)
          .padding(2.dp),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Star,
          contentDescription = "Room Owner",
          tint = CyberBackground,
          modifier = Modifier.size(12.dp)
        )
      }
    }

    // 2. Mute / Volume Mute Badge
    if (isMuted || volume == 0f) {
      Box(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .offset(x = 2.dp, y = 2.dp)
          .size(20.dp)
          .clip(CircleShape)
          .background(NeonCoral)
          .padding(2.dp),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = if (volume == 0f) Icons.Default.VolumeMute else Icons.Default.MicOff,
          contentDescription = "Muted",
          tint = Color.White,
          modifier = Modifier.size(13.dp)
        )
      }
    } else if (volume != 1.0f) {
      // Custom volume indicator pill
      Box(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .offset(y = 8.dp)
          .background(CyberSurfaceElevated, CircleShape)
          .border(0.5.dp, CyberOutline, CircleShape)
          .padding(horizontal = 4.dp, vertical = 1.dp)
      ) {
        Text(
          text = "${(volume * 100).toInt()}%",
          color = NeonCyan,
          fontSize = 9.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}
