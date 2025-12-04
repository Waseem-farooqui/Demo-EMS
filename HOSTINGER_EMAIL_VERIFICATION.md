# Hostinger Email Configuration Verification

This document verifies that Hostinger SMTP email configuration is properly set up across all configuration files.

## ✅ Configuration Files Status

### 1. **prod.env** (Production Template)
**Location:** `prod.env`  
**Status:** ✅ Configured

```env
# For Hostinger SMTP (Port 465 - SSL):
MAIL_HOST=smtp.hostinger.com
MAIL_PORT=465
MAIL_USERNAME=support@vertexdigitalsystem.com
MAIL_PASSWORD=CHANGE_THIS_TO_YOUR_EMAIL_PASSWORD
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=false
MAIL_SMTP_STARTTLS_REQUIRED=false
MAIL_SMTP_SSL_ENABLE=true
MAIL_SMTP_SSL_TRUST=smtp.hostinger.com
MAIL_SMTP_SSL_SOCKET_FACTORY_PORT=465
EMAIL_FROM_NAME=Employee Management System
EMAIL_FROM_ADDRESS=support@vertexdigitalsystem.com
```

**Notes:**
- ✅ Port 465 (SSL) configuration is set
- ✅ SSL socket factory port is configured (465)
- ✅ Alternative Port 587 (TLS) configuration is documented in comments
- ⚠️ **Action Required:** Update `MAIL_PASSWORD` with actual email password

### 2. **application-prod.properties** (Spring Boot Properties)
**Location:** `src/main/resources/application-prod.properties`  
**Status:** ✅ Configured

**Key Properties:**
```properties
# Email host and port (from environment variables)
spring.mail.host=${MAIL_HOST:smtp.gmail.com}
spring.mail.port=${MAIL_PORT:587}
spring.mail.username=${MAIL_USERNAME:}
spring.mail.password=${MAIL_PASSWORD:}

# SMTP Authentication & Security
spring.mail.properties.mail.smtp.auth=${MAIL_SMTP_AUTH:true}
spring.mail.properties.mail.smtp.starttls.enable=${MAIL_SMTP_STARTTLS_ENABLE:true}
spring.mail.properties.mail.smtp.starttls.required=${MAIL_SMTP_STARTTLS_REQUIRED:true}

# SSL Configuration (for port 465 - Hostinger SSL)
spring.mail.properties.mail.smtp.ssl.enable=${MAIL_SMTP_SSL_ENABLE:false}
spring.mail.properties.mail.smtp.ssl.trust=${MAIL_SMTP_SSL_TRUST:${MAIL_HOST}}
spring.mail.properties.mail.smtp.ssl.protocols=${MAIL_SMTP_SSL_PROTOCOLS:TLSv1.2}

# Socket factory for SSL (required for port 465)
spring.mail.properties.mail.smtp.socketFactory.class=javax.net.ssl.SSLSocketFactory
spring.mail.properties.mail.smtp.socketFactory.port=${MAIL_SMTP_SSL_SOCKET_FACTORY_PORT:}
spring.mail.properties.mail.smtp.socketFactory.fallback=false
```

**Documentation in File:**
- ✅ Hostinger Port 465 (SSL) configuration documented (lines 103-110)
- ✅ Hostinger Port 587 (TLS) configuration documented (lines 112-119)
- ✅ All environment variables properly mapped

### 3. **compose.yaml** (Docker Compose)
**Location:** `compose.yaml`  
**Status:** ✅ Configured

