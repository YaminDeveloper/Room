package com.example.audio

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.media.AudioManager
import android.util.Log

class BluetoothAudioManager(
  private val context: Context,
  private val onBluetoothStatusChanged: (Boolean) -> Unit
) {

  private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
  private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

  fun isBluetoothHeadsetConnected(): Boolean {
    return try {
      bluetoothAdapter?.getProfileConnectionState(BluetoothProfile.HEADSET) ==
        BluetoothProfile.STATE_CONNECTED
    } catch (e: SecurityException) {
      Log.w("BluetoothAudioManager", "Missing BLUETOOTH_CONNECT permission", e)
      false
    }
  }

  fun startBluetoothSco() {
    try {
      if (isBluetoothHeadsetConnected()) {
        @Suppress("DEPRECATION")
        audioManager.startBluetoothSco()
        @Suppress("DEPRECATION")
        audioManager.isBluetoothScoOn = true
        onBluetoothStatusChanged(true)
      }
    } catch (e: Exception) {
      Log.e("BluetoothAudioManager", "Failed to start Bluetooth SCO", e)
    }
  }

  fun stopBluetoothSco() {
    try {
      @Suppress("DEPRECATION")
      if (audioManager.isBluetoothScoOn) {
        @Suppress("DEPRECATION")
        audioManager.stopBluetoothSco()
        @Suppress("DEPRECATION")
        audioManager.isBluetoothScoOn = false
        onBluetoothStatusChanged(false)
      }
    } catch (e: Exception) {
      Log.e("BluetoothAudioManager", "Failed to stop Bluetooth SCO", e)
    }
  }
}
