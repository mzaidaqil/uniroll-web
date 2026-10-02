# Stage 1: build the static files (HTML, JS, CSS) with Node
FROM node:24-alpine AS build
WORKDIR /app

# Install dependencies first so this layer is cached until package-lock.json changes
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# Stage 2: nginx serves the files from stage 1; Node isn't in the final image
FROM nginx:1.30-alpine

# The nginx image fills ${...} variables in /etc/nginx/templates/*.template at startup
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html

# Defaults for docker compose; Cloud Run overrides both
ENV PORT=8080
ENV BACKEND_URL=http://backend:8080
EXPOSE 8080
