# =============================================================================
#  Multi-stage Dockerfile for the Next.js SSR frontend
#
#  Stage 1 — Builder: installs deps and compiles the production bundle
#  Stage 2 — Runner:  minimal Node.js image that serves via server.js
#
#  All NEXT_PUBLIC_* env vars used inside API routes / Server Components are
#  read at RUNTIME from the docker-compose `environment:` block.
#  Only the build-time placeholder is set here so `next build` succeeds
#  inside the container without needing access to the live backend.
# =============================================================================

# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM node:20-alpine AS builder

WORKDIR /app

# Install dependencies first — Docker layer-caches this step until
# package.json or package-lock.json changes.
COPY package*.json ./
RUN npm ci --legacy-peer-deps

# Copy source (node_modules, .next, .env.local excluded via .dockerignore)
COPY . .

# Build-time env — placeholder values so `next build` doesn't error.
# The real values are injected at RUNTIME by docker-compose.
ENV NEXT_PUBLIC_API_URL=http://localhost:8080
ENV NEXT_PUBLIC_MINIO_URL=http://localhost:9000
ENV NEXT_PUBLIC_KEYCLOAK_URL=http://localhost:8081/auth

RUN npm run build

# ── Stage 2: Runner ───────────────────────────────────────────────────────────
FROM node:20-alpine AS runner

WORKDIR /app

ENV NODE_ENV=production
# Port the standalone server listens on
ENV PORT=3000

# Copy only what Next.js standalone output needs
COPY --from=builder /app/public ./public
COPY --from=builder /app/.next/standalone ./
COPY --from=builder /app/.next/static ./.next/static

EXPOSE 3000

# The standalone output generates a self-contained server.js —
# no npm start needed, no node_modules copy required.
CMD ["node", "server.js"]
