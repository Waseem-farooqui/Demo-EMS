# Email Configuration Guide

## Overview
The Employee Management System now supports multiple email providers with fully configurable settings. You can use Gmail, Outlook, Office365, or any custom SMTP server.

## Supported Email Providers

### 1. Gmail (Default)
```properties
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
MAIL_SMTP_SSL_TRUST=smtp.gmail.com
```

**Important for Gmail:**
- You must use an **App Password**, not your regular Gmail password
- Enable 2-factor authentication on your Google account
- Generate an App Password: https://myaccount.google.com/apppasswords
- Use the 16-character app password (remove spaces)

### 2. Outlook/Hotmail
```properties
MAIL_HOST=smtp-mail.outlook.com
MAIL_PORT=587
MAIL_USERNAME=your-email@outlook.com
MAIL_PASSWORD=your-password
MAIL_SMTP_SSL_TRUST=smtp-mail.outlook.com
```

**For Outlook:**
- Use your regular Outlook password
- Ensure "Less secure app access" is enabled if required
- Works with @outlook.com, @hotmail.com addresses

### 3. Office365
```properties
MAIL_HOST=smtp.office365.com
MAIL_PORT=587
MAIL_USERNAME=your-email@yourdomain.com
MAIL_PASSWORD=your-password
MAIL_SMTP_SSL_TRUST=smtp.office365.com
```

**For Office365:**
- Use your Office365 email and password
- Works with custom domain emails configured through Office365
- May require admin approval for SMTP relay

### 4. Custom SMTP Server
```properties
MAIL_HOST=smtp.yourdomain.com
MAIL_PORT=587
MAIL_USERNAME=your-email@yourdomain.com
MAIL_PASSWORD=your-password
MAIL_SMTP_SSL_TRUST=smtp.yourdomain.com
```

## Configuration Methods

### Method 1: Environment Variables (Recommended for Production)

Set these environment variables before starting the application:

**Windows:**
```cmd
set MAIL_HOST=smtp.gmail.com
set MAIL_PORT=587
set MAIL_USERNAME=your-email@gmail.com
set MAIL_PASSWORD=your-app-password
set MAIL_SMTP_SSL_TRUST=smtp.gmail.com
set EMAIL_FROM_NAME=Your Company Name
```

**Linux/Mac:**
```bash
export MAIL_HOST=smtp.gmail.com
export MAIL_PORT=587
export MAIL_USERNAME=your-email@gmail.com
export MAIL_PASSWORD=your-app-password
export MAIL_SMTP_SSL_TRUST=smtp.gmail.com
export EMAIL_FROM_NAME="Your Company Name"
```

### Method 2: application.properties (Development Only)

Edit `src/main/resources/application.properties`:

```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.ssl.trust=smtp.gmail.com

app.email.from.name=Your Company Name
app.email.from.address=${spring.mail.username}
```

⚠️ **Warning:** Never commit real passwords to version control!

### Method 3: Docker Environment File

Create a `.env` file:

```env
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
MAIL_SMTP_SSL_TRUST=smtp.gmail.com
EMAIL_FROM_NAME=Your Company Name
```

Use with docker-compose:
```yaml
services:
  app:
    environment:
      - MAIL_HOST=${MAIL_HOST}
      - MAIL_PORT=${MAIL_PORT}
      - MAIL_USERNAME=${MAIL_USERNAME}
      - MAIL_PASSWORD=${MAIL_PASSWORD}
      - MAIL_SMTP_SSL_TRUST=${MAIL_SMTP_SSL_TRUST}
      - EMAIL_FROM_NAME=${EMAIL_FROM_NAME}
```

## Advanced Configuration Options

### All Available Email Properties

```properties
# Basic Email Configuration
spring.mail.host=${MAIL_HOST:smtp.gmail.com}
spring.mail.port=${MAIL_PORT:587}
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}

# SMTP Authentication & Security
spring.mail.properties.mail.smtp.auth=${MAIL_SMTP_AUTH:true}
spring.mail.properties.mail.smtp.starttls.enable=${MAIL_SMTP_STARTTLS_ENABLE:true}
spring.mail.properties.mail.smtp.starttls.required=${MAIL_SMTP_STARTTLS_REQUIRED:true}
spring.mail.properties.mail.smtp.ssl.trust=${MAIL_SMTP_SSL_TRUST}
spring.mail.properties.mail.smtp.ssl.protocols=${MAIL_SMTP_SSL_PROTOCOLS:TLSv1.2}

# Timeout Settings (milliseconds)
spring.mail.properties.mail.smtp.connectiontimeout=${MAIL_SMTP_CONNECTION_TIMEOUT:5000}
spring.mail.properties.mail.smtp.timeout=${MAIL_SMTP_TIMEOUT:5000}
spring.mail.properties.mail.smtp.writetimeout=${MAIL_SMTP_WRITE_TIMEOUT:5000}

# Email Sender Information
app.email.from.name=${EMAIL_FROM_NAME:Employee Management System}
app.email.from.address=${EMAIL_FROM_ADDRESS:${spring.mail.username}}

# Application URL (used in email links)
app.url=${APP_URL:http://localhost:4200}
```

