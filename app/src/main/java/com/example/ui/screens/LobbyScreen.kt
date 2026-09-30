package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.window.Dialog
import com.example.data.SquadRepository
import com.example.model.Friend
import com.example.model.PresenceStatus
import com.example.model.Room
import com.example.ui.components.GameBadge
import com.example.ui.theme.*

@Composable
fun LobbyScreen(
  onJoinRoom: (String) -> Unit,
  onOpenSettings: () -> Unit
) {
  val rooms by SquadRepository.rooms.collectAsState()
  val friends by SquadRepository.friends.collectAsState()
  val currentUser by SquadRepository.currentUser.collectAsState()
  val selectedFilter by SquadRepository.selectedGameFilter.collectAsState()

  var showCreateRoomDialog by remember { mutableStateOf(false) }

  val filteredRooms = remember(rooms, selectedFilter) {
    if (selectedFilter == "All") rooms else rooms.filter { it.activeGame == selectedFilter }
  }

  val gamesList = listOf("All", "PUBG Mobile", "Call of Duty Mobile", "Free Fire", "Roblox", "Brawl Stars")

  Scaffold(
    containerColor = CyberBackground,
    floatingActionButton = {
      FloatingActionButton(
        onClick = { showCreateRoomDialog = true },
        containerColor = NeonCyan,
        contentColor = CyberBackground,
        shape = CircleShape,
        modifier = Modifier.testTag("create_room_fab")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(imageVector = Icons.Default.Add, contentDescription = "Create Room")
          Spacer(modifier = Modifier.width(6.dp))
          Text(text = "New Room", fontWeight = FontWeight.Bold)
        }
      }
    }
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      item {
        Spacer(modifier = Modifier.height(8.dp))
        // Top Bar
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(StatusOnline)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "SquadPing Lobby",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary
              )
            }
            Text(
              text = "Live WebRTC Audio • 18ms Ping",
              fontSize = 12.sp,
              color = NeonCyan,
              fontWeight = FontWeight.Medium
            )
          }

          IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
              .size(42.dp)
              .clip(CircleShape)
              .background(CyberSurfaceVariant)
              .testTag("profile_button")
          ) {
            Icon(
              imageVector = Icons.Default.Person,
              contentDescription = "Profile & Settings",
              tint = NeonCyan
            )
          }
        }
      }

      // Online Friends Strip
      item {
        Column {
          Text(
            text = "Active Squad Friends (${friends.count { !it.isPendingRequest }})",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary
          )
          Spacer(modifier = Modifier.height(8.dp))
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            items(friends.filter { !it.isPendingRequest }) { friend ->
              FriendMiniCard(
                friend = friend,
                onJoinRoom = { roomId ->
                  SquadRepository.joinRoom(roomId)
                  onJoinRoom(roomId)
                }
              )
            }
          }
        }
      }

      // Quick Match Banner
      item {
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = CyberSurfaceElevated),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.FlashOn,
                  contentDescription = null,
                  tint = NeonCyan,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Instant Squad Match",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold,
                  color = TextPrimary
                )
              }
              Text(
                text = "Jump into the lowest latency open game room",
                fontSize = 12.sp,
                color = TextSecondary
              )
            }
            Button(
              onClick = {
                val joinable = rooms.firstOrNull { it.participantCount < it.maxParticipants && !it.isLocked }
                if (joinable != null) {
                  SquadRepository.joinRoom(joinable.id)
                  onJoinRoom(joinable.id)
                }
              },
              colors = ButtonDefaults.buttonColors(
                containerColor = NeonPurple,
                contentColor = Color.White
              ),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.testTag("quick_match_button")
            ) {
              Text("Quick Join", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
          }
        }
      }

      // Game Filter Chips
      item {
        val filterScrollState = rememberScrollState()
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(filterScrollState),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          gamesList.forEach { game ->
            val isSelected = selectedFilter == game
            FilterChip(
              selected = isSelected,
              onClick = { SquadRepository.setGameFilter(game) },
              label = {
                Text(
                  text = game,
                  fontSize = 12.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = NeonCyanDark,
                selectedLabelColor = Color.White,
                containerColor = CyberSurfaceVariant,
                labelColor = TextSecondary
              ),
              border = FilterChipDefaults.filterChipBorder(
                borderColor = if (isSelected) NeonCyan else CyberOutline,
                selectedBorderColor = NeonCyan,
                enabled = true,
                selected = isSelected
              )
            )
          }
        }
      }

      // Rooms Header
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Active Voice Rooms (${filteredRooms.size})",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
          Text(
            text = "Audio Only • Opus",
            fontSize = 11.sp,
            color = TextSecondary
          )
        }
      }

      // Rooms List
      items(filteredRooms) { room ->
        RoomCard(
          room = room,
          onJoin = {
            SquadRepository.joinRoom(room.id)
            onJoinRoom(room.id)
          }
        )
      }

      item {
        Spacer(modifier = Modifier.height(72.dp))
      }
    }
  }

  if (showCreateRoomDialog) {
    CreateRoomDialog(
      onDismiss = { showCreateRoomDialog = false },
      onCreate = { name, desc, game, maxSlots, locked ->
        val created = SquadRepository.createRoom(name, desc, game, maxSlots, locked)
        showCreateRoomDialog = false
        onJoinRoom(created.id)
      }
    )
  }
}

