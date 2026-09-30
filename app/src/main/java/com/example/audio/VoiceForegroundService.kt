package com.example.audio

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import com.example.data.SquadRepository

class VoiceForegroundService : Service() {

  companion object {
    const val TAG = "VoiceForegroundService"

    const val ACTION_START_VOICE = "com.example.action.START_VOICE"
    const val ACTION_STOP_VOICE = "com.example.action.STOP_VOICE"
    const val ACTION_UPDATE_STATE = "com.example.action.UPDATE_STATE"

    const val EXTRA_ROOM_ID = "extra_room_id"
    const val EXTRA_ROOM_NAME = "extra_room_name"
    const val EXTRA_ACTIVE_GAME = "extra_active_game"
    const val EXTRA_PARTICIPANT_COUNT = "extra_participant_count"

    var isRunning = false
      private set

    fun start(
      context: Context,
      roomId: String,
      roomName: String,
      activeGame: String,
      participantCount: Int
    ) {
      val intent = Intent(context, VoiceForegroundService::class.java).apply {
        action = ACTION_START_VOICE
        putExtra(EXTRA_ROOM_ID, roomId)
        putExtra(EXTRA_ROOM_NAME, roomName)
        putExtra(EXTRA_ACTIVE_GAME, activeGame)
        putExtra(EXTRA_PARTICIPANT_COUNT, participantCount)
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
      } else {
        context.startService(intent)
      }
    }

    fun stop(context: Context) {
      val intent = Intent(context, VoiceForegroundService::class.java).apply {
        action = ACTION_STOP_VOICE
      }
      context.startService(intent)
    }
  }

  private lateinit var notificationManager: VoiceNotificationManager
  private lateinit var audioFocusManager: AudioFocusManager
  private lateinit var audioRouteManager: AudioRouteManager
  private lateinit var bluetoothAudioManager: BluetoothAudioManager

  private var wakeLock: PowerManager.WakeLock? = null
  private var currentRoomName = "Squad Room"
  private var currentActiveGame = "PUBG Mobile"
  private var currentParticipantCount = 1

  override fun onCreate() {
    super.onCreate()
    Log.d(TAG, "Creating VoiceForegroundService")

    notificationManager = VoiceNotificationManager(this)

    audioFocusManager = AudioFocusManager(
      context = this,
      onFocusLost = {
        Log.w(TAG, "Audio focus lost or ducked")
      },
      onFocusRegained = {
        Log.d(TAG, "Audio focus restored")
      }
    )

    audioRouteManager = AudioRouteManager(
      context = this,
      onBecomingNoisy = {
        // Auto-mute on headphone unplug to prevent accidental hot-mic broadcast
        if (!SquadRepository.audioSettings.value.micMuted) {
          SquadRepository.toggleMic()
          updateNotification()
        }
      },
      onRouteChanged = { newRoute ->
        SquadRepository.updateAudioSettings(
          SquadRepository.audioSettings.value.copy(audioRoute = newRoute)
        )
      }
    )

    bluetoothAudioManager = BluetoothAudioManager(this) { isConnected ->
      Log.d(TAG, "Bluetooth headset connectivity changed: $isConnected")
    }

    // Acquire partial wake lock so background game audio and WebRTC don't sleep
    val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
    wakeLock = powerManager.newWakeLock(
      PowerManager.PARTIAL_WAKE_LOCK,
      "SquadPing::VoiceServiceWakeLock"
    ).apply {
      setReferenceCounted(false)
      acquire(4 * 60 * 60 * 1000L) // 4 hours maximum session timeout
    }

    audioRouteManager.startMonitoring()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val action = intent?.action ?: return START_STICKY

    when (action) {
      ACTION_START_VOICE -> {
        currentRoomName = intent.getStringExtra(EXTRA_ROOM_NAME) ?: "Squad Voice"
        currentActiveGame = intent.getStringExtra(EXTRA_ACTIVE_GAME) ?: "Gaming"
        currentParticipantCount = intent.getIntExtra(EXTRA_PARTICIPANT_COUNT, 1)

        val notification = notificationManager.buildNotification(
          roomName = currentRoomName,
          activeGame = currentActiveGame,
          isMuted = SquadRepository.audioSettings.value.micMuted,
          participantCount = currentParticipantCount
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          startForeground(
            VoiceNotificationManager.NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
          )
        } else {
          startForeground(VoiceNotificationManager.NOTIFICATION_ID, notification)
        }

        audioFocusManager.requestFocus()
        isRunning = true
      }

      VoiceNotificationManager.ACTION_TOGGLE_MUTE -> {
        SquadRepository.toggleMic()
        updateNotification()
      }

      VoiceNotificationManager.ACTION_LEAVE_ROOM -> {
        SquadRepository.leaveRoom()
        stopSelf()
      }

      ACTION_UPDATE_STATE -> {
        currentParticipantCount = intent.getIntExtra(EXTRA_PARTICIPANT_COUNT, currentParticipantCount)
        updateNotification()
      }

      ACTION_STOP_VOICE -> {
        stopSelf()
      }
    }

    return START_STICKY
  }

  private fun updateNotification() {
    notificationManager.update(
      roomName = currentRoomName,
      activeGame = currentActiveGame,
      isMuted = SquadRepository.audioSettings.value.micMuted,
      participantCount = currentParticipantCount
    )
  }

  override fun onDestroy() {
    super.onDestroy()
    Log.d(TAG, "Destroying VoiceForegroundService")
    isRunning = false

    audioFocusManager.abandonFocus()
    audioRouteManager.stopMonitoring()
    bluetoothAudioManager.stopBluetoothSco()

    wakeLock?.let {
      if (it.isHeld) it.release()
    }

    stopForeground(STOP_FOREGROUND_REMOVE)
  }

  override fun onBind(intent: Intent?): IBinder? = null
}
