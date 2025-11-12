# 📋 Complete Deployment Steps - Employee Management System

## Overview

This guide provides **complete step-by-step instructions** for deploying the Employee Management System to production using Docker and MySQL.

---

## 🎯 Pre-Deployment Requirements

### System Requirements
- **Operating System:** Linux, macOS, or Windows Server
- **CPU:** 2+ cores (4+ recommended)
- **RAM:** 2GB minimum (4GB recommended)
- **Disk Space:** 10GB minimum (50GB recommended for documents)
- **Network:** Internet connection for pulling Docker images

### Software Requirements
- **Docker:** Version 20.10 or higher
- **Docker Compose:** Version 2.0 or higher

### Access Requirements
- Server with SSH access (Linux/Mac) or RDP (Windows)
- Domain name (optional, can use IP address)
- SSL certificate (optional, recommended for production)
- Email account for notifications (Gmail/Outlook/Office365)

---

## 📥 Step 1: Install Docker

### On Ubuntu/Debian Linux

```bash
# Update package index
sudo apt-get update

# Install prerequisites
sudo apt-get install -y \
    apt-transport-https \
    ca-certificates \
    curl \
    gnupg \
    lsb-release

# Add Docker's official GPG key
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg

# Set up the stable repository
echo \
  "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Install Docker Engine
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

# Verify installation
docker --version
docker compose version

# Add your user to docker group (optional, avoid using sudo)
sudo usermod -aG docker $USER
newgrp docker
```

### On CentOS/RHEL

```bash
# Install Docker
sudo yum install -y yum-utils
sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
sudo yum install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

# Start Docker
sudo systemctl start docker
sudo systemctl enable docker

# Verify installation
docker --version
docker compose version
```

### On Windows Server

1. Download Docker Desktop for Windows from: https://www.docker.com/products/docker-desktop
2. Run the installer
3. Follow the installation wizard
4. Restart your computer
5. Verify: Open PowerShell and run `docker --version`

### On macOS

```bash
# Install using Homebrew
brew install --cask docker

# Or download Docker Desktop from: https://www.docker.com/products/docker-desktop

# Verify installation
docker --version
docker compose version
```

---

## 📦 Step 2: Download Application Files

### Option A: Using Git (Recommended)

```bash
# Install git if not already installed
sudo apt-get install -y git  # Ubuntu/Debian
sudo yum install -y git       # CentOS/RHEL

# Clone the repository
git clone <repository-url> /opt/employee-management
cd /opt/employee-management

# Verify files
ls -la
```

### Option B: Manual Upload

1. Download the application files as a ZIP
2. Upload to server using SCP/SFTP/FTP
3. Extract the files:

```bash
# Create directory
sudo mkdir -p /opt/employee-management

# Upload files to this directory
# Then extract if ZIP
unzip employee-management.zip -d /opt/employee-management
cd /opt/employee-management
```

---

## ⚙️ Step 3: Configure Environment Variables

### Create .env File

```bash
# Copy the example environment file
cp .env.example .env

# Edit the file
nano .env  # or vim .env or any text editor
```

### Configure Required Settings

Edit `.env` and set these **REQUIRED** values:

```env
# ===================================
# Database Configuration
# ===================================
DB_NAME=employee_management_system
DB_USERNAME=emsuser
DB_PASSWORD=YourVerySecurePassword123!
DB_ROOT_PASSWORD=YourRootPassword456!
DB_PORT=3307

# ===================================
# JWT Configuration (CRITICAL)
# ===================================
# Generate using: openssl rand -base64 64
JWT_SECRET=PASTE_YOUR_GENERATED_SECRET_HERE
JWT_EXPIRATION=86400000

# ===================================
# Email Configuration
# ===================================
# For Gmail
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-specific-password
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=true
MAIL_SMTP_STARTTLS_REQUIRED=true
MAIL_SMTP_SSL_TRUST=smtp.gmail.com

# For Outlook/Hotmail
# MAIL_HOST=smtp-mail.outlook.com
# MAIL_SMTP_SSL_TRUST=smtp-mail.outlook.com

# For Office365
# MAIL_HOST=smtp.office365.com
# MAIL_SMTP_SSL_TRUST=smtp.office365.com

EMAIL_FROM_NAME=Employee Management System
EMAIL_FROM_ADDRESS=your-email@gmail.com

# ===================================
# Application Configuration
# ===================================
APP_URL=http://your-server-ip
# Or: APP_URL=https://yourdomain.com

BACKEND_PORT=8080
FRONTEND_PORT=80

# ===================================
# CORS Configuration
# ===================================
# Update with your actual domain/IP
CORS_ALLOWED_ORIGINS=http://your-server-ip,http://localhost
# Or: CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://www.yourdomain.com
```

