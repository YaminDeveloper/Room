package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SquadRepository
import com.example.audio.VoiceBridge
import com.example.model.ChatMessage
import com.example.model.MemberRole
import com.example.model.MessageType
import com.example.model.RoomMember
import com.example.ui.components.GameBadge
import com.example.ui.components.SpeakingAvatar
import com.example.ui.components.VolumeSliderDialog
import com.example.ui.theme.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@Composable
fun VoiceRoomScreen(
  roomId: String,
  onLeaveRoom: () -> Unit
) {
  val context = LocalContext.current
  val currentRoom by SquadRepository.currentRoom.collectAsState()
  val members by SquadRepository.roomMembers.collectAsState()
  val chatMessages by SquadRepository.chatMessages.collectAsState()
  val audioSettings by SquadRepository.audioSettings.collectAsState()
  val currentUser by SquadRepository.currentUser.collectAsState()
  val friends by SquadRepository.friends.collectAsState()

  var messageInput by remember { mutableStateOf("") }
  var selectedMemberForVolume by remember { mutableStateOf<RoomMember?>(null) }
  var showInviteDialog by remember { mutableStateOf(false) }

  val coroutineScope = rememberCoroutineScope()
  val chatListState = rememberLazyListState()

  val room = currentRoom ?: return

  // Start background voice service when entering room
  LaunchedEffect(room.id) {
    VoiceBridge.startVoiceSession(context, room)
  }

  // Scroll to bottom when new messages arrive
  LaunchedEffect(chatMessages.size) {
    if (chatMessages.isNotEmpty()) {
      chatListState.animateScrollToItem(chatMessages.size - 1)
    }
  }

  // Handle Android system back gesture to exit voice room cleanly
  BackHandler {
    VoiceBridge.stopVoiceSession(context)
    SquadRepository.leaveRoom()
    onLeaveRoom()
  }

  Scaffold(
    containerColor = CyberBackground,
    topBar = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(CyberSurface)
          .border(0.5.dp, CyberOutline, RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
          .padding(horizontal = 14.dp, vertical = 10.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
              onClick = {
                VoiceBridge.stopVoiceSession(context)
                SquadRepository.leaveRoom()
                onLeaveRoom()
              },
              modifier = Modifier.size(36.dp).testTag("back_button")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Leave Voice",
                tint = TextPrimary
              )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = room.name,
                  fontSize = 17.sp,
                  fontWeight = FontWeight.Bold,
                  color = TextPrimary,
                  maxLines = 1
                )
                if (room.isLocked) {
                  Spacer(modifier = Modifier.width(6.dp))
                  Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = NeonAmber,
                    modifier = Modifier.size(14.dp)
                  )
                }
              }
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(NeonGreen)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "${room.pingMs}ms • WebRTC Opus SFU",
                  fontSize = 11.sp,
                  color = NeonCyan,
                  fontWeight = FontWeight.Medium
                )
              }
            }
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            GameBadge(gameTitle = room.activeGame)
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
              onClick = { showInviteDialog = true },
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(CyberSurfaceElevated)
                .testTag("invite_friends_button")
            ) {
              Icon(
                imageVector = Icons.Default.PersonAdd,
                contentDescription = "Invite Friends",
                tint = NeonCyan,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }
      }
    },
    bottomBar = {
      // Bottom Voice Control Dock
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(CyberSurface)
          .border(1.dp, CyberOutline, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
          .windowInsetsPadding(WindowInsets.navigationBars)
          .padding(horizontal = 16.dp, vertical = 10.dp)
      ) {
        // Background Voice Status Banner
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CyberSurfaceVariant)
            .padding(horizontal = 10.dp, vertical = 5.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Headset,
              contentDescription = "Background Audio",
              tint = NeonGreen,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Background Voice Active • Dual Game Audio Ready",
              fontSize = 11.sp,
              color = TextSecondary,
              fontWeight = FontWeight.Medium
            )
          }
          Text(
            text = "48kHz",
            fontSize = 10.sp,
            color = NeonCyan,
            fontWeight = FontWeight.Bold
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Mic Toggle
          val isMicMuted = audioSettings.micMuted
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
              onClick = { SquadRepository.toggleMic() },
              modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(if (isMicMuted) NeonCoral else NeonCyan)
                .testTag("toggle_mic_button")
            ) {
              Icon(
                imageVector = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = if (isMicMuted) "Unmute Mic" else "Mute Mic",
                tint = if (isMicMuted) Color.White else CyberBackground,
                modifier = Modifier.size(24.dp)
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = if (isMicMuted) "Muted" else "Mic On",
              fontSize = 11.sp,
              color = if (isMicMuted) NeonCoral else NeonCyan,
              fontWeight = FontWeight.SemiBold
            )
          }

          // Deafen / Speaker Toggle
          val isDeafened = audioSettings.deafened
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
              onClick = { SquadRepository.toggleDeafen() },
              modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(if (isDeafened) NeonCoral else CyberSurfaceElevated)
                .testTag("toggle_deafen_button")
            ) {
              Icon(
                imageVector = if (isDeafened) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                contentDescription = if (isDeafened) "Undeafen" else "Deafen",
                tint = if (isDeafened) Color.White else TextPrimary,
                modifier = Modifier.size(24.dp)
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = if (isDeafened) "Deafened" else "Speaker",
              fontSize = 11.sp,
              color = if (isDeafened) NeonCoral else TextSecondary,
              fontWeight = FontWeight.SemiBold
            )
          }

          // Leave Room Action
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
              onClick = {
                VoiceBridge.stopVoiceSession(context)
                SquadRepository.leaveRoom()
                onLeaveRoom()
              },
              modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(NeonCoral)
                .testTag("leave_room_button")
            ) {
              Icon(
                imageVector = Icons.Default.CallEnd,
                contentDescription = "Leave Voice Room",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Leave",
              fontSize = 11.sp,
              color = NeonCoral,
              fontWeight = FontWeight.SemiBold
            )
          }
        }
      }
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(horizontal = 14.dp)
    ) {
      Spacer(modifier = Modifier.height(10.dp))

      // Section 1: Squad Voice Hangout Grid
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Squad In Voice (${members.size}/${room.maxParticipants})",
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          color = TextSecondary
        )
        Text(
          text = "Tap avatar for volume",
          fontSize = 11.sp,
          color = NeonCyan,
          fontWeight = FontWeight.Medium
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Participant Cards Row/Grid
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        items(members) { member ->
          ParticipantCard(
            member = member,
            onClick = { selectedMemberForVolume = member }
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Section 2: Tactical Callouts Strip
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.FlashOn,
          contentDescription = null,
          tint = NeonCyan,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "Tactical Callouts (1-Tap)",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = TextSecondary
        )
      }
      Spacer(modifier = Modifier.height(6.dp))

      val callouts = listOf(
        "Enemy Spotted" to Icons.Default.MyLocation,
        "Need Ammo" to Icons.Default.MedicalServices,
        "Rush A" to Icons.AutoMirrored.Filled.DirectionsRun,
        "Fall Back" to Icons.Default.Shield,
        "Smoke Ready" to Icons.Default.Cloud,
        "Cover Me" to Icons.Default.Security
      )
      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(callouts) { (calloutText, iconVector) ->
          AssistChip(
            onClick = { SquadRepository.sendTacticalCallout(calloutText) },
            label = {
              Text(
                text = calloutText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan
              )
            },
            leadingIcon = {
              Icon(
                imageVector = iconVector,
                contentDescription = calloutText,
                tint = NeonCyan,
                modifier = Modifier.size(14.dp)
              )
            },
            colors = AssistChipDefaults.assistChipColors(
              containerColor = CyberSurfaceElevated,
              labelColor = NeonCyan
            ),
            border = AssistChipDefaults.assistChipBorder(
              borderColor = CyberOutline,
              borderWidth = 0.5.dp,
              enabled = true
            ),
            modifier = Modifier.testTag("tactical_callout_${calloutText.replace(" ", "_")}")
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Section 3: In-Room Live Text Chat Stream
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CyberOutline)),
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
        ) {
          LazyColumn(
            state = chatListState,
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(chatMessages) { msg ->
              ChatMessageItem(message = msg)
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Text Input Row
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            OutlinedTextField(
              value = messageInput,
              onValueChange = { messageInput = it },
              placeholder = { Text("Send squad message...", fontSize = 12.sp) },
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = CyberOutline,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
              ),
              shape = RoundedCornerShape(20.dp),
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("chat_input_field")
            )

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
              onClick = {
                if (messageInput.isNotBlank()) {
                  SquadRepository.sendChatMessage(messageInput)
                  messageInput = ""
                }
              },
              modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(NeonCyan)
                .testTag("send_chat_button")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send Message",
                tint = CyberBackground,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
    }
  }

  // Individual Participant Volume Slider Dialog
  selectedMemberForVolume?.let { member ->
    VolumeSliderDialog(
      member = member,
      isCurrentUserOwner = room.ownerId == currentUser?.id,
      onVolumeChange = { newVolume ->
        SquadRepository.setParticipantVolume(member.userId, newVolume)
      },
      onDismiss = { selectedMemberForVolume = null }
    )
  }

  // Invite Friends Dialog
  if (showInviteDialog) {
    InviteFriendsDialog(
      friends = friends.filter { !it.isPendingRequest },
      onInvite = { friend ->
        SquadRepository.sendChatMessage("Invited ${friend.displayName} to squad voice.")
        showInviteDialog = false
      },
      onDismiss = { showInviteDialog = false }
    )
  }
}

