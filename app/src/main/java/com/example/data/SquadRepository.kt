package com.example.data

import com.example.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

object SquadRepository {
  private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
  private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

  private val _currentUser = MutableStateFlow<User?>(
    User(
      id = "usr_pilot",
      username = "shadow_reaper",
      displayName = "ShadowReaper",
      avatarSeed = "shadow",
      bio = "CODM Sniper & PUBG Hot-Drop Squad Leader. Aggressive push only.",
      status = PresenceStatus.ONLINE,
      currentGame = null,
      favoriteGame = "PUBG Mobile"
    )
  )
  val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

  private val _currentRoom = MutableStateFlow<Room?>(null)
  val currentRoom: StateFlow<Room?> = _currentRoom.asStateFlow()

  private val _roomMembers = MutableStateFlow<List<RoomMember>>(emptyList())
  val roomMembers: StateFlow<List<RoomMember>> = _roomMembers.asStateFlow()

  private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
  val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

  private val _typingUsers = MutableStateFlow<List<String>>(emptyList())
  val typingUsers: StateFlow<List<String>> = _typingUsers.asStateFlow()

  private val _selectedGameFilter = MutableStateFlow("All")
  val selectedGameFilter: StateFlow<String> = _selectedGameFilter.asStateFlow()

  private val _audioSettings = MutableStateFlow(AudioSettings())
  val audioSettings: StateFlow<AudioSettings> = _audioSettings.asStateFlow()

  private val initialRooms = listOf(
    Room(
      id = "room_pubg_1",
      name = "PUBG Conqueror Push",
      description = "Erangel / Pochinki drops. Active mic required. No toxic.",
      type = RoomType.PUBLIC,
      activeGame = "PUBG Mobile",
      ownerId = "usr_viper",
      ownerName = "ViperStrike",
      participantCount = 3,
      maxParticipants = 4,
      isLocked = false,
      pingMs = 18
    ),
    Room(
      id = "room_codm_1",
      name = "CODM Legendary Ranked",
      description = "Hardpoint & S&D comms. Callouts only.",
      type = RoomType.PUBLIC,
      activeGame = "Call of Duty Mobile",
      ownerId = "usr_ghost",
      ownerName = "GhostOps",
      participantCount = 4,
      maxParticipants = 5,
      isLocked = false,
      pingMs = 24
    ),
    Room(
      id = "room_ff_1",
      name = "Free Fire Squad Rush",
      description = "Bermuda ranked grind. Quick revive tactics.",
      type = RoomType.PUBLIC,
      activeGame = "Free Fire",
      ownerId = "usr_blaze",
      ownerName = "BlazeX",
      participantCount = 2,
      maxParticipants = 4,
      isLocked = false,
      pingMs = 31
    ),
    Room(
      id = "room_roblox_1",
      name = "Roblox Chill Hangout",
      description = "BedWars & Arsenal squad matches.",
      type = RoomType.PUBLIC,
      activeGame = "Roblox",
      ownerId = "usr_pixel",
      ownerName = "PixelKnight",
      participantCount = 3,
      maxParticipants = 6,
      isLocked = false,
      pingMs = 15
    ),
    Room(
      id = "room_brawl_1",
      name = "Brawl Stars 3v3 Trophy Push",
      description = "Brawl Ball & Knockout strategy voice room.",
      type = RoomType.PUBLIC,
      activeGame = "Brawl Stars",
      ownerId = "usr_colt",
      ownerName = "ColtMain",
      participantCount = 2,
      maxParticipants = 3,
      isLocked = true,
      pingMs = 22
    )
  )

  private val _rooms = MutableStateFlow<List<Room>>(initialRooms)
  val rooms: StateFlow<List<Room>> = _rooms.asStateFlow()

