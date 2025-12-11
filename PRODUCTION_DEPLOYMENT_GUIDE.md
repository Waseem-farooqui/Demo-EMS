# Production Deployment Guide - Zero Downtime

This guide provides step-by-step instructions to deploy changes to production without affecting active clients.

## Prerequisites

- SSH access to production server
- Docker and Docker Compose installed
- Backup of current deployment
- `.env` file configured with production values

## Quick Deployment (Recommended)

Use the automated zero-downtime deployment script:

```bash
# Make script executable
chmod +x zero-downtime-deploy.sh

# Deploy all services (backend + frontend)
./zero-downtime-deploy.sh all

# Or deploy only backend
./zero-downtime-deploy.sh backend

# Or deploy only frontend
./zero-downtime-deploy.sh frontend
```

## Manual Deployment Steps

### Step 1: Pre-Deployment Checklist

1. **Verify current system status:**
   ```bash
   docker compose ps
   curl -f https://vertexdigitalsystem.com/api/actuator/health
   ```

2. **Create backup:**
   ```bash
   # Backup database
   docker exec ems-mysql mysqldump -u root -p"$DB_ROOT_PASSWORD" \
     employee_management_system > backup_$(date +%Y%m%d_%H%M%S).sql
   
   # Backup uploaded files
   docker cp ems-backend:/app/uploads ./backup_uploads_$(date +%Y%m%d_%H%M%S)
   ```

3. **Verify .env file:**
   ```bash
   cat .env | grep -E "MAIL_|DB_|JWT_|API_URL|FRONTEND_URL"
   ```

### Step 2: Deploy Backend (Zero Downtime)

**Option A: Rolling Update (Recommended)**

```bash
# 1. Build new backend image
docker compose build --no-cache backend

# 2. Start new backend container on different port
docker run -d \
  --name ems-backend-new \
  --network ems-network \
  -p 8081:8080 \
  --env-file .env \
  -v uploads_data:/app/uploads \
  $(docker images --format "{{.Repository}}:{{.Tag}}" | grep backend | head -1)

# 3. Wait for new container to be healthy
sleep 30
curl -f http://localhost:8081/api/actuator/health

# 4. If healthy, switch traffic
docker stop ems-backend
docker rm ems-backend

# 5. Start production backend
docker compose up -d backend

# 6. Verify health
curl -f http://localhost:8080/api/actuator/health

# 7. Cleanup temporary container
docker stop ems-backend-new 2>/dev/null || true
docker rm ems-backend-new 2>/dev/null || true
```

**Option B: Quick Restart (Minimal Downtime ~30 seconds)**

```bash
# 1. Build new image
docker compose build --no-cache backend

# 2. Restart backend (Docker Compose handles graceful shutdown)
docker compose up -d --no-deps backend

# 3. Wait for health check
sleep 30
curl -f http://localhost:8080/api/actuator/health
```

### Step 3: Deploy Frontend (Zero Downtime)

**Option A: Rolling Update (Recommended)**

```bash
# 1. Build new frontend image
docker compose build --no-cache frontend

# 2. Start new frontend container on different port
docker run -d \
  --name ems-frontend-new \
  --network ems-network \
  -p 8082:80 \
  -p 8443:443 \
  -v ${SSL_DIR:-./ssl}:/etc/nginx/ssl:ro \
  -v certbot_webroot:/var/www/certbot:ro \
  $(docker images --format "{{.Repository}}:{{.Tag}}" | grep frontend | head -1)

# 3. Wait for new container to be healthy
sleep 10
curl -f http://localhost:8082/

# 4. If healthy, switch traffic
docker stop ems-frontend
docker rm ems-frontend

# 5. Start production frontend
docker compose up -d frontend

# 6. Verify health
curl -f http://localhost/ || curl -f https://localhost/

# 7. Cleanup temporary container
docker stop ems-frontend-new 2>/dev/null || true
docker rm ems-frontend-new 2>/dev/null || true
```

