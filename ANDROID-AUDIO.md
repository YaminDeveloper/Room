# Android Native Audio & Background Voice Architecture

Background voice concurrency is the most critical technical requirement for SquadPing: players must talk with their squad while PUBG Mobile, COD Mobile, or Free Fire are actively running in the foreground, with both **game sound effects** and **friend voices** clearly audible.

---

## 1. The Gaming Concurrency Problem & Anti-Patterns

### Common Anti-Patterns to AVOID:
1. **Telecom / ConnectionService Call Mode**:
   - Calling `TelecomManager` or routing through cellular in-call audio forces Android into telephony mode.
   - *Result*: Android forcefully suppresses, ducks, or mutes media playback from third-party games, ruining game sound.
2. **`AUDIOFOCUS_GAIN` (Exclusive Focus)**:
   - Requesting exclusive permanent audio focus pauses background games or music players.
3. **Pure Flutter Implementation Without Foreground Service**:
   - As soon as the user minimizes the app to open PUBG, Android's Low Memory Killer (LMK) or Doze Mode will pause the WebRTC audio thread within 10–60 seconds.

---

## 2. SquadPing Native Architecture

```
                    Flutter UI / Dart Layer
                              |
                    [MethodChannel / EventChannel]
                              v
         +-----------------------------------------------+
         |             VoiceForegroundService            |
         |  - FOREGROUND_SERVICE_TYPE_MICROPHONE         |
         |  - Sticky lifecycle & wake lock              |
         +--------+-------------------+------------------+
                  |                   |
                  v                   v
     +--------------------+   +-----------------------+
     | AudioFocusManager  |   | VoiceNotificationMgr  |
     | - DUCK / TRANSIENT |   | - Mute/Unmute toggle  |
     | - Non-exclusive    |   | - Leave room button   |
     +---------+----------+   +-----------------------+
               |
               v
     +--------------------+   +-----------------------+
     | AudioRouteManager  |---| BluetoothAudioMgr     |
     | - Wired detection  |   | - BLE Gaming Latency  |
     | - Noisy broadcast  |   | - SCO fallback        |
     +--------------------+   +-----------------------+
```

---

## 3. Native Components & Responsibilities

### 3.1 `VoiceForegroundService.kt`
- Declared in `AndroidManifest.xml` with `android:foregroundServiceType="microphone"`.
- Runs with `START_STICKY`.
- Maintains CPU wake lock during active voice to prevent audio clock drift when screen is locked.
- Exposes bidirectional EventChannel streams for real-time mute and connection state.

### 3.2 `AudioFocusManager.kt`
- Implements `AudioManager.OnAudioFocusChangeListener`.
- Requests audio focus using `AudioAttributes.USAGE_VOICE_COMMUNICATION` with `AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`.
- **Game Audio Coexistence**:
  ```kotlin
  val audioAttributes = AudioAttributes.Builder()
      .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
      .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
      .build()

  val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
      .setAudioAttributes(audioAttributes)
      .setAcceptsDelayedFocusGain(true)
      .setOnAudioFocusChangeListener(this)
      .build()
  ```
- This guarantees that background games (media streams) are mixed simultaneously with incoming squad audio.

### 3.3 `AudioRouteManager.kt`
- Detects headset plug/unplug events via `Intent.ACTION_HEADSET_PLUG`.
- Registers broadcast receiver for `AudioManager.ACTION_AUDIO_BECOMING_NOISY`:
  - When headphones or Bluetooth disconnect unexpectedly, automatically triggers an emergency mic mute to prevent embarrassing hot-mic incidents.
- Routes playback to communication device via `AudioManager.setCommunicationDevice` (API 31+) or `setSpeakerphoneOn(true)`.

### 3.4 `BluetoothAudioManager.kt`
- Listens to `BluetoothProfile.SERVICE_CONNECTED` for `BluetoothHeadset` and `BluetoothA2dp`.
- Optimizes Bluetooth buffers for low-latency gaming earbuds.

### 3.5 `VoiceNotificationManager.kt`
- Builds persistent Notification with channel ID `squadping_voice_channel` (Importance: LOW / Ongoing).
- Provides instant pending intent actions:
  - **Mute / Unmute**: Directly toggles local track state without opening the app.
  - **Leave Room**: Immediately disconnects from the room.
  - **Tap Notification**: Resumes SquadPing full-screen activity.

---

## 4. Manifest Configuration & Permissions

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.WAKE_LOCK" />

<service
    android:name=".audio.VoiceForegroundService"
    android:exported="false"
    android:foregroundServiceType="microphone" />
```