  private val initialFriends = listOf(
    Friend(
      id = "usr_viper",
      username = "viper_strike",
      displayName = "ViperStrike",
      avatarSeed = "viper",
      status = PresenceStatus.IN_ROOM,
      currentGame = "PUBG Mobile",
      activeRoomId = "room_pubg_1",
      activeRoomName = "PUBG Conqueror Push"
    ),
    Friend(
      id = "usr_ghost",
      username = "ghost_ops",
      displayName = "GhostOps",
      avatarSeed = "ghost",
      status = PresenceStatus.GAMING,
      currentGame = "Call of Duty Mobile",
      activeRoomId = "room_codm_1",
      activeRoomName = "CODM Legendary Ranked"
    ),
    Friend(
      id = "usr_kestrel",
      username = "kestrel_pro",
      displayName = "Kestrel",
      avatarSeed = "kestrel",
      status = PresenceStatus.ONLINE,
      currentGame = null
    ),
    Friend(
      id = "usr_blaze",
      username = "blaze_x",
      displayName = "BlazeX",
      avatarSeed = "blaze",
      status = PresenceStatus.IN_ROOM,
      currentGame = "Free Fire",
      activeRoomId = "room_ff_1",
      activeRoomName = "Free Fire Squad Rush"
    ),
    Friend(
      id = "usr_nova",
      username = "nova_queen",
      displayName = "NovaQueen",
      avatarSeed = "nova",
      status = PresenceStatus.IDLE,
      currentGame = null
    ),
    Friend(
      id = "usr_req_1",
      username = "apex_predator",
      displayName = "ApexPredator",
      avatarSeed = "apex",
      status = PresenceStatus.ONLINE,
      isPendingRequest = true
    )
  )

  private val _friends = MutableStateFlow<List<Friend>>(initialFriends)
  val friends: StateFlow<List<Friend>> = _friends.asStateFlow()

  private var voiceSimulationJob: Job? = null

  init {
    startVoiceSimulationLoop()
  }

  fun setGameFilter(filter: String) {
    _selectedGameFilter.value = filter
  }

  fun login(username: String, displayName: String, favoriteGame: String) {
    _currentUser.value = User(
      id = "usr_" + UUID.randomUUID().toString().take(6),
      username = username.trim().lowercase(),
      displayName = displayName.trim().ifEmpty { username },
      avatarSeed = username,
      bio = "Mobile gamer on SquadPing",
      status = PresenceStatus.ONLINE,
      favoriteGame = favoriteGame
    )
  }

  fun quickGuestLogin() {
    val randomId = Random.nextInt(100, 999)
    login("squad_guest_$randomId", "GuestPlayer$randomId", "PUBG Mobile")
  }

  fun logout() {
    leaveRoom()
    _currentUser.value = null
  }

  fun joinRoom(roomId: String) {
    val targetRoom = _rooms.value.find { it.id == roomId } ?: return
    val user = _currentUser.value ?: return

    _currentRoom.value = targetRoom
    _currentUser.value = user.copy(
      status = PresenceStatus.IN_ROOM,
      currentGame = targetRoom.activeGame
    )

    // Setup initial squad participants for this room
    val members = mutableListOf(
      RoomMember(
        userId = user.id,
        displayName = user.displayName + " (You)",
        avatarSeed = user.avatarSeed,
        role = if (targetRoom.ownerId == user.id) MemberRole.OWNER else MemberRole.MEMBER,
        isSpeaking = false,
        isMuted = _audioSettings.value.micMuted,
        isDeafened = _audioSettings.value.deafened,
        volume = 1.0f,
        pingMs = 18
      ),
      RoomMember(
        userId = targetRoom.ownerId,
        displayName = targetRoom.ownerName,
        avatarSeed = targetRoom.ownerName,
        role = MemberRole.OWNER,
        isSpeaking = true,
        audioLevel = 0.75f,
        isMuted = false,
        volume = 1.0f,
        pingMs = 21
      )
    )

    if (targetRoom.id == "room_pubg_1") {
      members.add(
        RoomMember(
          userId = "usr_kestrel",
          displayName = "Kestrel",
          avatarSeed = "kestrel",
          role = MemberRole.MEMBER,
          isSpeaking = false,
          isMuted = false,
          volume = 1.2f,
          pingMs = 28
        )
      )
    }

    _roomMembers.value = members

    // Initial chat history
    _chatMessages.value = listOf(
      ChatMessage(
        id = "msg_sys_welcome",
        roomId = roomId,
        authorId = null,
        authorName = "System",
        content = "Welcome to ${targetRoom.name}. Voice channel connected with low latency.",
        messageType = MessageType.SYSTEM,
        formattedTime = timeFormat.format(Date())
      ),
      ChatMessage(
        id = "msg_1",
        roomId = roomId,
        authorId = targetRoom.ownerId,
        authorName = targetRoom.ownerName,
        avatarSeed = targetRoom.ownerName,
        content = "Hop in squad, we are matching right now!",
        messageType = MessageType.USER,
        formattedTime = timeFormat.format(Date(System.currentTimeMillis() - 60000))
      )
    )

    // Add join message
    sendSystemMessage("${user.displayName} joined the voice room.")
  }

