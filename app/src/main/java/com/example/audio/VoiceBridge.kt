package com.example.audio

import android.content.Context
import com.example.model.Room

object VoiceBridge {
  var isServiceEnabled: Boolean = true

  fun startVoiceSession(context: Context, room: Room) {
    if (!isServiceEnabled) return
    VoiceForegroundService.start(
      context = context.applicationContext,
      roomId = room.id,
      roomName = room.name,
      activeGame = room.activeGame,
      participantCount = room.participantCount
    )
  }

  fun stopVoiceSession(context: Context) {
    if (!isServiceEnabled) return
    VoiceForegroundService.stop(context.applicationContext)
  }
}