### Generate JWT Secret

**On Linux/Mac:**
```bash
openssl rand -base64 64
```

**On Windows (PowerShell):**
```powershell
$bytes = New-Object byte[] 64
(New-Object Security.Cryptography.RNGCryptoServiceProvider).GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

Copy the output and paste it as the `JWT_SECRET` value in `.env`

### Get Gmail App Password

1. Go to Google Account settings: https://myaccount.google.com/
2. Security → 2-Step Verification (enable if not already)
3. Security → App passwords
4. Generate new app password for "Mail"
5. Copy the 16-character password
6. Use this as `MAIL_PASSWORD` in `.env`

### Verify Configuration

```bash
# Check that .env file has all required values
cat .env | grep -E "DB_PASSWORD|JWT_SECRET|MAIL_PASSWORD|APP_URL|CORS_ALLOWED_ORIGINS"

# Make sure no placeholder values remain
```

---

## 🚀 Step 4: Deploy the Application

### Option A: Using Deployment Script (Recommended)

**On Linux/Mac:**

```bash
# Make script executable
chmod +x docker-deploy.sh

# Run deployment script
./docker-deploy.sh

# Select option 1 (Deploy)
```

**On Windows:**

```cmd
docker-deploy.bat

REM Select option 1 (Deploy)
```

### Option B: Manual Deployment

```bash
# Build images (this may take 5-10 minutes)
docker compose build --no-cache

# Start services in detached mode
docker compose up -d

# Wait for services to start (30-60 seconds)
sleep 60
```

### Monitor Deployment

```bash
# Check service status
docker compose ps

# Expected output:
# NAME            IMAGE              STATUS              PORTS
# ems-mysql       mysql:8.0         Up (healthy)        0.0.0.0:3307->3306/tcp
# ems-backend     ems-backend       Up (healthy)        0.0.0.0:8080->8080/tcp
# ems-frontend    ems-frontend      Up (healthy)        0.0.0.0:80->80/tcp
```

### View Logs

```bash
# View all logs
docker compose logs -f

# View specific service logs
docker compose logs -f backend
docker compose logs -f mysql
docker compose logs -f frontend

# Press Ctrl+C to exit log view
```

---

## ✅ Step 5: Verify Deployment

### Check Service Health

```bash
# Check backend health endpoint
curl http://localhost:8080/api/actuator/health

# Expected response:
# {"status":"UP"}
```

### Check MySQL Connection

```bash
# Connect to MySQL
docker compose exec mysql mysql -uemsuser -p${DB_PASSWORD} employee_management_system

# Once connected, verify tables:
SHOW TABLES;

# You should see tables like:
# users, organizations, employees, departments, documents, leaves, etc.

# Exit MySQL
EXIT;
```

### Access Frontend

```bash
# Check if frontend is accessible
curl http://localhost/

# Should return HTML content
```

### Test from Browser

1. Open browser
2. Navigate to: `http://YOUR_SERVER_IP` (or `http://localhost` if testing locally)
3. You should see the login page

---

## 🔐 Step 6: Initialize the System

### Create Root User

The system needs a root user to be created first. Use one of these methods:

#### Method 1: Using CURL (Command Line)

```bash
# Create root user
curl -X POST http://localhost:8080/api/init/create-root \
  -H "Content-Type: application/json" \
  -d '{
    "username": "rootadmin",
    "email": "root@yourdomain.com",
    "password": "ChangeThisPassword123!",
    "fullName": "Root Administrator"
  }'

# Expected response:
# {
#   "message": "Root user created successfully",
#   "username": "rootadmin",
#   "role": "ROOT"
# }
```

#### Method 2: Using Postman/Insomnia

