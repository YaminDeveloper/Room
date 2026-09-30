# Reference Repositories & Architectural Benchmarks

This document reviews the open-source projects evaluated during Phase 0 to guide system design, dependency selection, and compliance.

---

## 1. LiveKit Flutter SDK
- **Repository**: `livekit/client-sdk-flutter`
- **License**: Apache License 2.0
- **Primary Technologies**: Dart, Flutter, `flutter_webrtc`, LiveKit protocol buffer definitions.
- **Architectural Insights**:
  - Room event bus pattern (`RoomListener`, `TrackSubscribedEvent`, `SpeakingChangedEvent`).
  - High-performance reactive subscription to participant audio levels.
  - Safe track detachment and cleanup logic to prevent audio track leakage on room exit.
- **What We Use**: Official pub package dependency (`livekit_client`).
- **What We Do NOT Use**: Do not bundle raw C++ WebRTC binaries manually or duplicate internal protobuf parsers.
- **Code Copy Status**: **Zero code copied**. Direct SDK dependency.
- **Attribution**: Include Apache 2.0 attribution notice in app Open Source Licenses view.

---

## 2. LiveKit Android SDK
- **Repository**: `livekit/client-sdk-android`
- **License**: Apache License 2.0
- **Primary Technologies**: Kotlin, Android WebRTC native wrapper, AudioDeviceModule (ADM).
- **Architectural Insights**:
  - Critical audio device switching patterns: `AudioManager.setCommunicationDevice` (Android 12+ API 31) vs legacy `startBluetoothSco`.
  - Android 14 (API 34) foreground service constraints: explicit declaration of `FOREGROUND_SERVICE_TYPE_MICROPHONE`.
  - Recovery from audio focus loss when incoming phone calls arrive.
- **What We Use**: Architectural and lifecycle blueprint for our native Kotlin `VoiceForegroundService`.
- **What We Do NOT Use**: We do not duplicate their full Android UI or room wrappers.
- **Code Copy Status**: **Zero code copied**. Reference only.
- **Attribution**: Preserved in documentation and license audit.

---

## 3. LiveKit Server
- **Repository**: `livekit/livekit`
- **License**: Apache License 2.0
- **Primary Technologies**: Go, Pion WebRTC, Redis, WebSocket signaling.
- **Architectural Insights**:
  - SFU (Selective Forwarding Unit) media routing architecture.
  - Room token claims: `canPublish`, `canSubscribe`, `roomName`, `identity`, `metadata`.
  - Built-in audio level detection in the SFU eliminating client-side volume calculation overhead.
- **What We Use**: Run official pre-built binary / official Docker image (`livekit/livekit-server:latest`).
- **What We Do NOT Use**: No source code modifications or custom forks needed for MVP.
- **Code Copy Status**: **Zero code copied**. Standard infrastructure deployment.
- **Attribution**: Standard Apache 2.0 notice in deployment documentation.

---

## 4. LiveKit Flutter Components
- **Repository**: `livekit/components-flutter`
- **License**: Apache License 2.0
- **Primary Technologies**: Flutter, Provider/StateNotifier, Pre-built UI widgets.
- **Architectural Insights**:
  - Reactive participant list sorting (placing active speakers at the top).
  - Speaking indicator wave animation driven by RMS audio level floats.
- **What We Use**: Visual state model inspiration only.
- **What We Do NOT Use**: We do NOT use the bloated pre-packaged UI widgets (which bundle heavy video renderers and unnecessary desktop controls). We build our own lightweight gaming-focused UI.
- **Code Copy Status**: **Zero code copied**.
- **Attribution**: Noted in license audit.

---

## 5. Game-Chat
- **Repository**: `JrFarkade/Game-Chat`
- **License**: MIT License
- **Primary Technologies**: Flutter, Kotlin, Android Foreground Service, WebRTC.
- **Architectural Insights**:
  - Practical mobile gaming voice pattern: maintaining persistent voice connectivity while games like PUBG or CODM occupy the foreground.
  - Flutter MethodChannel communication with Android foreground service for lifecycle synchronization.
- **What We Use**: Architectural pattern for background notification management and audio focus mode handling.
- **What We Do NOT Use**: Do not use their backend (Firebase/custom sockets). Our backend is Fastify + LiveKit SFU.
- **Code Copy Status**: **Zero code copied**. Architectural pattern reference.
- **Attribution**: MIT attribution acknowledged in LICENSE-AUDIT.md.

---

## 6. Stoat / Revolt
- **Repository**: `stoatchat` (formerly Revolt)
- **License**: AGPLv3 (Server) / Apache 2.0 (Client components)
- **Primary Technologies**: Rust / Node.js, WebSockets, MongoDB/PostgreSQL.
- **Architectural Insights**:
  - Separation of persistent chat vs temporary ephemeral states (typing indicators, presence).
  - User blocking and permission model for community spaces.
- **What We Use**: Architectural separation principles only.
- **What We Do NOT Use**: Zero code or libraries imported due to AGPLv3 license boundary and distinct architectural stack.
- **Code Copy Status**: **Zero code copied**. Conceptual reference only.
- **Attribution**: Documented for full transparency.

---

## 7. WatchParty
- **Repository**: `KaranVishwakarma-1807/WatchParty`
- **License**: MIT License
- **Primary Technologies**: WebRTC, Node.js, Socket.IO.
- **Architectural Insights**:
  - Synchronized state-machine events for media playback.
- **What We Use**: Reserved for Phase 12 (Future Watch Together roadmap item).
- **What We Do NOT Use**: Excluded completely from MVP to keep APK lightweight.
- **Code Copy Status**: **Zero code copied**.
- **Attribution**: Retained in roadmap documentation.