  fun leaveRoom() {
    val room = _currentRoom.value
    val user = _currentUser.value
    if (room != null && user != null) {
      sendSystemMessage("${user.displayName} left the voice room.")
    }

    _currentRoom.value = null
    _roomMembers.value = emptyList()
    _chatMessages.value = emptyList()
    _typingUsers.value = emptyList()

    if (user != null) {
      _currentUser.value = user.copy(
        status = PresenceStatus.ONLINE,
        currentGame = null
      )
    }
  }

  fun createRoom(
    name: String,
    description: String,
    game: String,
    maxParticipants: Int,
    isLocked: Boolean
  ): Room {
    val user = _currentUser.value ?: throw IllegalStateException("Must be logged in")
    val newRoom = Room(
      id = "room_" + UUID.randomUUID().toString().take(8),
      name = name.trim().ifEmpty { "Squad Room" },
      description = description.trim().ifEmpty { "Gaming voice hangout" },
      type = if (isLocked) RoomType.PRIVATE else RoomType.PUBLIC,
      activeGame = game,
      ownerId = user.id,
      ownerName = user.displayName,
      participantCount = 1,
      maxParticipants = maxParticipants.coerceIn(2, 10),
      isLocked = isLocked,
      pingMs = 16
    )

    _rooms.value = listOf(newRoom) + _rooms.value
    joinRoom(newRoom.id)
    return newRoom
  }

  fun toggleMic() {
    val current = _audioSettings.value.micMuted
    val newMuted = !current
    _audioSettings.value = _audioSettings.value.copy(micMuted = newMuted)

    val currentUid = _currentUser.value?.id ?: return
    _roomMembers.value = _roomMembers.value.map { member ->
      if (member.userId == currentUid) {
        member.copy(isMuted = newMuted, isSpeaking = if (newMuted) false else member.isSpeaking)
      } else member
    }
  }

  fun toggleDeafen() {
    val current = _audioSettings.value.deafened
    val newDeafened = !current
    _audioSettings.value = _audioSettings.value.copy(deafened = newDeafened)

    val currentUid = _currentUser.value?.id ?: return
    _roomMembers.value = _roomMembers.value.map { member ->
      if (member.userId == currentUid) {
        member.copy(isDeafened = newDeafened)
      } else member
    }
  }

  fun setParticipantVolume(userId: String, volume: Float) {
    _roomMembers.value = _roomMembers.value.map { member ->
      if (member.userId == userId) {
        member.copy(volume = volume.coerceIn(0f, 2f))
      } else member
    }
  }

  fun sendChatMessage(text: String) {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return
    val user = _currentUser.value ?: return
    val room = _currentRoom.value ?: return

    val newMsg = ChatMessage(
      id = "msg_" + UUID.randomUUID().toString().take(8),
      roomId = room.id,
      authorId = user.id,
      authorName = user.displayName,
      avatarSeed = user.avatarSeed,
      content = trimmed,
      messageType = MessageType.USER,
      formattedTime = timeFormat.format(Date())
    )

    _chatMessages.value = _chatMessages.value + newMsg

    // Simulated quick response in room after tactical callouts
    if (trimmed.startsWith("Enemy") || trimmed.contains("ammo", ignoreCase = true) || trimmed.contains("rush", ignoreCase = true)) {
      simulateSquadTacticalReply(room.id)
    }
  }

  fun sendTacticalCallout(callout: String) {
    sendChatMessage("🎯 [Callout] $callout")
  }