1. Open Postman
2. Create new POST request
3. URL: `http://YOUR_SERVER_IP:8080/api/init/create-root`
4. Headers: `Content-Type: application/json`
5. Body (raw JSON):
```json
{
  "username": "rootadmin",
  "email": "root@yourdomain.com",
  "password": "ChangeThisPassword123!",
  "fullName": "Root Administrator"
}
```
6. Send request

#### Method 3: Using Frontend (if available)

1. Navigate to initialization page (if implemented)
2. Fill in root user details
3. Submit form

### Login as Root User

1. Open browser
2. Go to: `http://YOUR_SERVER_IP`
3. Login with:
   - Username: `rootadmin`
   - Password: `ChangeThisPassword123!`
4. You should be redirected to root dashboard

---

## 🏢 Step 7: Create First Organization

### Using Web Interface

1. Login as root user
2. Navigate to "Organizations" or "Create Organization"
3. Fill in organization details:
   - Organization Name: e.g., "Acme Corporation"
   - Organization Code: e.g., "ACME"
   - Description: Brief description
   - Contact Email: organization email
   - Contact Phone: phone number
   - Address: physical address
4. Upload organization logo (optional)
5. Click "Create Organization"

### Using API

```bash
curl -X POST http://localhost:8080/api/organizations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "name": "Acme Corporation",
    "code": "ACME",
    "description": "Main organization",
    "contactEmail": "contact@acme.com",
    "contactPhone": "+1234567890",
    "address": "123 Business Street, City, Country"
  }'
```

---

## 👥 Step 8: Create Super Admin for Organization

1. Login as root user
2. Navigate to "User Management" or "Create User"
3. Fill in super admin details:
   - Full Name: Admin name
   - Email: admin email
   - Role: SUPER_ADMIN
   - Organization: Select the organization created in Step 7
4. System will auto-generate username and temporary password
5. Credentials will be displayed on screen (save them!)
6. Optional: System sends email with credentials

---

## 🔒 Step 9: Configure Firewall (Security)

### On Ubuntu/Debian (UFW)

```bash
# Install UFW if not installed
sudo apt-get install -y ufw

# Allow SSH (IMPORTANT - do this first!)
sudo ufw allow 22/tcp

# Allow HTTP
sudo ufw allow 80/tcp

# Allow HTTPS (if using SSL)
sudo ufw allow 443/tcp

# Allow custom backend port if needed
# sudo ufw allow 8080/tcp

# Enable firewall
sudo ufw enable

# Check status
sudo ufw status
```

### On CentOS/RHEL (firewalld)

```bash
# Start firewalld
sudo systemctl start firewalld
sudo systemctl enable firewalld

# Allow HTTP
sudo firewall-cmd --permanent --add-service=http

# Allow HTTPS
sudo firewall-cmd --permanent --add-service=https

# Allow custom port if needed
# sudo firewall-cmd --permanent --add-port=8080/tcp

# Reload firewall
sudo firewall-cmd --reload

# Check status
sudo firewall-cmd --list-all
```

### On Windows Server

1. Open Windows Defender Firewall
2. Advanced settings
3. Inbound Rules → New Rule
4. Port → TCP → 80, 443
5. Allow the connection
6. Apply to Domain, Private, Public
7. Name the rule: "Employee Management System"

---

## 🌐 Step 10: Configure Domain and SSL (Optional but Recommended)

### Configure DNS

1. Login to your domain registrar (GoDaddy, Namecheap, etc.)
2. Add/Update A record:
   - Host: `@` (or `www`)
   - Type: `A`
   - Value: `YOUR_SERVER_IP`
   - TTL: 3600
3. Wait for DNS propagation (5-60 minutes)

### Install Nginx Reverse Proxy

```bash
# Install Nginx
sudo apt-get install -y nginx  # Ubuntu/Debian
# or
sudo yum install -y nginx      # CentOS/RHEL

# Create configuration
sudo nano /etc/nginx/sites-available/ems
```

### Nginx Configuration (Without SSL)

```nginx
server {
    listen 80;
    server_name yourdomain.com www.yourdomain.com;

    # Frontend
    location / {
        proxy_pass http://localhost:80;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Backend API
    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### Enable Site

```bash
# Create symlink
sudo ln -s /etc/nginx/sites-available/ems /etc/nginx/sites-enabled/

