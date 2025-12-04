# Hostinger SMTP Configuration Guide

This guide explains how to configure the Employee Management System to use Hostinger SMTP for sending emails.

## Hostinger SMTP Settings

Hostinger provides two options for SMTP:

### Option 1: Port 465 (SSL) - Recommended
- **SMTP Host:** `smtp.hostinger.com`
- **Port:** `465`
- **Encryption:** SSL/TLS
- **Authentication:** Required

### Option 2: Port 587 (TLS/STARTTLS)
- **SMTP Host:** `smtp.hostinger.com`
- **Port:** `587`
- **Encryption:** STARTTLS
- **Authentication:** Required

## Configuration Steps

### Step 1: Get Your Hostinger Email Credentials

1. Log in to your Hostinger control panel (hPanel)
2. Navigate to **Email** section
3. Create or select an email account
4. Note down:
   - Email address (e.g., `noreply@yourdomain.com`)
   - Email password

### Step 2: Configure Environment Variables

Edit your `.env` file and add the following configuration:

#### For Port 465 (SSL) - Recommended:

```bash
# Hostinger SMTP Configuration (Port 465 - SSL)
MAIL_HOST=smtp.hostinger.com
MAIL_PORT=465
MAIL_USERNAME=noreply@yourdomain.com
MAIL_PASSWORD=your_email_password
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=false
MAIL_SMTP_STARTTLS_REQUIRED=false
MAIL_SMTP_SSL_ENABLE=true
MAIL_SMTP_SSL_TRUST=smtp.hostinger.com
MAIL_SMTP_SSL_SOCKET_FACTORY_PORT=465
EMAIL_FROM_NAME=Employee Management System
EMAIL_FROM_ADDRESS=noreply@yourdomain.com
```

#### For Port 587 (TLS/STARTTLS):

```bash
# Hostinger SMTP Configuration (Port 587 - TLS)
MAIL_HOST=smtp.hostinger.com
MAIL_PORT=587
MAIL_USERNAME=noreply@yourdomain.com
MAIL_PASSWORD=your_email_password
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=true
MAIL_SMTP_STARTTLS_REQUIRED=true
MAIL_SMTP_SSL_ENABLE=false
MAIL_SMTP_SSL_TRUST=smtp.hostinger.com
EMAIL_FROM_NAME=Employee Management System
EMAIL_FROM_ADDRESS=noreply@yourdomain.com
```

### Step 3: Restart Containers

After updating the `.env` file, restart the backend container:

```bash
docker-compose restart backend
```

Or if using deployment scripts:

```bash
./deploy-backend-frontend.sh
```

## Complete .env Example

Here's a complete `.env` file example with Hostinger SMTP:

```bash
# Database Configuration
DB_ROOT_PASSWORD=your_mysql_root_password
DB_USERNAME=emsuser
DB_PASSWORD=your_db_password
DB_NAME=employee_management_system
DB_PORT=3307

# Backend Configuration
BACKEND_PORT=8080
FRONTEND_PORT=80
FRONTEND_HTTPS_PORT=443

# Hostinger SMTP Configuration
MAIL_HOST=smtp.hostinger.com
MAIL_PORT=465
MAIL_USERNAME=noreply@yourdomain.com
MAIL_PASSWORD=your_email_password
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=false
MAIL_SMTP_STARTTLS_REQUIRED=false
MAIL_SMTP_SSL_ENABLE=true
MAIL_SMTP_SSL_TRUST=smtp.hostinger.com
MAIL_SMTP_SSL_SOCKET_FACTORY_PORT=465
EMAIL_FROM_NAME=Employee Management System
EMAIL_FROM_ADDRESS=noreply@yourdomain.com

# Application URL
APP_URL=https://yourdomain.com

# JWT Secret (change this to a secure random string)
JWT_SECRET=your_secure_jwt_secret_key_at_least_256_bits
```

## Testing Email Configuration

### Test Email Sending

1. Log in to the application as an admin
2. Create a new user account
3. Check if the welcome email is sent
4. Check the backend logs for email sending status:

```bash
docker-compose logs backend | grep -i mail
```

### Common Issues and Solutions

#### Issue 1: Authentication Failed

**Error:** `535 Authentication failed`

**Solution:**
- Verify email username and password are correct
- Ensure you're using the full email address as username
- Check if the email account is active in Hostinger hPanel
- Try resetting the email password in Hostinger

#### Issue 2: Connection Timeout

**Error:** `Connection timeout` or `Could not connect to SMTP host`

**Solution:**
- Check if port 465 or 587 is blocked by firewall
- Verify `smtp.hostinger.com` is accessible from your server
- Try switching between port 465 and 587
- Check Hostinger server status

#### Issue 3: SSL/TLS Handshake Failed

**Error:** `SSL handshake failed` or `TLS handshake failed`

**Solution:**
- For port 465: Ensure `MAIL_SMTP_SSL_ENABLE=true` and `MAIL_SMTP_STARTTLS_ENABLE=false`
- For port 587: Ensure `MAIL_SMTP_SSL_ENABLE=false` and `MAIL_SMTP_STARTTLS_ENABLE=true`
- Verify `MAIL_SMTP_SSL_TRUST=smtp.hostinger.com`

#### Issue 4: Emails Not Received

**Solution:**
- Check spam/junk folder
- Verify recipient email address is correct
- Check backend logs for email sending errors
- Verify `EMAIL_FROM_ADDRESS` matches your Hostinger email
- Check Hostinger email account for any restrictions

## Port Comparison

| Port | Encryption | Configuration | Use Case |
|------|------------|---------------|----------|
| 465 | SSL/TLS | `MAIL_SMTP_SSL_ENABLE=true`<br>`MAIL_SMTP_STARTTLS_ENABLE=false` | Recommended, more secure |
| 587 | STARTTLS | `MAIL_SMTP_SSL_ENABLE=false`<br>`MAIL_SMTP_STARTTLS_ENABLE=true` | Alternative, works with most firewalls |

## Security Best Practices

1. **Use Strong Password:** Use a strong, unique password for your email account
2. **Don't Commit .env:** Never commit `.env` file to version control
3. **Use Port 465:** Prefer port 465 (SSL) for better security
4. **Regular Updates:** Keep your email password updated regularly
5. **Monitor Logs:** Regularly check email sending logs for issues

## Troubleshooting Commands

### Check Email Configuration

```bash
# View current email configuration
docker-compose exec backend env | grep MAIL

# Check backend logs for email errors
docker-compose logs backend | grep -i "mail\|smtp\|email"

# Test SMTP connection (if telnet is available)
telnet smtp.hostinger.com 465
# or
telnet smtp.hostinger.com 587
```

### Verify Environment Variables

```bash
# Check if .env file is loaded correctly
docker-compose config | grep MAIL
```

## Additional Resources

- [Hostinger Email Setup Guide](https://www.hostinger.com/tutorials/how-to-set-up-email)
- [Hostinger SMTP Settings](https://www.hostinger.com/tutorials/how-to-use-smtp)
- [Spring Mail Documentation](https://docs.spring.io/spring-framework/reference/integration/email.html)

## Support

If you continue to experience issues:

1. Check Hostinger server status
2. Verify email account is active in hPanel
3. Review backend application logs
4. Test with a different email provider to isolate the issue
5. Contact Hostinger support for SMTP-specific issues

