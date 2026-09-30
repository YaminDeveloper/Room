package com.example.model

enum class MessageType {
  USER,
  SYSTEM
}

data class ChatMessage(
  val id: String,
  val roomId: String,
  val authorId: String?,
  val authorName: String,
  val avatarSeed: String = "",
  val content: String,
  val messageType: MessageType = MessageType.USER,
  val timestamp: Long = System.currentTimeMillis(),
  val formattedTime: String = "Just now"
)
