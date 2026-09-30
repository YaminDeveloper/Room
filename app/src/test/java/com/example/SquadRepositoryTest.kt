package com.example

import com.example.data.SquadRepository
import org.junit.Assert.*
import org.junit.Test

class SquadRepositoryTest {

  @Test
  fun testUserLoginAndQuickGuest() {
    SquadRepository.login("tactical_ace", "TacticalAce", "PUBG Mobile")
    val user = SquadRepository.currentUser.value
    assertNotNull(user)
    assertEquals("tactical_ace", user?.username)
    assertEquals("TacticalAce", user?.displayName)
  }

  @Test
  fun testJoinAndLeaveVoiceRoom() {
    SquadRepository.login("ghost_pilot", "GhostPilot", "Call of Duty Mobile")
    val rooms = SquadRepository.rooms.value
    assertTrue(rooms.isNotEmpty())

    val targetRoom = rooms.first()
    SquadRepository.joinRoom(targetRoom.id)

    val currentRoom = SquadRepository.currentRoom.value
    assertNotNull(currentRoom)
    assertEquals(targetRoom.id, currentRoom?.id)

    val members = SquadRepository.roomMembers.value
    assertTrue(members.isNotEmpty())

    // Test mic toggle
    val initialMute = SquadRepository.audioSettings.value.micMuted
    SquadRepository.toggleMic()
    assertEquals(!initialMute, SquadRepository.audioSettings.value.micMuted)

    // Test individual participant volume adjustment
    val targetMember = members.first()
    SquadRepository.setParticipantVolume(targetMember.userId, 1.5f)
    val updatedMember = SquadRepository.roomMembers.value.first { it.userId == targetMember.userId }
    assertEquals(1.5f, updatedMember.volume, 0.01f)

    // Test sending chat message
    val initialMsgCount = SquadRepository.chatMessages.value.size
    SquadRepository.sendChatMessage("Watch safe zone flank!")
    val newMessages = SquadRepository.chatMessages.value
    assertEquals(initialMsgCount + 1, newMessages.size)
    assertEquals("Watch safe zone flank!", newMessages.last().content)

    // Leave room
    SquadRepository.leaveRoom()
    assertNull(SquadRepository.currentRoom.value)
    assertTrue(SquadRepository.roomMembers.value.isEmpty())
  }
}
