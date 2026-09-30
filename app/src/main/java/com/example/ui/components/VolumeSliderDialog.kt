package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.MemberRole
import com.example.model.RoomMember
import com.example.ui.theme.*

@Composable
fun VolumeSliderDialog(
  member: RoomMember,
  isCurrentUserOwner: Boolean,
  onVolumeChange: (Float) -> Unit,
  onDismiss: () -> Unit
) {
  Dialog(onDismissRequest = onDismiss) {
    VolumeSliderContent(
      member = member,
      isCurrentUserOwner = isCurrentUserOwner,
      onVolumeChange = onVolumeChange,
      onDismiss = onDismiss
    )
  }
}

@Composable
fun VolumeSliderContent(
  member: RoomMember,
  isCurrentUserOwner: Boolean,
  onVolumeChange: (Float) -> Unit,
  onDismiss: () -> Unit
) {
  var sliderPosition by remember { mutableStateOf(member.volume) }

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = CyberSurface),
    modifier = Modifier
      .fillMaxWidth()
      .padding(16.dp)
      .testTag("volume_dialog_${member.userId}")
  ) {
      Column(
        modifier = Modifier
          .padding(20.dp)
          .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Participant Audio",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
          IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close",
              tint = TextSecondary
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SpeakingAvatar(
          name = member.displayName,
          isSpeaking = member.isSpeaking,
          audioLevel = member.audioLevel,
          isMuted = member.isMuted,
          role = member.role,
          volume = sliderPosition,
          size = 64.dp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = member.displayName,
          fontWeight = FontWeight.Bold,
          fontSize = 16.sp,
          color = TextPrimary
        )

        Text(
          text = "Ping: ${member.pingMs}ms • Codec: Opus 24kbps",
          fontSize = 12.sp,
          color = TextSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "User Volume",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
          )
          Text(
            text = "${(sliderPosition * 100).toInt()}%",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = NeonCyan
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.VolumeDown,
            contentDescription = "Min Volume",
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
          )
          Slider(
            value = sliderPosition,
            onValueChange = {
              sliderPosition = it
              onVolumeChange(it)
            },
            valueRange = 0f..2.0f,
            steps = 19,
            colors = SliderDefaults.colors(
              thumbColor = NeonCyan,
              activeTrackColor = NeonCyan,
              inactiveTrackColor = CyberOutline
            ),
            modifier = Modifier
              .weight(1f)
              .padding(horizontal = 8.dp)
              .testTag("volume_slider")
          )
          Icon(
            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = "Max Volume",
            tint = NeonCyan,
            modifier = Modifier.size(20.dp)
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = {
              val newVol = if (sliderPosition > 0f) 0f else 1.0f
              sliderPosition = newVol
              onVolumeChange(newVol)
            },
            colors = ButtonDefaults.outlinedButtonColors(
              contentColor = if (sliderPosition == 0f) NeonCoral else TextPrimary
            ),
            modifier = Modifier.testTag("volume_mute_toggle_btn").weight(1f)
          ) {
            Text(if (sliderPosition == 0f) "Unmute User" else "Mute User")
          }

          Button(
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(
              containerColor = NeonCyan,
              contentColor = CyberBackground
            ),
            modifier = Modifier.testTag("volume_dialog_done_btn").weight(1f)
          ) {
            Text("Done", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
}
