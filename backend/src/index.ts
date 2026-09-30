import Fastify from 'fastify';
import cors from '@fastify/cors';
import helmet from '@fastify/helmet';
import jwt from '@fastify/jwt';
import rateLimit from '@fastify/rate-limit';
import websocket from '@fastify/websocket';

import { env } from './config/env.js';
import { prisma } from './lib/prisma.js';
import { wsManager } from './plugins/websocket.js';

import { authRoutes } from './modules/auth/auth.routes.js';
import { userRoutes } from './modules/users/users.routes.js';
import { friendRoutes } from './modules/friends/friends.routes.js';
import { roomRoutes } from './modules/rooms/rooms.routes.js';
import { chatRoutes } from './modules/chat/chat.routes.js';
import { livekitRoutes } from './modules/livekit/livekit.routes.js';
import { moderationRoutes } from './modules/moderation/moderation.routes.js';

async function bootstrap() {
  const fastify = Fastify({
    logger: env.NODE_ENV === 'development' ? {
      transport: {
        target: 'pino-pretty',
        options: { translateTime: 'HH:MM:ss Z', ignore: 'pid,hostname' }
      }
    } : true
  });

  // Security Plugins
  await fastify.register(cors, {
    origin: env.CORS_ORIGIN === '*' ? true : env.CORS_ORIGIN.split(','),
    methods: ['GET', 'POST', 'PATCH', 'DELETE', 'OPTIONS']
  });

  await fastify.register(helmet, {
    contentSecurityPolicy: false
  });

  await fastify.register(rateLimit, {
    max: 100,
    timeWindow: '1 minute'
  });

  // JWT Plugin
  await fastify.register(jwt, {
    secret: env.JWT_SECRET
  });

  fastify.decorate('authenticate', async (request: any, reply: any) => {
    try {
      await request.jwtVerify();
    } catch (err) {
      return reply.code(401).send({ error: 'Unauthorized: Invalid or expired token' });
    }
  });

  // WebSocket Plugin
  await fastify.register(websocket);

  // Health check endpoint
  fastify.get('/health', async () => {
    return {
      status: 'healthy',
      app: 'SquadPing API Gateway',
      timestamp: new Date().toISOString()
    };
  });

  // Realtime App WebSocket endpoint
  fastify.register(async function (wsServer) {
    wsServer.get('/ws/app', { websocket: true }, (connection, req) => {
      const url = new URL(req.url || '', `http://${req.headers.host}`);
      const token = url.searchParams.get('token');

      if (!token) {
        connection.socket.close(4001, 'Unauthorized: Missing token');
        return;
      }

      try {
        const decoded = fastify.jwt.verify<{ userId: string; username: string }>(token);
        wsManager.registerSession(decoded.userId, decoded.username, connection.socket);
      } catch (err) {
        connection.socket.close(4002, 'Unauthorized: Token verification failed');
      }
    });
  });

  // Register REST API Routes under /v1
  await fastify.register(async (api) => {
    api.register(authRoutes, { prefix: '/auth' });
    api.register(userRoutes, { prefix: '/users' });
    api.register(friendRoutes);
    api.register(roomRoutes);
    api.register(chatRoutes);
    api.register(livekitRoutes);
    api.register(moderationRoutes);
  }, { prefix: '/v1' });

  // Graceful Shutdown
  const signals: NodeJS.Signals[] = ['SIGINT', 'SIGTERM'];
  for (const signal of signals) {
    process.on(signal, async () => {
      fastify.log.info(`Received ${signal}, shutting down gracefully...`);
      await fastify.close();
      await prisma.$disconnect();
      process.exit(0);
    });
  }

  try {
    await fastify.listen({ port: env.PORT, host: env.HOST });
    fastify.log.info(`SquadPing backend running on http://${env.HOST}:${env.PORT}`);
  } catch (err) {
    fastify.log.error(err);
    process.exit(1);
  }
}

bootstrap();