### SSL/TLS Configuration

**For SSL (Port 465):**
```properties
MAIL_PORT=465
MAIL_SMTP_STARTTLS_ENABLE=false
spring.mail.properties.mail.smtp.ssl.enable=true
```

**For TLS (Port 587) - Recommended:**
```properties
MAIL_PORT=587
MAIL_SMTP_STARTTLS_ENABLE=true
MAIL_SMTP_STARTTLS_REQUIRED=true
```

## Testing Email Configuration

### 1. Check Email Settings
After starting the application, check the logs:
```
✓ Email configuration loaded: smtp.gmail.com:587
✓ From address: your-email@gmail.com
```

### 2. Test Email Functionality
The system sends emails for:
- ✉️ Email verification (user registration)
- ✉️ Welcome emails (after verification)
- ✉️ Account creation (new employees)
- ✉️ Password reset requests
- ✉️ Document expiry alerts
- ✉️ Organization creation

### 3. Verify Email Delivery
1. Create a test user account
2. Check if verification email arrives
3. Review application logs for any errors

## Troubleshooting

### Common Issues

#### 1. Authentication Failed
**Error:** `535 Authentication failed`

**Solutions:**
- Gmail: Ensure you're using an App Password, not regular password
- Outlook: Check if account is locked or requires additional verification
- Verify username and password are correct

#### 2. Connection Timeout
**Error:** `Connection timed out`

**Solutions:**
- Check firewall allows outbound connections on port 587/465
- Verify MAIL_HOST is correct
- Try different port (587 or 465)

#### 3. SSL/TLS Errors
**Error:** `SSL handshake failed`

**Solutions:**
- Ensure MAIL_SMTP_SSL_TRUST matches MAIL_HOST
- Try different SSL protocols: `TLSv1.2,TLSv1.3`
- Check if STARTTLS is enabled correctly

#### 4. Email Not Received
**Check:**
- Spam/Junk folder
- Email address is valid
- Application logs for send confirmation
- Email provider's sent items

### Debug Mode

Enable detailed email logging:
```properties
logging.level.org.springframework.mail=DEBUG
logging.level.javax.mail=DEBUG
```

## Security Best Practices

1. **Never Commit Passwords**
   - Use environment variables
   - Add `.env` to `.gitignore`
   - Use secrets management in production

2. **Use App Passwords (Gmail)**
   - Don't use regular account password
   - Create dedicated app password

3. **Restrict SMTP Access**
   - Use firewall rules
   - Allow only necessary IP addresses
   - Monitor email sending logs

4. **Rotate Credentials Regularly**
   - Change passwords periodically
   - Update app passwords if compromised

5. **Monitor Email Usage**
   - Track email send rates
   - Set up alerts for failures
   - Review logs regularly

## Production Deployment

### Using Environment Variables
```bash
# Set in your server environment
export MAIL_HOST=smtp.office365.com
export MAIL_PORT=587
export MAIL_USERNAME=noreply@yourcompany.com
export MAIL_PASSWORD=secure-password
export MAIL_SMTP_SSL_TRUST=smtp.office365.com
export EMAIL_FROM_NAME="Your Company EMS"
export APP_URL=https://ems.yourcompany.com

# Start application
java -jar EmployeeManagementSystem.jar
```

### Using Docker
```dockerfile
# Dockerfile
FROM openjdk:17-jdk-slim
COPY target/EmployeeManagementSystem.jar app.jar

ENV MAIL_HOST=smtp.gmail.com
ENV MAIL_PORT=587
# Other environment variables set via docker-compose or runtime

ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Using Kubernetes ConfigMap/Secrets
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: email-credentials
type: Opaque
data:
  mail-username: base64-encoded-email
  mail-password: base64-encoded-password

---
apiVersion: v1
kind: ConfigMap
metadata:
  name: email-config
data:
  MAIL_HOST: smtp.office365.com
  MAIL_PORT: "587"
  EMAIL_FROM_NAME: "Company EMS"
```

## Email Provider Comparison

| Provider | Port | Security | Rate Limit | Notes |
|----------|------|----------|------------|-------|
| Gmail | 587 | TLS | 500/day (free), 2000/day (workspace) | Requires App Password |
| Outlook | 587 | TLS | 300/day (free), 10000/day (Office365) | Simple setup |
| Office365 | 587 | TLS | 10000/day | Best for business |
| Custom SMTP | Varies | Varies | Varies | Full control |

## Support

For email configuration issues:
1. Check application logs
2. Verify email provider settings
3. Test with telnet: `telnet smtp.gmail.com 587`
4. Review this guide's troubleshooting section

---

**Last Updated:** November 7, 2025
**Version:** 2.0

