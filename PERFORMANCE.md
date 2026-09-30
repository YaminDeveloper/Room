# Performance, Resource Profiling & Optimization Guidelines

SquadPing is engineered specifically for low-end to high-end mobile devices operating under severe resource constraints (running 60fps/90fps mobile games alongside background voice chat).

---

## 1. Concrete Performance Benchmarks

| Metric | Target Ceiling | Rationale |
|---|---|---|
| **Cold Start Time** | < 600 ms | Instant entry into game lobby |
| **Idle Memory (Lobby/Home)** | < 35 MB RAM | No WebRTC or audio pipelines resident in memory |
| **Active Voice Memory** | < 65 MB RAM | Lightweight audio-only SFU track pipeline |
| **Background Voice CPU** | < 3.5% single core | Prevents frame drops in competitive games |
| **Battery Consumption** | < 4.5% per hour in voice | Extended 4–6 hour gaming sessions |
| **Voice Bandwidth** | < 28 kbps per participant | Low ping preservation over mobile data (4G/5G) |
| **Voice Latency (End-to-End)**| < 90 ms | Realtime tactical callouts |

---

## 2. Resource Consumption Breakdown & Anti-Patterns

### 2.1 Memory Optimization
- **Zero Video / Camera / Canvas Allocations**: MVP completely excludes camera managers, video texture renderers, and screen sharing capture modules, cutting binary and runtime memory by >40MB.
- **Image Optimization**: Avatars are constrained to 128x128 WebP assets loaded with Coil/cached in memory with strict 10MB LRU limit.
- **Message List Disposal**: Message history is capped in memory to 200 items per room; older messages are lazily evicted and re-queried on scroll.

### 2.2 Network Efficiency
- **No Polling**: REST polling is strictly banned. Presence, friend state, and room updates push over WebSocket only when active.
- **Opus DTX (Discontinuous Transmission)**: Audio packets are suppressed entirely when a user is silent.
- **Single WebSocket Connection**: One multiplexed socket connection handles all application state (chat, presence, typing, invitations).

### 2.3 Battery Conservation
- **In Lobby / Home**: Microphone hardware is strictly powered OFF. LiveKit engine is inactive.
- **In Active Voice Room**:
  - `VoiceForegroundService` holds a partial CPU wake lock to prevent voice dropouts.
  - UI updates are throttled to 10Hz for audio level meters; when app is backgrounded, UI state rendering is halted completely.

---

## 3. Estimated Binary (APK/AAB) Footprint

| Component / Layer | Estimated Size (AAB) | Estimated Size (Universal APK) |
|---|---|---|
| Flutter Engine / Native Compose Base | ~4.2 MB | ~11.0 MB |
| LiveKit WebRTC Core (Audio Only) | ~6.5 MB | ~14.8 MB |
| Network & Serialization (Retrofit/OkHttp/Moshi) | ~0.8 MB | ~1.5 MB |
| Local Database & Storage (Room/Preferences) | ~0.6 MB | ~1.2 MB |
| App Assets & Fonts (SVG/Icons/WebP) | ~0.9 MB | ~1.4 MB |
| **Total Estimated Footprint** | **~13.0 MB** | **~29.9 MB** |
