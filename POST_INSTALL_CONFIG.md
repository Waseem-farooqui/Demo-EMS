# 📋 Post-Installation Configuration Guide

Complete configuration checklist after fresh deployment on Ubuntu 24.04.

**Deployment Date:** _________________

---

## ✅ Immediate Actions (First 30 minutes)

### 1. Verify Deployment

```bash
# Check all services are running
docker compose ps

# Expected output:
# ems-backend    running    8080/tcp
# ems-frontend   running    80/tcp
# ems-mysql      running    3307/tcp

# Check service health
curl http://localhost:8080/api/actuator/health
curl http://localhost/
```

**Status:** ☐ All services running

---

### 2. Create ROOT Account

1. **Access Application:**
   - URL: `http://your-domain` or `http://your-server-ip`

2. **Click "Create ROOT Account"**
   - Username: `root` (recommended)
   - Email: Your admin email
   - Password: Strong password (16+ characters)

3. **Login with ROOT credentials**

**Status:** ☐ ROOT account created

---

### 3. Review Generated Credentials

Location: `/opt/EmployeeManagementSystem/.env`

```bash
# View credentials (secure - never commit to git)
cat /opt/EmployeeManagementSystem/.env

# Save to secure location (optional)
cp /opt/EmployeeManagementSystem/.env ~/ems-credentials-backup.env
chmod 600 ~/ems-credentials-backup.env
```

**Important Values:**
- `JWT_SECRET`: Auto-generated (64+ chars) ✓
- `DB_ROOT_PASSWORD`: Auto-generated (32+ chars) ✓
- `DB_PASSWORD`: Auto-generated (32+ chars) ✓

**Status:** ☐ Credentials reviewed and secured

---

## 🔐 Security Configuration (First Hour)

### 4. Configure Email (Optional but Recommended)

**If you skipped email during installation:**

```bash
# Edit .env file
nano /opt/EmployeeManagementSystem/.env

# Add/Update these lines:
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-gmail-app-password
```

**Gmail Setup:**
1. Enable 2-Factor Authentication
2. Go to: https://myaccount.google.com/apppasswords
3. Generate app password
4. Copy 16-character password
5. Add to .env file

```bash
# Restart services after email configuration
cd /opt/EmployeeManagementSystem
docker compose restart backend
```

**Status:** ☐ Email configured (or ☐ Skip if not needed)

---

### 5. SSL/HTTPS Setup (Highly Recommended for Production)

**Using Let's Encrypt (Free):**

```bash
# Install Certbot
sudo apt-get install -y certbot python3-certbot-nginx

# Stop frontend temporarily
docker compose stop frontend

# Get SSL certificate
sudo certbot certonly --standalone -d your-domain.com

# Start frontend
docker compose start frontend
```

**Configure Nginx SSL:**

Create `frontend/nginx-ssl.conf`:

```nginx
server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name your-domain.com;

    ssl_certificate /etc/letsencrypt/live/your-domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem;

    # Security headers
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    
    # ... rest of nginx config
}
```

**Update .env:**
```bash
APP_URL=https://your-domain.com
CORS_ALLOWED_ORIGINS=https://your-domain.com
```

**Restart:**
```bash
docker compose restart
```

**Auto-renewal:**
```bash
sudo certbot renew --dry-run
```

**Status:** ☐ SSL configured (or ☐ Skip for development)

---

### 6. Run Security Validation

```bash
cd /opt/EmployeeManagementSystem
chmod +x security-fixes.sh
./security-fixes.sh
```

**Expected Output:**
- ✅ No hardcoded credentials
- ✅ Environment variables set
- ✅ JWT secret is strong
- ✅ Password strength validated

**Status:** ☐ Security validation passed

---

### 7. Configure Firewall

**If not done during installation:**

```bash
# Enable UFW
sudo ufw --force enable

# Allow necessary ports
sudo ufw allow ssh
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS (if using SSL)
sudo ufw allow 8080/tcp  # Backend API

# Block database port from external access
sudo ufw deny 3307/tcp

# Check status
sudo ufw status verbose
```

**Status:** ☐ Firewall configured

---

## 🏢 Application Setup (First Day)

### 8. Create First Organization

1. **Login as ROOT**
2. **Navigate to "Organizations"**
3. **Click "Create Organization"**
4. **Fill in details:**
   - Organization Name
   - Contact Email
   - Contact Phone
   - Address
   - Super Admin details
   - Upload logo (optional)

5. **Credentials will be displayed** - Save them securely!

**Status:** ☐ First organization created

---

### 9. Configure Document Alert Policies

