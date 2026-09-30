# SquadPing — High-Level Architecture & System Design

SquadPing is an original, ultra-lightweight mobile gaming hangout and voice communication platform designed to keep players connected with zero friction while playing mobile titles (PUBG Mobile, CODM, Free Fire, Roblox, Brawl Stars, etc.).

---

## 1. Product Vision & Architectural Invariants

1. **Lightweight Core**: Cold start under 500ms, minimal memory footprint (<60MB active voice), low battery draw (<4%/hour in background voice), and conservative network bandwidth (<32 kbps per audio stream via Opus).
2. **Audio Only (MVP)**: Zero camera drivers, video codecs, screen capture, or WebRTC video pipelines initialized during MVP.
3. **Decoupled Responsibilities**:
   - **Application State & Metadata**: Fastify (Node.js/TypeScript) + PostgreSQL + Application WebSocket (`/ws/app`).
   - **Realtime Voice**: Dedicated LiveKit SFU (WebRTC / Opus).
   - **Background Audio & Device Hardware**: Native Android Foreground Service (`VoiceForegroundService.kt`), AudioFocusManager, and Bluetooth SCO/BLE manager.
4. **Lazy Initialization Rule**:
   - LiveKit WebRTC engine is **never** started when browsing Home, Lobby, or Friends.
   - Microphone hardware permissions and capture are initialized strictly upon entering an active voice room.
   - WebSocket connection for application state is paused or disconnected when the app is backgrounded unless the user is in an active voice room.
5. **Simultaneous Game Audio**: Voice must never hijack game audio into telephony mode (`MODE_IN_COMMUNICATION` with optimal audio focus ducking/transient release), allowing game gunshots, footsteps, and spatial audio to mix cleanly with squad chat.

---

## 2. Global Component Architecture

```
                  +----------------------------------------------+
                  |              Mobile Client (Flutter)         |
                  |                                              |
                  |   +-------------------+  +---------------+   |
                  |   | State & UI Layers |  | Native Bridge |   |
                  |   +---------+---------+  +-------+-------+   |
                  +-------------|--------------------|-----------+
                                |                    |
              +-----------------+                    v
              |                        Native Android Runtime
              |               +--------------------------------------+
              |               | VoiceForegroundService               |
              |               | AudioFocusManager (DUCK / TRANSIENT) |
              |               | AudioRouteManager (Headset/BT SCO)   |
              |               +------------------+-------------------+
              |                                  |
              v (HTTPS / WSS)                    v (WebRTC / SRTP)
   +----------------------+             +--------------------+
   | Fastify App Server   |             | LiveKit SFU Server |
   |  - Auth & Profile    |             |  - SFU Routing     |
   |  - Friends & Rooms   |             |  - Opus Audio Only |
   |  - Chat & Moderation |             |  - Audio Level Det |
   |  - LiveKit Token Mnt |             +--------------------+
   +----------+-----------+
              |
              +--------------------------+
              |                          |
              v                          v
     +-----------------+        +------------------+
     | PostgreSQL DB   |        | Redis (Optional) |
     | - Persistence   |        | - PubSub/State   |
     +-----------------+        +------------------+
```

---

## 3. Communication Protocols

| Channel | Protocol | Port | Responsibility |
|---|---|---|---|
| **App REST API** | HTTPS / JSON | 443 / 8080 | Auth, profiles, room CRUD, membership, LiveKit JWT minting |
| **App Realtime State**| WSS (WebSocket) | 443 / 8080 | Presence, room metadata, live chat messages, typing indicator |
| **Media Signaling** | WSS (LiveKit) | 7880 | WebRTC ICE negotiation, SDP exchange, track subscriptions |
| **Media Transport** | WebRTC (UDP/TCP)| 7881 / 50000-60000 | Opus RTP/SRTP voice packets, RTCP statistics |
| **Push Notifications**| HTTPS / FCM | 443 | Friend requests, room invites, offline mentions |

---

## 4. Subsystem Breakdown

### 4.1 Client Subsystems (Mobile)
- **Core Presentation**: Feature-driven screens (Auth, Lobby, Room, Friends, Settings).
- **Session Manager**: Manages auth tokens (Access + Refresh), persistent auto-login, and identity.
- **Lobby Service**: Lightweight HTTP/WebSocket querying of active public rooms and online friends with cursor-based pagination.
- **Room Engine**: Coordinates entering a room, joining the chat WebSocket channel, minting LiveKit credentials, and binding to the native audio service.
- **Native Audio Bridge**: Bidirectional MethodChannel/EventChannel connecting Flutter UI to Kotlin `VoiceForegroundService`.

### 4.2 Backend Subsystems (Fastify + PostgreSQL)
- `src/auth`: JWT issuance, bcrypt password hashing, token revocation.
- `src/users`: User profile data, username reservation, gaming status.
- `src/friends`: Directed friend graph, blocklists, pending request counters.
- `src/rooms`: Room lifecycle, ownership, lock state, max participants enforcement.
- `src/chat`: PostgreSQL-backed message persistence with cursor pagination.
- `src/websocket`: Low-overhead JSON-RPC / message router for presence and chat broadcast.
- `src/livekit`: LiveKit Server SDK integration with strict token permission minting.
- `src/moderation`: In-room kick, mute, user blocking, and structured abuse reporting.

---

## 5. Failure Modes & Graceful Degradation

1. **Network Switch (WiFi to Cellular)**:
   - LiveKit automatically renegotiates ICE in the background.
   - Application WebSocket reconnects with exponential backoff (`min 1s`, `max 30s`) and sends a `sync.ack` to retrieve missed chat messages.
2. **Audio Route Change (Headphones Unplugged / Bluetooth Disconnect)**:
   - `AudioRouteManager` intercepts `ACTION_AUDIO_BECOMING_NOISY` and immediately mutes the local microphone track to avoid accidental broadcast of ambient background noise.
   - Notification updates to reflect route transition.
3. **Low Memory Killer (LMK) Pressure**:
   - `VoiceForegroundService` runs with `START_STICKY` and displays an ongoing persistent notification (`FOREGROUND_SERVICE_TYPE_MICROPHONE`), protecting the voice process from OS termination while the user plays graphics-heavy games.
