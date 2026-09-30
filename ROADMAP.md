# SquadPing Product & Engineering Roadmap

SquadPing is developed in disciplined, verified milestones prioritizing an ultra-fast, rock-solid core MVP before entertaining auxiliary social expansions.

---

## Part 1: MVP Engineering Phases (Phases 0–10)

```
[Phase 0: Architecture & Audit]  <-- CURRENT MILESTONE
              |
              v
[Phase 1: Mobile UI & Foundation]
              |
              v
[Phase 2: Backend & Database Foundation]
              |
              v
[Phase 3: Friends & Realtime Presence]
              |
              v
[Phase 4: Room Management & Permissions]
              |
              v
[Phase 5: Room Text Chat & Ephemeral Engine]
              |
              v
[Phase 6: LiveKit WebRTC Voice Integration]
              |
              v
[Phase 7: Android Background Voice & Game Audio Concurrency]
              |
              v
[Phase 8: Performance Profiling & Binary Optimization]
              |
              v
[Phase 9: Comprehensive QA, Game Matrix & Audio Rig Tests]
              |
              v
[Phase 10: Production VPS & Docker Deployment]
```

### Phase Details

- **Phase 0: Architecture & Licensing Audit (Completed)**
  - Specification analysis, open-source repository license audit, database DDL, API contracts, WebSocket protocol, and Android concurrency design.
- **Phase 1: Mobile Application Foundation**
  - Feature-driven Flutter architecture, custom gaming design system (dark violet/cyan neon aesthetic), type-safe state management, secure token storage.
- **Phase 2: Backend & Database Foundation**
  - Fastify TypeScript server, PostgreSQL migrations, Argon2/Bcrypt auth, revocable JWT rotation, and profile endpoints.
- **Phase 3: Friends & Realtime Presence**
  - Bilateral friend graph, search, blocking, WebSocket presence connection (`online`, `idle`, `in_room`, `gaming`).
- **Phase 4: Rooms & Permissions**
  - Public lobby, private locked rooms, role engine (`owner`, `admin`, `member`), kick/mute actions, room invite flows.
- **Phase 5: Room Text Chat Subsystem**
  - Low-latency chat broadcast, PostgreSQL message persistence with cursor pagination, ephemeral in-memory typing indicators, anti-spam rate limiting.
- **Phase 6: LiveKit Voice Integration**
  - Server-side JWT minting, audio-only SFU connection, individual participant volume sliders, active speaker voice wave animations.
- **Phase 7: Android Native Background Voice**
  - Native Kotlin `VoiceForegroundService`, ongoing notification with Mute/Leave actions, `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK` for simultaneous PUBG/CODM game audio.
- **Phase 8: Performance & APK Trimming**
  - Lazy initialization enforcement, unused dependency pruning, ProGuard optimization, audio stream profiling.
- **Phase 9: Verification & Testing**
  - End-to-end integration tests, multi-participant room stress tests, Bluetooth headset transition tests.
- **Phase 10: Production Deployment**
  - Docker Compose topology, Nginx SSL termination, LiveKit SFU configuration, automated database backup routine.

---

## Part 2: Future Expansion Roadmap (Phases 11–17)

*Strictly deferred until Phase 10 MVP is verified in production.*

- **Phase 11: Mobile Screen Sharing (Android MediaProjection + LiveKit Video Track)**
  - Allows squad captains to stream their gameplay HUD in tactical briefing rooms.
- **Phase 12: Watch Together (Synchronized Media Playback)**
  - Coordinated YouTube/local video playback via WebSocket timeline synchronization (derived from WatchParty architecture).
- **Phase 13: Local DSP Voice Effects**
  - Fun gaming voice filters (robot, radio comms filter, megaphone, pitch modulation) using local client-side WebRTC audio processing.
- **Phase 14: Voice Stamps & Soundboard Reactions**
  - Short audio meme soundbites triggered in voice chat with rate limits and volume controls.
- **Phase 15: In-Room Casual Mini-Games**
  - Lightweight multiplayer turn-based games (Tic Tac Toe, Air Hockey, Trivia) running over WebSocket while waiting in lobby.
- **Phase 16: Gaming Communities & Clans**
  - Clan lobbies, tiered member roles, dedicated clan voice lounges, scheduled scrim tournaments.
- **Phase 17: Rich Media & Advanced Chat**
  - In-chat image/screenshot sharing, rich hyperlink previews, message reactions, and pinned tactical notes.
