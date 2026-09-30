import { FastifyInstance } from 'fastify';
import { WebSocket } from 'ws';
import { prisma } from '../lib/prisma.js';

interface SocketSession {
  userId: string;
  username: string;
  socket: WebSocket;
  activeRoomId?: string;
  typingTimeout?: NodeJS.Timeout;
}

class WebSocketManager {
  private userSessions = new Map<string, SocketSession>();
  private roomSubscribers = new Map<string, Set<string>>();

  registerSession(userId: string, username: string, socket: WebSocket) {
    // If an existing session exists, close it
    const existing = this.userSessions.get(userId);
    if (existing) {
      existing.socket.terminate();
    }

    const session: SocketSession = { userId, username, socket };
    this.userSessions.set(userId, session);

    // Update user status in DB
    prisma.user.update({
      where: { id: userId },
      data: { status: 'online' }
    }).catch(() => {});

    this.broadcastPresence(userId, 'online');

    socket.on('close', () => {
      this.handleDisconnect(userId);
    });

    socket.on('message', async (raw: string) => {
      try {
        const payload = JSON.parse(raw.toString());
        await this.handleClientEvent(session, payload);
      } catch (err) {
        socket.send(JSON.stringify({ event: 'error', data: { message: 'Invalid JSON payload' } }));
      }
    });

    socket.send(JSON.stringify({
      event: 'system.ready',
      data: { userId, message: 'Connected to SquadPing Realtime Engine' }
    }));
  }

  private async handleClientEvent(session: SocketSession, payload: { event: string; data?: any }) {
    const { event, data } = payload;
    const { userId, username, socket } = session;

    switch (event) {
      case 'room.subscribe': {
        const roomId = data?.roomId;
        if (!roomId) return;
        this.subscribeToRoom(roomId, userId);
        session.activeRoomId = roomId;
        socket.send(JSON.stringify({ event: 'room.subscribed', data: { roomId } }));
        break;
      }

      case 'chat.send': {
        const roomId = data?.roomId || session.activeRoomId;
        const content = data?.content?.trim();
        if (!roomId || !content || content.length > 500) return;

        // Verify membership
        const member = await prisma.roomMember.findUnique({
          where: { roomId_userId: { roomId, userId } }
        });
        if (!member || member.isMutedByAdmin) return;

        // Persist message
        const msg = await prisma.roomMessage.create({
          data: {
            roomId,
            userId,
            content,
            messageType: 'USER'
          },
          include: {
            user: { select: { displayName: true, username: true, avatarUrl: true } }
          }
        });

        // Broadcast to subscribers
        this.broadcastToRoom(roomId, 'chat.message', {
          id: msg.id,
          roomId: msg.roomId,
          userId: msg.userId,
          username: msg.user?.username || username,
          displayName: msg.user?.displayName || username,
          avatarUrl: msg.user?.avatarUrl,
          content: msg.content,
          messageType: msg.messageType,
          createdAt: msg.createdAt
        });
        break;
      }

      case 'chat.typing.start': {
        const roomId = data?.roomId || session.activeRoomId;
        if (!roomId) return;

        if (session.typingTimeout) clearTimeout(session.typingTimeout);

        this.broadcastToRoom(roomId, 'chat.typing', {
          roomId,
          userId,
          username,
          isTyping: true
        }, userId);

        // Auto-expire typing indicator after 5 seconds
        session.typingTimeout = setTimeout(() => {
          this.broadcastToRoom(roomId, 'chat.typing', {
            roomId,
            userId,
            username,
            isTyping: false
          }, userId);
        }, 5000);
        break;
      }

      case 'chat.typing.stop': {
        const roomId = data?.roomId || session.activeRoomId;
        if (!roomId) return;
        if (session.typingTimeout) clearTimeout(session.typingTimeout);

        this.broadcastToRoom(roomId, 'chat.typing', {
          roomId,
          userId,
          username,
          isTyping: false
        }, userId);
        break;
      }

      case 'presence.update': {
        const status = data?.status || 'online';
        const currentGame = data?.currentGame || null;
        await prisma.user.update({
          where: { id: userId },
          data: { status, currentGame }
        });
        this.broadcastPresence(userId, status, currentGame);
        break;
      }
    }
  }

  subscribeToRoom(roomId: string, userId: string) {
    if (!this.roomSubscribers.has(roomId)) {
      this.roomSubscribers.set(roomId, new Set());
    }
    this.roomSubscribers.get(roomId)!.add(userId);
  }

  unsubscribeFromRoom(roomId: string, userId: string) {
    const subs = this.roomSubscribers.get(roomId);
    if (subs) {
      subs.delete(userId);
      if (subs.size === 0) this.roomSubscribers.delete(roomId);
    }
  }

  broadcastToRoom(roomId: string, event: string, data: any, excludeUserId?: string) {
    const subscriberIds = this.roomSubscribers.get(roomId);
    if (!subscriberIds) return;

    const payload = JSON.stringify({ event, data });
    for (const subId of subscriberIds) {
      if (subId === excludeUserId) continue;
      const session = this.userSessions.get(subId);
      if (session && session.socket.readyState === WebSocket.OPEN) {
        session.socket.send(payload);
      }
    }
  }

  sendToUser(userId: string, event: string, data: any) {
    const session = this.userSessions.get(userId);
    if (session && session.socket.readyState === WebSocket.OPEN) {
      session.socket.send(JSON.stringify({ event, data }));
    }
  }

  broadcastPresence(userId: string, status: string, currentGame?: string | null) {
    const payload = JSON.stringify({
      event: 'presence.changed',
      data: { userId, status, currentGame }
    });
    for (const session of this.userSessions.values()) {
      if (session.socket.readyState === WebSocket.OPEN) {
        session.socket.send(payload);
      }
    }
  }

  private handleDisconnect(userId: string) {
    const session = this.userSessions.get(userId);
    if (session) {
      if (session.typingTimeout) clearTimeout(session.typingTimeout);
      if (session.activeRoomId) {
        this.unsubscribeFromRoom(session.activeRoomId, userId);
      }
      this.userSessions.delete(userId);
    }

    prisma.user.update({
      where: { id: userId },
      data: { status: 'offline' }
    }).catch(() => {});

    this.broadcastPresence(userId, 'offline');
  }
}

export const wsManager = new WebSocketManager();
