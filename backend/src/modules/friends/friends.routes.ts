import { FastifyInstance } from 'fastify';
import { z } from 'zod';
import { prisma } from '../../lib/prisma.js';
import { wsManager } from '../../plugins/websocket.js';

export async function friendRoutes(fastify: FastifyInstance) {
  fastify.get('/friends', { preHandler: [fastify.authenticate] }, async (request, reply) => {
    const userId = request.user.userId;

    const friendships = await prisma.friendship.findMany({
      where: { userId },
      include: {
        friend: {
          select: {
            id: true,
            username: true,
            displayName: true,
            avatarUrl: true,
            status: true,
            currentGame: true,
            roomMembers: {
              select: {
                roomId: true,
                room: { select: { name: true, activeGame: true } }
              },
              take: 1
            }
          }
        }
      }
    });

    const pendingRequests = await prisma.friendRequest.findMany({
      where: { receiverId: userId, status: 'pending' },
      include: {
        sender: {
          select: { id: true, username: true, displayName: true, avatarUrl: true }
        }
      }
    });

    return reply.send({
      friends: friendships.map(f => ({
        id: f.friend.id,
        username: f.friend.username,
        displayName: f.friend.displayName,
        avatarUrl: f.friend.avatarUrl,
        status: f.friend.status,
        currentGame: f.friend.currentGame,
        activeRoom: f.friend.roomMembers[0] ? {
          roomId: f.friend.roomMembers[0].roomId,
          name: f.friend.roomMembers[0].room.name,
          game: f.friend.roomMembers[0].room.activeGame
        } : null
      })),
      pendingRequests: pendingRequests.map(r => ({
        requestId: r.id,
        sender: r.sender,
        createdAt: r.createdAt
      }))
    });
  });

  fastify.post('/friends/request', { preHandler: [fastify.authenticate] }, async (request, reply) => {
    const body = request.body as { targetUserId?: string };
    const senderId = request.user.userId;
    const receiverId = body.targetUserId;

    if (!receiverId || receiverId === senderId) {
      return reply.code(400).send({ error: 'Invalid target user' });
    }

    // Check if blocked
    const isBlocked = await prisma.block.findFirst({
      where: {
        OR: [
          { blockerId: senderId, blockedId: receiverId },
          { blockerId: receiverId, blockedId: senderId }
        ]
      }
    });
    if (isBlocked) {
      return reply.code(403).send({ error: 'Unable to send request' });
    }

    const existingReq = await prisma.friendRequest.findFirst({
      where: {
        OR: [
          { senderId, receiverId },
          { senderId: receiverId, receiverId: senderId }
        ]
      }
    });

    if (existingReq) {
      return reply.code(409).send({ error: 'Request already exists' });
    }

    const req = await prisma.friendRequest.create({
      data: { senderId, receiverId, status: 'pending' }
    });

    // Realtime notification
    wsManager.sendToUser(receiverId, 'friend.request_received', {
      requestId: req.id,
      senderId,
      senderUsername: request.user.username
    });

    return reply.code(201).send({ message: 'Friend request sent', requestId: req.id });
  });

  fastify.post('/friends/requests/:id/accept', { preHandler: [fastify.authenticate] }, async (request, reply) => {
    const { id } = request.params as { id: string };
    const userId = request.user.userId;

    const req = await prisma.friendRequest.findUnique({ where: { id } });
    if (!req || req.receiverId !== userId || req.status !== 'pending') {
      return reply.code(404).send({ error: 'Request not found' });
    }

    await prisma.$transaction([
      prisma.friendRequest.update({
        where: { id },
        data: { status: 'accepted' }
      }),
      prisma.friendship.create({
        data: { userId: req.receiverId, friendId: req.senderId }
      }),
      prisma.friendship.create({
        data: { userId: req.senderId, friendId: req.receiverId }
      })
    ]);

    wsManager.sendToUser(req.senderId, 'friend.request_accepted', {
      userId,
      username: request.user.username
    });

    return reply.send({ message: 'Friend request accepted' });
  });

  fastify.post('/friends/requests/:id/reject', { preHandler: [fastify.authenticate] }, async (request, reply) => {
    const { id } = request.params as { id: string };
    const userId = request.user.userId;

    const req = await prisma.friendRequest.findUnique({ where: { id } });
    if (!req || req.receiverId !== userId) {
      return reply.code(404).send({ error: 'Request not found' });
    }

    await prisma.friendRequest.delete({ where: { id } });
    return reply.send({ message: 'Friend request rejected' });
  });

  fastify.delete('/friends/:id', { preHandler: [fastify.authenticate] }, async (request, reply) => {
    const { id: friendId } = request.params as { id: string };
    const userId = request.user.userId;

    await prisma.friendship.deleteMany({
      where: {
        OR: [
          { userId, friendId },
          { userId: friendId, friendId: userId }
        ]
      }
    });

    return reply.code(204).send();
  });

  fastify.post('/users/:id/block', { preHandler: [fastify.authenticate] }, async (request, reply) => {
    const { id: targetId } = request.params as { id: string };
    const userId = request.user.userId;

    if (userId === targetId) {
      return reply.code(400).send({ error: 'Cannot block yourself' });
    }

    await prisma.$transaction([
      prisma.block.upsert({
        where: { blockerId_blockedId: { blockerId: userId, blockedId: targetId } },
        create: { blockerId: userId, blockedId: targetId },
        update: {}
      }),
      prisma.friendship.deleteMany({
        where: {
          OR: [
            { userId, friendId: targetId },
            { userId: targetId, friendId: userId }
          ]
        }
      })
    ]);

    return reply.send({ message: 'User blocked' });
  });
}
