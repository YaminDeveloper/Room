# Production Deployment & Infrastructure Specification

SquadPing follows a lean single-host deployment model designed for rapid setup, minimal ops overhead, and horizontal scalability when player traffic surges.

---

## 1. Production Topology & Domain Architecture

```
Internet / Mobile Clients
        |
        +---- api.squadping.gg (HTTPS / WSS) ----> [Nginx Reverse Proxy]
        |                                                  |
        |                                                  v
        |                                         [Fastify API (Port 8080)]
        |                                                  |
        |                                                  v
        |                                         [PostgreSQL 16 (Port 5432)]
        |
        +---- rtc.squadping.gg (WSS Signaling) ---> [LiveKit Server (Port 7880)]
        |
        +---- Media Traffic (WebRTC UDP/TCP) ----> [LiveKit SFU (Ports 7881, 50000-60000)]
```

### Routing Rules:
- **`api.squadping.gg`**: Proxied via Nginx to Fastify API server with HTTP/2 and WebSocket upgrade support.
- **`rtc.squadping.gg`**: Bypasses API server; mapped directly to LiveKit Server for high-throughput WebRTC signaling and UDP voice packet streaming.

---

## 2. Docker Compose Configuration (`docker-compose.yml`)

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    container_name: squadping_postgres
    restart: unless-stopped
    environment:
      POSTGRES_USER: ${DB_USER:-squadping}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
      POSTGRES_DB: squadping_db
    volumes:
      - pgdata:/var/lib/postgresql/data
    ports:
      - "127.0.0.1:5432:5432"
    networks:
      - squadping_net

  api:
    build:
      context: ./backend
      dockerfile: Dockerfile
    container_name: squadping_api
    restart: unless-stopped
    depends_on:
      - postgres
    environment:
      NODE_ENV: production
      PORT: 8080
      DATABASE_URL: postgresql://${DB_USER:-squadping}:${DB_PASSWORD}@postgres:5432/squadping_db
      JWT_SECRET: ${JWT_SECRET}
      LIVEKIT_API_KEY: ${LIVEKIT_API_KEY}
      LIVEKIT_API_SECRET: ${LIVEKIT_API_SECRET}
      LIVEKIT_HOST: http://livekit:7880
    ports:
      - "127.0.0.1:8080:8080"
    networks:
      - squadping_net

  livekit:
    image: livekit/livekit-server:latest
    container_name: squadping_livekit
    restart: unless-stopped
    command: --config /etc/livekit.yaml
    volumes:
      - ./config/livekit.yaml:/etc/livekit.yaml
    network_mode: "host" # Mandatory for low-latency WebRTC UDP port ranges

  nginx:
    image: nginx:alpine
    container_name: squadping_nginx
    restart: unless-stopped
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/conf.d:/etc/nginx/conf.d
      - /etc/letsencrypt:/etc/letsencrypt:ro
    depends_on:
      - api
    networks:
      - squadping_net

volumes:
  pgdata:

networks:
  squadping_net:
    driver: bridge
```

---

## 3. LiveKit Server Configuration (`livekit.yaml`)

```yaml
port: 7880
rtc:
  tcp_port: 7881
  port_range_start: 50000
  port_range_end: 60000
  use_external_ip: true

audio:
  # Optimize Opus for mobile gaming callouts
  max_bitrate: 32000
  audio_level_interval: 100

keys:
  ${LIVEKIT_API_KEY}: ${LIVEKIT_API_SECRET}

turn:
  enabled: true
  domain: rtc.squadping.gg
  tls_port: 5349
```

---

## 4. Operational Boundaries

- **No Over-Engineering**: Strictly no Kubernetes, Kafka, RabbitMQ, or microservice meshes in Phase 0–10. A single high-bandwidth VPS ($20-$40/mo) easily handles 1,000+ concurrent active voice participants.
- **Backups**: Nightly encrypted `pg_dump` snapshots transferred to S3/Cloud Storage.
