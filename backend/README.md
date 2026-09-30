# SquadPing Backend Service

Ultra-lightweight Fastify + TypeScript + PostgreSQL + LiveKit SFU backend service for SquadPing gaming voice hangout.

---

## 1. Quick Start with Docker (Recommended)

To deploy on your VPS:

```bash
# 1. Unzip archive on your server
unzip squadping-backend.zip
cd backend

# 2. Copy and customize environment variables
cp .env.example .env

# 3. Spin up PostgreSQL, Fastify API, and LiveKit Server
docker compose up -d --build

# 4. Run Prisma database schema migration
docker compose exec api npx prisma db push
```

The services will be available at:
- Fastify REST API: `http://localhost:8080/v1`
- Realtime App WebSocket: `ws://localhost:8080/ws/app`
- LiveKit Signaling: `ws://localhost:7880`
- LiveKit WebRTC UDP Media: Ports `50000-50050`

---

## 2. Local Development Without Docker

Requirements:
- Node.js >= 20
- PostgreSQL >= 15

```bash
# Install dependencies
npm install

# Push Prisma schema to local DB
npx prisma db push

# Generate Prisma Client
npx prisma generate

# Run in watch mode
npm run dev
```

---

## 3. Endpoints Overview

- **Auth**: `POST /v1/auth/register`, `POST /v1/auth/login`, `POST /v1/auth/refresh`, `POST /v1/auth/logout`
- **User Profile**: `GET /v1/users/me`, `PATCH /v1/users/me`, `GET /v1/users/search?q={query}`
- **Friends**: `GET /v1/friends`, `POST /v1/friends/request`, `POST /v1/friends/requests/:id/accept`, `POST /v1/users/:id/block`
- **Rooms**: `GET /v1/rooms`, `POST /v1/rooms`, `GET /v1/rooms/:id`, `POST /v1/rooms/:id/join`, `POST /v1/rooms/:id/leave`
- **LiveKit Voice Token**: `POST /v1/livekit/token` (Validates membership & mints audio-only WebRTC token)
- **Room Text Chat**: `GET /v1/rooms/:id/messages?limit=50&cursor={id}`
- **Moderation**: `POST /v1/reports`, `POST /v1/rooms/:id/kick/:userId`, `POST /v1/rooms/:id/mute/:userId`
- **Realtime WebSocket**: `ws://localhost:8080/ws/app?token={jwt}` (Handles `chat.send`, `chat.typing`, `presence.update`)
