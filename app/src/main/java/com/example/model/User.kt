package com.example.model

enum class PresenceStatus(val label: String) {
  ONLINE("Online"),
  IDLE("Idle"),
  IN_ROOM("In Voice Room"),
  GAMING("In-Game"),
  OFFLINE("Offline")
}

data class User(
  val id: String,
  val username: String,
  val displayName: String,
  val avatarSeed: String,
  val bio: String = "",
  val status: PresenceStatus = PresenceStatus.ONLINE,
  val currentGame: String? = null,
  val favoriteGame: String = "PUBG Mobile"
)
