# ─── BUILDER STAGE ───────────────────────────────────────────────────────────
FROM node:20-alpine AS builder
WORKDIR /app

# Install build dependencies if needed (none since we downgraded Prisma)
RUN apk add --no-repeat --no-cache libc6-compat

# Copy dependency configs
COPY package.json package-lock.json ./
RUN npm ci

# Copy schema and generate client
COPY prisma ./prisma
RUN npx prisma generate

# Copy source and config files
COPY tsconfig.json next.config.ts eslint.config.mjs ./
COPY src ./src
COPY public ./public
COPY styles ./src/styles

# Build the Next.js application
ENV NEXT_TELEMETRY_DISABLED=1
ENV NODE_ENV=production
RUN npm run build

# ─── RUNNER STAGE ────────────────────────────────────────────────────────────
FROM node:20-alpine AS runner
WORKDIR /app

ENV NODE_ENV=production
ENV PORT=3000
ENV HOSTNAME="0.0.0.0"

# Create a non-root system user
RUN addgroup --system --gid 1001 nodejs
RUN adduser --system --uid 1001 nextjs

# Copy build output and public files
COPY --from=builder /app/public ./public
COPY --from=builder /app/.next/standalone ./
COPY --from=builder /app/.next/static ./.next/static
COPY --from=builder /app/prisma ./prisma
COPY --from=builder /app/node_modules ./node_modules

# Ensure correct permissions for sqlite database file creation
RUN chown -R nextjs:nodejs /app

USER nextjs

EXPOSE 3000

# Start Next.js stand-alone runner
CMD ["node", "server.js"]
