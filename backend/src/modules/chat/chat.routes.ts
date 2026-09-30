import { FastifyInstance } from 'fastify';
import { prisma } from '../../lib/prisma.js';

export async function chatRoutes(fastify: FastifyInstance) {
  // Cursor-based message history retrieval
  fastify.get('/rooms/:id/messages', { preHandler: [fastify.authenticate] }, async (request, reply) => {
    const { id: roomId } = request.params as { id: string };
    const { cursor, limit = '50' } = request.query as { cursor?: string; limit?: string };
    const take = Math.min(parseInt(limit) || 50, 100);

    const messages = await prisma.roomMessage.findMany({
      where: {
        roomId,
        deletedAt: null
      },
      take: take + 1,
      ...(cursor ? { skip: 1, cursor: { id: cursor } } : {}),
      orderBy: { createdAt: 'desc' },
      include: {
        user: { select: { id: true, username: true, displayName: true, avatarUrl: true } }
      }
    });

    let nextCursor: string | null = null;
    if (messages.length > take) {
      const nextItem = messages.pop();
      nextCursor = nextItem?.id || null;
    }

    return reply.send({
      messages: messages.reverse().map(m => ({
        id: m.id,
        roomId: m.roomId,
        userId: m.userId,
        username: m.user?.username || 'System',
        displayName: m.user?.displayName || 'System',
        avatarUrl: m.user?.avatarUrl,
        content: m.content,
        messageType: m.messageType,
        createdAt: m.createdAt
      })),
      nextCursor
    });
  });
}
