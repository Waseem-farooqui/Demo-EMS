# 🚀 QUICK START - Employee Management System Deployment

## For the Impatient

Want to deploy **RIGHT NOW**? Follow these 5 steps:

---

## Step 1: Install Docker (if not installed)

**Ubuntu/Debian:**
```bash
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER
newgrp docker
```

**Windows:** Download from https://www.docker.com/products/docker-desktop

**Verify:**
```bash
docker --version
docker compose version
```

---

## Step 2: Get Application

```bash
# Option A: Git
git clone <repository-url> /opt/employee-management
cd /opt/employee-management

# Option B: Download ZIP and extract
cd /opt/employee-management
```

---

## Step 3: Configure Environment

```bash
# Copy template
cp .env.example .env

# Edit configuration
nano .env
```

**Critical settings to change:**
```env
# Generate JWT secret: openssl rand -base64 64
JWT_SECRET=YOUR_GENERATED_SECRET_HERE

# Database passwords
DB_PASSWORD=YourSecurePassword123!
DB_ROOT_PASSWORD=YourRootPassword456!

# Email (for Gmail, use app password)
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password

# Your domain or IP
APP_URL=http://your-server-ip
CORS_ALLOWED_ORIGINS=http://your-server-ip
```

---

## Step 4: Deploy

**Linux/Mac:**
```bash
chmod +x docker-deploy.sh
./docker-deploy.sh
# Select: 1 (Deploy)
```

**Windows:**
```cmd
docker-deploy.bat
REM Select: 1 (Deploy)
```

**Manual:**
```bash
docker compose build --no-cache
docker compose up -d
```

**Wait 60 seconds** for services to start.

---

## Step 5: Initialize System

```bash
# Create root user
curl -X POST http://localhost:8080/api/init/create-root \
  -H "Content-Type: application/json" \
  -d '{
    "username": "rootadmin",
    "email": "root@yourdomain.com",
    "password": "ChangeMe123!",
    "fullName": "Root Administrator"
  }'
```

---

## ✅ Verify Deployment

```bash
# Check services
docker compose ps
# All should show "healthy"

# Check backend
curl http://localhost:8080/api/actuator/health
# Response: {"status":"UP"}

# Open browser
# Go to: http://localhost
# Login with root credentials
```

---

## 🎉 That's It!

**Your system is now running!**

- **Frontend:** http://localhost
- **Backend:** http://localhost:8080/api
- **Root User:** rootadmin / ChangeMe123!

---

## 📚 Next Steps

1. **Login** as root user
2. **Create** your first organization
3. **Add** super admin for the organization
4. **Create** departments and employees
5. **Configure** alerts and settings

---

## 🛠️ Common Commands

```bash
# View logs
docker compose logs -f

# Restart
docker compose restart

# Stop
docker compose down

# Update
git pull && docker compose up -d --build
```

---

## 🆘 Troubleshooting

**Container won't start:**
```bash
docker compose logs [service-name]
```

**Can't access frontend:**
```bash
# Check if running
docker compose ps

# Restart
docker compose restart frontend
```

**Database connection failed:**
```bash
# Wait 30 seconds for MySQL to initialize
# Check logs
docker compose logs mysql
```

---

## 📖 Full Documentation

For complete deployment guide with SSL, backups, monitoring, etc:

👉 **[COMPLETE_DEPLOYMENT_STEPS.md](./COMPLETE_DEPLOYMENT_STEPS.md)**

For all documentation:

👉 **[DEPLOYMENT_INDEX.md](./DEPLOYMENT_INDEX.md)**

---

## ⚠️ Production Checklist

Before going live:

- [ ] Change root password after first login
- [ ] Generate new JWT_SECRET (not default)
- [ ] Use strong database passwords
- [ ] Configure email with app password
- [ ] Setup SSL certificate
- [ ] Configure firewall
- [ ] Setup automated backups
- [ ] Update CORS_ALLOWED_ORIGINS
- [ ] Test disaster recovery

---

## 📊 What You Get

- ✅ Complete Employee Management System
- ✅ MySQL 8.0 database
- ✅ Document management with OCR
- ✅ Leave management system
- ✅ Attendance tracking
- ✅ Multi-tenancy support
- ✅ Email notifications
- ✅ Role-based access control
- ✅ Dashboard with analytics

---

## 🎓 Documentation Structure

```
DEPLOYMENT_INDEX.md                 ← Start here for overview
├── COMPLETE_DEPLOYMENT_STEPS.md   ← Full 14-step guide
├── DEPLOYMENT_FLOWCHART.md        ← Visual workflow
├── DOCKER_DEPLOYMENT.md           ← Docker deep dive
├── DOCKER_QUICK_REFERENCE.md      ← Command reference
├── MYSQL_PRODUCTION_GUIDE.md      ← Database guide
├── PRODUCTION_READY.md            ← Feature overview
└── PRE_DEPLOYMENT_CHECKLIST.md    ← Verification checklist
```

---

## 💡 Tips

1. **First time?** Follow [COMPLETE_DEPLOYMENT_STEPS.md](./COMPLETE_DEPLOYMENT_STEPS.md)
2. **Experienced?** Use this quick start
3. **Need help?** Check troubleshooting sections in full guides
4. **Production?** Review [PRE_DEPLOYMENT_CHECKLIST.md](./PRE_DEPLOYMENT_CHECKLIST.md)

---

## 🚀 Deploy Now!

```bash
# Copy-paste this entire block:
cd /opt
git clone <your-repo-url> employee-management
cd employee-management
cp .env.example .env
# Now edit .env with your values
nano .env
# Then deploy:
chmod +x docker-deploy.sh && ./docker-deploy.sh
```

---

**Total Time: 15-30 minutes** ⏱️

**Difficulty: Easy** 👍

**Result: Production-ready system** 🎉

---

*Last Updated: November 12, 2025*

