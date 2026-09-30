import { FastifyInstance } from 'fastify';
import { z } from 'zod';
import { prisma } from '../../lib/prisma.js';
import { wsManager } from '../../plugins/websocket.js';

const createRoomSchema = z.object({
  name: z.string().min(2).max(64),
  description: z.string().max(255).optional(),
  type: z.enum(['PUBLIC', 'PRIVATE']).default('PUBLIC'),
  maxParticipants: z.number().int().min(2).max(10).default(4),
  isLocked: z.boolean().default(false),
  activeGame: z.string().max(64).default('PUBG Mobile')
});

export async function roomRoutes(fastify: FastifyInstance) {
  // List active rooms with cursor pagination
  fastify.get('/rooms', { preHandler: [fastify.authenticate] }, async (request, reply) => {
    const { cursor, limit = '20', game } = request.query as { cursor?: string; limit?: string; game?: string };
    const take = Math.min(parseInt(limit) || 20, 50);

    const where: any = { type: 'PUBLIC' };
    if (game && game !== 'All') {
      where.activeGame = game;
    }

    const rooms = await prisma.room.findMany({
      where,
      take: take + 1,
      ...(cursor ? { skip: 1, cursor: { id: cursor } } : {}),
      orderBy: { createdAt: 'desc' },
      include: {
        owner: { select: { id: true, username: true, displayName: true } },
        _count: { select: { members: true } }
      }
    });

    let nextCursor: string | null = null;
    if (rooms.length > take) {
      const nextItem = rooms.pop();
      nextCursor = nextItem?.id || null;
    }

    return reply.send({
      rooms: rooms.map(r => ({
        id: r.id,
        name: r.name,
        description: r.description,
        type: r.type,
        activeGame: r.activeGame,
        isLocked: r.isLocked,
        participantCount: r._count.members,
        maxParticipants: r.maxParticipants,
        owner: r.owner,
        createdAt: r.createdAt
      })),
      nextCursor
    });
  });

  // Create room
  fastify.post('/rooms', { preHandler: [fastify.authenticate] }, async (request, reply) => {
    const parse = createRoomSchema.safeParse(request.body);
    if (!parse.success) {
      return reply.code(400).send({ error: 'Validation failed', details: parse.error.flatten() });
    }

    const userId = request.user.userId;
    const data = parse.data;

    const room = await prisma.room.create({
      data: {
        ownerId: userId,
        name: data.name,
        description: data.description,
        type: data.type,
        maxParticipants: data.maxParticipants,
        isLocked: data.isLocked,
        activeGame: data.activeGame,
        members: {
          create: {
            userId,
            role: 'owner'
          }
        }
      },
      include: {
        owner: { select: { id: true, username: true, displayName: true } }
      }
    });

    return reply.code(201).send(room);
  });

  // Get room details
  fastify.get('/rooms/:id', { preHandler: [fastify.authenticate] }, async (request, reply) => {
    const { id } = request.params as { id: string };

    const room = await prisma.room.findUnique({
      where: { id },
      include: {
        owner: { select: { id: true, username: true, displayName: true } },
        members: {
          include: {
            user: { select: { id: true, username: true, displayName: true, avatarUrl: true, status: true } }
          }
        }
      }
    });

    if (!room) {
      return reply.code(404).send({ error: 'Room not found' });
    }

    return reply.send(room);
  });

  // Join room
  fastify.post('/rooms/:id/join', { preHandler: [fastify.authenticate] }, async (request, reply) => {
    const { id: roomId } = request.params as { id: string };
    const userId = request.user.userId;

    const room = await prisma.room.findUnique({
      where: { id: roomId },
      include: { _count: { select: { members: true } } }
    });

    if (!room) {
      return reply.code(404).send({ error: 'Room not found' });
    }

    if (room._count.members >= room.maxParticipants) {
      return reply.code(409).send({ error: 'Room is full' });
    }

    // Leave any prior rooms
    await prisma.roomMember.deleteMany({ where: { userId } });

    const member = await prisma.roomMember.create({
      data: {
        roomId,
        userId,
        role: room.ownerId === userId ? 'owner' : 'member'
      },
      include: {
        user: { select: { id: true, username: true, displayName: true, avatarUrl: true } }
      }
    });

    // Notify room subscribers
    wsManager.broadcastToRoom(roomId, 'chat.system', {
      id: `sys_${Date.now()}`,
      roomId,
      content: `${request.user.username} joined the voice room.`,
      messageType: 'SYSTEM',
      createdAt: new Date().toISOString()
    });

    return reply.send(member);
  });

  // Leave room
  fastify.post('/rooms/:id/leave', { preHandler: [fastify.authenticate] }, async (request, reply) => {
    const { id: roomId } = request.params as { id: string };
    const userId = request.user.userId;

    await prisma.roomMember.deleteMany({
      where: { roomId, userId }
    });

    // Check if room is empty
    const remainingCount = await prisma.roomMember.count({ where: { roomId } });
    if (remainingCount === 0) {
      await prisma.room.delete({ where: { id: roomId } });
    } else {
      // If owner left, transfer to oldest member
      const room = await prisma.room.findUnique({ where: { id: roomId } });
      if (room?.ownerId === userId) {
        const nextOwner = await prisma.roomMember.findFirst({
          where: { roomId },
          orderBy: { joinedAt: 'asc' }
        });
        if (nextOwner) {
          await prisma.$transaction([
            prisma.room.update({ where: { id: roomId }, data: { ownerId: nextOwner.userId } }),
            prisma.roomMember.update({ where: { id: nextOwner.id }, data: { role: 'owner' } })
          ]);
        }
      }
    }

    wsManager.broadcastToRoom(roomId, 'chat.system', {
      id: `sys_${Date.now()}`,
      roomId,
      content: `${request.user.username} left the voice room.`,
      messageType: 'SYSTEM',
      createdAt: new Date().toISOString()
    });

    return reply.code(204).send();
  });
}
