package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.data.SquadRepository
import com.example.model.Friend
import com.example.model.PresenceStatus
import com.example.ui.theme.*

@Composable
fun FriendsScreen(
  onJoinRoom: (String) -> Unit
) {
  val friends by SquadRepository.friends.collectAsState()
  var selectedTab by remember { mutableStateOf(0) }
  var addFriendInput by remember { mutableStateOf("") }
  var showAddFriendDialog by remember { mutableStateOf(false) }

  val tabs = listOf(
    "Friends (${friends.count { !it.isPendingRequest && !it.isBlocked }})",
    "Requests (${friends.count { it.isPendingRequest }})",
    "Blocked (${friends.count { it.isBlocked }})"
  )

  Scaffold(
    containerColor = CyberBackground,
    topBar = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(CyberSurface)
          .padding(horizontal = 16.dp, vertical = 12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Squad Friends",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = TextPrimary
          )
          Button(
            onClick = { showAddFriendDialog = true },
            colors = ButtonDefaults.buttonColors(
              containerColor = NeonCyan,
              contentColor = CyberBackground
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.height(34.dp).testTag("add_friend_btn")
          ) {
            Icon(Icons.Default.PersonAdd, contentDescription = "Add Friend", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add", fontWeight = FontWeight.Bold, fontSize = 12.sp)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        TabRow(
          selectedTabIndex = selectedTab,
          containerColor = CyberSurface,
          contentColor = NeonCyan,
          divider = {}
        ) {
          tabs.forEachIndexed { index, title ->
            Tab(
              selected = selectedTab == index,
              onClick = { selectedTab = index },
              text = {
                Text(
                  text = title,
                  fontSize = 12.sp,
                  fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                  color = if (selectedTab == index) NeonCyan else TextSecondary
                )
              }
            )
          }
        }
      }
    }
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      when (selectedTab) {
        0 -> {
          val activeFriends = friends.filter { !it.isPendingRequest && !it.isBlocked }
          if (activeFriends.isEmpty()) {
            item {
              EmptyStateCard(text = "No friends added yet. Tap 'Add' to connect with teammates!")
            }
          } else {
            items(activeFriends) { friend ->
              FriendRowItem(friend = friend, onJoinRoom = onJoinRoom)
            }
          }
        }
        1 -> {
          val requests = friends.filter { it.isPendingRequest }
          if (requests.isEmpty()) {
            item {
              EmptyStateCard(text = "No pending friend requests.")
            }
          } else {
            items(requests) { request ->
              FriendRequestItem(
                friend = request,
                onAccept = { SquadRepository.acceptFriendRequest(request.id) },
                onReject = { SquadRepository.rejectFriendRequest(request.id) }
              )
            }
          }
        }
        2 -> {
          val blocked = friends.filter { it.isBlocked }
          if (blocked.isEmpty()) {
            item {
              EmptyStateCard(text = "No blocked users.")
            }
          } else {
            items(blocked) { user ->
              Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(user.displayName, color = TextPrimary, fontWeight = FontWeight.Bold)
                  TextButton(onClick = { SquadRepository.acceptFriendRequest(user.id) }) {
                    Text("Unblock", color = NeonCyan)
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  if (showAddFriendDialog) {
    AlertDialog(
      onDismissRequest = { showAddFriendDialog = false },
      containerColor = CyberSurface,
      title = { Text("Add Gaming Friend", color = TextPrimary, fontWeight = FontWeight.Bold) },
      text = {
        Column {
          Text("Enter friend's Gamertag or Username:", color = TextSecondary, fontSize = 12.sp)
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = addFriendInput,
            onValueChange = { addFriendInput = it },
            placeholder = { Text("e.g. apex_sniper") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("add_friend_input")
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (addFriendInput.isNotBlank()) {
              SquadRepository.addFriend(addFriendInput)
              addFriendInput = ""
              showAddFriendDialog = false
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CyberBackground)
        ) {
          Text("Add Friend", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddFriendDialog = false }) {
          Text("Cancel", color = TextSecondary)
        }
      }
    )
  }
}

@Composable
private fun FriendRowItem(
  friend: Friend,
  onJoinRoom: (String) -> Unit
) {
  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = CyberSurface),
    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CyberOutline)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(contentAlignment = Alignment.BottomEnd) {
          Box(
            modifier = Modifier
              .size(42.dp)
              .clip(CircleShape)
              .background(NeonCyanDark),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = friend.displayName.take(2).uppercase(),
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp
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
              .size(11.dp)
              .clip(CircleShape)
              .background(statusColor)
              .border(1.5.dp, CyberSurface, CircleShape)
          )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
          Text(
            text = friend.displayName,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
          val subtext = when {
            friend.activeRoomName != null -> "In Voice: ${friend.activeRoomName}"
            friend.currentGame != null -> "Playing ${friend.currentGame}"
            else -> friend.status.label
          }
          Text(
            text = subtext,
            fontSize = 11.sp,
            color = if (friend.activeRoomId != null) NeonCyan else TextSecondary
          )
        }
      }

      if (friend.activeRoomId != null) {
        Button(
          onClick = {
            SquadRepository.joinRoom(friend.activeRoomId)
            onJoinRoom(friend.activeRoomId)
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = NeonPurple,
            contentColor = Color.White
          ),
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
          modifier = Modifier.height(32.dp)
        ) {
          Icon(Icons.Default.Headset, contentDescription = "Join", modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Join Voice", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun FriendRequestItem(
  friend: Friend,
  onAccept: () -> Unit,
  onReject: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = CyberSurface),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(NeonPurpleDark),
          contentAlignment = Alignment.Center
        ) {
          Text(friend.displayName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(friend.displayName, fontWeight = FontWeight.Bold, color = TextPrimary)
          Text("Wants to join squad", color = TextSecondary, fontSize = 11.sp)
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedButton(
          onClick = onReject,
          shape = RoundedCornerShape(6.dp),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
          modifier = Modifier.height(30.dp)
        ) {
          Text("Decline", fontSize = 11.sp)
        }
        Button(
          onClick = onAccept,
          colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CyberBackground),
          shape = RoundedCornerShape(6.dp),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
          modifier = Modifier.height(30.dp)
        ) {
          Text("Accept", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun EmptyStateCard(text: String) {
  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = CyberSurfaceVariant),
    modifier = Modifier.fillMaxWidth()
  ) {
    Box(
      modifier = Modifier.padding(24.dp).fillMaxWidth(),
      contentAlignment = Alignment.Center
    ) {
      Text(text = text, color = TextSecondary, fontSize = 13.sp)
    }
  }
}