@Composable
private fun ParticipantCard(
  member: RoomMember,
  onClick: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = CyberSurfaceElevated),
    border = CardDefaults.outlinedCardBorder().copy(
      brush = androidx.compose.ui.graphics.SolidColor(
        if (member.isSpeaking) NeonCyan else CyberOutline
      )
    ),
    onClick = onClick,
    modifier = Modifier
      .width(115.dp)
      .testTag("participant_card_${member.userId}")
  ) {
    Column(
      modifier = Modifier.padding(10.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      SpeakingAvatar(
        name = member.displayName,
        isSpeaking = member.isSpeaking,
        audioLevel = member.audioLevel,
        isMuted = member.isMuted,
        role = member.role,
        volume = member.volume,
        size = 52.dp,
        onClick = onClick
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = member.displayName,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary,
        maxLines = 1
      )

      val roleText = when (member.role) {
        MemberRole.OWNER -> "Host"
        MemberRole.ADMIN -> "Admin"
        MemberRole.MEMBER -> "Member"
      }
      Text(
        text = roleText,
        fontSize = 10.sp,
        color = if (member.role == MemberRole.OWNER) NeonAmber else TextSecondary
      )
    }
  }
}

@Composable
private fun ChatMessageItem(message: ChatMessage) {
  if (message.messageType == MessageType.SYSTEM) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 2.dp),
      contentAlignment = Alignment.Center
    ) {
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(12.dp))
          .background(CyberSurfaceElevated)
          .padding(horizontal = 10.dp, vertical = 4.dp)
      ) {
        Text(
          text = message.content,
          color = NeonCyan,
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium
        )
      }
    }
  } else {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Box(
        modifier = Modifier
          .size(28.dp)
          .clip(CircleShape)
          .background(NeonPurpleDark),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = message.authorName.take(1).uppercase(),
          color = Color.White,
          fontWeight = FontWeight.Bold,
          fontSize = 12.sp
        )
      }

      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(
            text = message.authorName,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = TextPrimary
          )
          Text(
            text = message.formattedTime,
            fontSize = 10.sp,
            color = TextMuted
          )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = message.content,
          fontSize = 13.sp,
          color = TextSecondary
        )
      }
    }
  }
}

@Composable
private fun InviteFriendsDialog(
  friends: List<com.example.model.Friend>,
  onInvite: (com.example.model.Friend) -> Unit,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = CyberSurface,
    title = {
      Text(
        text = "Invite Friends to Voice",
        color = TextPrimary,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        if (friends.isEmpty()) {
          Text("No online friends to invite.", color = TextSecondary, fontSize = 13.sp)
        } else {
          friends.forEach { friend ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(CyberSurfaceElevated)
                .clickable { onInvite(friend) }
                .padding(10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(friend.displayName, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(friend.status.label, color = NeonCyan, fontSize = 11.sp)
              }
              Button(
                onClick = { onInvite(friend) },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CyberBackground),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                modifier = Modifier.height(30.dp)
              ) {
                Text("Invite", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Close", color = NeonCyan)
      }
    }
  )
}
