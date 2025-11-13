# 🚀 Docker Deployment Guide - Employee Management System

Complete guide for deploying the Employee Management System using Docker and Docker Compose.

---

## 📋 Table of Contents

1. [Prerequisites](#prerequisites)
2. [Fresh Deployment](#fresh-deployment)
3. [Zero-Downtime Update](#zero-downtime-update)
4. [Production Configuration](#production-configuration)
5. [Monitoring & Maintenance](#monitoring--maintenance)
6. [Troubleshooting](#troubleshooting)

---

## 🔧 Prerequisites

### Required Software

- **Docker**: Version 20.10 or higher
- **Docker Compose**: Version 2.0 or higher
- **Git**: For cloning the repository

### System Requirements

**Minimum:**
- CPU: 2 cores
- RAM: 4GB
- Disk: 20GB free space

**Recommended:**
- CPU: 4 cores
- RAM: 8GB
- Disk: 50GB free space
- SSD for better performance

### Port Requirements

Ensure these ports are available:
- **80**: Frontend (HTTP)
- **443**: Frontend (HTTPS) - if using SSL
- **8080**: Backend API
- **3307**: MySQL (external access - optional)

---

## 📦 Part 1: Fresh Deployment

### Step 1: Clone the Repository

```bash
git clone <repository-url>
cd EmployeeManagementSystem
```

### Step 2: Create Environment Configuration

Create a `.env` file in the project root:

```bash
# .env file for Production

# Database Configuration
DB_NAME=employee_management_system
DB_USERNAME=emsuser
DB_PASSWORD=YourSecurePasswordHere123!
DB_ROOT_PASSWORD=YourRootPasswordHere456!
DB_PORT=3307

# JWT Configuration (CRITICAL - Change in production!)
JWT_SECRET=YourVeryLongAndSecureRandomStringAtLeast256BitsForProductionUseChangeThis!
JWT_EXPIRATION=86400000

# Email Configuration
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-specific-password
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=true
MAIL_SMTP_STARTTLS_REQUIRED=true
MAIL_SMTP_SSL_TRUST=smtp.gmail.com
EMAIL_FROM_NAME=Employee Management System
EMAIL_FROM_ADDRESS=your-email@gmail.com

# Application URLs
APP_URL=http://your-domain.com
CORS_ALLOWED_ORIGINS=http://your-domain.com,https://your-domain.com,http://localhost

# Port Configuration
BACKEND_PORT=8080
FRONTEND_PORT=80
```

**⚠️ IMPORTANT: Security Checklist**

- [ ] Change `JWT_SECRET` to a unique 256+ character random string
- [ ] Use strong database passwords (16+ characters)
- [ ] Use app-specific password for Gmail (not your account password)
- [ ] Update `APP_URL` to your actual domain
- [ ] Update `CORS_ALLOWED_ORIGINS` to your actual domain

### Step 3: Configure Frontend Environment

Update `frontend/src/environments/environment.prod.ts`:

```typescript
export const environment = {
  production: true,
  apiUrl: 'http://your-domain.com:8080/api',
  apiBaseUrl: 'http://your-domain.com:8080',
  frontendUrl: 'http://your-domain.com',
  maxFileSize: 10485760, // 10MB
  allowedFileTypes: ['image/jpeg', 'image/png', 'image/jpg', 'application/pdf']
};
```

### Step 4: Build and Start Services

```bash
# Build and start all services
docker-compose up -d --build

# Verify services are running
docker-compose ps

# Check logs
docker-compose logs -f
```

### Step 5: Verify Deployment

```bash
# Check MySQL
docker-compose exec mysql mysql -u root -p -e "SHOW DATABASES;"

# Check Backend Health
curl http://localhost:8080/api/actuator/health

# Check Frontend
curl http://localhost/
```

### Step 6: Create ROOT User

Access the application at `http://your-domain.com` or `http://localhost` and:

1. Click "Create ROOT Account" (first-time setup)
2. Fill in details:
   - Username: `root`
   - Email: `root@yourdomain.com`
   - Password: Strong password (16+ chars)
3. Login with ROOT account
4. Create your first organization

### Step 7: SSL/HTTPS Setup (Recommended)

Using Nginx as reverse proxy with Let's Encrypt:

```bash
# Install Certbot
sudo apt-get update
sudo apt-get install certbot python3-certbot-nginx

# Get SSL Certificate
sudo certbot --nginx -d your-domain.com

# Auto-renewal
sudo certbot renew --dry-run
```

Update `.env`:
```bash
APP_URL=https://your-domain.com
CORS_ALLOWED_ORIGINS=https://your-domain.com
```

Restart services:
```bash
docker-compose restart backend
```

---

## 🔄 Part 2: Zero-Downtime Update (Rolling Update)

### Strategy Overview

1. Pull latest code
2. Build new images
3. Start new containers
4. Health check
5. Switch traffic
6. Stop old containers

### Step 1: Backup Current State

```bash
# Backup database
docker-compose exec mysql mysqldump -u root -p${DB_ROOT_PASSWORD} employee_management_system > backup_$(date +%Y%m%d_%H%M%S).sql

# Backup uploads
docker cp ems-backend:/app/uploads ./uploads_backup_$(date +%Y%m%d_%H%M%S)

# Backup environment
cp .env .env.backup_$(date +%Y%m%d_%H%M%S)
```

### Step 2: Pull Latest Code

```bash
# Fetch latest changes
git fetch origin

# Check what will be updated
git log HEAD..origin/main --oneline

# Pull changes
git pull origin main
```

### Step 3: Update with Blue-Green Deployment

Create `docker-compose.blue-green.yml`:

```yaml
version: '3.8'

services:
  # New Backend (Blue)
  backend-blue:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: ems-backend-blue
    restart: unless-stopped
    env_file: .env
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/${DB_NAME:-employee_management_system}?useSSL=false&serverTimezone=UTC
      SPRING_DATASOURCE_USERNAME: ${DB_USERNAME}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      SPRING_PROFILES_ACTIVE: prod
    ports:
      - "8081:8080"  # Temporary port
    volumes:
      - uploads_data:/app/uploads
    networks:
      - ems-network
    depends_on:
      mysql:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/api/actuator/health"]
      interval: 10s
      timeout: 5s
      retries: 3
      start_period: 60s

  # New Frontend (Blue)
  frontend-blue:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    container_name: ems-frontend-blue
    restart: unless-stopped
    ports:
      - "81:80"  # Temporary port
    networks:
      - ems-network
    depends_on:
      - backend-blue
    healthcheck:
      test: ["CMD", "wget", "--quiet", "--tries=1", "--spider", "http://localhost/"]
      interval: 10s
      timeout: 5s
      retries: 3

volumes:
  uploads_data:
    external: true
    name: employeemanagementsystem_uploads_data

networks:
  ems-network:
    external: true
    name: employeemanagementsystem_ems-network
```

### Step 4: Execute Rolling Update

```bash
#!/bin/bash
# update.sh - Zero Downtime Update Script

set -e

echo "🚀 Starting Zero-Downtime Update..."

# 1. Build new images
echo "📦 Building new images..."
docker-compose -f docker-compose.blue-green.yml build

# 2. Start new containers (Blue)
echo "🔵 Starting Blue environment..."
docker-compose -f docker-compose.blue-green.yml up -d

# 3. Wait for health checks
echo "⏳ Waiting for health checks..."
sleep 30

# 4. Check backend health
echo "🏥 Checking backend health..."
max_attempts=30
attempt=0
until curl -f http://localhost:8081/api/actuator/health || [ $attempt -eq $max_attempts ]; do
    attempt=$((attempt+1))
    echo "Attempt $attempt/$max_attempts - Waiting for backend..."
    sleep 2
done

if [ $attempt -eq $max_attempts ]; then
    echo "❌ Backend health check failed!"
    docker-compose -f docker-compose.blue-green.yml logs backend-blue
    docker-compose -f docker-compose.blue-green.yml down
    exit 1
fi

echo "✅ Backend is healthy!"

# 5. Check frontend health
echo "🏥 Checking frontend health..."
max_attempts=10
attempt=0
until curl -f http://localhost:81/ || [ $attempt -eq $max_attempts ]; do
    attempt=$((attempt+1))
    echo "Attempt $attempt/$max_attempts - Waiting for frontend..."
    sleep 2
done

if [ $attempt -eq $max_attempts ]; then
    echo "❌ Frontend health check failed!"
    docker-compose -f docker-compose.blue-green.yml logs frontend-blue
    docker-compose -f docker-compose.blue-green.yml down
    exit 1
fi

echo "✅ Frontend is healthy!"

# 6. Switch ports (Update main docker-compose.yml to use new builds)
echo "🔄 Switching traffic..."
docker-compose up -d --no-deps --build backend frontend

# 7. Wait for new main containers
sleep 20

# 8. Stop blue/green containers
echo "🧹 Cleaning up blue environment..."
docker-compose -f docker-compose.blue-green.yml down

# 9. Remove old images
echo "🗑️ Removing old images..."
docker image prune -f

echo "✅ Update completed successfully!"
echo "🎉 Application is running on updated version with zero downtime!"
```

Make the script executable and run:

```bash
chmod +x update.sh
./update.sh
```

### Step 5: Verify Update

```bash
# Check running containers
docker-compose ps

# Check logs for errors
docker-compose logs --tail=100 backend
docker-compose logs --tail=100 frontend

# Test application
curl http://localhost:8080/api/actuator/health
curl http://localhost/
```

### Step 6: Rollback (if needed)

```bash
# Quick rollback script
#!/bin/bash
# rollback.sh

echo "⏪ Rolling back to previous version..."

# Restore from backup
git reset --hard HEAD~1

# Rebuild and restart
docker-compose up -d --build

echo "✅ Rollback completed!"
```

---

## ⚙️ Part 3: Production Configuration

### Environment Variables Reference

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `DB_NAME` | Database name | employee_management_system | Yes |
| `DB_USERNAME` | Database user | emsuser | Yes |
| `DB_PASSWORD` | Database password | - | Yes |
| `DB_ROOT_PASSWORD` | MySQL root password | - | Yes |
| `JWT_SECRET` | JWT signing secret | - | Yes |
| `JWT_EXPIRATION` | Token expiration (ms) | 86400000 | No |
| `MAIL_HOST` | SMTP server | smtp.gmail.com | Yes |
| `MAIL_PORT` | SMTP port | 587 | Yes |
| `MAIL_USERNAME` | Email account | - | Yes |
| `MAIL_PASSWORD` | Email password | - | Yes |
| `APP_URL` | Application URL | - | Yes |
| `CORS_ALLOWED_ORIGINS` | Allowed origins | - | Yes |

### Security Hardening

#### 1. Database Security

```bash
# In .env
DB_PASSWORD=$(openssl rand -base64 32)
DB_ROOT_PASSWORD=$(openssl rand -base64 32)
```

#### 2. JWT Secret

```bash
# Generate strong JWT secret
JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n')
```

#### 3. Firewall Configuration

```bash
# Allow only necessary ports
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow 22/tcp  # SSH
sudo ufw enable

# Block direct database access from outside
sudo ufw deny 3307/tcp
```

#### 4. Docker Security

Update `docker-compose.yml`:

```yaml
backend:
  security_opt:
    - no-new-privileges:true
  read_only: true
  tmpfs:
    - /tmp
  cap_drop:
    - ALL
  cap_add:
    - NET_BIND_SERVICE
```

### Performance Optimization

#### 1. Database Tuning

Create `mysql.cnf`:

```ini
[mysqld]
# Performance
innodb_buffer_pool_size = 2G
innodb_log_file_size = 256M
max_connections = 200
query_cache_size = 64M

# Security
bind-address = 0.0.0.0
skip-name-resolve
```

Mount in docker-compose.yml:

```yaml
mysql:
  volumes:
    - ./mysql.cnf:/etc/mysql/conf.d/custom.cnf:ro
```

#### 2. Backend Optimization

Update Dockerfile:

```dockerfile
# Increase heap size for production
ENTRYPOINT ["java", \
    "-Xms512m", \
    "-Xmx2048m", \
    "-XX:+UseG1GC", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=prod", \
    "-jar", \
    "app.jar"]
```

#### 3. Nginx Caching

Update `frontend/nginx.conf`:

```nginx
http {
    # Enable gzip
    gzip on;
    gzip_vary on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml;
    
    # Cache static files
    location ~* \.(jpg|jpeg|png|gif|ico|css|js)$ {
        expires 30d;
        add_header Cache-Control "public, immutable";
    }
}
```

---

## 📊 Part 4: Monitoring & Maintenance

### Health Checks

```bash
# Check all services
docker-compose ps

# Check logs
docker-compose logs -f --tail=100

# Check resource usage
docker stats

# Check disk space
df -h
docker system df
```

### Automated Monitoring Script

Create `monitor.sh`:

```bash
#!/bin/bash
# monitor.sh - Health monitoring script

# Check services
echo "🏥 Checking service health..."

# MySQL
if docker-compose exec -T mysql mysqladmin ping -h localhost -u root -p${DB_ROOT_PASSWORD} --silent; then
    echo "✅ MySQL: Healthy"
else
    echo "❌ MySQL: Unhealthy"
fi

# Backend
if curl -f http://localhost:8080/api/actuator/health > /dev/null 2>&1; then
    echo "✅ Backend: Healthy"
else
    echo "❌ Backend: Unhealthy"
fi

# Frontend
if curl -f http://localhost/ > /dev/null 2>&1; then
    echo "✅ Frontend: Healthy"
else
    echo "❌ Frontend: Unhealthy"
fi

# Disk space
echo ""
echo "💾 Disk Usage:"
df -h | grep -E '^/dev/'

# Docker volumes
echo ""
echo "📦 Volume Usage:"
docker system df -v | grep -A 20 "Local Volumes"
```

### Automated Backups

Create `backup.sh`:

```bash
#!/bin/bash
# backup.sh - Automated backup script

BACKUP_DIR="/backups"
DATE=$(date +%Y%m%d_%H%M%S)

# Create backup directory
mkdir -p $BACKUP_DIR

# Backup database
echo "📦 Backing up database..."
docker-compose exec -T mysql mysqldump -u root -p${DB_ROOT_PASSWORD} \
    --single-transaction \
    --routines \
    --triggers \
    employee_management_system | gzip > $BACKUP_DIR/db_backup_$DATE.sql.gz

# Backup uploads
echo "📦 Backing up uploads..."
docker cp ems-backend:/app/uploads $BACKUP_DIR/uploads_$DATE

# Backup configuration
echo "📦 Backing up configuration..."
cp .env $BACKUP_DIR/env_$DATE
cp docker-compose.yml $BACKUP_DIR/docker-compose_$DATE.yml

# Retention: Keep only last 7 days
find $BACKUP_DIR -name "*.gz" -mtime +7 -delete
find $BACKUP_DIR -type d -name "uploads_*" -mtime +7 -exec rm -rf {} +

echo "✅ Backup completed: $BACKUP_DIR"
```

Schedule with cron:

```bash
# Run daily at 2 AM
0 2 * * * /path/to/backup.sh >> /var/log/ems-backup.log 2>&1
```

### Log Rotation

Create `docker-compose.override.yml`:

```yaml
version: '3.8'

services:
  backend:
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
  
  frontend:
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
  
  mysql:
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
```

---

## 🔧 Part 5: Troubleshooting

### Common Issues

#### Issue 1: Backend Won't Start

```bash
# Check logs
docker-compose logs backend

# Common causes:
# 1. Database not ready
docker-compose restart mysql
sleep 30
docker-compose restart backend

# 2. Port already in use
sudo lsof -i :8080
# Kill the process or change BACKEND_PORT in .env

# 3. Out of memory
docker stats
# Increase server RAM or reduce heap size in Dockerfile
```

#### Issue 2: Frontend Not Loading

```bash
# Check nginx logs
docker-compose logs frontend

# Check if backend is accessible
curl http://localhost:8080/api/actuator/health

# Rebuild frontend
docker-compose up -d --build frontend
```

#### Issue 3: Database Connection Issues

```bash
# Check MySQL is running
docker-compose ps mysql

# Check MySQL logs
docker-compose logs mysql

# Test connection
docker-compose exec mysql mysql -u $DB_USERNAME -p$DB_PASSWORD -e "SELECT 1"

# Reset database (DANGER - data loss!)
docker-compose down -v
docker-compose up -d
```

#### Issue 4: Email Not Sending

```bash
# Check backend logs
docker-compose logs backend | grep -i mail

# Test SMTP connection
docker-compose exec backend bash
telnet smtp.gmail.com 587

# For Gmail: Use App Password
# https://support.google.com/accounts/answer/185833
```

#### Issue 5: Out of Disk Space

```bash
# Check disk usage
df -h
docker system df

# Clean up
docker system prune -a --volumes

# Remove old images
docker image prune -a

# Remove old containers
docker container prune
```

### Debug Mode

Enable debug logging temporarily:

```bash
# Update .env
LOGGING_LEVEL_ROOT=DEBUG

# Restart
docker-compose restart backend

# Check logs
docker-compose logs -f backend
```

### Performance Issues

```bash
# Check resource usage
docker stats

# Check slow queries (MySQL)
docker-compose exec mysql mysql -u root -p -e "
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 2;
SHOW VARIABLES LIKE 'slow_query_log%';
"

# View slow queries
docker-compose exec mysql tail -f /var/lib/mysql/slow-query.log
```

---

## 📝 Quick Reference Commands

```bash
# Start services
docker-compose up -d

# Stop services
docker-compose down

# Restart specific service
docker-compose restart backend

# View logs
docker-compose logs -f [service_name]

# Execute command in container
docker-compose exec backend bash
docker-compose exec mysql mysql -u root -p

# Scale service (if configured)
docker-compose up -d --scale backend=3

# Update services
docker-compose pull
docker-compose up -d

# Clean up
docker-compose down -v --remove-orphans
docker system prune -a

# Backup database
docker-compose exec mysql mysqldump -u root -p employee_management_system > backup.sql

# Restore database
docker-compose exec -T mysql mysql -u root -p employee_management_system < backup.sql
```

---

## ✅ Production Checklist

Before going live:

- [ ] `.env` file configured with production values
- [ ] JWT_SECRET changed to secure random string
- [ ] Strong database passwords set
- [ ] Email SMTP configured and tested
- [ ] Frontend environment.prod.ts updated with production URLs
- [ ] SSL/HTTPS configured
- [ ] Firewall rules configured
- [ ] Backup script scheduled
- [ ] Monitoring script scheduled
- [ ] Log rotation configured
- [ ] Health checks passing
- [ ] Test user workflows
- [ ] ROOT account created
- [ ] First organization created
- [ ] Document upload tested
- [ ] Email notifications tested
- [ ] All services running without errors

---

## 🆘 Support

If you encounter issues:

1. Check logs: `docker-compose logs -f`
2. Check this troubleshooting guide
3. Check GitHub issues
4. Create new issue with:
   - Docker version
   - Docker Compose version
   - Error logs
   - Steps to reproduce

---

**Deployment Guide Version:** 1.0
**Last Updated:** November 14, 2025
**Docker Version:** 20.10+
**Docker Compose Version:** 2.0+

