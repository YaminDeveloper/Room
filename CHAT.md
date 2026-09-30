# Room Text Chat Subsystem Architecture

SquadPing decouples voice audio from room text chat. While voice flows directly through LiveKit WebRTC SFU, text chat is orchestrated through the Fastify backend, persisted in PostgreSQL, and broadcast in real-time via WebSockets.

---

## 1. Data Flow Architecture

```
User A (Flutter)
     |
     | [chat.send { roomId, content }]
     v
Fastify WebSocket Layer
     |
     +---> 1. Validate Auth & Rate Limits (Max 5 msg / 3s, Max 500 chars)
     |
     +---> 2. Validate Room Membership & Non-Muted / Non-Blocked Status
     |
     +---> 3. Persist into PostgreSQL (INSERT INTO room_messages ...)
     |
     +---> 4. Fan-out to In-Memory Subscribers via WebSocket (`chat.message`)
                |
                +---> User B (In Room)
                +---> User C (In Room)
                +---> User D (In Room)
```

---

## 2. In-Memory Client Message Cache & Pagination

1. **Initial Room Open**:
   - Client fetches latest 50 messages via `GET /rooms/:id/messages?limit=50`.
   - Results are rendered bottom-up (chronological order with inverted list scroll).
2. **Realtime Ingestion**:
   - As `chat.message` events arrive over WebSocket, they are appended to the tail of the local observable list.
   - Duplication protection: Client dedupes by message `id` in case of socket reconnections.
3. **Scroll to Top (Load Older Messages)**:
   - When the user scrolls to the top of the message list, the client issues `GET /rooms/:id/messages?limit=50&before={oldest_message_timestamp}`.
   - Older messages are prepended seamlessly without scroll jump.

---

## 3. System Messages & Moderation

System messages originate strictly from backend triggers and use `message_type: 'SYSTEM'` with `user_id: NULL`:
- `User joined the room`
- `User left the room`
- `[Admin] User was kicked from the room`
- `[Admin] User was muted`
- `Room was locked by owner`
- `Room was renamed to "..."`

Clients style system messages as subtle, centered badge alerts distinctly separated from player chat bubbles.

---

## 4. Anti-Spam & Content Moderation

- **Character Limit**: Hard ceiling of 500 UTF-8 characters per message.
- **Fastify Leaky Bucket Rate Limiter**: 5 messages per 3 seconds per user session.
- **Duplicate Suppression**: Rejects identical consecutive messages within 5 seconds.
- **Soft Deletion**: Message deletion sets `deleted_at = NOW()`, preserving moderation audit trails for abuse reports while removing the text content from clients.
