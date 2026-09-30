# REST API Specification (Fastify + TypeScript)

Base URL: `https://api.squadping.gg/v1`

---

## 1. Authentication & Session Management

### `POST /auth/register`
Creates a new user profile with unique username and hashed password.
- **Request Body**:
  ```json
  {
    "username": "sniper_ace",
    "display_name": "SniperAce",
    "email": "player@example.com",
    "password": "SecurePassword123!"
  }
  ```
- **Response** (`201 Created`):
  ```json
  {
    "user": {
      "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
      "username": "sniper_ace",
      "display_name": "SniperAce",
      "avatar_url": null,
      "status": "online"
    },
    "tokens": {
      "access_token": "eyJhbGciOi...",
      "refresh_token": "d7a8e2...",
      "expires_in": 900
    }
  }
  ```

### `POST /auth/login`
Authenticates with email/username and password.
- **Request Body**:
  ```json
  {
    "login": "sniper_ace",
    "password": "SecurePassword123!"
  }
  ```
- **Response** (`200 OK`): Same structure as register.

### `POST /auth/refresh`
Rotates refresh token and returns a fresh short-lived access token.
- **Request Body**:
  ```json
  {
    "refresh_token": "d7a8e2..."
  }
  ```
- **Response** (`200 OK`):
  ```json
  {
    "access_token": "eyJhbGciOi...",
    "refresh_token": "e9b1f4...",
    "expires_in": 900
  }
  ```

### `POST /auth/logout`
Revokes the refresh token record in PostgreSQL.
- **Headers**: `Authorization: Bearer <access_token>`
- **Response** (`204 No Content`)

---

## 2. User & Profile

### `GET /me`
Retrieves authenticated user profile and active gaming presence.
- **Headers**: `Authorization: Bearer <access_token>`
- **Response** (`200 OK`):
  ```json
  {
    "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "username": "sniper_ace",
    "display_name": "SniperAce",
    "avatar_url": "https://cdn.squadping.gg/avatars/f47a.png",
    "bio": "CODM scrims & PUBG squad leader",
    "status": "gaming",
    "current_game": "Call of Duty Mobile"
  }
  ```

### `PATCH /me`
Updates profile metadata (display name, bio, avatar, status).
- **Request Body**:
  ```json
  {
    "display_name": "Ace[TLG]",
    "bio": "Road to Conqueror",
    "current_game": "PUBG Mobile"
  }
  ```
- **Response** (`200 OK`): Updated profile object.

### `GET /users/search?q={query}`
Searches users by username or display name with limit 20.

---

## 3. Friends & Social Graph

### `GET /friends`
Lists accepted friends, their current presence status, and current room ID if joinable.
- **Response** (`200 OK`):
  ```json
  [
    {
      "id": "a1b2c3d4-...",
      "username": "ghost_rider",
      "display_name": "Ghost",
      "avatar_url": null,
      "status": "in_room",
      "current_game": "Free Fire",
      "active_room_id": "r8e9a1b2-..."
    }
  ]
  ```

### `POST /friends/request`
Sends a friend request to a target user.
- **Request Body**: `{ "target_user_id": "a1b2c3d4-..." }`

### `POST /friends/requests/:id/accept` / `POST /friends/requests/:id/reject`
Accepts or declines a pending friend request.

### `DELETE /friends/:id`
Removes friend relationship from `friendships` table.

### `POST /users/:id/block` & `DELETE /users/:id/block`
Blocks/unblocks a user.

---

## 4. Rooms & Lobbies

### `GET /rooms`
Retrieves active public rooms for the Lobby with cursor pagination.
- **Query Params**: `limit=20&cursor={created_at}`
- **Response** (`200 OK`):
  ```json
  {
    "rooms": [
      {
        "id": "r8e9a1b2-...",
        "name": "PUBG Squad Push",
        "description": "Erangel hot-drops, mic required",
        "type": "PUBLIC",
        "active_game": "PUBG Mobile",
        "participant_count": 3,
        "max_participants": 4,
        "is_locked": false,
        "owner": {
          "id": "f47ac10b-...",
          "display_name": "SniperAce"
        }
      }
    ],
    "next_cursor": "2026-09-30T12:00:00Z"
  }
  ```

### `POST /rooms`
Creates a new voice room.
- **Request Body**:
  ```json
  {
    "name": "Ranked CODM Grind",
    "description": "Need 1 sniper",
    "type": "PUBLIC",
    "active_game": "Call of Duty Mobile",
    "max_participants": 5
  }
  ```

### `GET /rooms/:id`
Retrieves room details, active participants, and user's role.

### `POST /rooms/:id/join`
Verifies capacity and permissions, adds user to `room_members`.

### `POST /rooms/:id/leave`
Removes user from `room_members`, auto-transfers ownership if owner leaves, or marks room closed if empty.

### `POST /rooms/:id/invite`
Invites a friend to join the room. Sends FCM push notification and in-app WebSocket event.

---

## 5. LiveKit Token Exchange

### `POST /livekit/token`
Generates a cryptographically signed, short-lived LiveKit access token with voice permissions.
- **Request Body**:
  ```json
  {
    "room_id": "r8e9a1b2-..."
  }
  ```
- **Validation**:
  1. Authenticates user JWT.
  2. Verifies user is an active member in `room_members` for `room_id`.
  3. Verifies user is not blocked or banned.
- **Response** (`200 OK`):
  ```json
  {
    "token": "eyJhbGciOi...",
    "livekit_url": "wss://rtc.squadping.gg"
  }
  ```
- **Token Permissions**:
  `canPublish: true`, `canSubscribe: true`, `canPublishData: true`, `hidden: false`. Video publishing is strictly disabled.

---

## 6. Chat History

### `GET /rooms/:id/messages`
Retrieves paginated text messages.
- **Query Params**: `limit=50&before={timestamp}`
- **Response** (`200 OK`):
  ```json
  {
    "messages": [
      {
        "id": "m1a2-...",
        "room_id": "r8e9a1b2-...",
        "user_id": "f47ac10b-...",
        "username": "sniper_ace",
        "display_name": "SniperAce",
        "content": "Landing Pochinki, watch warehouse!",
        "message_type": "USER",
        "created_at": "2026-09-30T12:05:00Z"
      }
    ],
    "has_more": true
  }
  ```

---

## 7. Moderation

### `POST /reports`
Submits abuse report with reason (`harassment`, `spam`, `hate_speech`, `cheating`). Rate-limited to 5 per hour.
