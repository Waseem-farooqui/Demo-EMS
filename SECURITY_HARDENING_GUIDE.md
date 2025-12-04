# Security Hardening Guide

## Overview
This guide explains the security enhancements implemented to protect your Ubuntu server and Spring Boot application from attacks.

## What Was Implemented

### 1. Spring Security Enhancements

#### StrictHttpFirewall Configuration
- **Blocks malicious URLs** containing dangerous characters (`;`, `//`, `%`, etc.)
- **Logs all blocked requests** with IP addresses and user agents
- **Prevents SQL injection** attempts via URL manipulation
- **Blocks path traversal** attempts

#### Security Request Filter
- **Pre-filters requests** before they reach Spring Security
- **Detects common attack patterns**:
  - SQL injection (`union select`, `drop table`)
  - XSS attempts (`<script>`, `javascript:`)
  - Path traversal (`../`)
  - Command injection (`exec`, `eval`)
  - Event handler injection (`onerror`, `onload`)

#### Enhanced Logging
- **SecurityEventLogger** logs all security events to `/var/log/fail2ban-spring-boot.log`
- **Fail2Ban integration** automatically bans IPs after repeated attacks
- **Detailed logging** includes IP, URI, method, user agent, and reason

### 2. Ubuntu Server Hardening

#### Firewall (UFW)
- **Default deny** all incoming connections
- **Only allows** necessary ports (SSH, HTTP, HTTPS, Backend API)
- **Blocks** all other ports by default

#### Fail2Ban Configuration
- **Monitors** Spring Boot security logs
- **Automatically bans** IPs after 5 failed attempts
- **Ban duration**: 1 hour
- **Detection window**: 10 minutes

#### SSH Hardening
- **Root login disabled**
- **Max auth tries**: 3 attempts
- **Password authentication**: Enabled (consider disabling for key-only)
- **X11 forwarding**: Disabled
- **TCP forwarding**: Disabled

#### Automatic Security Updates
- **Enabled** automatic installation of security patches
- **Daily checks** for updates
- **Automatic cleanup** of old packages

#### Kernel Security Parameters
- **IP spoofing protection** enabled
- **ICMP redirects** blocked
- **SYN flood protection** enabled
- **Martian packet logging** enabled

## Installation

### 1. Run Ubuntu Security Hardening Script

```bash
sudo chmod +x ubuntu-server-security-hardening.sh
sudo ./ubuntu-server-security-hardening.sh
```

**⚠️ IMPORTANT**: Test SSH access before closing your current session!

### 2. Rebuild Backend with Security Enhancements

The backend code has been updated with:
- Enhanced `SecurityConfig` with StrictHttpFirewall
- `SecurityRequestFilter` for pre-filtering malicious requests
- `SecurityEventLogger` for comprehensive security logging
- Updated `JwtAuthenticationEntryPoint` with security event logging

Rebuild the backend:
```bash
docker-compose build backend
docker-compose up -d backend
```

### 3. Verify Security Logging

Check if security logs are being written:
```bash
sudo tail -f /var/log/fail2ban-spring-boot.log
```

## Monitoring Security Events

### View Blocked Requests
```bash
sudo tail -f /var/log/fail2ban-spring-boot.log | grep "BLOCKED MALICIOUS REQUEST"
```

### View Unauthorized Access Attempts
```bash
sudo tail -f /var/log/fail2ban-spring-boot.log | grep "UNAUTHORIZED ACCESS"
```

### Check Fail2Ban Status
```bash
sudo fail2ban-client status
sudo fail2ban-client status spring-boot
```

### View Banned IPs
```bash
sudo fail2ban-client status spring-boot
```

### Unban an IP (if needed)
```bash
sudo fail2ban-client set spring-boot unbanip <IP_ADDRESS>
```

## Security Best Practices

### 1. Regular Security Audits
Run the security monitoring script daily:
```bash
sudo /usr/local/bin/security-monitor.sh
```

