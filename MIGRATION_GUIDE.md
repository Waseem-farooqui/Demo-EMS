# 🔄 Migration Guide - Existing Containers to Updated Configuration

## Overview

This guide helps you migrate your **currently running containers** to the new configuration with all the fixes we've applied.

**Time Required:** 15-30 minutes  
**Downtime:** Minimal (can be done with zero downtime using backup/restore)

---

## ⚠️ Pre-Migration Checklist

Before starting, ensure you have:

- [ ] **Backup created** (database + uploads)
- [ ] **`.env` file** with all required variables
- [ ] **Access to server** (SSH or direct)
- [ ] **Docker and Docker Compose** installed
- [ ] **15-30 minutes** of maintenance window

---

## 📋 Step-by-Step Migration Process

### Step 1: Create Backup (CRITICAL)

**⚠️ DO NOT SKIP THIS STEP!**

```bash
# Create backup directory
mkdir -p /backups/ems/migration_$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/backups/ems/migration_$(date +%Y%m%d_%H%M%S)"

# Backup database
docker-compose exec -T mysql mysqldump \
    -u root -p"${DB_ROOT_PASSWORD}" \
    --single-transaction \
    --routines \
    --triggers \
    employee_management_system > "$BACKUP_DIR/database_backup.sql"

# Backup uploads
docker cp ems-backend:/app/uploads "$BACKUP_DIR/uploads" 2>/dev/null || echo "No uploads to backup"

# Backup .env file
cp .env "$BACKUP_DIR/.env.backup" 2>/dev/null || echo "No .env to backup"

echo "✅ Backup created in: $BACKUP_DIR"
```

**Verify backup:**
```bash
ls -lh "$BACKUP_DIR"
# Should see: database_backup.sql, uploads/, .env.backup
```

---

### Step 2: Verify .env File

**Check if `.env` exists and has all required variables:**

```bash
# Check if .env exists
if [ ! -f .env ]; then
    echo "⚠️  .env file not found!"
    echo "Creating from .env.example..."
    cp .env.example .env
    echo "⚠️  Please edit .env with your values before continuing!"
    exit 1
fi

# Verify critical variables
source .env
MISSING=()

[ -z "$DB_PASSWORD" ] && MISSING+=("DB_PASSWORD")
[ -z "$DB_ROOT_PASSWORD" ] && MISSING+=("DB_ROOT_PASSWORD")
[ -z "$JWT_SECRET" ] && MISSING+=("JWT_SECRET")

if [ ${#MISSING[@]} -gt 0 ]; then
    echo "❌ Missing required variables: ${MISSING[*]}"
    echo "Please add them to .env file"
    exit 1
fi

echo "✅ .env file verified"
```

**Required variables in `.env`:**
```bash
# Database
DB_NAME=employee_management_system
DB_USERNAME=emsuser
DB_PASSWORD=your_password
DB_ROOT_PASSWORD=your_root_password

# JWT
JWT_SECRET=your_256_bit_secret

# URLs (for production)
API_URL=http://your-domain:8080/api
API_BASE_URL=http://your-domain:8080
FRONTEND_URL=http://your-domain
APP_URL=http://your-domain
CORS_ALLOWED_ORIGINS=http://your-domain

# Email (if using)
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password
```

---

### Step 3: Stop Current Containers

```bash
# Stop all services gracefully
echo "Stopping services..."
docker-compose stop

# Wait for graceful shutdown
sleep 10

# Verify stopped
docker-compose ps
# Should show all containers as "Exited"
```

---

### Step 4: Remove Old Containers and Images

```bash
# Remove containers (keeps volumes)
echo "Removing containers..."
docker-compose down

# Remove old images to force rebuild
echo "Removing old images..."
docker rmi $(docker images | grep -E 'demo-ems|employeemanagementsystem|ems-' | awk '{print $3}') 2>/dev/null || true

# Clean up dangling images
docker image prune -f

echo "✅ Old containers and images removed"
```

---

### Step 5: Verify Updated Files

**Check that all fixes are in place:**

