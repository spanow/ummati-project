# syntax=docker/dockerfile:1

# ---------- Étape 1 : build ----------
FROM node:22-alpine AS build
WORKDIR /build

# package*.json d'abord : npm ci n'est rejoué que si les dépendances changent.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# ---------- Étape 2 : exécution ----------
# L'application est configurée en SSR (outputMode: server) : on sert donc via Node,
# pas via un nginx statique, sinon les routes rendues côté serveur ne fonctionnent pas.
FROM node:22-alpine AS runtime

RUN apk add --no-cache curl \
    && addgroup -S ummati && adduser -S ummati -G ummati

WORKDIR /app
COPY --from=build --chown=ummati:ummati /build/dist/ummati-front ./dist/ummati-front

USER ummati

ENV NODE_ENV=production \
    PORT=4000

EXPOSE 4000

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
    CMD curl -fsS http://localhost:4000/ || exit 1

CMD ["node", "dist/ummati-front/server/server.mjs"]