# Test configuration
sudo nginx -t

# Reload Nginx
sudo systemctl reload nginx
```

### Install SSL Certificate (Let's Encrypt)

```bash
# Install Certbot
sudo apt-get install -y certbot python3-certbot-nginx

# Obtain certificate
sudo certbot --nginx -d yourdomain.com -d www.yourdomain.com

# Follow prompts:
# - Enter email address
# - Agree to terms
# - Choose to redirect HTTP to HTTPS (recommended)

# Certbot automatically updates Nginx configuration

# Test auto-renewal
sudo certbot renew --dry-run
```

### Update .env for HTTPS

```bash
nano .env

# Update these values:
APP_URL=https://yourdomain.com
CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://www.yourdomain.com
```

### Restart Services

```bash
cd /opt/employee-management
docker compose restart backend
```

---

## 🔄 Step 11: Setup Automated Backups

### Create Backup Script

```bash
# Create backup directory
sudo mkdir -p /opt/backups/ems

# Create backup script
sudo nano /opt/backups/ems/backup.sh
```

### Backup Script Content

```bash
#!/bin/bash

# Configuration
BACKUP_DIR="/opt/backups/ems"
DATE=$(date +%Y%m%d_%H%M%S)
DB_PASSWORD="YOUR_DB_PASSWORD"  # Get from .env
DB_USERNAME="emsuser"
DB_NAME="employee_management_system"
RETENTION_DAYS=30

# Create backup directory
mkdir -p ${BACKUP_DIR}/${DATE}

# Backup database
cd /opt/employee-management
docker compose exec -T mysql mysqldump -u${DB_USERNAME} -p${DB_PASSWORD} ${DB_NAME} | gzip > ${BACKUP_DIR}/${DATE}/database.sql.gz

# Backup uploads
docker cp ems-backend:/app/uploads ${BACKUP_DIR}/${DATE}/

# Backup .env file
cp /opt/employee-management/.env ${BACKUP_DIR}/${DATE}/

# Remove old backups
find ${BACKUP_DIR} -type d -mtime +${RETENTION_DAYS} -exec rm -rf {} +

# Log
echo "Backup completed: ${DATE}" >> ${BACKUP_DIR}/backup.log

# Optional: Upload to cloud storage (S3, Google Drive, etc.)
# aws s3 sync ${BACKUP_DIR}/${DATE} s3://your-bucket/ems-backups/${DATE}
```

### Make Script Executable

```bash
sudo chmod +x /opt/backups/ems/backup.sh
```

### Schedule Daily Backups

```bash
# Edit crontab
sudo crontab -e

# Add this line (runs daily at 2 AM)
0 2 * * * /opt/backups/ems/backup.sh

# Save and exit
```

### Test Backup

```bash
# Run manually
sudo /opt/backups/ems/backup.sh

# Check backup
ls -lah /opt/backups/ems/
```

---

## 📊 Step 12: Setup Monitoring (Optional)

### Enable Docker Stats

```bash
# View real-time stats
docker compose stats

# Or continuously
watch -n 5 'docker compose stats --no-stream'
```

### Setup Log Rotation

```bash
# Edit Docker daemon config
sudo nano /etc/docker/daemon.json
```

Add:
```json
{
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  }
}
```

```bash
# Restart Docker
sudo systemctl restart docker

# Restart containers
cd /opt/employee-management
docker compose down
docker compose up -d
```

### Setup Uptime Monitoring

Use external services like:
- UptimeRobot (free): https://uptimerobot.com
- Pingdom: https://www.pingdom.com
- StatusCake: https://www.statuscake.com

Configure to monitor: `http://yourdomain.com` and `http://yourdomain.com/api/actuator/health`

---

## 🔧 Step 13: Performance Optimization

### Adjust Docker Resources

Edit `compose.yaml`:

```yaml
backend:
  # ... existing config ...
  deploy:
    resources:
      limits:
        cpus: '2'
        memory: 2G
      reservations:
        cpus: '1'
        memory: 1G
```

### Optimize MySQL

```bash
# Connect to MySQL
docker compose exec mysql mysql -uroot -p

# Run optimization
OPTIMIZE TABLE employees, documents, leaves, users, departments;
ANALYZE TABLE employees, documents, leaves, users, departments;

# Exit
EXIT;
```

