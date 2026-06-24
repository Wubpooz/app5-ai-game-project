import { betterAuth } from 'better-auth';
import { prismaAdapter } from 'better-auth/adapters/prisma';
import { prisma } from './prisma';

export const auth = betterAuth({
  database: prismaAdapter(prisma, {
    provider: 'sqlite',
  }),
  emailAndPassword: {
    enabled: true,
    requireEmailVerification: false,
  },
  session: {
    expiresIn: 60 * 60 * 24 * 30, // 30 days
    updateAge: 60 * 60 * 24,       // refresh every 24 hours
    cookieCache: {
      enabled: true,
      maxAge: 60 * 5, // 5 minutes
    },
  },
  user: {
    additionalFields: {
      rating: {
        type: 'number',
        defaultValue: 1200,
      },
      gamesPlayed: {
        type: 'number',
        defaultValue: 0,
      },
      wins: {
        type: 'number',
        defaultValue: 0,
      },
      losses: {
        type: 'number',
        defaultValue: 0,
      },
      draws: {
        type: 'number',
        defaultValue: 0,
      },
    },
  },
});

export type Session = typeof auth.$Infer.Session;
