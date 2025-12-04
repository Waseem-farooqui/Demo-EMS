# Hostinger SMTP Port 587 Authentication Fix

## Current Issue
- Port 587 (TLS/STARTTLS) is configured
- Email account exists and is active
- Still getting: `535 5.7.8 Error: authentication failed`

## Root Causes & Solutions

### 1. Firewall Check (Most Likely NOT the Issue)
The firewall script sets `ufw default allow outgoing`, which should allow SMTP. However, verify:

```bash
# Check firewall status
sudo ufw status verbose

# Should show: "Default: deny (incoming), allow (outgoing)"
```

**If outgoing is blocked:**
```bash
sudo ufw default allow outgoing
sudo ufw reload
```

**Run the diagnostic script:**
```bash
chmod +x check-smtp-connectivity.sh
sudo ./check-smtp-connectivity.sh
```

### 2. Password Verification (MOST LIKELY ISSUE)

Even though the account is active, the password might be incorrect:

1. **Test webmail login:**
   - Go to: https://webmail.hostinger.com
   - Email: `support@vertexdigitalsystem.com`
   - Password: `wuf202019$WUF`
   - If login fails → Password is wrong

2. **Reset password in Hostinger:**
   - Log in to hPanel: https://hpanel.hostinger.com
   - Go to Email section
   - Click three dots (⋮) next to `support@vertexdigitalsystem.com`
   - Select "Change password"
   - Set a new strong password
   - Update `.env` file with new password

3. **Special characters in password:**
   - The password contains `$` which might need escaping
   - In `.env` file, you can use quotes if needed:
     ```env
     MAIL_PASSWORD="wuf202019\$WUF"
     ```
   - Or escape the dollar sign:
     ```env
     MAIL_PASSWORD=wuf202019\$$WUF
     ```

### 3. Docker Network Restrictions

Check if Docker can reach SMTP:

```bash
# Test from backend container
docker compose exec backend bash -c "timeout 5 bash -c 'echo > /dev/tcp/smtp.hostinger.com/587' && echo 'Connection OK' || echo 'Connection FAILED'"
```

**If connection fails:**
- Check Docker network configuration
- Ensure backend container has internet access
- Check if Docker is using a proxy

### 4. Hostinger IP Whitelisting

Some Hostinger accounts require IP whitelisting for SMTP:

1. **Check your server's public IP:**
   ```bash
   curl -s ifconfig.me
   # or
   curl -s ipinfo.io/ip
   ```

2. **Check Hostinger settings:**
   - Log in to hPanel
   - Go to Email section
   - Look for "SMTP Access" or "IP Whitelist" settings
   - Add your server's IP address if required

### 5. Correct Port 587 Configuration

Ensure your `.env` file has EXACTLY these settings for port 587:

```env
MAIL_HOST=smtp.hostinger.com
MAIL_PORT=587
MAIL_USERNAME=support@vertexdigitalsystem.com
MAIL_PASSWORD=wuf202019$WUF
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=true
MAIL_SMTP_STARTTLS_REQUIRED=true
MAIL_SMTP_SSL_ENABLE=false
MAIL_SMTP_SSL_TRUST=smtp.hostinger.com
EMAIL_FROM_NAME=VERTEX
EMAIL_FROM_ADDRESS=support@vertexdigitalsystem.com
```

**Critical settings for port 587:**
- ✅ `MAIL_SMTP_STARTTLS_ENABLE=true` (MUST be true)
- ✅ `MAIL_SMTP_STARTTLS_REQUIRED=true` (MUST be true)
- ✅ `MAIL_SMTP_SSL_ENABLE=false` (MUST be false for port 587)
- ✅ `MAIL_SMTP_SSL_TRUST=smtp.hostinger.com` (should be set)

### 6. Application Properties Verification

Check if `application-prod.properties` correctly maps the environment variables:

The file should have:
```properties
spring.mail.properties.mail.smtp.starttls.enable=${MAIL_SMTP_STARTTLS_ENABLE:true}
spring.mail.properties.mail.smtp.starttls.required=${MAIL_SMTP_STARTTLS_REQUIRED:true}
spring.mail.properties.mail.smtp.ssl.enable=${MAIL_SMTP_SSL_ENABLE:false}
```

### 7. Test SMTP Connection Manually

Test SMTP from your server:

```bash
# Install mail utilities if needed
sudo apt-get install -y telnet openssl

# Test port 587
openssl s_client -connect smtp.hostinger.com:587 -starttls smtp
```

If this connects, SMTP server is reachable.

## Step-by-Step Fix Procedure

### Step 1: Verify Password
```bash
# Test webmail login
# Go to: https://webmail.hostinger.com
# Login with: support@vertexdigitalsystem.com / wuf202019$WUF
```

### Step 2: Check Firewall
```bash
sudo ./check-smtp-connectivity.sh
```

### Step 3: Fix Firewall (if needed)
```bash
chmod +x fix-smtp-firewall.sh
sudo ./fix-smtp-firewall.sh
```

### Step 4: Verify .env Configuration
```bash
# On production server
cat .env | grep MAIL_
```

Should show:
```
MAIL_HOST=smtp.hostinger.com
MAIL_PORT=587
MAIL_USERNAME=support@vertexdigitalsystem.com
MAIL_PASSWORD=wuf202019$WUF
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=true
MAIL_SMTP_STARTTLS_REQUIRED=true
MAIL_SMTP_SSL_ENABLE=false
MAIL_SMTP_SSL_TRUST=smtp.hostinger.com
```

### Step 5: Restart Backend
```bash
docker compose restart backend
```

### Step 6: Check Logs
```bash
docker compose logs backend | grep -i "mail\|smtp\|email" | tail -20
```

## Alternative: Try Port 465 (SSL)

If port 587 still doesn't work, try port 465:

```env
MAIL_PORT=465
MAIL_SMTP_STARTTLS_ENABLE=false
MAIL_SMTP_STARTTLS_REQUIRED=false
MAIL_SMTP_SSL_ENABLE=true
MAIL_SMTP_SSL_SOCKET_FACTORY_PORT=465
```

## Most Common Fix

**90% of the time, the issue is the password.**

1. Reset password in Hostinger hPanel
2. Update `.env` with new password
3. Restart backend: `docker compose restart backend`

## Contact Hostinger Support

If nothing works:
1. Contact Hostinger support
2. Ask about SMTP authentication requirements
3. Verify if IP whitelisting is needed
4. Check if there are any account restrictions

