# 🚀 Quick Deployment Reference Card

**Ubuntu 24.04 LTS - Fresh Deployment**

---

## One-Command Deployment

```bash
# Download and run deployment script
wget https://raw.githubusercontent.com/YOUR_REPO/main/fresh-deploy.sh
chmod +x fresh-deploy.sh
./fresh-deploy.sh
```

Or if you have the repo:

```bash
cd EmployeeManagementSystem
chmod +x fresh-deploy.sh
./fresh-deploy.sh
```

---

## What Gets Installed

✅ System updates
✅ Docker & Docker Compose
✅ Application code
✅ MySQL database
✅ Backend (Spring Boot)
✅ Frontend (Angular + Nginx)
✅ Automated backups
✅ Firewall rules

**Time Required:** 15-20 minutes

---

## Post-Installation (5 Minutes)

### 1. Access Application
```
URL: http://your-server-ip
```

### 2. Create ROOT Account
- Click "Create ROOT Account"
- Fill in details
- Login

### 3. Create Organization
- Navigate to Organizations
- Click "Create Organization"
- Fill details
- Save credentials!

**Done!** 🎉

---

## Configuration Needed

### Must Configure:
- [ ] ROOT account password
- [ ] First organization
- [ ] Admin credentials

### Should Configure:
- [ ] Email notifications
- [ ] SSL/HTTPS
- [ ] Firewall rules

### Optional:
- [ ] Custom domain
- [ ] Backup schedule
- [ ] Alert policies

**Guide:** `POST_INSTALL_CONFIG.md`

---

## Essential Commands

```bash
# Check status
docker compose ps

# View logs
docker compose logs -f

# Restart
docker compose restart

# Stop
docker compose down

# Start
docker compose up -d

# Backup
./backup.sh

# Monitor
./monitor.sh

# Update
./update.sh
```

---

## Default Ports

- **Frontend:** 80 (HTTP)
- **Backend:** 8080 (API)
- **Database:** 3307 (internal)

---

## Important Files

```
/opt/EmployeeManagementSystem/
├── .env                    # Credentials (SECURE!)
├── docker-compose.yml      # Service config
├── backup.sh              # Backup script
├── monitor.sh             # Health check
├── deployment-info.txt    # Install info
└── POST_INSTALL_CONFIG.md # This guide
```

---

## Security Checklist

- [ ] Change default passwords
- [ ] Configure firewall
- [ ] Enable SSL/HTTPS
- [ ] Secure .env file (chmod 600)
- [ ] Setup backups
- [ ] Review security audit

**Run:** `./security-fixes.sh`

---

## Troubleshooting

### Services not starting?
```bash
docker compose logs backend
docker compose restart
```

### Can't access application?
```bash
# Check firewall
sudo ufw status

# Check services
docker compose ps
```

### Email not working?
- Check `.env` has MAIL_USERNAME and MAIL_PASSWORD
- Use Gmail app password (not regular password)
- Restart: `docker compose restart backend`

---

## Quick Health Check

```bash
# All should return success
curl http://localhost:8080/api/actuator/health
curl http://localhost/
docker compose ps
```

---

## Next Steps

1. ✅ Complete POST_INSTALL_CONFIG.md checklist
2. ✅ Configure email notifications
3. ✅ Setup SSL/HTTPS
4. ✅ Create first organization
5. ✅ Add employees
6. ✅ Test all features

---

## Support

**Documentation:**
- Full Guide: `DOCKER_DEPLOYMENT_GUIDE.md`
- Configuration: `POST_INSTALL_CONFIG.md`
- Security: `SECURITY_AUDIT_REPORT.md`
- Cleanup: `VM_CLEANUP_GUIDE.md`

**Commands:**
- Health: `./monitor.sh`
- Backup: `./backup.sh`
- Security: `./security-fixes.sh`
- Update: `./update.sh`

---

## Credentials Location

**CRITICAL - SECURE THESE FILES:**

```bash
# Main credentials
cat /opt/EmployeeManagementSystem/.env

# Deployment info
cat /opt/EmployeeManagementSystem/deployment-info.txt

# Backup credentials
cp .env ~/.ems-credentials-backup
chmod 600 ~/.ems-credentials-backup
```

**Never commit .env to Git!**

---

*Quick Reference Card v1.0*
*For Ubuntu 24.04 LTS*