**Environment Variables:**
```yaml
# Email Configuration
# For Hostinger: Use smtp.hostinger.com with port 465 (SSL) or 587 (TLS)
# Port 465 (SSL): Set MAIL_PORT=465, MAIL_SMTP_SSL_ENABLE=true, MAIL_SMTP_STARTTLS_ENABLE=false
# Port 587 (TLS): Set MAIL_PORT=587, MAIL_SMTP_SSL_ENABLE=false, MAIL_SMTP_STARTTLS_ENABLE=true
MAIL_HOST: ${MAIL_HOST:-smtp.gmail.com}
MAIL_PORT: ${MAIL_PORT:-587}
MAIL_USERNAME: ${MAIL_USERNAME}
MAIL_PASSWORD: ${MAIL_PASSWORD}
MAIL_SMTP_AUTH: ${MAIL_SMTP_AUTH:-true}
MAIL_SMTP_STARTTLS_ENABLE: ${MAIL_SMTP_STARTTLS_ENABLE:-true}
MAIL_SMTP_STARTTLS_REQUIRED: ${MAIL_SMTP_STARTTLS_REQUIRED:-true}
MAIL_SMTP_SSL_ENABLE: ${MAIL_SMTP_SSL_ENABLE:-false}
MAIL_SMTP_SSL_TRUST: ${MAIL_SMTP_SSL_TRUST:-smtp.gmail.com}
MAIL_SMTP_SSL_SOCKET_FACTORY_PORT: ${MAIL_SMTP_SSL_SOCKET_FACTORY_PORT:-}
EMAIL_FROM_NAME: ${EMAIL_FROM_NAME:-Employee Management System}
EMAIL_FROM_ADDRESS: ${EMAIL_FROM_ADDRESS:-${MAIL_USERNAME}}
```

**Notes:**
- ✅ All Hostinger SMTP environment variables are defined
- ✅ SSL socket factory port is included
- ✅ Documentation comments explain Hostinger configuration

### 4. **application.properties** (Development)
**Location:** `src/main/resources/application.properties`  
**Status:** ✅ Documented

**Notes:**
- ✅ Hostinger mentioned in comments (line 57)
- ✅ Uses environment variables (same as production)
- ✅ Properly configured for development use

## 📋 Hostinger SMTP Settings Summary

### Port 465 (SSL) - Recommended for Hostinger
```
MAIL_HOST=smtp.hostinger.com
MAIL_PORT=465
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=false
MAIL_SMTP_STARTTLS_REQUIRED=false
MAIL_SMTP_SSL_ENABLE=true
MAIL_SMTP_SSL_TRUST=smtp.hostinger.com
MAIL_SMTP_SSL_SOCKET_FACTORY_PORT=465
```

### Port 587 (TLS/STARTTLS) - Alternative
```
MAIL_HOST=smtp.hostinger.com
MAIL_PORT=587
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=true
MAIL_SMTP_STARTTLS_REQUIRED=true
MAIL_SMTP_SSL_ENABLE=false
MAIL_SMTP_SSL_TRUST=smtp.hostinger.com
```

## ✅ Verification Checklist

- [x] `prod.env` has Hostinger configuration
- [x] `prod.env` includes SSL socket factory port (465)
- [x] `application-prod.properties` maps all environment variables
- [x] `application-prod.properties` has SSL socket factory configuration
- [x] `compose.yaml` includes all Hostinger SMTP environment variables
- [x] Documentation exists in `HOSTINGER_SMTP_SETUP.md`
- [x] Alternative Port 587 configuration is documented
- [ ] **TODO:** Update `.env` file on production server with actual credentials

## 🚀 Production Deployment Steps

1. **Copy template to .env:**
   ```bash
   cp prod.env .env
   ```

2. **Edit .env and update:**
   - `MAIL_PASSWORD` - Your Hostinger email password
   - `MAIL_USERNAME` - Verify it's `support@vertexdigitalsystem.com`
   - All other `CHANGE_THIS` values

3. **Verify configuration:**
   ```bash
   # Check environment variables are loaded
   docker compose config | grep MAIL
   ```

4. **Restart backend:**
   ```bash
   docker compose restart backend
   ```

5. **Test email:**
   - Create a new user account
   - Check if welcome email is sent
   - Check backend logs: `docker compose logs backend | grep -i mail`

## 🔍 Troubleshooting

### If emails are not sending:

1. **Check environment variables:**
   ```bash
   docker compose exec backend env | grep MAIL
   ```

2. **Check backend logs:**
   ```bash
   docker compose logs backend | grep -i "mail\|smtp\|email"
   ```

3. **Verify Hostinger email account:**
   - Log in to Hostinger hPanel
   - Check email account is active
   - Verify password is correct

4. **Test SMTP connection:**
   ```bash
   # From your server
   telnet smtp.hostinger.com 465
   # or
   telnet smtp.hostinger.com 587
   ```

## 📝 Notes

- **Port 465 (SSL)** is recommended for Hostinger as it's more secure
- **Port 587 (TLS)** is an alternative if port 465 is blocked
- SSL socket factory port (465) is **required** for port 465 to work correctly
- All configuration uses environment variables for security
- Never commit `.env` file with actual passwords

