package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.audio.VoiceBridge
import com.example.data.SquadRepository
import com.example.model.MemberRole
import com.example.model.RoomMember
import com.example.ui.components.VolumeSliderContent
import com.example.ui.screens.CreateRoomContent
import com.example.ui.screens.VoiceRoomScreen
import com.example.ui.theme.MyApplicationTheme
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-xxhdpi")
class SquadRobolectricUiTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Before
  fun setUp() {
    VoiceBridge.isServiceEnabled = false
    SquadRepository.leaveRoom()
    SquadRepository.loginGuest("TestPlayer")
  }

  @After
  fun tearDown() {
    VoiceBridge.isServiceEnabled = true
    SquadRepository.leaveRoom()
  }

  @Test
  fun testRoomCreationFlow() {
    var createdName: String? = null
    var createdGame: String? = null

    composeTestRule.setContent {
      MyApplicationTheme {
        CreateRoomContent(
          onDismiss = {},
          onCreate = { name, desc, game, maxSlots, locked ->
            val created = SquadRepository.createRoom(name, desc, game, maxSlots, locked)
            createdName = created.name
            createdGame = created.activeGame
          }
        )
      }
    }

    composeTestRule.waitForIdle()

    // Input room details
    composeTestRule.onNodeWithTag("room_name_input")
      .performTextInput("Conqueror Push")
    composeTestRule.onNodeWithTag("room_desc_input")
      .performTextInput("Mic on, hot drops only")

    // Confirm room creation
    composeTestRule.onNodeWithTag("confirm_create_room_btn")
      .performClick()
    composeTestRule.waitForIdle()

    // Assert room created with expected attributes
    assertNotNull("Created room name should not be null", createdName)
    assertEquals("Conqueror Push", createdName)
    assertEquals("PUBG Mobile", createdGame)
  }

  @Test
  fun testTacticalCalloutsFlow() {
    val room = SquadRepository.createRoom(
      name = "Callout Testing Room",
      description = "Testing tactical chips",
      game = "PUBG Mobile",
      maxParticipants = 4,
      isLocked = false
    )
    SquadRepository.joinRoom(room.id)

    composeTestRule.setContent {
      MyApplicationTheme {
        VoiceRoomScreen(
          roomId = room.id,
          onLeaveRoom = {}
        )
      }
    }

    composeTestRule.waitForIdle()

    // Find and click the "Enemy Spotted" tactical callout chip
    composeTestRule.onNodeWithTag("tactical_callout_Enemy_Spotted")
      .assertIsDisplayed()
      .performClick()
    composeTestRule.waitForIdle()

    // Verify the tactical message appears in the chat message log
    val messages = SquadRepository.chatMessages.value
    assertTrue("Chat should contain tactical callout", messages.any { it.content.contains("Enemy Spotted") })
  }

  @Test
  fun testMicAndDeafenToggles() {
    val room = SquadRepository.createRoom(
      name = "Audio Controls Room",
      description = "Mute/Deafen testing",
      game = "Call of Duty Mobile",
      maxParticipants = 4,
      isLocked = false
    )
    SquadRepository.joinRoom(room.id)

    composeTestRule.setContent {
      MyApplicationTheme {
        VoiceRoomScreen(
          roomId = room.id,
          onLeaveRoom = {}
        )
      }
    }

    composeTestRule.waitForIdle()

    val initialMic = SquadRepository.audioSettings.value.micMuted
    composeTestRule.onNodeWithTag("toggle_mic_button").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    assertNotEquals(initialMic, SquadRepository.audioSettings.value.micMuted)

    val initialDeafen = SquadRepository.audioSettings.value.deafened
    composeTestRule.onNodeWithTag("toggle_deafen_button").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    assertNotEquals(initialDeafen, SquadRepository.audioSettings.value.deafened)
  }

  @Test
  fun testVolumeDialogInteraction() {
    var changedVolume: Float? = null
    var dismissed = false

    val testMember = RoomMember(
      userId = "usr_tester_1",
      displayName = "TesterOne",
      avatarSeed = "test1",
      role = MemberRole.MEMBER,
      isSpeaking = false,
      audioLevel = 0.5f,
      isMuted = false,
      volume = 1.0f,
      pingMs = 20
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        VolumeSliderContent(
          member = testMember,
          isCurrentUserOwner = true,
          onVolumeChange = { changedVolume = it },
          onDismiss = { dismissed = true }
        )
      }
    }

    composeTestRule.waitForIdle()

    // Verify dialog appears
    composeTestRule.onNodeWithTag("volume_dialog_usr_tester_1")
      .assertIsDisplayed()

    // Click mute toggle inside volume dialog
    composeTestRule.onNodeWithTag("volume_mute_toggle_btn")
      .performClick()
    composeTestRule.waitForIdle()

    assertEquals(0.0f, changedVolume)

    // Dismiss dialog
    composeTestRule.onNodeWithTag("volume_dialog_done_btn")
      .performClick()
    composeTestRule.waitForIdle()

    assertTrue("Dialog should be dismissed", dismissed)
  }
}
