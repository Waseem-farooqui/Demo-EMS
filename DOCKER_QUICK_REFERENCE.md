# 🐳 Docker Quick Reference - Employee Management System

## Quick Deploy Commands

### Windows
```cmd
docker-deploy.bat
```

### Linux/Mac
```bash
chmod +x docker-deploy.sh
./docker-deploy.sh
```

### Manual
```bash
# Start everything
docker compose up -d

# Stop everything  
docker compose down

# View logs
docker compose logs -f

# Restart
docker compose restart
```

## Service URLs

| Service | URL | Purpose |
|---------|-----|---------|
| Frontend | http://localhost | Main application UI |
| Backend API | http://localhost:8080/api | REST API endpoints |
| Health Check | http://localhost:8080/api/actuator/health | System health status |
| MySQL | localhost:3307 | Database (internal) |

## Essential Commands

```bash
# Check status
docker compose ps

# View all logs
docker compose logs -f

# View specific service logs
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f mysql

# Restart a service
docker compose restart backend

# Rebuild after code changes
docker compose build --no-cache
docker compose up -d

# Backup database
docker compose exec mysql mysqldump -uemsuser -pemspassword employee_management_system > backup.sql

# Restore database
docker compose exec -i mysql mysql -uemsuser -pemspassword employee_management_system < backup.sql

# Access MySQL shell
docker compose exec mysql mysql -uemsuser -pemspassword employee_management_system

# Clean everything (WARNING: Deletes all data!)
docker compose down -v --rmi all
```

## Environment Variables (.env)

**Critical Settings:**
```env
# Database
DB_PASSWORD=YourSecurePassword
DB_ROOT_PASSWORD=YourRootPassword

# JWT
JWT_SECRET=GenerateSecure256BitKey

# Email
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password

# App
APP_URL=https://yourdomain.com
CORS_ALLOWED_ORIGINS=https://yourdomain.com
```

## Troubleshooting

| Problem | Solution |
|---------|----------|
| Port already in use | Change `BACKEND_PORT` or `FRONTEND_PORT` in `.env` |
| Database connection failed | Wait 30s for MySQL to initialize |
| Backend unhealthy | Check logs: `docker compose logs backend` |
| Permission denied | Run as administrator/sudo |
| Out of disk space | Clean: `docker system prune -a` |

## Health Check Status

```bash
# Quick health check
curl http://localhost:8080/api/actuator/health

# Expected response
{"status":"UP"}
```

## Container Resource Usage

```bash
# View real-time stats
docker compose stats

# Check container details
docker compose ps
```

## First-Time Setup

1. Copy environment file:
   ```bash
   cp .env.example .env
   ```

2. Edit `.env` with your values

3. Start services:
   ```bash
   docker compose up -d
   ```

4. Check health:
   ```bash
   curl http://localhost:8080/api/actuator/health
   ```

5. Access application:
   - Frontend: http://localhost
   - Backend: http://localhost:8080

## Production Checklist

- [ ] Set strong database passwords
- [ ] Generate secure JWT secret
- [ ] Configure email with app password
- [ ] Set correct APP_URL
- [ ] Restrict CORS origins
- [ ] Enable SSL/TLS (reverse proxy)
- [ ] Setup automated backups
- [ ] Monitor logs regularly
- [ ] Keep images updated

## File Structure

```
.
├── docker-compose.yaml       # Main orchestration file
├── .env                       # Environment configuration
├── Dockerfile                 # Backend image definition
├── frontend/
│   ├── Dockerfile            # Frontend image definition
│   └── nginx.conf            # Nginx configuration
├── docker-deploy.sh          # Linux/Mac deployment script
└── docker-deploy.bat         # Windows deployment script
```

## Support Commands

```bash
# Check Docker version
docker --version
docker compose version

# View Docker disk usage
docker system df

# Clean unused images/containers
docker system prune -a

# View network details
docker network ls
docker network inspect ems-network

# View volume details  
docker volume ls
docker volume inspect ems_mysql_data
docker volume inspect ems_uploads_data
```

---

**Ready for Production! 🚀**

For detailed documentation, see: [DOCKER_DEPLOYMENT.md](./DOCKER_DEPLOYMENT.md)