```bash
# Check database init.sql exists
if [ ! -f database/init.sql ]; then
    echo "❌ database/init.sql missing!"
    exit 1
fi

# Check compose.yaml has environment variables
if ! grep -q "SPRING_DATASOURCE_URL" compose.yaml; then
    echo "❌ compose.yaml missing database URL config!"
    exit 1
fi

# Check frontend Dockerfile has build args
if ! grep -q "ARG API_URL" frontend/Dockerfile; then
    echo "❌ frontend/Dockerfile missing build args!"
    exit 1
fi

# Check application-prod.properties uses mysql container
if ! grep -q "mysql:3306" src/main/resources/application-prod.properties; then
    echo "❌ application-prod.properties not using mysql container!"
    exit 1
fi

echo "✅ All updated files verified"
```

---

### Step 6: Rebuild Containers

```bash
# Pull latest code (if using git)
git pull origin main 2>/dev/null || echo "Not using git, skipping pull"

# Rebuild all containers with no cache
echo "Rebuilding containers (this may take 5-10 minutes)..."
docker-compose build --no-cache

echo "✅ Containers rebuilt"
```

---

### Step 7: Start Services

```bash
# Start all services
echo "Starting services..."
docker-compose up -d

# Wait for services to initialize
echo "Waiting for services to start (60 seconds)..."
sleep 60

# Check status
docker-compose ps
```

---

### Step 8: Verify Database Tables

**Check if tables were created:**

```bash
# Wait for MySQL to be ready
echo "Waiting for MySQL..."
sleep 30

# Check if documents table exists
docker-compose exec mysql mysql -u root -p"${DB_ROOT_PASSWORD}" \
    employee_management_system -e "SHOW TABLES;" 2>/dev/null

# Specifically check documents table
docker-compose exec mysql mysql -u root -p"${DB_ROOT_PASSWORD}" \
    employee_management_system -e "DESCRIBE documents;" 2>/dev/null

if [ $? -eq 0 ]; then
    echo "✅ Database tables exist"
else
    echo "⚠️  Tables may not exist yet, checking backend logs..."
    docker-compose logs backend | grep -i "table\|create\|jpa" | tail -20
fi
```

---

### Step 9: Verify Services Health

```bash
# Check backend health
echo "Checking backend health..."
MAX_ATTEMPTS=30
ATTEMPT=0

while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
    if curl -f http://localhost:8080/api/actuator/health > /dev/null 2>&1; then
        echo "✅ Backend is healthy"
        break
    fi
    ATTEMPT=$((ATTEMPT + 1))
    echo "  Attempt $ATTEMPT/$MAX_ATTEMPTS - Waiting..."
    sleep 2
done

# Check frontend
if curl -f http://localhost/ > /dev/null 2>&1; then
    echo "✅ Frontend is responding"
else
    echo "⚠️  Frontend may not be ready yet"
fi
```

---

### Step 10: Verify Configuration

**Check that production configs are active:**

```bash
# Check backend uses prod profile
docker-compose logs backend | grep -i "profile" | tail -5
# Should see: "The following profiles are active: prod"

# Check frontend uses production URLs
docker-compose exec frontend cat /usr/share/nginx/html/main*.js | grep -i "your-domain" | head -3
# Should see your production URLs, not localhost

# Check environment variables
docker-compose exec backend env | grep -E "SPRING_PROFILES_ACTIVE|JWT_SECRET|DB_" | head -10
```

---

### Step 11: Test Application

```bash
# Test backend API
curl http://localhost:8080/api/actuator/health
# Should return: {"status":"UP"}

# Test frontend
curl -I http://localhost/
# Should return: HTTP/1.1 200 OK

# Test database connection
docker-compose exec backend curl http://localhost:8080/api/actuator/health
# Should show database is connected
```

---

### Step 12: Monitor Logs

**Watch for errors:**

```bash
# Monitor all logs
docker-compose logs -f

# Or monitor specific service
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f mysql
```

**Look for:**
- ✅ "Started EmployeeManagementSystemApplication"
- ✅ "The following profiles are active: prod"
- ✅ "Hibernate: create table" (if tables being created)
- ❌ Any ERROR or EXCEPTION messages

---

## 🔄 Zero-Downtime Migration (Advanced)

If you need zero downtime:

### Option A: Blue-Green Deployment

```bash
# 1. Build new containers
docker-compose build --no-cache

# 2. Start new containers on different ports
docker-compose -f compose.yaml up -d backend-blue frontend-blue

# 3. Test new containers
curl http://localhost:8081/api/actuator/health

# 4. Switch traffic (update load balancer/proxy)
# 5. Stop old containers
docker-compose stop backend frontend

# 6. Start new containers on main ports
docker-compose up -d backend frontend

# 7. Remove blue containers
docker-compose -f compose.yaml down backend-blue frontend-blue
```