@Composable
private fun FriendMiniCard(
  friend: Friend,
  onJoinRoom: (String) -> Unit
) {
  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = CyberSurface),
    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CyberOutline)),
    modifier = Modifier.width(135.dp)
  ) {
    Column(
      modifier = Modifier.padding(10.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Box(contentAlignment = Alignment.BottomEnd) {
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(NeonCyanDark),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = friend.displayName.take(2).uppercase(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
          )
        }
        val statusColor = when (friend.status) {
          PresenceStatus.IN_ROOM -> NeonPurple
          PresenceStatus.GAMING -> NeonCyan
          PresenceStatus.ONLINE -> StatusOnline
          else -> StatusOffline
        }
        Box(
          modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(statusColor)
            .border(1.dp, CyberSurface, CircleShape)
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = friend.displayName,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary,
        maxLines = 1
      )

      Text(
        text = friend.currentGame ?: friend.status.label,
        fontSize = 10.sp,
        color = if (friend.activeRoomId != null) NeonCyan else TextSecondary,
        maxLines = 1
      )

      if (friend.activeRoomId != null) {
        Spacer(modifier = Modifier.height(6.dp))
        Button(
          onClick = { onJoinRoom(friend.activeRoomId) },
          colors = ButtonDefaults.buttonColors(
            containerColor = NeonPurple,
            contentColor = Color.White
          ),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
          shape = RoundedCornerShape(6.dp),
          modifier = Modifier.fillMaxWidth().height(26.dp)
        ) {
          Text("Join Room", fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun RoomCard(
  room: Room,
  onJoin: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = CyberSurface),
    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CyberOutline)),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onJoin() }
      .testTag("room_card_${room.id}")
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        GameBadge(gameTitle = room.activeGame)

        Row(verticalAlignment = Alignment.CenterVertically) {
          if (room.isLocked) {
            Icon(
              imageVector = Icons.Default.Lock,
              contentDescription = "Locked Room",
              tint = NeonAmber,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
          }
          Text(
            text = "${room.pingMs}ms",
            fontSize = 11.sp,
            color = NeonGreen,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.width(8.dp))
          Box(
            modifier = Modifier
              .background(CyberSurfaceElevated, RoundedCornerShape(6.dp))
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(
              text = "${room.participantCount}/${room.maxParticipants} Squad",
              fontSize = 11.sp,
              color = TextPrimary,
              fontWeight = FontWeight.SemiBold
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = room.name,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary
      )

      Text(
        text = room.description,
        fontSize = 12.sp,
        color = TextSecondary,
        maxLines = 2
      )

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "Host",
            tint = TextMuted,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Host: ${room.ownerName}",
            fontSize = 12.sp,
            color = TextSecondary
          )
        }

        Button(
          onClick = onJoin,
          colors = ButtonDefaults.buttonColors(
            containerColor = NeonCyan,
            contentColor = CyberBackground
          ),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.height(34.dp).testTag("join_room_btn_${room.id}")
        ) {
          Icon(
            imageVector = Icons.Default.Headset,
            contentDescription = "Join Voice",
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("Join Voice", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
      }
    }
  }
}

@Composable
fun CreateRoomDialog(
  onDismiss: () -> Unit,
  onCreate: (name: String, desc: String, game: String, maxSlots: Int, locked: Boolean) -> Unit
) {
  Dialog(onDismissRequest = onDismiss) {
    CreateRoomContent(onDismiss = onDismiss, onCreate = onCreate)
  }
}

@Composable
fun CreateRoomContent(
  onDismiss: () -> Unit,
  onCreate: (name: String, desc: String, game: String, maxSlots: Int, locked: Boolean) -> Unit
) {
  var name by remember { mutableStateOf("") }
  var desc by remember { mutableStateOf("") }
  var selectedGame by remember { mutableStateOf("PUBG Mobile") }
  var maxParticipants by remember { mutableStateOf(4) }
  var isLocked by remember { mutableStateOf(false) }

  val games = listOf("PUBG Mobile", "Call of Duty Mobile", "Free Fire", "Roblox", "Brawl Stars")

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = CyberSurface),
    modifier = Modifier
      .fillMaxWidth()
      .padding(16.dp)
      .testTag("create_room_dialog")
  ) {
      Column(
        modifier = Modifier
          .padding(20.dp)
          .verticalScroll(rememberScrollState())
      ) {
        Text(
          text = "Create Voice Hangout",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = TextPrimary
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Room Name") },
          placeholder = { Text("e.g. Conqueror Push") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("room_name_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = desc,
          onValueChange = { desc = it },
          label = { Text("Description / Rules") },
          placeholder = { Text("e.g. Hot drops, mic required") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("room_desc_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = "Target Game",
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          color = TextSecondary
        )

        Spacer(modifier = Modifier.height(6.dp))

        val scroll = rememberScrollState()
        Row(
          modifier = Modifier.horizontalScroll(scroll),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          games.forEach { g ->
            val sel = selectedGame == g
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (sel) NeonCyanDark else CyberSurfaceVariant)
                .clickable { selectedGame = g }
                .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
              Text(
                text = g,
                fontSize = 11.sp,
                color = if (sel) Color.White else TextSecondary,
                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Squad Max Slots: $maxParticipants", fontSize = 13.sp, color = TextPrimary)
          Row {
            IconButton(
              onClick = { if (maxParticipants > 2) maxParticipants-- },
              modifier = Modifier.size(32.dp)
            ) {
              Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = NeonCyan)
            }
            IconButton(
              onClick = { if (maxParticipants < 8) maxParticipants++ },
              modifier = Modifier.size(32.dp)
            ) {
              Icon(Icons.Default.Add, contentDescription = "Increase", tint = NeonCyan)
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Lock Room (Invite Only)", fontSize = 13.sp, color = TextPrimary)
          Switch(
            checked = isLocked,
            onCheckedChange = { isLocked = it },
            colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
            Text("Cancel")
          }
          Button(
            onClick = {
              onCreate(name, desc, selectedGame, maxParticipants, isLocked)
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = NeonCyan,
              contentColor = CyberBackground
            ),
            modifier = Modifier.testTag("confirm_create_room_btn").weight(1f)
          ) {
            Text("Create", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
}