  private fun sendSystemMessage(content: String) {
    val room = _currentRoom.value ?: return
    val sysMsg = ChatMessage(
      id = "sys_" + UUID.randomUUID().toString().take(8),
      roomId = room.id,
      authorId = null,
      authorName = "System",
      content = content,
      messageType = MessageType.SYSTEM,
      formattedTime = timeFormat.format(Date())
    )
    _chatMessages.value = _chatMessages.value + sysMsg
  }

  private fun simulateSquadTacticalReply(roomId: String) {
    scope.launch {
      delay(1200)
      if (_currentRoom.value?.id == roomId) {
        val replies = listOf(
          "Copy that, covering your flank!",
          "I have smoke ready, moving in.",
          "Enemy spotted 210 bearing!",
          "Reloading, give me 3 seconds!"
        )
        val reply = replies.random()
        val teammate = _roomMembers.value.firstOrNull { it.userId != _currentUser.value?.id }
        if (teammate != null) {
          _chatMessages.value = _chatMessages.value + ChatMessage(
            id = "msg_" + UUID.randomUUID().toString().take(8),
            roomId = roomId,
            authorId = teammate.userId,
            authorName = teammate.displayName,
            avatarSeed = teammate.avatarSeed,
            content = reply,
            messageType = MessageType.USER,
            formattedTime = timeFormat.format(Date())
          )
        }
      }
    }
  }

  private fun startVoiceSimulationLoop() {
    voiceSimulationJob?.cancel()
    voiceSimulationJob = scope.launch {
      while (isActive) {
        delay(2400)
        val room = _currentRoom.value ?: continue
        val currentUid = _currentUser.value?.id

        // Realistic active speaker simulation: cycle speech states among teammates
        _roomMembers.value = _roomMembers.value.map { member ->
          if (member.userId == currentUid) {
            // Local user voice activity state
            val isMuted = _audioSettings.value.micMuted
            val isDeaf = _audioSettings.value.deafened
            member.copy(isMuted = isMuted, isDeafened = isDeaf)
          } else {
            val shouldSpeak = Random.nextFloat() > 0.45f && !member.isMuted
            val level = if (shouldSpeak) Random.nextFloat().coerceIn(0.3f, 0.95f) else 0f
            member.copy(isSpeaking = shouldSpeak, audioLevel = level)
          }
        }
      }
    }
  }

  fun updateAudioSettings(newSettings: AudioSettings) {
    _audioSettings.value = newSettings
  }

  fun updateUserProfile(displayName: String, bio: String, favoriteGame: String) {
    val current = _currentUser.value ?: return
    _currentUser.value = current.copy(
      displayName = displayName.trim().ifEmpty { current.displayName },
      bio = bio.trim(),
      favoriteGame = favoriteGame
    )
  }

  fun addFriend(username: String) {
    val clean = username.trim().lowercase()
    if (clean.isEmpty()) return
    val newFriend = Friend(
      id = "usr_" + UUID.randomUUID().toString().take(6),
      username = clean,
      displayName = clean.replaceFirstChar { it.uppercase() },
      avatarSeed = clean,
      status = PresenceStatus.ONLINE,
      currentGame = "PUBG Mobile"
    )
    _friends.value = listOf(newFriend) + _friends.value
  }

  fun acceptFriendRequest(id: String) {
    _friends.value = _friends.value.map { friend ->
      if (friend.id == id) friend.copy(isPendingRequest = false, status = PresenceStatus.ONLINE) else friend
    }
  }

  fun rejectFriendRequest(id: String) {
    _friends.value = _friends.value.filter { it.id != id }
  }

  fun blockUser(id: String) {
    _friends.value = _friends.value.map { friend ->
      if (friend.id == id) friend.copy(isBlocked = true) else friend
    }
  }

  fun loginGuest(name: String) {
    _currentUser.value = User(
      id = "usr_guest_" + UUID.randomUUID().toString().take(6),
      username = name.lowercase().replace(" ", "_"),
      displayName = name,
      avatarSeed = name,
      bio = "Guest Squad Player",
      status = PresenceStatus.ONLINE,
      favoriteGame = "PUBG Mobile"
    )
  }
}
