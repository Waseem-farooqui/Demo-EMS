# Environment Files Setup Guide

This project uses three environment files for different deployment scenarios.

## Files Overview

1. **`local.env`** - For local development on your machine
2. **`prod.env`** - Production template (safe to commit)
3. **`.env`** - Production actual file (gitignored, contains sensitive data)

## Local Development Setup

For local development, use `local.env`:

```bash
# Start containers with local environment
docker compose --env-file local.env up -d

# Or set it as default
cp local.env .env
docker compose up -d
```

**Note:** You'll need to update `local.env` with your local email credentials if you want to test email functionality.

## Production Setup

### Step 1: Copy the template
On your production server:
```bash
cp prod.env .env
```

### Step 2: Edit .env with your actual values
```bash
nano .env  # or use your preferred editor
```

**IMPORTANT:** Update all values marked with `CHANGE_THIS`:
- `DB_PASSWORD` - Secure database password
- `DB_ROOT_PASSWORD` - Secure MySQL root password
- `JWT_SECRET` - Generate with: `openssl rand -base64 64`
- `MAIL_PASSWORD` - Your email account password/app password
- Any other values specific to your setup

### Step 3: Verify .env is gitignored
```bash
# Check if .env is ignored (should show nothing)
git status .env

# If it shows up, it's not ignored - check .gitignore
```

### Step 4: Start services
```bash
docker compose up -d
```

## Environment Variables Reference

### Database
- `DB_NAME` - Database name
- `DB_USERNAME` - Database user
- `DB_PASSWORD` - Database password (CHANGE IN PRODUCTION!)
- `DB_ROOT_PASSWORD` - MySQL root password (CHANGE IN PRODUCTION!)
- `DB_PORT` - External database port (default: 3307)

### Security
- `JWT_SECRET` - Secret key for JWT tokens (MUST be at least 256 bits)
- `JWT_EXPIRATION` - Token expiration in milliseconds (default: 86400000 = 24 hours)

### Email
- `MAIL_HOST` - SMTP server hostname
- `MAIL_PORT` - SMTP port (465 for SSL, 587 for TLS)
- `MAIL_USERNAME` - Email username
- `MAIL_PASSWORD` - Email password/app password
- `EMAIL_FROM_ADDRESS` - Sender email address

### Application
- `APP_URL` - Base URL of your application
- `CORS_ALLOWED_ORIGINS` - Comma-separated list of allowed origins
- `SPRING_PROFILES_ACTIVE` - Spring profile (dev/prod)

## Security Best Practices

1. **Never commit `.env` to version control** - It's already in `.gitignore`
2. **Use strong passwords** - Generate secure random passwords
3. **Rotate secrets regularly** - Especially JWT_SECRET in production
4. **Limit file permissions** - On production: `chmod 600 .env`
5. **Use different credentials** - Never reuse passwords between environments

## Troubleshooting

### Docker Compose not reading .env
- Make sure `.env` is in the project root (same directory as `compose.yaml`)
- Check file permissions: `ls -la .env`
- Verify syntax: No spaces around `=` sign

### CORS errors
- Check `CORS_ALLOWED_ORIGINS` includes your domain
- Include both HTTP and HTTPS if using both
- Include `www` subdomain if using it

### Database connection issues
- Verify `DB_PASSWORD` matches MySQL container password
- Check `DB_PORT` is not conflicting with other services
- Ensure MySQL container is running: `docker compose ps mysql`

