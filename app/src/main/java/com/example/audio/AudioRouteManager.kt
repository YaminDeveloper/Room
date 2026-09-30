package com.example.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log

class AudioRouteManager(
  private val context: Context,
  private val onBecomingNoisy: () -> Unit,
  private val onRouteChanged: (String) -> Unit
) {

  private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
  private var isReceiverRegistered = false

  private val routeReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      when (intent?.action) {
        AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
          Log.w("AudioRouteManager", "Headphones disconnected! Triggering emergency mic mute.")
          onBecomingNoisy()
        }
        Intent.ACTION_HEADSET_PLUG -> {
          val state = intent.getIntExtra("state", -1)
          val routeName = if (state == 1) "Wired Headset" else "Speakerphone"
          Log.d("AudioRouteManager", "Headset plug state: $state -> $routeName")
          onRouteChanged(routeName)
        }
      }
    }
  }

  fun startMonitoring() {
    if (!isReceiverRegistered) {
      val filter = IntentFilter().apply {
        addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        addAction(Intent.ACTION_HEADSET_PLUG)
      }
      context.registerReceiver(routeReceiver, filter)
      isReceiverRegistered = true
    }
  }

  fun stopMonitoring() {
    if (isReceiverRegistered) {
      try {
        context.unregisterReceiver(routeReceiver)
      } catch (e: Exception) {
        Log.e("AudioRouteManager", "Error unregistering receiver", e)
      }
      isReceiverRegistered = false
    }
  }

  fun setSpeakerphone(enable: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      if (enable) {
        val speakerDevice = audioManager.availableCommunicationDevices.firstOrNull {
          it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        }
        speakerDevice?.let { audioManager.setCommunicationDevice(it) }
      } else {
        audioManager.clearCommunicationDevice()
      }
    } else {
      @Suppress("DEPRECATION")
      audioManager.isSpeakerphoneOn = enable
    }
  }
}