### Option B: Database Migration Only

```bash
# 1. Backup database
./backup.sh

# 2. Stop backend only
docker-compose stop backend

# 3. Rebuild backend
docker-compose build --no-cache backend

# 4. Start backend
docker-compose up -d backend

# 5. Frontend continues running (no changes needed)
```

---

## 🐛 Troubleshooting

### Tables Not Created

**Problem:** `documents` table doesn't exist

**Solution:**
```bash
# Check backend logs
docker-compose logs backend | grep -i "error\|exception"

# Check JPA configuration
docker-compose exec backend env | grep JPA_DDL_AUTO
# Should be: JPA_DDL_AUTO=update

# Restart backend
docker-compose restart backend

# Wait and check again
sleep 30
docker-compose exec mysql mysql -u root -p employee_management_system -e "SHOW TABLES;"
```

### Frontend Shows Wrong URLs

**Problem:** Frontend still using localhost or old IPs

**Solution:**
```bash
# Rebuild frontend with no cache
docker-compose build --no-cache frontend
docker-compose up -d frontend

# Verify
docker-compose exec frontend cat /usr/share/nginx/html/main*.js | grep -i "api"
```

### Database Connection Failed

**Problem:** Backend can't connect to database

**Solution:**
```bash
# Check MySQL is running
docker-compose ps mysql

# Check network connectivity
docker-compose exec backend ping mysql

# Check database URL
docker-compose exec backend env | grep SPRING_DATASOURCE_URL
# Should use: jdbc:mysql://mysql:3306/...

# Check credentials
docker-compose exec backend env | grep DB_PASSWORD
```

### Container Won't Start

**Problem:** Container exits immediately

**Solution:**
```bash
# Check logs
docker-compose logs backend --tail=50

# Check health
docker-compose ps

# Try starting manually
docker-compose up backend
# (Press Ctrl+C after seeing error)
```

---

## ✅ Post-Migration Verification

### Checklist

- [ ] All containers running: `docker-compose ps`
- [ ] Backend healthy: `curl http://localhost:8080/api/actuator/health`
- [ ] Frontend accessible: `curl http://localhost/`
- [ ] Database tables exist: Check via MySQL
- [ ] Production config active: Check backend logs
- [ ] Frontend uses production URLs: Check built JS
- [ ] No errors in logs: `docker-compose logs | grep -i error`
- [ ] Application functional: Test login, create user, etc.

### Quick Test Commands

```bash
# Full health check
./monitor.sh

# Or manually:
curl http://localhost:8080/api/actuator/health
curl http://localhost/
docker-compose ps
docker-compose logs --tail=20
```

---

## 📊 Migration Summary

**What Changed:**
1. ✅ Database URL now uses `mysql` container name
2. ✅ All secrets moved to `.env` file
3. ✅ Frontend URLs configurable via build args
4. ✅ OCR API key moved to environment variable
5. ✅ Database initialization script added
6. ✅ Production configurations properly set

**What to Monitor:**
- Database table creation
- Backend startup logs
- Frontend build process
- Service health checks

---

## 🆘 Rollback Plan

If something goes wrong:

```bash
# 1. Stop new containers
docker-compose down

# 2. Restore backup
BACKUP_DIR="/backups/ems/migration_YYYYMMDD_HHMMSS"
docker-compose up -d mysql
sleep 30
docker-compose exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" < "$BACKUP_DIR/database_backup.sql"

# 3. Restore .env
cp "$BACKUP_DIR/.env.backup" .env

# 4. Restore uploads
docker cp "$BACKUP_DIR/uploads" ems-backend:/app/uploads

# 5. Start old version (if you have old images)
docker-compose up -d
```

---

## 📝 Next Steps After Migration

1. **Verify all functionality works**
2. **Test user login and creation**
3. **Test document upload**
4. **Check email notifications** (if configured)
5. **Monitor logs for 24 hours**
6. **Set up automated backups**
7. **Configure monitoring alerts**

---

**Migration Complete!** 🎉

If you encounter any issues, check the troubleshooting section or review the logs.

