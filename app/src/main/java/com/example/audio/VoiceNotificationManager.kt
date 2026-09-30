package com.example.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

class VoiceNotificationManager(private val context: Context) {

  companion object {
    const val CHANNEL_ID = "squadping_voice_channel"
    const val NOTIFICATION_ID = 4040
    const val ACTION_TOGGLE_MUTE = "com.example.action.TOGGLE_MUTE"
    const val ACTION_LEAVE_ROOM = "com.example.action.LEAVE_ROOM"
  }

  private val notificationManager =
    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

  init {
    createChannel()
  }

  private fun createChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        "SquadPing Gaming Voice",
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "Shows persistent voice status while playing games in the background"
        setShowBadge(false)
        enableVibration(false)
        enableLights(false)
      }
      notificationManager.createNotificationChannel(channel)
    }
  }

  fun buildNotification(
    roomName: String,
    activeGame: String,
    isMuted: Boolean,
    participantCount: Int
  ): Notification {
    val openAppIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val openAppPendingIntent = PendingIntent.getActivity(
      context,
      0,
      openAppIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val toggleMuteIntent = Intent(context, VoiceForegroundService::class.java).apply {
      action = ACTION_TOGGLE_MUTE
    }
    val toggleMutePendingIntent = PendingIntent.getService(
      context,
      1,
      toggleMuteIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val leaveRoomIntent = Intent(context, VoiceForegroundService::class.java).apply {
      action = ACTION_LEAVE_ROOM
    }
    val leaveRoomPendingIntent = PendingIntent.getService(
      context,
      2,
      leaveRoomIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val micStatusText = if (isMuted) "Mic MUTED" else "Mic ON"
    val subText = "$activeGame • $micStatusText ($participantCount in squad)"

    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle("Voice Hangout: $roomName")
      .setContentText(subText)
      .setContentIntent(openAppPendingIntent)
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setCategory(NotificationCompat.CATEGORY_SERVICE)
      .addAction(
        NotificationCompat.Action.Builder(
          android.R.drawable.ic_btn_speak_now,
          if (isMuted) "Unmute" else "Mute",
          toggleMutePendingIntent
        ).build()
      )
      .addAction(
        NotificationCompat.Action.Builder(
          android.R.drawable.ic_menu_close_clear_cancel,
          "Leave Room",
          leaveRoomPendingIntent
        ).build()
      )

    return builder.build()
  }

  fun update(
    roomName: String,
    activeGame: String,
    isMuted: Boolean,
    participantCount: Int
  ) {
    val notification = buildNotification(roomName, activeGame, isMuted, participantCount)
    notificationManager.notify(NOTIFICATION_ID, notification)
  }
}
