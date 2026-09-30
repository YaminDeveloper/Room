# Security, Access Control & Threat Modeling

Security in SquadPing is built around **zero client trust** and **least-privilege credential scopes**.

---

## 1. Secrets Management Policy

The mobile client (Flutter / Android) **never** receives or stores:
- `LIVEKIT_API_KEY`
- `LIVEKIT_API_SECRET`
- `DATABASE_URL` / `DATABASE_PASSWORD`
- `JWT_SIGNING_SECRET`
- `FCM_SERVER_KEY`

All administrative and infrastructure credentials reside exclusively on the server, loaded via secure environment variables (`.env`). The client only ever handles short-lived, scoped access tokens.

---

## 2. Server-Side Authorization Matrix

Every mutation and token exchange must validate all layers before execution:

| Operation | Required Role / Checks | Verification Source |
|---|---|---|
| **Issue LiveKit Token** | Authenticated user + Verified room member + Not muted + Room not deleted | `room_members`, `rooms`, `blocks` |
| **Join Private Room** | Authenticated user + Valid invitation OR Room Owner/Admin invite bypass | `room_invitations`, `rooms` |
| **Send Chat Message** | Room member + Non-muted + Character length check + Under rate-limit | `room_members`, In-memory rate limiter |
| **Kick Member** | Room Owner or Admin only; target cannot be owner | `room_members.role` |
| **Mute Member (Admin)** | Room Owner or Admin only | `room_members.role` |
| **Delete Room** | Room Owner only | `rooms.owner_id` |
| **Delete Message** | Message Author OR Room Owner OR Room Admin | `room_messages.user_id`, `room_members.role` |

---

## 3. Rate Limiting & Anti-Abuse Specifications

Rate limiting is enforced at the Fastify gateway using a sliding-window algorithm:

| Endpoint / Action | Limit | Window | Action on Violation |
|---|---|---|---|
| `POST /auth/login` | 5 attempts | 60 seconds | `429 Too Many Requests` + Captcha requirement |
| `POST /auth/register` | 3 attempts | 1 hour | `429 Too Many Requests` |
| `POST /livekit/token` | 10 requests | 60 seconds | Temporary token throttle |
| `POST /friends/request` | 10 requests | 5 minutes | Anti-spam friend flood prevention |
| `POST /rooms` | 5 rooms | 10 minutes | Room creation rate ceiling |
| `chat.send` (WebSocket) | 5 messages | 3 seconds | Dropped with warning event |
| `POST /reports` | 5 reports | 1 hour | Prevents report griefing |

---

## 4. Threat Matrix & Mitigations

1. **Token Impersonation / Hijack**:
   - Short access token lifespan (15 minutes).
   - LiveKit tokens restricted to exact room and identity with maximum 2-hour TTL.
2. **Eavesdropping on Private Rooms**:
   - LiveKit SFU denies subscription if user is not authorized in token claims.
   - SRTP (Secure Real-time Transport Protocol) encryption for all media in transit.
3. **Friend Request & Invite Spam**:
   - Bilateral blocking: if User A blocks User B, User B cannot see User A in search, cannot send requests, and cannot join User A's rooms.
4. **Malicious Payloads & Injection**:
   - Strict Zod/Moshi schema parsing for all JSON payloads.
   - Parameterized SQL queries via ORM to completely eliminate SQL injection.
