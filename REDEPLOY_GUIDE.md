# Redeploy Frontend and Backend Containers

This guide explains how to redeploy the frontend and backend containers after making code changes.

## Quick Redeploy (Recommended)

### Option 1: Rebuild and Restart All Services
```bash
# Rebuild images with latest code changes and restart containers
docker compose up -d --build
```

### Option 2: Rebuild Specific Services Only
```bash
# Rebuild only backend
docker compose up -d --build backend

# Rebuild only frontend
docker compose up -d --build frontend

# Rebuild both
docker compose up -d --build backend frontend
```

### Option 3: Force Rebuild (No Cache)
If you want to ensure a completely fresh build:
```bash
# Rebuild everything from scratch (no cache)
docker compose build --no-cache
docker compose up -d
```

## Step-by-Step Process

### 1. Stop Running Containers (Optional)
```bash
docker compose down
```

### 2. Rebuild Images
```bash
# Rebuild all services
docker compose build

# Or rebuild specific services
docker compose build backend
docker compose build frontend
```

### 3. Start Services
```bash
# Start all services
docker compose up -d

# Or start specific services
docker compose up -d backend
docker compose up -d frontend
```

## Using the Deployment Script

You can also use the provided deployment script:

```bash
# Make script executable (if not already)
chmod +x docker-deploy.sh

# Run the script
./docker-deploy.sh
```

Then select option `1` to deploy (build and start).

## Verify Deployment

### Check Container Status
```bash
docker compose ps
```

### View Logs
```bash
# All services
docker compose logs -f

# Backend only
docker compose logs -f backend

# Frontend only
docker compose logs -f frontend
```

### Check Health
```bash
# Backend health check
curl http://localhost:8080/api/actuator/health

# Frontend (should return HTML)
curl http://localhost:80
```

## Common Issues and Solutions

### Issue: Changes Not Reflected
**Solution**: Force rebuild without cache
```bash
docker compose build --no-cache
docker compose up -d
```

### Issue: Port Already in Use
**Solution**: Stop existing containers first
```bash
docker compose down
docker compose up -d --build
```

### Issue: Frontend Build Fails
**Solution**: Check Node.js version and dependencies
```bash
# Rebuild frontend with verbose output
docker compose build --no-cache frontend
```

### Issue: Backend Build Fails
**Solution**: Check Maven dependencies
```bash
# Rebuild backend with verbose output
docker compose build --no-cache backend
```

## Quick Reference Commands

```bash
# Rebuild and restart everything
docker compose up -d --build

# Rebuild specific service
docker compose up -d --build <service-name>

# View logs
docker compose logs -f <service-name>

# Stop all services
docker compose down

# Stop and remove volumes (⚠️ deletes data)
docker compose down -v

# Restart without rebuild
docker compose restart

# Check status
docker compose ps

# Execute command in running container
docker compose exec backend <command>
docker compose exec frontend <command>
```

## For Your Current Changes

Since you've made changes to:
- **Backend**: `EmployeeService.java`, `NotificationRepository.java`
- **Frontend**: `smtp-configuration.component.html`, `smtp-configuration.component.css`

Run this command to redeploy both:

```bash
docker compose up -d --build backend frontend
```

This will:
1. Rebuild the backend image with your Java changes
2. Rebuild the frontend image with your Angular changes
3. Restart both containers
4. Keep the database and other services running

## Production Deployment

For production, you may want to:
1. Build images with tags
2. Push to a container registry
3. Pull and deploy on production server

```bash
# Build with tags
docker compose build
docker tag ems-backend:latest your-registry/ems-backend:v1.0.0
docker tag ems-frontend:latest your-registry/ems-frontend:v1.0.0

# Push to registry
docker push your-registry/ems-backend:v1.0.0
docker push your-registry/ems-frontend:v1.0.0

# On production server, pull and deploy
docker pull your-registry/ems-backend:v1.0.0
docker pull your-registry/ems-frontend:v1.0.0
docker compose up -d
```

