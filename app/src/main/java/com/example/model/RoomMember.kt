package com.example.model

enum class MemberRole {
  OWNER,
  ADMIN,
  MEMBER
}

data class RoomMember(
  val userId: String,
  val displayName: String,
  val avatarSeed: String,
  val role: MemberRole = MemberRole.MEMBER,
  val isSpeaking: Boolean = false,
  val audioLevel: Float = 0f,
  val isMuted: Boolean = false,
  val isDeafened: Boolean = false,
  val volume: Float = 1.0f,
  val pingMs: Int = 22
)
