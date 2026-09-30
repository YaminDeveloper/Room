package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SquadRepository
import com.example.ui.navigation.Screen
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.util.PerformanceProfiler

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    PerformanceProfiler.recordAppLaunch()
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        SquadPingApp()
      }
    }
  }
}

@Composable
fun SquadPingApp() {
  val currentUser by SquadRepository.currentUser.collectAsState()
  val currentRoom by SquadRepository.currentRoom.collectAsState()

  LaunchedEffect(Unit) {
    PerformanceProfiler.recordFirstFrame()
  }

  var currentScreen by remember { mutableStateOf<Screen>(Screen.Lobby) }

  // Synchronize screen state if a room is joined or left
  LaunchedEffect(currentRoom) {
    val room = currentRoom
    if (room != null) {
      currentScreen = Screen.VoiceRoom(room.id)
    } else if (currentScreen is Screen.VoiceRoom) {
      currentScreen = Screen.Lobby
    }
  }

  // Handle system back navigation across secondary tabs
  BackHandler(enabled = currentScreen !is Screen.Lobby && currentScreen !is Screen.Auth) {
    if (currentScreen is Screen.VoiceRoom) {
      SquadRepository.leaveRoom()
      currentScreen = Screen.Lobby
    } else {
      currentScreen = Screen.Lobby
    }
  }

  if (currentUser == null) {
    AuthScreen(
      onAuthenticated = {
        currentScreen = Screen.Lobby
      }
    )
  } else {
    Scaffold(
      containerColor = CyberBackground,
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      bottomBar = {
        // Only show bottom navigation bar when not inside an active voice room
        if (currentScreen !is Screen.VoiceRoom) {
          NavigationBar(
            containerColor = CyberSurface,
            tonalElevation = 0.dp,
            modifier = Modifier
              .border(0.5.dp, CyberOutline, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
              .windowInsetsPadding(WindowInsets.navigationBars)
              .testTag("bottom_nav_bar")
          ) {
            NavigationBarItem(
              selected = currentScreen is Screen.Lobby,
              onClick = { currentScreen = Screen.Lobby },
              icon = {
                Icon(
                  imageVector = Icons.Default.SportsEsports,
                  contentDescription = "Lobby"
                )
              },
              label = {
                Text(
                  "Lobby",
                  fontSize = 11.sp,
                  fontWeight = if (currentScreen is Screen.Lobby) FontWeight.Bold else FontWeight.Normal
                )
              },
              colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CyberBackground,
                selectedTextColor = NeonCyan,
                indicatorColor = NeonCyan,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
              ),
              modifier = Modifier.testTag("nav_lobby")
            )

            NavigationBarItem(
              selected = currentScreen is Screen.Friends,
              onClick = { currentScreen = Screen.Friends },
              icon = {
                Icon(
                  imageVector = Icons.Default.People,
                  contentDescription = "Friends"
                )
              },
              label = {
                Text(
                  "Friends",
                  fontSize = 11.sp,
                  fontWeight = if (currentScreen is Screen.Friends) FontWeight.Bold else FontWeight.Normal
                )
              },
              colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CyberBackground,
                selectedTextColor = NeonCyan,
                indicatorColor = NeonCyan,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
              ),
              modifier = Modifier.testTag("nav_friends")
            )

            NavigationBarItem(
              selected = currentScreen is Screen.Settings,
              onClick = { currentScreen = Screen.Settings },
              icon = {
                Icon(
                  imageVector = Icons.Default.Tune,
                  contentDescription = "Audio & Profile"
                )
              },
              label = {
                Text(
                  "Audio",
                  fontSize = 11.sp,
                  fontWeight = if (currentScreen is Screen.Settings) FontWeight.Bold else FontWeight.Normal
                )
              },
              colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CyberBackground,
                selectedTextColor = NeonCyan,
                indicatorColor = NeonCyan,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
              ),
              modifier = Modifier.testTag("nav_settings")
            )
          }
        }
      }
    ) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(CyberBackground)
          .padding(innerPadding)
          .windowInsetsPadding(WindowInsets.statusBars)
      ) {
        when (val screen = currentScreen) {
          is Screen.Lobby -> {
            LobbyScreen(
              onJoinRoom = { roomId ->
                currentScreen = Screen.VoiceRoom(roomId)
              },
              onOpenSettings = {
                currentScreen = Screen.Settings
              }
            )
          }
          is Screen.VoiceRoom -> {
            VoiceRoomScreen(
              roomId = screen.roomId,
              onLeaveRoom = {
                currentScreen = Screen.Lobby
              }
            )
          }
          is Screen.Friends -> {
            FriendsScreen(
              onJoinRoom = { roomId ->
                currentScreen = Screen.VoiceRoom(roomId)
              }
            )
          }
          is Screen.Settings -> {
            ProfileSettingsScreen(
              onBack = { currentScreen = Screen.Lobby },
              onLogout = { currentScreen = Screen.Auth }
            )
          }
          is Screen.Auth -> {
            AuthScreen(onAuthenticated = { currentScreen = Screen.Lobby })
          }
        }
      }
    }
  }
}
