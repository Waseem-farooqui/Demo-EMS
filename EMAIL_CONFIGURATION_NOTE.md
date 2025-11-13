# ⚠️ Email Configuration Required for Production

## Issue Fixed
The application was failing to start because `MAIL_USERNAME` and `MAIL_PASSWORD` environment variables were not set.

## Solution Applied
Added default empty values to allow the application to start without email configuration:

```properties
spring.mail.username=${MAIL_USERNAME:}
spring.mail.password=${MAIL_PASSWORD:}
```

## Development Mode
The application will now start successfully even without email configuration. Email-related features will fail gracefully with error logs when credentials are not provided.

## Production Deployment

### ⚠️ CRITICAL: Email Configuration Required

For production, you **MUST** set these environment variables:

```bash
# Required for email functionality
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-specific-password
```

### How to Set Environment Variables

#### Option 1: Using .env file (Docker)
```bash
# .env file
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
```

#### Option 2: Using Docker Compose
```yaml
# docker-compose.yml
environment:
  MAIL_USERNAME: your-email@gmail.com
  MAIL_PASSWORD: your-app-password
```

#### Option 3: System Environment Variables
```bash
# Linux/Mac
export MAIL_USERNAME=your-email@gmail.com
export MAIL_PASSWORD=your-app-password

# Windows
set MAIL_USERNAME=your-email@gmail.com
set MAIL_PASSWORD=your-app-password
```

## Gmail Setup

1. **Enable 2-Factor Authentication** on your Gmail account
2. **Generate App Password**:
   - Go to: https://myaccount.google.com/apppasswords
   - Select "Mail" and your device
   - Copy the 16-character password
3. **Use App Password** (NOT your Gmail password)

## Email Features Affected

Without email configuration, these features will log errors but won't crash the application:

- ✉️ Welcome emails after email verification
- ✉️ Password reset emails
- ✉️ Username reminder emails
- ✉️ Account creation emails
- ✉️ Organization creation emails
- ✉️ Document expiry alert emails

In-app notifications will still work normally.

## Testing Email Configuration

After setting credentials, test with:

```bash
# Start application
mvn spring-boot:run

# Check logs for email errors
tail -f logs/application.log | grep -i mail

# Test by creating a user or triggering password reset
```

## Security Note

✅ **Credentials are NOT hardcoded** - They must be provided via environment variables
✅ **Default values are empty** - Safe for version control
✅ **Production requires configuration** - Ensures security

## Quick Start Checklist

- [ ] Application starts successfully (even without email)
- [ ] Set MAIL_USERNAME in production
- [ ] Set MAIL_PASSWORD in production  
- [ ] Test email functionality
- [ ] Monitor logs for email errors

---

**Status:** Application now starts without email configuration. Configure email for production use.