### 2. Review Logs Regularly
- Check `/var/log/fail2ban-spring-boot.log` daily
- Monitor application logs for suspicious activity
- Review SSH access logs: `sudo tail -f /var/log/auth.log`

### 3. Keep System Updated
Security updates are automatic, but verify:
```bash
sudo apt-get update
sudo apt-get upgrade
```

### 4. Firewall Management
- **View status**: `sudo ufw status verbose`
- **Allow new port**: `sudo ufw allow <PORT>/tcp`
- **Deny IP**: `sudo ufw deny from <IP_ADDRESS>`

### 5. SSH Security
- **Use SSH keys** instead of passwords
- **Change SSH port** (optional): Edit `/etc/ssh/sshd_config`
- **Limit SSH access** to specific IPs (optional)

## What Gets Blocked

The following types of requests are automatically blocked:

1. **URLs with semicolons** (`;`) - Common in SQL injection
2. **URLs with double slashes** (`//`) - Path traversal attempts
3. **SQL injection patterns** - `union select`, `drop table`, etc.
4. **XSS attempts** - `<script>`, `javascript:`, event handlers
5. **Path traversal** - `../` patterns
6. **Command injection** - `exec`, `eval` patterns
7. **Null bytes** - `%00` encoding
8. **Newline/carriage return** - `%0a`, `%0d`

## Fail2Ban Rules

Fail2Ban automatically bans IPs that:
- Make 5+ blocked malicious requests within 10 minutes
- Attempt unauthorized access repeatedly
- Trigger security exceptions

**Ban Duration**: 1 hour
**Detection Window**: 10 minutes
**Max Retries**: 5

## Troubleshooting

### If Legitimate Requests Are Blocked

1. **Check the log** to see why:
   ```bash
   sudo tail -100 /var/log/fail2ban-spring-boot.log
   ```

2. **Unban the IP** if needed:
   ```bash
   sudo fail2ban-client set spring-boot unbanip <IP_ADDRESS>
   ```

3. **Adjust patterns** in `SecurityRequestFilter.java` if needed

### If Fail2Ban Isn't Working

1. **Check Fail2Ban status**:
   ```bash
   sudo systemctl status fail2ban
   ```

2. **Check log file permissions**:
   ```bash
   sudo ls -la /var/log/fail2ban-spring-boot.log
   sudo chmod 644 /var/log/fail2ban-spring-boot.log
   ```

3. **Restart Fail2Ban**:
   ```bash
   sudo systemctl restart fail2ban
   ```

### If Security Logs Aren't Being Written

1. **Check file permissions**:
   ```bash
   sudo touch /var/log/fail2ban-spring-boot.log
   sudo chmod 644 /var/log/fail2ban-spring-boot.log
   sudo chown root:root /var/log/fail2ban-spring-boot.log
   ```

2. **Check Docker volume mounts** - Ensure logs directory is mounted

## Additional Security Recommendations

1. **Enable HTTPS** - Use SSL/TLS certificates
2. **Rate Limiting** - Consider adding rate limiting for API endpoints
3. **IP Whitelisting** - For admin endpoints, consider IP whitelisting
4. **Regular Backups** - Ensure regular backups are configured
5. **Monitor Disk Space** - Set up alerts for disk usage
6. **Review Access Logs** - Regularly review who is accessing your system
7. **Keep Dependencies Updated** - Regularly update application dependencies
8. **Use Strong Passwords** - Enforce strong password policies
9. **Enable 2FA** - Consider two-factor authentication for admin accounts
10. **Regular Security Scans** - Run security scans with tools like Lynis

## Support

If you encounter issues:
1. Check application logs: `docker-compose logs backend`
2. Check security logs: `sudo tail -f /var/log/fail2ban-spring-boot.log`
3. Check Fail2Ban logs: `sudo tail -f /var/log/fail2ban.log`
4. Review firewall rules: `sudo ufw status verbose`

