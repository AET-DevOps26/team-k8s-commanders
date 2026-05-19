# ── Build stage ──────────────────────────────────────────────────────────────
FROM node:24-alpine AS builder

# Bump npm so bundled deps (e.g. picomatch in tinyglobby) match patched versions scanners expect
RUN npm install -g npm@latest

WORKDIR /app

COPY package.json package-lock.json* ./
RUN npm ci

# Copy source (api.ts is generated; include it from the repo)
COPY . .

# Build-time API URL (optional; runtime PUBLIC_API_URL takes precedence)
ARG VITE_API_URL=http://localhost:8080
ENV VITE_API_URL=${VITE_API_URL}

RUN npm run build

# ── Serve stage ───────────────────────────────────────────────────────────────
# `stable-alpine` tracks nginx stable on a current Alpine (patches CVEs in distro libs sooner than pinned old minors)
FROM nginx:stable-alpine AS runner

COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

# Runtime env var — set via docker run -e or Kubernetes env
ENV PUBLIC_API_URL=""

EXPOSE 3000

ENTRYPOINT ["/docker-entrypoint.sh"]
