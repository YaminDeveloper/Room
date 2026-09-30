# LiveKit Voice Chat Integration & Media Architecture

SquadPing relies on LiveKit for ultra-low latency WebRTC audio communication.

---

## 1. Security Invariant: Token Minting & Scope Isolation

**NEVER bundle LiveKit API keys or API secrets inside the client application.**

### Token Issuance Flow:
```
Mobile Client (Flutter)
       |
       | 1. POST /livekit/token { roomId } (Bearer JWT)
       v
Fastify Backend
       |
       | 2. Verify User Authentication
       | 3. Verify Room Exists & User is Active Member
       | 4. Verify User is NOT muted/kicked/blocked
       | 5. Mint LiveKit AccessToken using Server Secret
       v
Mobile Client (Flutter)
       |
       | 6. Connect to wss://rtc.squadping.gg with ephemeral token
       v
LiveKit SFU Server
```

### Generated Token Claims:
```typescript
const at = new AccessToken(LIVEKIT_API_KEY, LIVEKIT_API_SECRET, {
  identity: user.id,
  name: user.displayName,
  metadata: JSON.stringify({ avatar: user.avatarUrl }),
  ttl: '2h'
});

at.addGrant({
  roomJoin: true,
  room: roomId,
  canPublish: !member.isMutedByAdmin,
  canSubscribe: true,
  canPublishData: false, // MVP voice only
  canPublishSources: [TrackSource.MICROPHONE], // Strictly audio only! Zero camera / screen
  hidden: false
});
```

---

## 2. Room Voice Lifecycle

### 2.1 Entering a Voice Room
1. **Fetch Room Metadata**: Verify membership status and participant list via REST.
2. **Obtain LiveKit Token**: Issue `POST /livekit/token`.
3. **Initialize Native Service**: Notify Android `VoiceForegroundService` that a voice session is launching.
4. **Connect LiveKit Client**: Connect to LiveKit SFU:
   ```dart
   final room = Room(
     roomOptions: RoomOptions(
       adaptiveStream: true,
       defaultAudioPublishOptions: AudioPublishOptions(
         audioBitrate: 24000, // 24 kbps Opus optimized for gaming voice clarity
         dtx: true,           // Discontinuous Transmission saves bandwidth when silent
       ),
     ),
   );
   await room.connect(livekitUrl, token);
   ```
5. **Publish Microphone Track**: Request runtime `RECORD_AUDIO` permission if not already granted; publish local audio track.
6. **Subscribe to Remote Tracks**: Listen to `TrackSubscribedEvent` and bind remote audio streams.
7. **Start Speaking Indicator**: Listen to `ActiveSpeakersChangedEvent` to animate talking participant avatars.

### 2.2 Exiting a Voice Room
1. **Unpublish & Mute**: Unpublish and stop the local microphone track.
2. **Disconnect SFU**: Call `room.disconnect()` to release peer connections.
3. **Notify Backend**: Call `POST /rooms/:id/leave`.
4. **Stop Foreground Service**: Terminate `VoiceForegroundService` and dismiss the ongoing notification.

---

## 3. Audio Optimization (Opus Codec & Bandwidth)

- **Target Bitrate**: 24 kbps (Opus mono, voice optimized).
- **DTX (Discontinuous Transmission)**: Enabled. Drops packet transmission during player silence, reducing mobile data consumption by up to 60%.
- **Packet Loss Concealment (PLC)**: Enabled via LiveKit WebRTC engine.
- **Echo Cancellation (AEC) & Noise Suppression (NS)**: Enabled with hardware acoustic echo cancellation priority to prevent game audio feedback loop into the microphone.

---

## 4. Client-Side Individual Volume Control

Every participant's playback volume is adjustable strictly on the client side:
- Player A can set Player B to 150% volume (boosted) and Player C to 30% volume.
- Implementation: Directly sets gain on the WebRTC remote audio track:
  ```dart
  remoteParticipant.audioTracks.firstOrNull?.track?.setVolume(sliderValue);
  ```
- No server-side audio re-encoding or CPU overhead is incurred.

---

## 5. Realtime Speaking Indicator

- LiveKit SFU analyzes audio packet levels on the server and emits speaker updates via WebRTC data channel events.
- Client subscribes to `room.events.listen<ActiveSpeakersChangedEvent>()`.
- Zero polling of the Fastify backend for voice activity.
