import { FastifyInstance } from 'fastify';
import { z } from 'zod';
import { prisma } from '../../lib/prisma.js';

const updateProfileSchema = z.object({
  displayName: z.string().min(2).max(50).optional(),
  bio: z.string().max(200).optional(),
  avatarUrl: z.string().url().max(512).optional(),
  currentGame: z.string().max(64).nullable().optional()
});

export async function userRoutes(fastify: FastifyInstance) {
  fastify.get('/me', { preHandler: [fastify.authenticate] }, async (request, reply) => {
    const user = await prisma.user.findUnique({
      where: { id: request.user.userId },
      select: {
        id: true,
        username: true,
        displayName: true,
        avatarUrl: true,
        bio: true,
        status: true,
        currentGame: true,
        createdAt: true
      }
    });

    if (!user) {
      return reply.code(404).send({ error: 'User not found' });
    }

    return reply.send(user);
  });

  fastify.patch('/me', { preHandler: [fastify.authenticate] }, async (request, reply) => {
    const parse = updateProfileSchema.safeParse(request.body);
    if (!parse.success) {
      return reply.code(400).send({ error: 'Validation failed' });
    }

    const updated = await prisma.user.update({
      where: { id: request.user.userId },
      data: parse.data,
      select: {
        id: true,
        username: true,
        displayName: true,
        avatarUrl: true,
        bio: true,
        status: true,
        currentGame: true
      }
    });

    return reply.send(updated);
  });

  fastify.get('/search', { preHandler: [fastify.authenticate] }, async (request, reply) => {
    const query = (request.query as { q?: string }).q?.trim();
    if (!query || query.length < 2) {
      return reply.send([]);
    }

    const users = await prisma.user.findMany({
      where: {
        OR: [
          { username: { contains: query.toLowerCase(), mode: 'insensitive' } },
          { displayName: { contains: query, mode: 'insensitive' } }
        ],
        NOT: { id: request.user.userId }
      },
      select: {
        id: true,
        username: true,
        displayName: true,
        avatarUrl: true,
        status: true,
        currentGame: true
      },
      take: 20
    });

    return reply.send(users);
  });
}
