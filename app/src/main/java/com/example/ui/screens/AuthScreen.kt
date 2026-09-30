package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SquadRepository
import com.example.ui.theme.*

@Composable
fun AuthScreen(
  onAuthenticated: () -> Unit
) {
  var usernameInput by remember { mutableStateOf("") }
  var displayNameInput by remember { mutableStateOf("") }
  var selectedGame by remember { mutableStateOf("PUBG Mobile") }
  val scrollState = rememberScrollState()

  val gameOptions = listOf(
    "PUBG Mobile",
    "Call of Duty Mobile",
    "Free Fire",
    "Roblox",
    "Brawl Stars"
  )

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(CyberBackground)
      .windowInsetsPadding(WindowInsets.safeDrawing)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Spacer(modifier = Modifier.height(24.dp))

      // Logo & Brand Header
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(80.dp)
          .clip(CircleShape)
          .background(
            Brush.linearGradient(listOf(NeonCyan, NeonPurple))
          )
          .border(2.dp, NeonCyan, CircleShape)
      ) {
        Icon(
          imageVector = Icons.Default.Headset,
          contentDescription = "SquadPing Logo",
          tint = CyberBackground,
          modifier = Modifier.size(44.dp)
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = "SquadPing",
        fontSize = 28.sp,
        fontWeight = FontWeight.Black,
        color = TextPrimary,
        letterSpacing = 1.sp
      )

      Text(
        text = "Ultra-Lightweight Gaming Voice Hangout",
        fontSize = 13.sp,
        color = NeonCyan,
        fontWeight = FontWeight.SemiBold
      )

      Spacer(modifier = Modifier.height(24.dp))

      // Feature highlights
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        FeaturePill(icon = Icons.Default.Bolt, title = "Low Latency", modifier = Modifier.weight(1f))
        FeaturePill(icon = Icons.Default.Games, title = "Game Audio", modifier = Modifier.weight(1f))
        FeaturePill(icon = Icons.Default.Shield, title = "Zero Bloat", modifier = Modifier.weight(1f))
      }

      Spacer(modifier = Modifier.height(28.dp))

      // Quick Join Card
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(CyberOutline, NeonPurple.copy(alpha = 0.5f)))),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          Text(
            text = "Create Gamer Identity",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
          Text(
            text = "Set your gaming alias to jump into squad voice channels",
            fontSize = 12.sp,
            color = TextSecondary
          )

          Spacer(modifier = Modifier.height(16.dp))

          OutlinedTextField(
            value = usernameInput,
            onValueChange = { usernameInput = it },
            label = { Text("Gamertag / Username") },
            placeholder = { Text("e.g. shadow_sniper") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = NeonCyan,
              unfocusedBorderColor = CyberOutline,
              focusedLabelColor = NeonCyan,
              cursorColor = NeonCyan,
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("username_input")
          )

          Spacer(modifier = Modifier.height(12.dp))

          OutlinedTextField(
            value = displayNameInput,
            onValueChange = { displayNameInput = it },
            label = { Text("Display Name") },
            placeholder = { Text("e.g. ShadowSniper") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = NeonCyan,
              unfocusedBorderColor = CyberOutline,
              focusedLabelColor = NeonCyan,
              cursorColor = NeonCyan,
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("display_name_input")
          )

          Spacer(modifier = Modifier.height(16.dp))

          Text(
            text = "Primary Game",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary
          )

          Spacer(modifier = Modifier.height(8.dp))

          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            gameOptions.chunked(2).forEach { rowGames ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                rowGames.forEach { game ->
                  val isSelected = selectedGame == game
                  Box(
                    modifier = Modifier
                      .weight(1f)
                      .clip(RoundedCornerShape(8.dp))
                      .background(if (isSelected) NeonCyanDark else CyberSurfaceVariant)
                      .border(
                        1.dp,
                        if (isSelected) NeonCyan else CyberOutline,
                        RoundedCornerShape(8.dp)
                      )
                      .clickable { selectedGame = game }
                      .padding(vertical = 10.dp, horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = game,
                      color = if (isSelected) Color.White else TextSecondary,
                      fontSize = 12.sp,
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                  }
                }
                if (rowGames.size == 1) {
                  Spacer(modifier = Modifier.weight(1f))
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(20.dp))

          Button(
            onClick = {
              val uname = usernameInput.ifEmpty { "gamer_" + (100..999).random() }
              val dname = displayNameInput.ifEmpty { uname }
              SquadRepository.login(uname, dname, selectedGame)
              onAuthenticated()
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = NeonCyan,
              contentColor = CyberBackground
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("login_button")
          ) {
            Text(
              text = "Enter SquadPing",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          OutlinedButton(
            onClick = {
              SquadRepository.quickGuestLogin()
              onAuthenticated()
            },
            colors = ButtonDefaults.outlinedButtonColors(
              contentColor = NeonPurple
            ),
            border = ButtonDefaults.outlinedButtonBorder.copy(
              brush = Brush.linearGradient(listOf(NeonPurple, NeonCyan))
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("quick_guest_button")
          ) {
            Text(
              text = "Quick 1-Tap Guest Access",
              fontSize = 14.sp,
              fontWeight = FontWeight.SemiBold
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}

@Composable
private fun FeaturePill(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(10.dp))
      .background(CyberSurfaceVariant)
      .border(0.5.dp, CyberOutline, RoundedCornerShape(10.dp))
      .padding(vertical = 10.dp, horizontal = 8.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Icon(
        imageVector = icon,
        contentDescription = title,
        tint = NeonCyan,
        modifier = Modifier.size(18.dp)
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = TextPrimary
      )
    }
  }
}
