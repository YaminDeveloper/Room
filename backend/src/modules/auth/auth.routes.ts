import { FastifyInstance, FastifyPluginOptions } from 'fastify';
import { z } from 'zod';
import bcrypt from 'bcryptjs';
import crypto from 'crypto';
import { prisma } from '../../lib/prisma.js';
import { env } from '../../config/env.js';

const registerSchema = z.object({
  username: z.string().min(3).max(32).regex(/^[a-zA-Z0-9_]+$/),
  displayName: z.string().min(2).max(50),
  email: z.string().email(),
  password: z.string().min(6)
});

const loginSchema = z.object({
  login: z.string().min(1), // email or username
  password: z.string().min(1)
});

export async function authRoutes(fastify: FastifyInstance, _opts: FastifyPluginOptions) {
  fastify.post('/register', async (request, reply) => {
    const parse = registerSchema.safeParse(request.body);
    if (!parse.success) {
      return reply.code(400).send({ error: 'Validation failed', details: parse.error.flatten() });
    }

    const { username, displayName, email, password } = parse.data;
    const lowerUsername = username.toLowerCase();
    const lowerEmail = email.toLowerCase();

    const existing = await prisma.user.findFirst({
      where: {
        OR: [{ username: lowerUsername }, { email: lowerEmail }]
      }
    });

    if (existing) {
      return reply.code(409).send({ error: 'Username or email already registered' });
    }

    const passwordHash = await bcrypt.hash(password, 10);
    const user = await prisma.user.create({
      data: {
        username: lowerUsername,
        displayName,
        email: lowerEmail,
        passwordHash,
        status: 'online'
      }
    });

    const accessToken = fastify.jwt.sign(
      { userId: user.id, username: user.username },
      { expiresIn: env.JWT_EXPIRES_IN }
    );

    const refreshTokenRaw = crypto.randomBytes(32).toString('hex');
    const tokenHash = crypto.createHash('sha256').update(refreshTokenRaw).digest('hex');
    const expiresAt = new Date(Date.now() + env.REFRESH_TOKEN_EXPIRES_DAYS * 24 * 60 * 60 * 1000);

    await prisma.refreshToken.create({
      data: {
        userId: user.id,
        tokenHash,
        expiresAt
      }
    });

    return reply.code(201).send({
      user: {
        id: user.id,
        username: user.username,
        displayName: user.displayName,
        avatarUrl: user.avatarUrl,
        status: user.status
      },
      tokens: {
        accessToken,
        refreshToken: refreshTokenRaw,
        expiresIn: 900
      }
    });
  });

  fastify.post('/login', async (request, reply) => {
    const parse = loginSchema.safeParse(request.body);
    if (!parse.success) {
      return reply.code(400).send({ error: 'Validation failed' });
    }

    const { login, password } = parse.data;
    const lower = login.toLowerCase();

    const user = await prisma.user.findFirst({
      where: {
        OR: [{ username: lower }, { email: lower }]
      }
    });

    if (!user) {
      return reply.code(401).send({ error: 'Invalid credentials' });
    }

    const valid = await bcrypt.compare(password, user.passwordHash);
    if (!valid) {
      return reply.code(401).send({ error: 'Invalid credentials' });
    }

    const accessToken = fastify.jwt.sign(
      { userId: user.id, username: user.username },
      { expiresIn: env.JWT_EXPIRES_IN }
    );

    const refreshTokenRaw = crypto.randomBytes(32).toString('hex');
    const tokenHash = crypto.createHash('sha256').update(refreshTokenRaw).digest('hex');
    const expiresAt = new Date(Date.now() + env.REFRESH_TOKEN_EXPIRES_DAYS * 24 * 60 * 60 * 1000);

    await prisma.refreshToken.create({
      data: {
        userId: user.id,
        tokenHash,
        expiresAt
      }
    });

    return reply.send({
      user: {
        id: user.id,
        username: user.username,
        displayName: user.displayName,
        avatarUrl: user.avatarUrl,
        status: user.status
      },
      tokens: {
        accessToken,
        refreshToken: refreshTokenRaw,
        expiresIn: 900
      }
    });
  });

  fastify.post('/refresh', async (request, reply) => {
    const body = request.body as { refreshToken?: string };
    if (!body?.refreshToken) {
      return reply.code(400).send({ error: 'Missing refresh token' });
    }

    const tokenHash = crypto.createHash('sha256').update(body.refreshToken).digest('hex');
    const tokenRecord = await prisma.refreshToken.findUnique({
      where: { tokenHash },
      include: { user: true }
    });

    if (!tokenRecord || tokenRecord.expiresAt < new Date()) {
      if (tokenRecord) {
        await prisma.refreshToken.delete({ where: { id: tokenRecord.id } });
      }
      return reply.code(401).send({ error: 'Invalid or expired refresh token' });
    }

    // Rotate refresh token
    const newRefreshRaw = crypto.randomBytes(32).toString('hex');
    const newTokenHash = crypto.createHash('sha256').update(newRefreshRaw).digest('hex');
    const expiresAt = new Date(Date.now() + env.REFRESH_TOKEN_EXPIRES_DAYS * 24 * 60 * 60 * 1000);

    await prisma.$transaction([
      prisma.refreshToken.delete({ where: { id: tokenRecord.id } }),
      prisma.refreshToken.create({
        data: {
          userId: tokenRecord.userId,
          tokenHash: newTokenHash,
          expiresAt
        }
      })
    ]);

    const newAccessToken = fastify.jwt.sign(
      { userId: tokenRecord.user.id, username: tokenRecord.user.username },
      { expiresIn: env.JWT_EXPIRES_IN }
    );

    return reply.send({
      accessToken: newAccessToken,
      refreshToken: newRefreshRaw,
      expiresIn: 900
    });
  });

  fastify.post('/logout', { preHandler: [fastify.authenticate] }, async (request, reply) => {
    const body = request.body as { refreshToken?: string };
    if (body?.refreshToken) {
      const tokenHash = crypto.createHash('sha256').update(body.refreshToken).digest('hex');
      await prisma.refreshToken.deleteMany({ where: { tokenHash } });
    }
    return reply.code(204).send();
  });
}
