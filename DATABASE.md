# Database Architecture & Schema Specification

SquadPing uses **PostgreSQL 16+** with **Prisma ORM** (or Drizzle) for robust typing, migration safety, and query performance.

---

## 1. Schema Design Principles

1. **UUIDv4 Primary Keys**: Non-sequential, globally unique IDs prevent enumeration attacks across public APIs.
2. **Strict Foreign Key Constraints**: Cascading deletes applied where appropriate (e.g. room deletion cleans up room memberships and messages).
3. **Optimized Indexes for Real-Time Querying**: Compound indexes tailored for cursor-based pagination and zero-table-scan lookups.
4. **No Heavy Ephemeral State in PostgreSQL**: Typing indicators, audio speaking levels, and sub-second presence heartbeats are handled purely in-memory via WebSockets to eliminate database I/O thrashing.

---

## 2. Core Tables DDL & Models

```sql
-- 1. Users Table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(32) NOT NULL UNIQUE,
    display_name VARCHAR(50) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(512),
    bio VARCHAR(200),
    status VARCHAR(20) NOT NULL DEFAULT 'offline', -- 'online', 'idle', 'in_room', 'gaming', 'offline'
    current_game VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX idx_users_username_lower ON users (LOWER(username));

-- 2. Refresh Tokens Table (Revocable Auth Sessions)
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    device_info VARCHAR(255),
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);

-- 3. Friend Requests Table
CREATE TABLE friend_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    receiver_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'pending', -- 'pending', 'accepted', 'rejected'
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_friend_request UNIQUE(sender_id, receiver_id),
    CONSTRAINT chk_friend_self CHECK (sender_id != receiver_id)
);
CREATE INDEX idx_friend_requests_receiver ON friend_requests (receiver_id, status);
CREATE INDEX idx_friend_requests_sender ON friend_requests (sender_id, status);

-- 4. Friendships Table (Bilateral Friendship)
CREATE TABLE friendships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    friend_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_friendship UNIQUE(user_id, friend_id),
    CONSTRAINT chk_friendship_self CHECK (user_id != friend_id)
);
CREATE INDEX idx_friendships_user ON friendships (user_id);
CREATE INDEX idx_friendships_friend ON friendships (friend_id);

-- 5. User Blocks Table
CREATE TABLE blocks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    blocker_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    blocked_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_block UNIQUE(blocker_id, blocked_id),
    CONSTRAINT chk_block_self CHECK (blocker_id != blocked_id)
);
CREATE INDEX idx_blocks_blocker ON blocks (blocker_id);
CREATE INDEX idx_blocks_blocked ON blocks (blocked_id);

-- 6. Rooms Table
CREATE TABLE rooms (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    name VARCHAR(64) NOT NULL,
    description VARCHAR(255),
    type VARCHAR(20) NOT NULL DEFAULT 'PUBLIC', -- 'PUBLIC', 'PRIVATE'
    max_participants SMALLINT NOT NULL DEFAULT 8,
    is_locked BOOLEAN NOT NULL DEFAULT FALSE,
    active_game VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_rooms_type_locked ON rooms (type, is_locked, created_at DESC);
CREATE INDEX idx_rooms_owner ON rooms (owner_id);

-- 7. Room Members Table
CREATE TABLE room_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL DEFAULT 'member', -- 'owner', 'admin', 'member'
    is_muted_by_admin BOOLEAN NOT NULL DEFAULT FALSE,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_room_member UNIQUE(room_id, user_id)
);
CREATE INDEX idx_room_members_room ON room_members (room_id);
CREATE INDEX idx_room_members_user ON room_members (user_id);

-- 8. Room Messages Table (Persistent Text Chat)
CREATE TABLE room_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL, -- NULL for system messages
    content TEXT NOT NULL,
    message_type VARCHAR(20) NOT NULL DEFAULT 'USER', -- 'USER', 'SYSTEM'
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);
CREATE INDEX idx_room_messages_cursor ON room_messages (room_id, created_at DESC, id DESC);

-- 9. Reports Table (Moderation)
CREATE TABLE reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    room_id UUID REFERENCES rooms(id) ON DELETE SET NULL,
    reason VARCHAR(50) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'REVIEWED', 'DISMISSED', 'ACTIONED'
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_reports_status ON reports (status, created_at DESC);

-- 10. Notifications Table
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(100) NOT NULL,
    body VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL, -- 'FRIEND_REQUEST', 'FRIEND_ACCEPTED', 'ROOM_INVITE'
    metadata JSONB,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_notifications_user_read ON notifications (user_id, is_read, created_at DESC);
```

---

## 3. Cursor-Based Message Pagination Strategy

We strictly avoid `OFFSET` pagination because it causes linear performance degradation and duplicate messages during live message ingestion.

**Query Pattern**:
```sql
SELECT id, room_id, user_id, content, message_type, created_at
FROM room_messages
WHERE room_id = $1
  AND ($2::TIMESTAMPTZ IS NULL OR created_at < $2)
  AND deleted_at IS NULL
ORDER BY created_at DESC
LIMIT 50;
```
The client caches the oldest `created_at` timestamp as its pagination cursor to load older history seamlessly on upward scroll.
