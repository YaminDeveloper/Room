import { FastifyInstance } from 'fastify';
import { AccessToken, TrackSource } from 'livekit-server-sdk';
import { prisma } from '../../lib/prisma.js';
import { env } from '../../config/env.js';

export async function livekitRoutes(fastify: FastifyInstance) {
  fastify.post('/livekit/token', { preHandler: [fastify.authenticate] }, async (request, reply) => {
    const { roomId } = request.body as { roomId?: string };
    const userId = request.user.userId;

    if (!roomId) {
      return reply.code(400).send({ error: 'Missing roomId' });
    }

    // 1. Verify room exists
    const room = await prisma.room.findUnique({
      where: { id: roomId }
    });
    if (!room) {
      return reply.code(404).send({ error: 'Room does not exist' });
    }

    // 2. Verify user is registered room member
    const member = await prisma.roomMember.findUnique({
      where: {
        roomId_userId: { roomId, userId }
      },
      include: {
        user: { select: { displayName: true, username: true, avatarUrl: true } }
      }
    });

    if (!member) {
      return reply.code(403).send({ error: 'You are not a member of this voice room' });
    }

    // 3. Mint short-lived LiveKit token (2 hours max)
    const at = new AccessToken(env.LIVEKIT_API_KEY, env.LIVEKIT_API_SECRET, {
      identity: userId,
      name: member.user.displayName,
      metadata: JSON.stringify({
        username: member.user.username,
        avatarUrl: member.user.avatarUrl,
        role: member.role
      }),
      ttl: '2h'
    });

    // 4. Enforce audio-only grants
    at.addGrant({
      roomJoin: true,
      room: roomId,
      canPublish: !member.isMutedByAdmin,
      canSubscribe: true,
      canPublishData: false, // Voice MVP: data sent via app WebSocket
      canPublishSources: [TrackSource.MICROPHONE], // Strictly audio only! Zero video
      hidden: false
    });

    const token = await at.toJwt();

    return reply.send({
      token,
      livekitUrl: env.LIVEKIT_URL,
      roomId,
      identity: userId
    });
  });
}
