# 🚀 Employee Management System - Production Deployment Guide

## 📋 Table of Contents
1. [Pre-Deployment Checklist](#pre-deployment-checklist)
2. [Environment Setup](#environment-setup)
3. [Database Setup](#database-setup)
4. [Backend Deployment](#backend-deployment)
5. [Frontend Deployment](#frontend-deployment)
6. [Post-Deployment](#post-deployment)
7. [Monitoring & Maintenance](#monitoring--maintenance)

---

## ✅ Pre-Deployment Checklist

### System Requirements
- [ ] Java 11 or higher installed
- [ ] MySQL 8.0+ or PostgreSQL 12+ installed
- [ ] Node.js 16+ and npm installed
- [ ] Tesseract OCR installed (for document processing)
- [ ] SSL certificate obtained (for HTTPS)
- [ ] Domain name configured
- [ ] Email SMTP credentials ready

### Code Readiness
- [x] All compilation errors fixed
- [x] Multi-tenancy implemented and tested
- [x] Leave management system complete
- [x] Authentication & authorization working
- [x] Email service configured
- [x] Document upload/storage working
- [x] ROTA parsing functional

---

## 🔧 Environment Setup

### 1. Install Dependencies

**Backend (Java/Spring Boot):**
```bash
# Install Java 11
sudo apt update
sudo apt install openjdk-11-jdk

# Install Maven
sudo apt install maven

# Install MySQL
sudo apt install mysql-server
sudo mysql_secure_installation
```

**Frontend (Angular):**
```bash
# Install Node.js
curl -fsSL https://deb.nodesource.com/setup_16.x | sudo -E bash -
sudo apt install -y nodejs

# Install Angular CLI
npm install -g @angular/cli
```

**Tesseract OCR:**
```bash
# Ubuntu/Debian
sudo apt install tesseract-ocr tesseract-ocr-eng

# Check installation
tesseract --version
```

---

## 🗄️ Database Setup

### 1. Create Production Database

```sql
-- Connect to MySQL
mysql -u root -p

-- Create database
CREATE DATABASE employee_management_system
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Create production user
CREATE USER 'emp_admin'@'localhost' IDENTIFIED BY 'SECURE_PASSWORD_HERE';
GRANT ALL PRIVILEGES ON employee_management_system.* TO 'emp_admin'@'localhost';
FLUSH PRIVILEGES;

-- Exit MySQL
EXIT;
```

### 2. Run Database Migration

The application will auto-create tables on first run with `spring.jpa.hibernate.ddl-auto=update`.

**For production, use Flyway or Liquibase for controlled migrations:**

```bash
# Change to validate after first deployment
spring.jpa.hibernate.ddl-auto=validate
```

### 3. Verify Database

```sql
USE employee_management_system;
SHOW TABLES;

-- Expected tables:
-- users, employees, organizations, departments, leaves, leave_balances,
-- documents, rotas, rota_schedules, attendance, notifications
```

---

## 🎯 Backend Deployment

### 1. Update Configuration

**Set Environment Variables:**

```bash
# Create .env file
export DB_USERNAME=emp_admin
export DB_PASSWORD=your_secure_password
export JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n')
export MAIL_USERNAME=your-email@gmail.com
export MAIL_PASSWORD=your-app-password
export APP_URL=https://yourdomain.com
export TESSERACT_DATA_PATH=/usr/share/tesseract-ocr/4.00/tessdata
```

### 2. Build Backend

```bash
cd EmployeeManagementSystem

# Clean and build
mvn clean package -DskipTests -Pprod

# Or with tests
mvn clean package -Pprod
```

**Output:** `target/EmployeeManagementSystem-0.0.1-SNAPSHOT.jar`

### 3. Run as Service

**Create systemd service file:**

```bash
sudo nano /etc/systemd/system/employee-management.service
```

**Content:**
```ini
[Unit]
Description=Employee Management System Backend
After=syslog.target network.target mysql.service

[Service]
Type=simple
User=www-data
WorkingDirectory=/opt/employee-management
ExecStart=/usr/bin/java -jar \
    -Dspring.profiles.active=prod \
    -Xms512m -Xmx2g \
    /opt/employee-management/EmployeeManagementSystem.jar

Restart=always
RestartSec=10

Environment="DB_USERNAME=emp_admin"
Environment="DB_PASSWORD=your_password"
Environment="JWT_SECRET=your_jwt_secret"
Environment="MAIL_USERNAME=your-email@gmail.com"
Environment="MAIL_PASSWORD=your-app-password"
Environment="APP_URL=https://yourdomain.com"

StandardOutput=journal
StandardError=journal
SyslogIdentifier=employee-management

[Install]
WantedBy=multi-user.target
```

**Enable and start:**
```bash
sudo systemctl daemon-reload
sudo systemctl enable employee-management
sudo systemctl start employee-management
sudo systemctl status employee-management
```

**View logs:**
```bash
sudo journalctl -u employee-management -f
```

### 4. Configure Nginx Reverse Proxy

```bash
sudo nano /etc/nginx/sites-available/employee-management
```

**Content:**
```nginx
server {
    listen 80;
    server_name yourdomain.com www.yourdomain.com;
    
    # Redirect to HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name yourdomain.com www.yourdomain.com;
    
    # SSL Certificate
    ssl_certificate /etc/letsencrypt/live/yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/yourdomain.com/privkey.pem;
    
    # Security headers
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    
    # Backend API
    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # WebSocket support
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        
        # Timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }
    
    # Frontend (Angular)
    location / {
        root /var/www/employee-management/frontend;
        try_files $uri $uri/ /index.html;
        
        # Cache static assets
        location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
            expires 1y;
            add_header Cache-Control "public, immutable";
        }
    }
    
    # File upload size
    client_max_body_size 10M;
}
```

**Enable site:**
```bash
sudo ln -s /etc/nginx/sites-available/employee-management /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

### 5. Setup SSL with Let's Encrypt

```bash
sudo apt install certbot python3-certbot-nginx
sudo certbot --nginx -d yourdomain.com -d www.yourdomain.com
sudo certbot renew --dry-run
```

---

## 🎨 Frontend Deployment

### 1. Update Environment

**Edit `frontend/src/environments/environment.prod.ts`:**

```typescript
export const environment = {
  production: true,
  apiUrl: 'https://yourdomain.com/api',
  appUrl: 'https://yourdomain.com'
};
```

### 2. Build Frontend

```bash
cd frontend

# Install dependencies
npm install

# Build for production
ng build --configuration production

# Output: dist/employee-management-system/
```

### 3. Deploy to Server

```bash
# Copy build files to server
scp -r dist/employee-management-system/* user@server:/var/www/employee-management/frontend/

# Or if on same server
sudo mkdir -p /var/www/employee-management/frontend
sudo cp -r dist/employee-management-system/* /var/www/employee-management/frontend/
sudo chown -R www-data:www-data /var/www/employee-management/frontend
```

---

## 🔍 Post-Deployment

### 1. Create ROOT User

```bash
curl -X POST https://yourdomain.com/api/auth/create-root \
  -H "Content-Type: application/json" \
  -d '{
    "username": "root",
    "password": "SecureRootPassword123!",
    "email": "admin@yourdomain.com"
  }'
```

**IMPORTANT:** Change password immediately after first login!

### 2. Health Checks

```bash
# Backend health
curl https://yourdomain.com/api/actuator/health

# Frontend
curl https://yourdomain.com

# Database connection
mysql -u emp_admin -p -e "USE employee_management_system; SELECT COUNT(*) FROM users;"
```

### 3. Test Critical Flows

- [ ] ROOT login
- [ ] Create organization
- [ ] SUPER_ADMIN login
- [ ] Create employee
- [ ] Apply leave
- [ ] Upload document
- [ ] Upload ROTA
- [ ] View dashboard
- [ ] Multi-tenancy isolation

---

## 📊 Monitoring & Maintenance

### 1. Setup Monitoring

**Application Logs:**
```bash
sudo journalctl -u employee-management -f --since today
```

**Access Logs:**
```bash
sudo tail -f /var/log/nginx/access.log
```

**Error Logs:**
```bash
sudo tail -f /var/log/nginx/error.log
```

### 2. Database Backups

**Create backup script:**
```bash
#!/bin/bash
# /opt/scripts/backup-db.sh

BACKUP_DIR="/backups/mysql"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/employee_management_$DATE.sql"

mkdir -p $BACKUP_DIR

mysqldump -u emp_admin -p'password' employee_management_system \
    --single-transaction --routines --triggers --events > $BACKUP_FILE

# Keep only last 7 days
find $BACKUP_DIR -name "employee_management_*.sql" -mtime +7 -delete

# Compress
gzip $BACKUP_FILE
```

**Schedule daily backups:**
```bash
sudo crontab -e

# Add line:
0 2 * * * /opt/scripts/backup-db.sh
```

### 3. Performance Optimization

**MySQL Tuning:**
```sql
-- Check slow queries
SELECT * FROM mysql.slow_log LIMIT 10;

-- Add indexes
CREATE INDEX idx_employee_org ON employees(organization_id);
CREATE INDEX idx_leave_emp ON leaves(employee_id);
CREATE INDEX idx_balance_emp_fy ON leave_balances(employee_id, financial_year);
```

**JVM Tuning:**
```bash
# In systemd service file
-Xms512m          # Initial heap
-Xmx2g            # Max heap
-XX:+UseG1GC      # Garbage collector
```

### 4. Security Hardening

**Firewall:**
```bash
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS
sudo ufw enable
```

**Fail2Ban:**
```bash
sudo apt install fail2ban
sudo systemctl enable fail2ban
```

**Update regularly:**
```bash
sudo apt update && sudo apt upgrade -y
```

---

## 🐳 Docker Deployment (Alternative)

### Docker Compose Setup

**Create `docker-compose.yml`:**

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: rootpassword
      MYSQL_DATABASE: employee_management_system
      MYSQL_USER: emp_admin
      MYSQL_PASSWORD: password
    volumes:
      - mysql-data:/var/lib/mysql
    ports:
      - "3306:3306"
    networks:
      - emp-network

  backend:
    build:
      context: .
      dockerfile: Dockerfile
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_USERNAME: emp_admin
      DB_PASSWORD: password
      JWT_SECRET: your-jwt-secret
      MAIL_USERNAME: your-email@gmail.com
      MAIL_PASSWORD: your-password
      APP_URL: https://yourdomain.com
    ports:
      - "8080:8080"
    depends_on:
      - mysql
    networks:
      - emp-network

  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    ports:
      - "80:80"
      - "443:443"
    depends_on:
      - backend
    networks:
      - emp-network

volumes:
  mysql-data:

networks:
  emp-network:
    driver: bridge
```

**Deploy:**
```bash
docker-compose up -d
```

---

## 🔥 Troubleshooting

### Common Issues

**1. Backend won't start:**
```bash
# Check logs
sudo journalctl -u employee-management -n 50

# Common causes:
# - Database connection failure
# - Port 8080 already in use
# - Missing environment variables
```

**2. Frontend 404 errors:**
```bash
# Check nginx config
sudo nginx -t

# Verify files
ls -la /var/www/employee-management/frontend/
```

**3. Database connection errors:**
```bash
# Test connection
mysql -u emp_admin -p -h localhost employee_management_system

# Check user permissions
SHOW GRANTS FOR 'emp_admin'@'localhost';
```

**4. Email not sending:**
```bash
# Check SMTP credentials
# Enable "Less secure app access" for Gmail
# Or use App Password
```

---

## 📞 Support & Maintenance

### Regular Tasks

- **Daily:** Check application logs
- **Weekly:** Review database performance
- **Monthly:** Update dependencies, security patches
- **Quarterly:** Full system backup test and restore

### Performance Metrics to Monitor

- Response time (should be < 500ms)
- CPU usage (should be < 70%)
- Memory usage (should be < 80%)
- Database connections (monitor pool usage)
- Disk space (maintain 20% free)

---

## ✅ Deployment Complete!

Your Employee Management System is now live at: **https://yourdomain.com**

**Next Steps:**
1. Login as ROOT
2. Create first organization
3. Test all features
4. Train administrators
5. Monitor for 24 hours

**For issues:** Check logs first, then consult troubleshooting guide.

---

**Deployed:** November 6, 2025  
**Version:** 1.0.0  
**Status:** ✅ Production Ready

