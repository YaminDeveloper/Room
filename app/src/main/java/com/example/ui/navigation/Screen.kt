package com.example.ui.navigation

sealed class Screen {
  object Auth : Screen()
  object Lobby : Screen()
  data class VoiceRoom(val roomId: String) : Screen()
  object Friends : Screen()
  object Settings : Screen()
}
