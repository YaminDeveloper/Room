import { z } from 'zod';
import dotenv from 'dotenv';

dotenv.config();

const envSchema = z.object({
  NODE_ENV: z.enum(['development', 'production', 'test']).default('development'),
  PORT: z.coerce.number().default(8080),
  HOST: z.string().default('0.0.0.0'),
  DATABASE_URL: z.string().default('postgresql://squadping:squadpass@localhost:5432/squadping_db?schema=public'),
  JWT_SECRET: z.string().min(16).default('super_secret_squadping_jwt_key_2026_dev'),
  JWT_EXPIRES_IN: z.string().default('15m'),
  REFRESH_TOKEN_EXPIRES_DAYS: z.coerce.number().default(30),
  LIVEKIT_API_KEY: z.string().default('devkey'),
  LIVEKIT_API_SECRET: z.string().default('secret_must_be_at_least_32_characters_long_12345'),
  LIVEKIT_URL: z.string().default('wss://rtc.squadping.gg'),
  CORS_ORIGIN: z.string().default('*')
});

export const env = envSchema.parse(process.env);
