# 🚀 Quick Start - Production Deployment

## For Existing Running Containers (Migration)

**Use the migration guide:**
```bash
# Follow step-by-step instructions
cat MIGRATION_GUIDE.md
```

**Quick migration (if you're confident):**
```bash
# 1. Backup
./backup.sh

# 2. Stop and remove
docker-compose down
docker rmi $(docker images | grep -E 'ems-|demo-ems' | awk '{print $3}') 2>/dev/null || true

# 3. Rebuild and start
docker-compose build --no-cache
docker-compose up -d

# 4. Verify
docker-compose ps
curl http://localhost:8080/api/actuator/health
```

---

## For Fresh Deployment (New Server)

**One-command deployment:**
```bash
chmod +x production-deploy.sh
./production-deploy.sh
```

**What it does:**
1. ✅ Checks prerequisites (Docker, Docker Compose)
2. ✅ Verifies .env file and configuration
3. ✅ Creates backup of existing deployment
4. ✅ Stops and removes all containers
5. ✅ Removes old images
6. ✅ Verifies all required files
7. ✅ Runs security checks
8. ✅ Builds new images
9. ✅ Starts services in correct order
10. ✅ Verifies database tables
11. ✅ Checks service health
12. ✅ Validates production configuration

**Time:** 20-30 minutes

---

## Prerequisites

Before running, ensure:

1. **`.env` file exists** with all required variables:
   ```bash
   cp .env.example .env
   nano .env  # Edit with your values
   ```

2. **Required variables in `.env`:**
   ```bash
   DB_PASSWORD=your_strong_password
   DB_ROOT_PASSWORD=your_root_password
   JWT_SECRET=$(openssl rand -base64 64)
   API_URL=http://your-domain:8080/api
   FRONTEND_URL=http://your-domain
   APP_URL=http://your-domain
   CORS_ALLOWED_ORIGINS=http://your-domain
   ```

3. **All code files updated** (from git pull or manual update)

---

## Usage

### Option 1: Interactive (Recommended)
```bash
./production-deploy.sh
```
Follow the prompts and verify each step.

### Option 2: Automated (For CI/CD)
```bash
# Set all environment variables first
export DB_PASSWORD=...
export JWT_SECRET=...
# ... etc

./production-deploy.sh
```

---

## After Deployment

### Verify Deployment
```bash
# Check services
docker-compose ps

# Check health
curl http://localhost:8080/api/actuator/health

# Check logs
docker-compose logs -f
```

### Test Application
1. Open browser: `http://your-domain`
2. Create ROOT account
3. Create organization
4. Test document upload
5. Test user creation

---

## Troubleshooting

### Services Won't Start
```bash
# Check logs
docker-compose logs backend
docker-compose logs frontend
docker-compose logs mysql

# Check configuration
docker-compose config
```

### Database Issues
```bash
# Check MySQL logs
docker-compose logs mysql

# Check tables
docker-compose exec mysql mysql -u root -p employee_management_system -e "SHOW TABLES;"

# Restart backend
docker-compose restart backend
```

### Frontend Issues
```bash
# Rebuild frontend
docker-compose build --no-cache frontend
docker-compose up -d frontend

# Check built files
docker-compose exec frontend ls -la /usr/share/nginx/html/
```

---

## Script Comparison

| Script | Purpose | When to Use |
|--------|---------|-------------|
| `production-deploy.sh` | **Complete production deployment** | Fresh deployment or major update |
| `docker-deploy.sh` | Simple deployment menu | Quick deployments |
| `fresh-deploy.sh` | Full Ubuntu setup + deployment | New server setup |
| `update.sh` | Zero-downtime update | Updates to running system |
| `MIGRATION_GUIDE.md` | Step-by-step migration | Existing containers migration |

---

## Quick Reference

```bash
# Full production deployment
./production-deploy.sh

# Quick restart
docker-compose restart

# View logs
docker-compose logs -f

# Backup
./backup.sh

# Monitor
./monitor.sh

# Stop everything
docker-compose down

# Clean everything (removes data!)
docker-compose down -v
docker system prune -a
```

---

**For detailed instructions, see:**
- `MIGRATION_GUIDE.md` - Step-by-step migration
- `ENV_SETUP_GUIDE.md` - Environment configuration
- `PRODUCTION_READINESS_CHECKLIST.md` - Pre-deployment checklist

