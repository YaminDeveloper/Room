# Comprehensive Verification, Testing & QA Strategy

Testing SquadPing focuses intensely on real-world mobile gaming conditions: background lifecycle persistence, concurrent game audio, Bluetooth headset transitions, and strict multi-tenant authorization security.

---

## 1. Backend Automated Test Matrix (Node.js / Vitest / Supertest)

### 1.1 Authentication & Profile
- [x] Register with valid email/username generates hashed password and JWT pair.
- [x] Duplicate username or email registration returns `409 Conflict`.
- [x] Login with bad password returns `401 Unauthorized` without leaking user existence.
- [x] Token refresh rotates refresh token; revoked refresh tokens return `401`.
- [x] `GET /me` returns correct identity and status.

### 1.2 Friends & Permissions
- [x] Friend request flow: Send -> Accept -> Bilateral entry in `friendships`.
- [x] Blocking user immediately hides profile and terminates pending requests.
- [x] Blocked user cannot view blocker's public or private rooms.

### 1.3 Room Lifecycle & LiveKit Token Security
- [x] Creating a room establishes creator as `owner`.
- [x] Maximum participants enforcement rejects join when room is full.
- [x] `POST /livekit/token` mints JWT with exact room claims and audio-only track source.
- [x] Non-member request to `POST /livekit/token` returns `403 Forbidden`.
- [x] Muted member receives token with `canPublish: false`.

### 1.4 Chat & Anti-Spam
- [x] Sending message inserts row into `room_messages` and broadcasts to room sockets.
- [x] Message exceeding 500 characters rejected with `400 Bad Request`.
- [x] User sending >5 messages in 3 seconds throttled with rate-limit event.

---

## 2. Client & Native Audio Verification (Mobile / Android)

### 2.1 Concurrency & Game Audio Coexistence Test
| Scenario | Action | Expected Result |
|---|---|---|
| **Game Launch** | Join SquadPing voice room -> Minimize app -> Launch PUBG Mobile | Game audio (gunshots/footsteps) and voice chat both audible simultaneously. |
| **Notification Controls** | Toggle Mute button in ongoing notification | Microphone input cuts immediately; visual indicator updates on other clients. |
| **App Switching** | Switch between CODM and Discord/WhatsApp | SquadPing voice session remains connected with 0s interruption. |
| **Screen Lock** | Turn screen off while in voice chat | Audio streams continue uninterrupted without packet drop. |

### 2.2 Audio Route & Hardware Device Transitions
- **Speaker to Wired Headset**: Connecting wired 3.5mm/USB-C headphones reroutes audio without crash or focus drop.
- **Wired Headset Unplug**: Intercepts `ACTION_AUDIO_BECOMING_NOISY`, immediately mutes mic to protect player privacy.
- **Bluetooth Gaming Earbuds**: Seamless handover between handset speaker and Bluetooth SCO/A2DP with latency under 120ms.

### 2.3 Network Resiliency & Switching
- **WiFi to 5G/4G Handover**: LiveKit WebRTC ICE restarts connection without forcing user out of room.
- **Temporary Packet Loss (20%)**: Opus PLC (Packet Loss Concealment) maintains intelligible speech without robotic clicks.
- **WebSocket Reconnect**: App socket reconnects and fetches missed chat history since last sequence ID.

---

## 3. Real Device & Game Matrix

Testing is validated across top mobile gaming titles and hardware classes:
1. **Target Mobile Games**:
   - PUBG Mobile
   - Call of Duty Mobile (CODM)
   - Free Fire / Free Fire MAX
   - Roblox
   - Brawl Stars
2. **Audio Hardware Targets**:
   - Built-in Bottom Speaker + Top Earpiece
   - Wired 3.5mm / USB-C Headset
   - Standard Bluetooth 5.0 Headset (e.g. Sony WH-1000XM4)
   - Low-Latency Gaming TWS Earbuds (AptX Low Latency / AAC Gaming Mode)
