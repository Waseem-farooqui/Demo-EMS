# 🚀 Quick Deployment Flowchart

```
┌─────────────────────────────────────────────────────────────┐
│              EMPLOYEE MANAGEMENT SYSTEM                      │
│           Complete Deployment Workflow                       │
└─────────────────────────────────────────────────────────────┘

STEP 1: PREPARE SERVER
├── Install Docker (20.10+)
├── Install Docker Compose (2.0+)
└── Verify: docker --version && docker compose version
    │
    ├─── Success ────┐
    └─── Failed ─────┴─→ [STOP: Install Docker first]

STEP 2: GET APPLICATION FILES
├── Option A: git clone <repo-url>
├── Option B: Download and extract ZIP
└── Navigate to: /opt/employee-management
    │
    └─── Files Ready ────┐

STEP 3: CONFIGURE ENVIRONMENT
├── Copy: cp .env.example .env
├── Edit .env file:
│   ├── Set DB_PASSWORD
│   ├── Generate JWT_SECRET (openssl rand -base64 64)
│   ├── Configure MAIL_USERNAME & MAIL_PASSWORD
│   ├── Set APP_URL (your domain or IP)
│   └── Set CORS_ALLOWED_ORIGINS
└── Verify: No placeholder values remain
    │
    ├─── All Set ────┐
    └─── Missing ────┴─→ [STOP: Complete configuration]

STEP 4: DEPLOY WITH DOCKER
├── Option A: ./docker-deploy.sh (Linux/Mac)
├── Option B: docker-deploy.bat (Windows)
└── Option C: docker compose up -d (Manual)
    │
    ├── Building images... (5-10 minutes)
    ├── Starting MySQL... (30 seconds)
    ├── Starting Backend... (60 seconds)
    └── Starting Frontend... (10 seconds)
        │
        └─── All Running ────┐

STEP 5: VERIFY DEPLOYMENT
├── Check status: docker compose ps
│   └── All containers "healthy"? ───┐
│       ├─── YES ────┐               │
│       └─── NO ─────┴→ Check logs   │
│                                     │
├── Test backend health:              │
│   curl http://localhost:8080/api/actuator/health
│   └── {"status":"UP"}? ────────────┤
│       ├─── YES ────┐                │
│       └─── NO ─────┴→ Check logs    │
│                                     │
└── Access frontend: http://localhost ┤
    └── Login page loads? ────────────┤
        ├─── YES ────┐                │
        └─── NO ─────┴→ Check Nginx   │
                                      │
                All Healthy ──────────┘

STEP 6: INITIALIZE SYSTEM
├── Create root user via API:
│   POST /api/init/create-root
│   {
│     "username": "rootadmin",
│     "email": "root@domain.com",
│     "password": "SecurePass123!",
│     "fullName": "Root Admin"
│   }
│   │
│   └─── Root Created ────┐
│
└── Login as root user
    └── Access root dashboard ────┐

STEP 7: CREATE ORGANIZATION
├── Login as root
├── Navigate to "Organizations"
├── Fill organization details:
│   ├── Name: "Acme Corp"
│   ├── Code: "ACME"
│   ├── Contact info
│   └── Logo (optional)
└── Click "Create"
    │
    └─── Organization Created ────┐

STEP 8: CREATE SUPER ADMIN
├── Navigate to "User Management"
├── Create new user:
│   ├── Full Name
│   ├── Email
│   ├── Role: SUPER_ADMIN
│   └── Organization: Select from list
├── System generates username & password
└── Save credentials (displayed on screen)
    │
    └─── Super Admin Created ────┐

STEP 9: SECURE THE SERVER
├── Configure firewall:
│   ├── Allow port 22 (SSH)
│   ├── Allow port 80 (HTTP)
│   ├── Allow port 443 (HTTPS)
│   └── Enable firewall
│
└── Optional: Setup fail2ban
    │
    └─── Server Secured ────┐

STEP 10: CONFIGURE DOMAIN & SSL (Optional but Recommended)
├── Point DNS A record to server IP
├── Install Nginx reverse proxy
├── Configure Nginx for the app
├── Install Let's Encrypt SSL:
│   └── certbot --nginx -d yourdomain.com
├── Update .env with HTTPS URLs
└── Restart: docker compose restart
    │
    └─── SSL Configured ────┐

STEP 11: SETUP BACKUPS
├── Create backup script: /opt/backups/ems/backup.sh
├── Make executable: chmod +x backup.sh
├── Schedule cron job: 0 2 * * *
└── Test backup: ./backup.sh
    │
    └─── Backups Running ────┐

STEP 12: FINAL VERIFICATION
├── ✓ All containers healthy
├── ✓ Can login as root
├── ✓ Can create organizations
├── ✓ Can create users
├── ✓ Email notifications work
├── ✓ SSL is active (if configured)
├── ✓ Firewall is configured
├── ✓ Backups are scheduled
└── ✓ Application accessible from internet
    │
    └─── ALL VERIFIED ────┐

┌─────────────────────────────────────────────┐
│     🎉 DEPLOYMENT COMPLETE! 🎉              │
│                                             │
│  Your system is now running in production!  │
│                                             │
│  Access: http://yourdomain.com              │
│  API: http://yourdomain.com/api             │
│  Health: /api/actuator/health               │
└─────────────────────────────────────────────┘

═══════════════════════════════════════════════════════════════

QUICK TROUBLESHOOTING GUIDE

┌─ Container won't start
│  └─→ docker compose logs [service-name]
│      └─→ Check error message
│          ├─→ Port conflict? Change port in .env
│          ├─→ Permission issue? Run as sudo
│          └─→ Missing env var? Check .env

┌─ Can't connect to database
│  └─→ docker compose ps mysql
│      ├─→ Not healthy? Wait 30 seconds
│      ├─→ Still failing? Check DB_PASSWORD in .env
│      └─→ Restart: docker compose restart mysql

┌─ Frontend shows error
│  └─→ Check backend is running
│      └─→ curl http://localhost:8080/api/actuator/health
│          ├─→ Failed? Check backend logs
│          └─→ OK? Check Nginx config

┌─ Email not sending
│  └─→ Verify MAIL_* settings in .env
│      ├─→ Gmail? Use app password, not regular password
│      ├─→ Outlook? Enable SMTP in account settings
│      └─→ Check logs: docker compose logs backend | grep -i mail

┌─ CORS error in browser
│  └─→ Update CORS_ALLOWED_ORIGINS in .env
│      └─→ Restart backend: docker compose restart backend

═══════════════════════════════════════════════════════════════

COMMON COMMANDS REFERENCE

# View logs
docker compose logs -f [service]

# Restart service
docker compose restart [service]

# Stop all
docker compose down

# Start all
docker compose up -d

# Check status
docker compose ps

# Backup database
docker compose exec -T mysql mysqldump -uemsuser -p employee_management_system > backup.sql

# Access MySQL
docker compose exec mysql mysql -uemsuser -p employee_management_system

# View resource usage
docker compose stats

# Update application
git pull && docker compose build --no-cache && docker compose up -d

═══════════════════════════════════════════════════════════════

TIME ESTIMATE

Task                          | Time Required
------------------------------|---------------
Install Docker                | 10-15 minutes
Download application          | 5 minutes
Configure .env               | 10 minutes
Build & deploy              | 10-15 minutes
Initialize system           | 5 minutes
Configure domain/SSL        | 15-30 minutes (optional)
Setup backups              | 10 minutes

TOTAL (Basic):               45-60 minutes
TOTAL (With SSL & Backups):  70-90 minutes

═══════════════════════════════════════════════════════════════

SUPPORT RESOURCES

📖 Complete Guide: COMPLETE_DEPLOYMENT_STEPS.md
🐳 Docker Guide: DOCKER_DEPLOYMENT.md
⚡ Quick Reference: DOCKER_QUICK_REFERENCE.md
🗄️ MySQL Guide: MYSQL_PRODUCTION_GUIDE.md
✅ Checklist: PRE_DEPLOYMENT_CHECKLIST.md
📊 Production Ready: PRODUCTION_READY.md

═══════════════════════════════════════════════════════════════
```

