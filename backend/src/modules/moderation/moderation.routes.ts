import { FastifyInstance } from 'fastify';
import { z } from 'zod';
import { prisma } from '../../lib/prisma.js';
import { wsManager } from '../../plugins/websocket.js';

const reportSchema = z.object({
  targetUserId: z.string().uuid(),
  roomId: z.string().uuid().optional(),
  reason: z.string().min(2).max(50),
  description: z.string().max(500).optional()
});

export async function moderationRoutes(fastify: FastifyInstance) {
  fastify.post('/reports', { preHandler: [fastify.authenticate] }, async (request, reply) => {
    const parse = reportSchema.safeParse(request.body);
    if (!parse.success) {
      return reply.code(400).send({ error: 'Validation failed' });
    }

    const report = await prisma.report.create({
      data: {
        reporterId: request.user.userId,
        targetUserId: parse.data.targetUserId,
        roomId: parse.data.roomId,
        reason: parse.data.reason,
        description: parse.data.description
      }
    });

    return reply.code(201).send({ message: 'Report submitted for review', reportId: report.id });
  });

  // Kick member from room
  fastify.post('/rooms/:id/kick/:targetUserId', { preHandler: [fastify.authenticate] }, async (request, reply) => {
    const { id: roomId, targetUserId } = request.params as { id: string; targetUserId: string };
    const requesterId = request.user.userId;

    const requesterMember = await prisma.roomMember.findUnique({
      where: { roomId_userId: { roomId, userId: requesterId } }
    });

    if (!requesterMember || (requesterMember.role !== 'owner' && requesterMember.role !== 'admin')) {
      return reply.code(403).send({ error: 'Only room owner or admin can kick members' });
    }

    await prisma.roomMember.deleteMany({
      where: { roomId, userId: targetUserId }
    });

    wsManager.broadcastToRoom(roomId, 'chat.system', {
      id: `sys_${Date.now()}`,
      roomId,
      content: `A player was removed from the room by an admin.`,
      messageType: 'SYSTEM',
      createdAt: new Date().toISOString()
    });

    return reply.send({ message: 'User kicked from room' });
  });

  // Mute member in room
  fastify.post('/rooms/:id/mute/:targetUserId', { preHandler: [fastify.authenticate] }, async (request, reply) => {
    const { id: roomId, targetUserId } = request.params as { id: string; targetUserId: string };
    const requesterId = request.user.userId;

    const requesterMember = await prisma.roomMember.findUnique({
      where: { roomId_userId: { roomId, userId: requesterId } }
    });

    if (!requesterMember || (requesterMember.role !== 'owner' && requesterMember.role !== 'admin')) {
      return reply.code(403).send({ error: 'Only room owner or admin can mute members' });
    }

    await prisma.roomMember.update({
      where: { roomId_userId: { roomId, userId: targetUserId } },
      data: { isMutedByAdmin: true }
    });

    wsManager.broadcastToRoom(roomId, 'chat.system', {
      id: `sys_${Date.now()}`,
      roomId,
      content: `A player was muted by an admin.`,
      messageType: 'SYSTEM',
      createdAt: new Date().toISOString()
    });

    return reply.send({ message: 'User muted by admin' });
  });
}
