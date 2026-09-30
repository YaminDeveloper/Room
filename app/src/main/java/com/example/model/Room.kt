package com.example.model

enum class RoomType {
  PUBLIC,
  PRIVATE
}

data class Room(
  val id: String,
  val name: String,
  val description: String,
  val type: RoomType = RoomType.PUBLIC,
  val activeGame: String,
  val ownerId: String,
  val ownerName: String,
  val participantCount: Int = 1,
  val maxParticipants: Int = 4,
  val isLocked: Boolean = false,
  val pingMs: Int = 24
)
