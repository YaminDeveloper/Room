package com.example.model

data class Friend(
  val id: String,
  val username: String,
  val displayName: String,
  val avatarSeed: String,
  val status: PresenceStatus = PresenceStatus.ONLINE,
  val currentGame: String? = null,
  val activeRoomId: String? = null,
  val activeRoomName: String? = null,
  val isPendingRequest: Boolean = false,
  val isBlocked: Boolean = false
)