1. **Login as SUPER_ADMIN** (from organization)
2. **Navigate to "Alert Configuration"**
3. **Create alert policies:**

**Recommended Configurations:**

| Document Type | Priority | Days Before | Frequency | Type |
|--------------|----------|-------------|-----------|------|
| Passport | CRITICAL | 60 | DAILY | BOTH |
| Visa | CRITICAL | 30 | DAILY | BOTH |
| Contract | WARNING | 30 | ONCE | IN-APP |
| Right to Work | ATTENTION | 60 | DAILY | BOTH |

**Status:** ☐ Alert policies configured

---

### 10. Setup Leave Management Rules

Default rules are pre-configured:
- ✓ Sick Leave: 2 consecutive days require certificate
- ✓ Casual Leave: 1 day maximum per request
- ✓ Total Annual Leave: 20 days per year
- ✓ Leave types: Casual, Sick, Annual, Other

**Customize if needed** in Leave Management settings.

**Status:** ☐ Leave rules reviewed

---

## 🔧 System Configuration (First Week)

### 11. Setup Automated Backups

**If not configured during installation:**

```bash
cd /opt/EmployeeManagementSystem

# Make backup script executable
chmod +x backup.sh

# Test backup
./backup.sh

# Setup cron job (daily at 2 AM)
crontab -e

# Add line:
0 2 * * * cd /opt/EmployeeManagementSystem && ./backup.sh >> /var/log/ems-backup.log 2>&1
```

**Verify:**
```bash
# Check cron jobs
crontab -l

# Check backup directory
ls -lh /backups/ems/
```

**Status:** ☐ Automated backups configured

---

### 12. Setup Monitoring

```bash
cd /opt/EmployeeManagementSystem

# Make monitor script executable
chmod +x monitor.sh

# Run manual check
./monitor.sh

# Setup monitoring cron (every hour)
crontab -e

# Add line:
0 * * * * cd /opt/EmployeeManagementSystem && ./monitor.sh >> /var/log/ems-monitor.log 2>&1
```

**Status:** ☐ Monitoring configured

---

### 13. Configure Log Rotation

Create `/etc/logrotate.d/ems`:

```bash
sudo nano /etc/logrotate.d/ems
```

Add:
```
/var/log/ems*.log {
    daily
    missingok
    rotate 14
    compress
    delaycompress
    notifempty
    create 0640 your-username your-username
}
```

**Test:**
```bash
sudo logrotate -d /etc/logrotate.d/ems
```

**Status:** ☐ Log rotation configured

---

## 📊 Performance Optimization (Optional)

### 14. Database Tuning

Create `mysql-custom.cnf`:

```bash
sudo nano /opt/EmployeeManagementSystem/mysql-custom.cnf
```

Add:
```ini
[mysqld]
# Performance
innodb_buffer_pool_size = 2G
innodb_log_file_size = 256M
max_connections = 200
query_cache_size = 64M
query_cache_type = 1

# Security
bind-address = 0.0.0.0
skip-name-resolve

# Character set
character-set-server = utf8mb4
collation-server = utf8mb4_unicode_ci
```

**Update docker-compose.yml:**
```yaml
mysql:
  volumes:
    - ./mysql-custom.cnf:/etc/mysql/conf.d/custom.cnf:ro
```

**Restart:**
```bash
docker compose restart mysql
```

**Status:** ☐ Database tuned (or ☐ Skip)

---

### 15. Enable Docker Logging Limits

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

**Apply:**
```bash
docker compose up -d
```

**Status:** ☐ Logging limits configured

---

## 🧪 Testing & Validation (After Configuration)

### 16. Functional Testing

Test each feature:

- [ ] **User Authentication**
  - ROOT login works
  - Organization creation works
  - SUPER_ADMIN login works
  - Password reset works (if email configured)

- [ ] **Employee Management**
  - Create employee
  - Edit employee
  - View employee list
  - Delete employee

- [ ] **Document Management**
  - Upload document
  - OCR extraction works
  - Document preview works
  - Download document works
  - Document alerts work

- [ ] **Leave Management**
  - Apply for leave
  - Approve/reject leave
  - Leave balance updates
  - Leave validation works

- [ ] **ROTA Management**
  - Create schedule
  - Assign employees
  - View schedules

- [ ] **Notifications**
  - In-app notifications work
  - Email notifications work (if configured)
  - Document expiry alerts work

**Status:** ☐ All features tested

---

### 17. Performance Testing

```bash
# Check response times
time curl http://localhost:8080/api/actuator/health

# Check resource usage
docker stats

# Check disk space
df -h
```

