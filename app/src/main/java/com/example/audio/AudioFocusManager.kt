package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log

class AudioFocusManager(
  private val context: Context,
  private val onFocusLost: () -> Unit,
  private val onFocusRegained: () -> Unit
) : AudioManager.OnAudioFocusChangeListener {

  private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
  private var focusRequest: AudioFocusRequest? = null
  private var hasFocus = false

  fun requestFocus(): Boolean {
    val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

      focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(audioAttributes)
        .setAcceptsDelayedFocusGain(true)
        .setOnAudioFocusChangeListener(this)
        .build()

      audioManager.requestAudioFocus(focusRequest!!)
    } else {
      @Suppress("DEPRECATION")
      audioManager.requestAudioFocus(
        this,
        AudioManager.STREAM_VOICE_CALL,
        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
      )
    }

    hasFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
    Log.d("AudioFocusManager", "Requested focus with TRANSIENT_MAY_DUCK: $hasFocus")
    return hasFocus
  }

  fun abandonFocus() {
    if (!hasFocus) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
    } else {
      @Suppress("DEPRECATION")
      audioManager.abandonAudioFocus(this)
    }
    hasFocus = false
  }

  override fun onAudioFocusChange(focusChange: Int) {
    when (focusChange) {
      AudioManager.AUDIOFOCUS_LOSS -> {
        Log.d("AudioFocusManager", "Permanent focus loss")
        hasFocus = false
        onFocusLost()
      }
      AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
      AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
        Log.d("AudioFocusManager", "Transient focus loss (incoming call or alert)")
        onFocusLost()
      }
      AudioManager.AUDIOFOCUS_GAIN -> {
        Log.d("AudioFocusManager", "Focus regained")
        hasFocus = true
        onFocusRegained()
      }
    }
  }
}
