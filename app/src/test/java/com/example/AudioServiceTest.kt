package com.example

import com.example.audio.VoiceForegroundService
import com.example.audio.VoiceNotificationManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AudioServiceTest {

  @Test
  fun testServiceActionConstants() {
    assertEquals("com.example.action.START_VOICE", VoiceForegroundService.ACTION_START_VOICE)
    assertEquals("com.example.action.STOP_VOICE", VoiceForegroundService.ACTION_STOP_VOICE)
    assertEquals("com.example.action.TOGGLE_MUTE", VoiceNotificationManager.ACTION_TOGGLE_MUTE)
    assertEquals("com.example.action.LEAVE_ROOM", VoiceNotificationManager.ACTION_LEAVE_ROOM)
  }

  @Test
  fun testServiceExtras() {
    assertEquals("extra_room_id", VoiceForegroundService.EXTRA_ROOM_ID)
    assertEquals("extra_room_name", VoiceForegroundService.EXTRA_ROOM_NAME)
    assertEquals("extra_active_game", VoiceForegroundService.EXTRA_ACTIVE_GAME)
    assertEquals("extra_participant_count", VoiceForegroundService.EXTRA_PARTICIPANT_COUNT)
  }
}