**Expected:**
- Response time < 500ms
- CPU usage < 50% idle
- Memory usage < 80%
- Disk space > 20% free

**Status:** ☐ Performance acceptable

---

## 📝 Documentation & Handover

### 18. Update Documentation

Create `/opt/EmployeeManagementSystem/DEPLOYMENT_NOTES.md`:

```markdown
# Deployment Notes

## Server Information
- Server IP: _______________
- Domain: _______________
- OS: Ubuntu 24.04 LTS
- Deployment Date: _______________

## Access Credentials
- ROOT Username: _______________
- ROOT Email: _______________
- Organization: _______________
- SUPER_ADMIN: _______________

## Configuration
- SSL: [ ] Yes  [ ] No
- Email: [ ] Configured  [ ] Not configured
- Backups: [ ] Automated  [ ] Manual
- Monitoring: [ ] Enabled  [ ] Disabled

## Important Files
- Environment: /opt/EmployeeManagementSystem/.env
- Backups: /backups/ems/
- Logs: /var/log/ems*.log

## Support Contacts
- Technical: _______________
- Email: _______________
- Phone: _______________
```

**Status:** ☐ Documentation created

---

### 19. Create Admin Guide

Provide to organization admins:

```markdown
# Quick Admin Guide

## Access
URL: http://your-domain
Username: [provided separately]
Password: [provided separately]

## First Login
1. Login with provided credentials
2. Change password immediately
3. Complete profile
4. Create departments
5. Add employees

## Support
Email: support@your-company.com
Documentation: http://your-domain/docs
```

**Status:** ☐ Admin guide created

---

## ✅ Final Checklist

**Before Going Live:**

- [ ] All services running and healthy
- [ ] ROOT account created and tested
- [ ] First organization created
- [ ] SSL/HTTPS configured (production)
- [ ] Email notifications tested
- [ ] Firewall configured
- [ ] Automated backups working
- [ ] Monitoring setup
- [ ] Security validation passed
- [ ] All features tested
- [ ] Performance acceptable
- [ ] Documentation complete
- [ ] Admin credentials secured
- [ ] Backup credentials saved

**Production Readiness:**
- [ ] SSL certificate valid
- [ ] DNS configured correctly
- [ ] Email working
- [ ] Backups tested and verified
- [ ] Disaster recovery plan documented
- [ ] Support team trained
- [ ] Users notified

---

## 🆘 Troubleshooting

### Services Not Starting

```bash
# Check logs
docker compose logs backend
docker compose logs frontend
docker compose logs mysql

# Restart services
docker compose restart

# Rebuild if needed
docker compose down
docker compose up -d --build
```

### Email Not Working

```bash
# Check email configuration
cat /opt/EmployeeManagementSystem/.env | grep MAIL

# Test SMTP connection
telnet smtp.gmail.com 587

# Check backend logs
docker compose logs backend | grep -i mail
```

### Database Connection Issues

```bash
# Check MySQL is running
docker compose ps mysql

# Test connection
docker compose exec mysql mysql -u root -p

# Check database
docker compose exec mysql mysql -u root -p -e "SHOW DATABASES;"
```

### Performance Issues

```bash
# Check resource usage
docker stats

# Check disk space
df -h

# Clean Docker
docker system prune -f
```

---

## 📞 Support

**Documentation:**
- Full Deployment Guide: `/opt/EmployeeManagementSystem/DOCKER_DEPLOYMENT_GUIDE.md`
- Security Audit: `/opt/EmployeeManagementSystem/SECURITY_AUDIT_REPORT.md`
- Cleanup Guide: `/opt/EmployeeManagementSystem/VM_CLEANUP_GUIDE.md`

**Useful Commands:**
```bash
# View all logs
docker compose logs -f

# Restart service
docker compose restart backend

# Check health
./monitor.sh

# Create backup
./backup.sh

# Security check
./security-fixes.sh
```

---

## 📅 Maintenance Schedule

**Daily:**
- ✓ Automated backups (2 AM)
- ✓ Health monitoring (hourly)

**Weekly:**
- [ ] Review error logs
- [ ] Check disk space
- [ ] Test backup restoration

**Monthly:**
- [ ] Update system packages
- [ ] Review security settings
- [ ] Check SSL certificate expiry
- [ ] Test disaster recovery

**Quarterly:**
- [ ] Full security audit
- [ ] Performance review
- [ ] Update documentation
- [ ] User training review

---

**Configuration Completed:** ☐

**Signed Off By:** _________________

**Date:** _________________

---

*Post-Installation Configuration Guide - Version 1.0*
*Last Updated: November 14, 2025*

