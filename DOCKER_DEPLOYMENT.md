# Employee Management System - Docker Deployment Guide

This guide explains how to deploy the Employee Management System using Docker and Docker Compose for production environments.

## 📋 Prerequisites

- Docker 20.10+ installed
- Docker Compose 2.0+ installed
- Minimum 2GB RAM available
- Minimum 10GB disk space

## 🚀 Quick Start

### 1. Setup Environment Variables

Copy the example environment file and configure it:

```bash
# Linux/Mac
cp .env.example .env

# Windows
copy .env.example .env
```

Edit `.env` file with your production values:

```bash
# Required changes:
# 1. Database passwords
DB_PASSWORD=YourSecurePassword123!
DB_ROOT_PASSWORD=YourRootPassword456!

# 2. JWT Secret (generate with: openssl rand -base64 64)
JWT_SECRET=your-generated-jwt-secret-here

# 3. Email configuration
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password

# 4. Application URL
APP_URL=https://yourdomain.com

# 5. CORS allowed origins
CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://www.yourdomain.com
```

### 2. Deploy with One Command

**Linux/Mac:**
```bash
chmod +x docker-deploy.sh
./docker-deploy.sh
```

**Windows:**
```cmd
docker-deploy.bat
```

Or manually:
```bash
docker compose build --no-cache
docker compose up -d
```

### 3. Access the Application

- **Frontend:** http://localhost (or your configured port)
- **Backend API:** http://localhost:8080/api
- **Health Check:** http://localhost:8080/api/actuator/health

## 📦 What's Included

The Docker setup includes three services:

1. **MySQL Database** (mysql:8.0)
   - Persistent storage with Docker volumes
   - Health checks configured
   - UTF-8 MB4 encoding for full Unicode support

2. **Backend Application** (Spring Boot)
   - Java 11 runtime
   - Tesseract OCR for document processing
   - Multi-stage build for optimized image size
   - Health checks configured

3. **Frontend Application** (Angular + Nginx)
   - Nginx web server
   - Optimized production build
   - SPA routing configured
   - Static asset caching

## 🔧 Configuration

### Environment Variables

All configuration is done through environment variables in the `.env` file:

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_NAME` | Database name | employee_management_system |
| `DB_USERNAME` | Database user | emsuser |
| `DB_PASSWORD` | Database password | *Required* |
| `DB_ROOT_PASSWORD` | MySQL root password | *Required* |
| `JWT_SECRET` | JWT signing key | *Required* |
| `MAIL_HOST` | SMTP server | smtp.gmail.com |
| `MAIL_PORT` | SMTP port | 587 |
| `MAIL_USERNAME` | Email username | *Required* |
| `MAIL_PASSWORD` | Email password | *Required* |
| `APP_URL` | Application URL | http://localhost |
| `BACKEND_PORT` | Backend port | 8080 |
| `FRONTEND_PORT` | Frontend port | 80 |

### Email Provider Configuration

**Gmail:**
```env
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_SMTP_SSL_TRUST=smtp.gmail.com
```

**Outlook/Hotmail:**
```env
MAIL_HOST=smtp-mail.outlook.com
MAIL_PORT=587
MAIL_SMTP_SSL_TRUST=smtp-mail.outlook.com
```

**Office365:**
```env
MAIL_HOST=smtp.office365.com
MAIL_PORT=587
MAIL_SMTP_SSL_TRUST=smtp.office365.com
```

## 🛠️ Common Operations

### View Logs
```bash
# All services
docker compose logs -f

# Specific service
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f mysql
```

### Restart Services
```bash
# All services
docker compose restart

# Specific service
docker compose restart backend
```

### Stop Services
```bash
docker compose down
```

### Update Application
```bash
# Pull latest code
git pull

# Rebuild and restart
docker compose build --no-cache
docker compose up -d
```

### Backup Database
```bash
# Linux/Mac
docker compose exec -T mysql mysqldump -u${DB_USERNAME} -p${DB_PASSWORD} ${DB_NAME} > backup_$(date +%Y%m%d_%H%M%S).sql

# Windows
docker compose exec -T mysql mysqldump -uemsuser -pemspassword employee_management_system > backup.sql
```

### Restore Database
```bash
# Linux/Mac
docker compose exec -T mysql mysql -u${DB_USERNAME} -p${DB_PASSWORD} ${DB_NAME} < backup.sql

