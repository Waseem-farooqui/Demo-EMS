# 🚀 Employee Management System - Deployment

Complete Docker-based Employee Management System with Document Management, Leave Management, ROTA Scheduling, and Multi-Tenancy.

---

## 📋 Quick Start

### Prerequisites
- Docker 20.10+
- Docker Compose 2.0+
- 8GB RAM (minimum 4GB)
- 50GB disk space

### Fresh Deployment

```bash
# 1. Clone repository
git clone <repository-url>
cd EmployeeManagementSystem

# 2. Create .env file
cp .env.example .env
# Edit .env with your configuration

# 3. Deploy
docker-compose up -d --build

# 4. Verify
docker-compose ps
curl http://localhost:8080/api/actuator/health
```

### Access Application
- **Frontend:** http://localhost
- **Backend API:** http://localhost:8080/api
- **Database:** localhost:3307

---

## 📚 Documentation

- **[Docker Deployment Guide](DOCKER_DEPLOYMENT_GUIDE.md)** - Complete deployment instructions
- **[Production Readiness Checklist](PRODUCTION_READINESS_CHECKLIST.md)** - Pre-deployment verification

---

## 🔧 Management Scripts

### Update (Zero Downtime)
```bash
chmod +x update.sh
./update.sh
```

### Backup
```bash
chmod +x backup.sh
./backup.sh
```

### Monitor
```bash
chmod +x monitor.sh
./monitor.sh
```

### Rollback
```bash
chmod +x rollback.sh
./rollback.sh
```

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Nginx (Frontend)                      │
│                   Angular Application                    │
│                        Port: 80                          │
└───────────────────────┬─────────────────────────────────┘
                        │
                        │ HTTP/REST API
                        │
┌───────────────────────▼─────────────────────────────────┐
│              Spring Boot Backend                         │
│     Document Management | Leave Management              │
│     ROTA Scheduling | Multi-Tenancy                     │
│                    Port: 8080                            │
└───────────────────────┬─────────────────────────────────┘
                        │
                        │ JDBC
                        │
┌───────────────────────▼─────────────────────────────────┐
│                   MySQL 8.0                              │
│              employee_management_system                  │
│                    Port: 3307                            │
└─────────────────────────────────────────────────────────┘
```

---

## ✨ Features

### 🏢 Multi-Tenancy
- Complete organization isolation
- Per-organization branding
- Organization-specific users and data

### 👥 User Management
- Role-based access control (ROOT, SUPER_ADMIN, ADMIN, USER)
- Self-service profile creation
- System-generated credentials

### 📄 Document Management
- OCR-based data extraction
- Support for Passport, Visa, Contracts, Resumes
- MD5 deduplication
- Expiry alerts with configurable frequency

### 🏖️ Leave Management
- Multiple leave types (Casual, Sick, Annual)
- Smart validation rules
- Approval workflow
- Leave balance tracking

### 📅 ROTA Management
- Schedule management
- Employee shift assignments
- Department-based rotas

### 🔔 Notifications
- In-app notifications
- Email notifications
- Document expiry alerts
- Leave request alerts

---

## 🔐 Security

- JWT-based authentication
- HTTPS/SSL support
- Role-based authorization
- Organization-based data isolation
- CORS protection
- SQL injection prevention
- XSS protection

---

## ⚙️ Configuration

### Required Environment Variables

```bash
# Database
DB_NAME=employee_management_system
DB_USERNAME=emsuser
DB_PASSWORD=<your-password>
DB_ROOT_PASSWORD=<root-password>

# JWT (Change in production!)
JWT_SECRET=<256-character-random-string>

# Email
MAIL_HOST=smtp.gmail.com
MAIL_USERNAME=<your-email>
MAIL_PASSWORD=<app-password>

# Application
APP_URL=http://your-domain.com
CORS_ALLOWED_ORIGINS=http://your-domain.com
```

---

## 📊 Monitoring

### Health Checks
```bash
# Backend
curl http://localhost:8080/api/actuator/health

# Frontend
curl http://localhost/

# All services
docker-compose ps
./monitor.sh
```

### Logs
```bash
# View all logs
docker-compose logs -f

# Specific service
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f mysql

# Last 100 lines
docker-compose logs --tail=100 backend
```

### Resource Usage
```bash
docker stats
```

---

## 🔄 Updates

### Zero-Downtime Update
```bash
git pull origin main
./update.sh
```

### Manual Update
```bash
docker-compose pull
docker-compose up -d --build
```

### Rollback
```bash
./rollback.sh
```

---

## 💾 Backup & Recovery

### Manual Backup
```bash
./backup.sh
```

### Automated Backup (Cron)
```bash
# Daily at 2 AM
0 2 * * * /path/to/backup.sh >> /var/log/ems-backup.log 2>&1
```

### Restore Database
```bash
docker-compose exec -T mysql mysql -u root -p employee_management_system < backup.sql
```

---

## 🐛 Troubleshooting

### Backend Won't Start
```bash
docker-compose logs backend
docker-compose restart mysql
sleep 30
docker-compose restart backend
```

### Database Connection Issues
```bash
docker-compose ps mysql
docker-compose logs mysql
docker-compose exec mysql mysql -u root -p -e "SELECT 1"
```

### Email Not Sending
```bash
docker-compose logs backend | grep -i mail
# Check email configuration in .env
# Use app-specific password for Gmail
```

### Out of Disk Space
```bash
docker system prune -a --volumes
docker image prune -a
```

---

## 📞 Support

### Common Commands
```bash
# Start services
docker-compose up -d

# Stop services
docker-compose down

# Restart service
docker-compose restart backend

# View logs
docker-compose logs -f backend

# Shell access
docker-compose exec backend bash
docker-compose exec mysql mysql -u root -p

# Clean up
docker system prune -a
```

---

## 📝 Production Checklist

Before going live:

- [ ] Change JWT_SECRET
- [ ] Set strong database passwords
- [ ] Configure SSL/HTTPS
- [ ] Update CORS origins
- [ ] Configure email
- [ ] Test backups
- [ ] Configure monitoring
- [ ] Test rollback
- [ ] Review [Production Readiness Checklist](PRODUCTION_READINESS_CHECKLIST.md)

---

## 🏷️ Version

**Version:** 1.0.0
**Docker:** 20.10+
**Docker Compose:** 2.0+
**Last Updated:** November 14, 2025

---

## 📄 License

Proprietary - All Rights Reserved

---

## 🎯 Quick Links

- [Full Deployment Guide](DOCKER_DEPLOYMENT_GUIDE.md)
- [Production Checklist](PRODUCTION_READINESS_CHECKLIST.md)
- Frontend: http://localhost
- Backend API: http://localhost:8080/api
- Health Check: http://localhost:8080/api/actuator/health

---

**Made with ❤️ for efficient employee management**

