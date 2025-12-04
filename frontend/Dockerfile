# Multi-stage build for Angular Frontend

# Stage 1: Build
FROM node:18-alpine AS build

WORKDIR /app

# Install network tools for troubleshooting
RUN apk add --no-cache curl bind-tools

# Configure DNS for better connectivity
RUN echo "nameserver 8.8.8.8" > /etc/resolv.conf && \
    echo "nameserver 8.8.4.4" >> /etc/resolv.conf && \
    echo "nameserver 1.1.1.1" >> /etc/resolv.conf || true

# Test DNS resolution before npm install
RUN echo "Testing DNS resolution..." && \
    nslookup registry.npmjs.org || echo "DNS test completed" || true

# Build arguments for environment variables
ARG API_URL
ARG API_BASE_URL
ARG FRONTEND_URL

# Set default values if not provided
ENV API_URL=${API_URL:-http://localhost:8080/api}
ENV API_BASE_URL=${API_BASE_URL:-http://localhost:8080}
ENV FRONTEND_URL=${FRONTEND_URL:-http://localhost}

# Copy package files
COPY package*.json ./

# Install dependencies with retry logic and timeout settings
# Use npm install instead of npm ci for compatibility
RUN npm config set registry https://registry.npmjs.org/ && \
    npm config set fetch-retries 5 && \
    npm config set fetch-retry-mintimeout 20000 && \
    npm config set fetch-retry-maxtimeout 120000 && \
    npm install --legacy-peer-deps || \
    (echo "First attempt failed, waiting 10 seconds and retrying..." && \
     sleep 10 && \
     npm install --legacy-peer-deps) || \
    (echo "Second attempt failed, waiting 20 seconds and retrying..." && \
     sleep 20 && \
     npm install --legacy-peer-deps --verbose)

# Copy source code
COPY . .

# Replace placeholders in environment.prod.ts with actual values
RUN sed -i "s|\${API_URL}|${API_URL}|g" src/environments/environment.prod.ts && \
    sed -i "s|\${API_BASE_URL}|${API_BASE_URL}|g" src/environments/environment.prod.ts && \
    sed -i "s|\${FRONTEND_URL}|${FRONTEND_URL}|g" src/environments/environment.prod.ts

# Build for production
RUN npm run build -- --configuration=production

# List build output for debugging
RUN echo "=== Checking build output ===" && \
    ls -la dist/ && \
    echo "=== Contents of dist/frontend ===" && \
    ls -la dist/frontend/ && \
    echo "=== Checking for browser folder ===" && \
    ls -la dist/frontend/browser/ || echo "No browser folder found"

# Stage 2: Serve with Nginx
FROM nginx:alpine

# Remove default nginx static assets
RUN rm -rf /usr/share/nginx/html/*

# Copy custom nginx config
COPY nginx.conf /etc/nginx/nginx.conf

# Create SSL directory (certificates will be mounted as volume)
RUN mkdir -p /etc/nginx/ssl && \
    mkdir -p /var/www/certbot && \
    chmod 755 /etc/nginx/ssl && \
    chmod 755 /var/www/certbot

# Copy built app from build stage (Angular 17+ uses dist/frontend/browser)
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html

# Set proper permissions for nginx user
RUN chown -R nginx:nginx /usr/share/nginx/html && \
    chmod -R 755 /usr/share/nginx/html

# Verify files copied and check permissions
RUN ls -la /usr/share/nginx/html/ && \
    echo "index.html exists:" && \
    test -f /usr/share/nginx/html/index.html && echo "YES" || echo "NO"

# Expose ports 80 (HTTP) and 443 (HTTPS)
EXPOSE 80 443

# Health check (check HTTPS if available, fallback to HTTP)
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s \
  CMD wget --quiet --tries=1 --spider https://localhost/ 2>/dev/null || \
      wget --quiet --tries=1 --spider http://localhost/ || exit 1

# Start nginx
CMD ["nginx", "-g", "daemon off;"]

