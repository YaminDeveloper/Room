package com.example.model

data class AudioSettings(
  val micMuted: Boolean = false,
  val deafened: Boolean = false,
  val gameAudioDuckingPercent: Float = 0.6f,
  val noiseSuppression: Boolean = true,
  val echoCancellation: Boolean = true,
  val backgroundVoiceActive: Boolean = true,
  val audioRoute: String = "Speakerphone",
  val selectedBitrateKbps: Int = 24
)