**Option B: Quick Restart (Minimal Downtime ~10 seconds)**

```bash
# 1. Build new image
docker compose build --no-cache frontend

# 2. Restart frontend
docker compose up -d --no-deps frontend

# 3. Wait for health check
sleep 10
curl -f http://localhost/ || curl -f https://localhost/
```

### Step 4: Verify Deployment

```bash
# Check all containers are running
docker compose ps

# Check backend health
curl -f https://vertexdigitalsystem.com/api/actuator/health

# Check frontend
curl -f https://vertexdigitalsystem.com/

# Check logs for errors
docker compose logs backend --tail=50
docker compose logs frontend --tail=50
```

### Step 5: Monitor for Issues

```bash
# Watch logs in real-time
docker compose logs -f

# Monitor specific service
docker compose logs -f backend | grep -i error

# Check resource usage
docker stats --no-stream
```

## Rollback Procedure

If deployment fails, rollback immediately:

```bash
# Stop new containers
docker compose stop backend frontend

# Restore from backup
# 1. Restore database
docker exec -i ems-mysql mysql -u root -p"$DB_ROOT_PASSWORD" \
  employee_management_system < backup_YYYYMMDD_HHMMSS.sql

# 2. Restore uploaded files
docker cp ./backup_uploads_YYYYMMDD_HHMMSS/. ems-backend:/app/uploads/

# 3. Start old containers (if you have backup images)
docker compose up -d backend frontend
```

## Database Migrations

If your deployment includes database schema changes:

1. **Check Flyway status:**
   ```bash
   docker exec ems-mysql mysql -u root -p"$DB_ROOT_PASSWORD" \
     -e "USE employee_management_system; SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;"
   ```

2. **Backend will automatically run migrations on startup** (if `SPRING_FLYWAY_ENABLED=true`)

3. **Monitor migration logs:**
   ```bash
   docker compose logs backend | grep -i flyway
   ```

## Best Practices

1. **Deploy during low-traffic hours** (if possible)
2. **Always create backups** before deployment
3. **Test in staging** environment first
4. **Monitor logs** for at least 15 minutes after deployment
5. **Have rollback plan** ready
6. **Notify team** about deployment window

## Troubleshooting

### Backend won't start

```bash
# Check logs
docker compose logs backend

# Check database connectivity
docker exec ems-backend curl -f http://localhost:8080/api/actuator/health

# Verify environment variables
docker exec ems-backend env | grep -E "DB_|MAIL_|JWT_"
```

### Frontend won't start

```bash
# Check logs
docker compose logs frontend

# Verify nginx config
docker exec ems-frontend nginx -t

# Check SSL certificates
docker exec ems-frontend ls -la /etc/nginx/ssl/
```

### Health checks failing

```bash
# Check container status
docker compose ps

# Check resource limits
docker stats --no-stream

# Restart unhealthy container
docker compose restart backend
```

## Deployment Checklist

- [ ] Backup database created
- [ ] Backup of uploaded files created
- [ ] `.env` file verified
- [ ] Code changes committed and pushed
- [ ] New Docker images built successfully
- [ ] Health checks passing
- [ ] Frontend accessible
- [ ] Backend API responding
- [ ] No errors in logs
- [ ] Team notified of deployment

## Quick Reference Commands

```bash
# Build and deploy backend only
docker compose build --no-cache backend && docker compose up -d --no-deps backend

# Build and deploy frontend only
docker compose build --no-cache frontend && docker compose up -d --no-deps frontend

# Build and deploy all
docker compose build --no-cache && docker compose up -d

# View logs
docker compose logs -f

# Check status
docker compose ps

# Restart service
docker compose restart backend

# Stop all services
docker compose down

# Start all services
docker compose up -d
```

## Support

If you encounter issues during deployment:

1. Check logs: `docker compose logs -f`
2. Verify health: `curl -f https://vertexdigitalsystem.com/api/actuator/health`
3. Review backup: Ensure backups are valid
4. Rollback if necessary: Follow rollback procedure above

