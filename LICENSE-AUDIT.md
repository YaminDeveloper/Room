# Open Source License Audit

| Repository | License | What We Use | What We Do NOT Use | Attribution Required? | Usage Type |
|---|---|---|---|---|---|
| **livekit/client-sdk-flutter** | Apache 2.0 | Official Flutter package for WebRTC/SFU client signaling, room connection, and track management. | Manual C++ binary bindings or sample application code. | Yes (Apache 2.0 Notice) | Official SDK Dependency |
| **livekit/client-sdk-android** | Apache 2.0 | Audio device routing patterns (`AudioManager` + API 31 `setCommunicationDevice`) and Android 14 foreground service patterns. | SDK UI components or full Android client library. | Yes (Apache 2.0 Notice) | Architectural Reference |
| **livekit/livekit** | Apache 2.0 | Official Docker image for SFU server instance handling audio routing, Opus transcoding, and ICE negotiation. | Source forks or internal modifications. | Yes (Apache 2.0 Notice) | Infrastructure Binary / Container |
| **livekit/components-flutter** | Apache 2.0 | Concept of sorting speaking participants to top of UI list. | Pre-built UI widgets, video components, or layout packages. | No code used | Conceptual Reference |
| **JrFarkade/Game-Chat** | MIT | Architectural concept of Flutter MethodChannel to Kotlin foreground service for gaming audio concurrency. | Backend code, UI code, or raw service code. | Yes (MIT License preservation) | Architectural Reference |
| **stoatchat (Revolt)** | AGPLv3 / Apache 2.0 | Conceptual distinction between persistent message tables and ephemeral in-memory typing states. | Zero code, zero schemas, zero libraries (strictly avoided to preserve clean license boundary). | None (No code or assets used) | Conceptual Benchmark |
| **KaranVishwakarma-1807/WatchParty** | MIT | Roadmap study for future synchronized video state machines. | Not included in MVP. | None in MVP | Roadmap Reference |

---

## License Compliance Policy

1. **No Source Copying**: All core product code (Dart, Kotlin, TypeScript, SQL) is written originally from first principles to match the SquadPing specification.
2. **Standard Package Ingestion**: Third-party functionality is brought in strictly via trusted package managers (`pub.dev`, `npm`, `mavenCentral`) under permissive licenses (Apache 2.0, MIT, BSD-3-Clause).
3. **No Viral / Copyleft Contamination**: AGPLv3, GPLv2/3, and SSPL dependencies are strictly prohibited across client and backend modules.
4. **Third-Party Attribution**: An in-app "Open Source Credits" section will be generated in Settings prior to public release listing all Apache 2.0 and MIT packages.