### Enable Gzip Compression

Already enabled in nginx.conf for frontend. Verify:

```bash
curl -H "Accept-Encoding: gzip" -I http://yourdomain.com
# Should see: Content-Encoding: gzip
```

---

## ✅ Step 14: Final Verification Checklist

Go through this checklist to ensure everything is working:

- [ ] Docker containers are running: `docker compose ps`
- [ ] Backend health check passes: `curl http://localhost:8080/api/actuator/health`
- [ ] MySQL is accessible and has tables
- [ ] Frontend loads in browser
- [ ] Can login as root user
- [ ] Can create organizations
- [ ] Can create super admin users
- [ ] Email notifications work (test by creating user)
- [ ] Document upload works
- [ ] SSL certificate is valid (if configured)
- [ ] Firewall rules are in place
- [ ] Backups are scheduled and working
- [ ] DNS is pointing to correct server
- [ ] Application is accessible from internet
- [ ] All sensitive data is secured

---

## 🎉 Deployment Complete!

Your Employee Management System is now **fully deployed and running in production**!

### Access Points

- **Frontend:** http://yourdomain.com (or https:// if SSL configured)
- **Backend API:** http://yourdomain.com/api
- **Health Check:** http://yourdomain.com/api/actuator/health

### Default Credentials

- **Root User:** As created in Step 6
- **Super Admin:** As created in Step 8 (temporary password, must change on first login)

### Next Steps

1. **Change root password** after first login
2. **Create departments** for each organization
3. **Create admins** for each department
4. **Add employees** to the system
5. **Upload documents** and configure alerts
6. **Setup leave policies** and quotas
7. **Configure attendance** settings
8. **Train users** on the system

---

## 🛠️ Maintenance Commands

### View Logs
```bash
cd /opt/employee-management
docker compose logs -f backend
```

### Restart Services
```bash
docker compose restart
```

### Stop Services
```bash
docker compose down
```

### Update Application
```bash
git pull
docker compose build --no-cache
docker compose up -d
```

### Backup Manually
```bash
/opt/backups/ems/backup.sh
```

### Restore from Backup
```bash
# Restore database
gunzip < /opt/backups/ems/YYYYMMDD_HHMMSS/database.sql.gz | \
  docker compose exec -T mysql mysql -uemsuser -pPASSWORD employee_management_system

# Restore uploads
docker cp /opt/backups/ems/YYYYMMDD_HHMMSS/uploads ems-backend:/app/
```

---

## 🆘 Troubleshooting

### Services Won't Start

```bash
# Check logs
docker compose logs

# Check specific service
docker compose logs mysql
docker compose logs backend

# Restart with fresh state
docker compose down -v
docker compose up -d
```

### Can't Connect to Database

```bash
# Check MySQL is running
docker compose ps mysql

# Wait for health check
docker compose ps  # Status should be "healthy"

# Verify credentials in .env
cat .env | grep DB_
```

### Frontend Shows 502 Error

```bash
# Backend might not be ready yet
docker compose logs backend

# Check backend health
curl http://localhost:8080/api/actuator/health

# Restart backend
docker compose restart backend
```

### Email Not Sending

```bash
# Check email configuration in .env
cat .env | grep MAIL_

# Test email manually or check backend logs
docker compose logs backend | grep -i mail
```

---

## 📞 Support

For issues or questions:
1. Check logs: `docker compose logs -f`
2. Review documentation in the repository
3. Check firewall and network settings
4. Verify environment variables in .env
5. Contact system administrator

---

## 📚 Additional Resources

- **Docker Deployment Guide:** `DOCKER_DEPLOYMENT.md`
- **Quick Reference:** `DOCKER_QUICK_REFERENCE.md`
- **MySQL Guide:** `MYSQL_PRODUCTION_GUIDE.md`
- **Production Ready:** `PRODUCTION_READY.md`
- **Pre-Deployment Checklist:** `PRE_DEPLOYMENT_CHECKLIST.md`

---

**🎊 Congratulations! Your Employee Management System is now live and ready for use! 🎊**

*Deployment Guide Version 1.0 - November 12, 2025*

