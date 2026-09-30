# WebSocket Realtime Protocol Specification

Application WebSocket Endpoint: `wss://api.squadping.gg/ws/app`

---

## 1. Connection & Handshake

To prevent unauthorized socket connections and simplify routing, WebSocket authentication uses the user's short-lived JWT passed during the opening handshake query parameter:

```
wss://api.squadping.gg/ws/app?token=eyJhbGciOi...
```

Upon successful authentication:
- Client receives: `{"event": "system.ready", "data": {"session_id": "ws_sess_..."}}`
- Presence is registered in-memory as `online`.

---

## 2. Event Envelope Protocol

All messages adhere to a standardized JSON schema:

```json
{
  "event": "namespace.action",
  "data": { ... },
  "id": "optional-client-request-id"
}
```

---

## 3. Client-to-Server Events

### 3.1 `room.subscribe`
Registers socket to receive updates for an active room.
```json
{
  "event": "room.subscribe",
  "data": { "room_id": "r8e9a1b2-..." }
}
```

### 3.2 `chat.send`
Submits a text chat message to the room.
```json
{
  "event": "chat.send",
  "data": {
    "room_id": "r8e9a1b2-...",
    "content": "Rotate to safe zone now"
  }
}
```
**Validation**:
- Length: 1 to 500 characters.
- Rate limit: max 5 messages per 3 seconds.
- Verifies room membership.

### 3.3 `chat.typing.start` / `chat.typing.stop`
Temporary typing indicator signals.
```json
{
  "event": "chat.typing.start",
  "data": { "room_id": "r8e9a1b2-..." }
}
```

### 3.4 `chat.message.delete`
Requests deletion of a message (user own message or admin/owner moderation).
```json
{
  "event": "chat.message.delete",
  "data": {
    "room_id": "r8e9a1b2-...",
    "message_id": "m1a2-..."
  }
}
```

### 3.5 `presence.update`
Updates status and game title (e.g. when launching a game).
```json
{
  "event": "presence.update",
  "data": {
    "status": "gaming",
    "current_game": "PUBG Mobile"
  }
}
```

---

## 4. Server-to-Client Events

### 4.1 `chat.message`
Broadcasts a newly persisted message to all members subscribed to the room.
```json
{
  "event": "chat.message",
  "data": {
    "id": "m1a2-...",
    "room_id": "r8e9a1b2-...",
    "user_id": "f47ac10b-...",
    "username": "sniper_ace",
    "display_name": "SniperAce",
    "content": "Rotate to safe zone now",
    "message_type": "USER",
    "created_at": "2026-09-30T12:05:00Z"
  }
}
```

### 4.2 `chat.system`
Delivers room system notifications.
```json
{
  "event": "chat.system",
  "data": {
    "id": "sys_9812-...",
    "room_id": "r8e9a1b2-...",
    "content": "Ghost joined the room",
    "message_type": "SYSTEM",
    "created_at": "2026-09-30T12:05:05Z"
  }
}
```

### 4.3 `chat.typing`
Broadcasts typing users in a room.
```json
{
  "event": "chat.typing",
  "data": {
    "room_id": "r8e9a1b2-...",
    "user_id": "a1b2c3d4-...",
    "username": "ghost_rider",
    "is_typing": true
  }
}
```

### 4.4 `chat.message.deleted`
Broadcasts message retraction.
```json
{
  "event": "chat.message.deleted",
  "data": {
    "room_id": "r8e9a1b2-...",
    "message_id": "m1a2-..."
  }
}
```

### 4.5 `presence.changed`
Broadcasts friend online/offline/gaming changes to active friends.
```json
{
  "event": "presence.changed",
  "data": {
    "user_id": "a1b2c3d4-...",
    "status": "gaming",
    "current_game": "Call of Duty Mobile"
  }
}
```

---

## 5. Ephemeral In-Memory Typing & Presence State Engine

- Typing states have an automatic **5-second TTL** on the backend. If a user crashes, drops connectivity, or closes the app without emitting `chat.typing.stop`, the timer automatically clears the state and broadcasts `is_typing: false`.
- On socket disconnection (`close` or network drop), all active typing flags and room subscriptions for that socket are immediately evicted without database queries.