# Windows
type backup.sql | docker compose exec -T mysql mysql -uemsuser -pemspassword employee_management_system
```

### Access MySQL Shell
```bash
docker compose exec mysql mysql -u${DB_USERNAME} -p${DB_PASSWORD} ${DB_NAME}
```

### Check Container Status
```bash
docker compose ps
```

### View Resource Usage
```bash
docker compose stats
```

## 🔍 Troubleshooting

### Container Won't Start

Check logs:
```bash
docker compose logs backend
```

Common issues:
- Database connection failure: Check if MySQL is ready (wait 30s after start)
- Port already in use: Change `BACKEND_PORT` or `FRONTEND_PORT` in `.env`
- Missing environment variables: Ensure all required variables are set

### Database Connection Issues

1. Verify MySQL is running:
```bash
docker compose ps mysql
```

2. Check MySQL logs:
```bash
docker compose logs mysql
```

3. Test connection:
```bash
docker compose exec mysql mysqladmin ping -h localhost -u root -p
```

### Application Health Check Failing

1. Check backend health endpoint:
```bash
curl http://localhost:8080/api/actuator/health
```

2. If unhealthy, check backend logs:
```bash
docker compose logs backend
```

### Permission Issues

If you encounter permission issues with uploads:
```bash
docker compose exec backend chown -R appuser:root /app/uploads
```

### Clean Installation

Remove everything and start fresh:
```bash
# WARNING: This will delete all data!
docker compose down -v --rmi all
docker compose build --no-cache
docker compose up -d
```

## 📊 Monitoring

### Health Checks

All services include health checks:

- **MySQL:** Checked every 10s
- **Backend:** Checked every 30s (starts after 60s)
- **Frontend:** Checked every 30s (starts after 10s)

View health status:
```bash
docker compose ps
```

### Actuator Endpoints

The backend exposes monitoring endpoints:

- Health: `http://localhost:8080/api/actuator/health`
- Info: `http://localhost:8080/api/actuator/info`
- Metrics: `http://localhost:8080/api/actuator/metrics`

## 🔒 Security Considerations

### Production Checklist

- [ ] Change all default passwords in `.env`
- [ ] Generate a secure JWT secret (at least 256 bits)
- [ ] Use app-specific passwords for email
- [ ] Enable SSL/TLS (use reverse proxy like Nginx or Traefik)
- [ ] Restrict CORS origins to your domain only
- [ ] Regular database backups
- [ ] Keep Docker images updated
- [ ] Monitor logs for suspicious activity
- [ ] Use Docker secrets for sensitive data (advanced)

### Enable SSL/TLS

For production, use a reverse proxy with SSL:

**Nginx Example:**
```nginx
server {
    listen 443 ssl http2;
    server_name yourdomain.com;

    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;

    location / {
        proxy_pass http://localhost:80;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

## 🌐 Scaling

### Horizontal Scaling

To run multiple backend instances:

```yaml
backend:
  # ... existing config ...
  deploy:
    replicas: 3
```

Use a load balancer (Nginx, HAProxy, etc.) to distribute traffic.

### Vertical Scaling

Adjust resource limits:

```yaml
backend:
  # ... existing config ...
  deploy:
    resources:
      limits:
        cpus: '2'
        memory: 4G
      reservations:
        cpus: '1'
        memory: 2G
```

## 📁 Volume Management

Docker volumes persist data:

- `mysql_data`: Database files
- `uploads_data`: Uploaded documents

Backup volumes:
```bash
# Create backup
docker run --rm -v ems_mysql_data:/data -v $(pwd):/backup alpine tar czf /backup/mysql_data_backup.tar.gz -C /data .

# Restore backup
docker run --rm -v ems_mysql_data:/data -v $(pwd):/backup alpine tar xzf /backup/mysql_data_backup.tar.gz -C /data
```

## 🆘 Support

For issues or questions:

1. Check this documentation
2. Review logs: `docker compose logs -f`
3. Check GitHub issues
4. Contact system administrator

## 📝 Additional Notes

- First startup may take 2-3 minutes for database initialization
- Backend health check allows 60s startup time
- All containers restart automatically unless stopped
- Timezone is set to UTC by default
- Database uses UTF-8 MB4 encoding for full Unicode support

## 🔄 Updates and Maintenance

### Regular Updates

```bash
# 1. Backup database
./docker-deploy.sh  # Select option 5

# 2. Pull latest changes
git pull

# 3. Rebuild and restart
docker compose build --no-cache
docker compose up -d

# 4. Verify deployment
docker compose ps
docker compose logs -f
```

### Database Migrations

The application uses Hibernate with `ddl-auto=update` in production. Schema changes are applied automatically on startup. For manual migrations:

1. Create SQL migration file
2. Apply using MySQL client:
```bash
docker compose exec -T mysql mysql -u${DB_USERNAME} -p${DB_PASSWORD} ${DB_NAME} < migration.sql
```

---

**System Ready for Production Deployment with Docker! 🚀**

