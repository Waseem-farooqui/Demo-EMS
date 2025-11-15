# Environment Variables Setup Guide

## Overview

All sensitive configuration and environment-specific values are now managed through `.env` file. This ensures:
- ✅ No hardcoded credentials in source code
- ✅ Easy configuration for different environments
- ✅ Secure deployment practices

## Quick Start

1. **Copy the example file:**
   ```bash
   cp .env.example .env
   ```

2. **Edit `.env` with your values:**
   ```bash
   nano .env  # or use your preferred editor
   ```

3. **Set all required variables** (see sections below)

4. **Deploy:**
   ```bash
   docker-compose up -d --build
   ```

## Required Variables

### Database Configuration
```bash
DB_NAME=employee_management_system
DB_USERNAME=emsuser
DB_PASSWORD=your_strong_password_here
DB_ROOT_PASSWORD=your_strong_root_password_here
DB_PORT=3307

# Optional: Override database URL (default uses 'mysql' container name for Docker)
# For Docker: Leave unset (uses 'mysql' container name automatically)
# For non-Docker: Set to jdbc:mysql://localhost:3306/employee_management_system?...
SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/employee_management_system?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true
```

### JWT Configuration
```bash
# Generate with: openssl rand -base64 64
JWT_SECRET=your_256_bit_random_string_here
JWT_EXPIRATION=86400000
```

### Email Configuration
```bash
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_specific_password
EMAIL_FROM_ADDRESS=your_email@gmail.com
```

### Application URLs
```bash
APP_URL=https://yourdomain.com
FRONTEND_URL=https://yourdomain.com
BACKEND_URL=https://yourdomain.com:8080
API_URL=https://yourdomain.com:8080/api
CORS_ALLOWED_ORIGINS=https://yourdomain.com
```

## How It Works

### Backend Configuration

The backend reads from `.env` via `compose.yaml`:
- Environment variables are passed to the container
- Spring Boot reads from `application-prod.properties`
- Properties file uses `${VAR_NAME:default}` syntax
- Environment variables override defaults

**Example:**
```properties
# application-prod.properties
jwt.secret=${JWT_SECRET:CHANGE_THIS}
```

If `JWT_SECRET` is set in `.env`, it's used. Otherwise, the default is used.

### Frontend Configuration

The frontend uses build-time replacement:
1. `environment.prod.ts` has placeholders: `${API_URL}`
2. Docker build replaces placeholders with actual values
3. Build args come from `.env` via `compose.yaml`

**Example:**
```typescript
// environment.prod.ts (source)
apiUrl: '${API_URL}'

// After build (in container)
apiUrl: 'https://yourdomain.com:8080/api'
```

## Variable Reference

### Database
| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `DB_NAME` | Database name | `employee_management_system` | No |
| `DB_USERNAME` | Database user | `emsuser` | No |
| `DB_PASSWORD` | Database password | - | **Yes** |
| `DB_ROOT_PASSWORD` | MySQL root password | - | **Yes** |
| `DB_PORT` | Database port | `3307` | No |
| `SPRING_DATASOURCE_URL` | Full JDBC URL | `jdbc:mysql://mysql:3306/...` (uses 'mysql' container) | No |
| `SPRING_DATASOURCE_USERNAME` | Override datasource username | Uses `DB_USERNAME` | No |
| `SPRING_DATASOURCE_PASSWORD` | Override datasource password | Uses `DB_PASSWORD` | No |

### Security
| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `JWT_SECRET` | JWT signing key (256+ bits) | - | **Yes** |
| `JWT_EXPIRATION` | Token expiration (ms) | `86400000` | No |

### Email
| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `MAIL_HOST` | SMTP server | `smtp.gmail.com` | No |
| `MAIL_PORT` | SMTP port | `587` | No |
| `MAIL_USERNAME` | Email username | - | **Yes** (if using email) |
| `MAIL_PASSWORD` | Email password/app password | - | **Yes** (if using email) |
| `EMAIL_FROM_ADDRESS` | Sender email | `${MAIL_USERNAME}` | No |

### Application URLs
| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `APP_URL` | Base application URL | `http://localhost` | **Yes** |
| `FRONTEND_URL` | Frontend URL | `http://localhost` | **Yes** |
| `API_URL` | Full API URL | `http://localhost:8080/api` | **Yes** |
| `API_BASE_URL` | API base URL | `http://localhost:8080` | **Yes** |
| `BACKEND_URL` | Backend URL | `http://localhost:8080` | **Yes** |
| `CORS_ALLOWED_ORIGINS` | Allowed CORS origins | `http://localhost` | **Yes** |

### OCR (Optional)
| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `OCR_API_KEY` | OCR.space API key | - | No |
| `OCR_CLOUD_ENABLED` | Enable cloud OCR | `false` | No |
| `OCR_LOCAL_ENABLED` | Enable local OCR | `true` | No |
| `TESSERACT_DATA_PATH` | Tesseract data path | `/usr/share/tesseract-ocr/4.00/tessdata` | No |

### Ports
| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `BACKEND_PORT` | Backend port | `8080` | No |
| `FRONTEND_PORT` | Frontend port | `80` | No |

## Security Best Practices

1. **Never commit `.env` to Git**
   - Already in `.gitignore`
   - Only commit `.env.example`

2. **Use strong passwords**
   - Database: 32+ characters
   - JWT Secret: 64+ characters (256+ bits)

3. **Generate secure values:**
   ```bash
   # JWT Secret
   openssl rand -base64 64
   
   # Database password
   openssl rand -base64 32
   ```

4. **Use app-specific passwords for email**
   - Gmail: https://myaccount.google.com/apppasswords
   - Never use your account password

5. **Restrict file permissions:**
   ```bash
   chmod 600 .env
   ```

## Troubleshooting

### Variables Not Being Read

1. **Check `.env` file exists:**
   ```bash
   ls -la .env
   ```

2. **Verify Docker Compose reads it:**
   ```bash
   docker-compose config | grep -i "your_variable"
   ```

3. **Check container environment:**
   ```bash
   docker-compose exec backend env | grep -i "your_variable"
   ```

### Frontend URLs Not Updating

1. **Rebuild frontend:**
   ```bash
   docker-compose build --no-cache frontend
   docker-compose up -d frontend
   ```

2. **Verify build args:**
   ```bash
   docker-compose config | grep -A 5 "frontend:"
   ```

3. **Check built files:**
   ```bash
   docker-compose exec frontend cat /usr/share/nginx/html/main*.js | grep -i "your_url"
   ```

### Backend Not Using Variables

1. **Check Spring profile:**
   ```bash
   docker-compose logs backend | grep -i "profile"
   ```

2. **Verify environment variables:**
   ```bash
   docker-compose exec backend env | grep -i "spring\|jwt\|mail"
   ```

3. **Check application properties:**
   ```bash
   docker-compose exec backend cat /app/application-prod.properties
   ```

## Example .env File

```bash
# Database
DB_NAME=employee_management_system
DB_USERNAME=emsuser
DB_PASSWORD=MyStr0ng!P@ssw0rd123
DB_ROOT_PASSWORD=MyR00t!P@ssw0rd456
DB_PORT=3307

# JWT
JWT_SECRET=abc123def456ghi789jkl012mno345pqr678stu901vwx234yz567abc890def123ghi456jkl789mno012pqr345stu678
JWT_EXPIRATION=86400000

# Email
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=myapp@gmail.com
MAIL_PASSWORD=abcd efgh ijkl mnop
EMAIL_FROM_ADDRESS=myapp@gmail.com

# URLs
APP_URL=https://ems.example.com
FRONTEND_URL=https://ems.example.com
BACKEND_URL=https://ems.example.com:8080
API_URL=https://ems.example.com:8080/api
API_BASE_URL=https://ems.example.com:8080
CORS_ALLOWED_ORIGINS=https://ems.example.com,https://www.ems.example.com

# Ports
BACKEND_PORT=8080
FRONTEND_PORT=80

# OCR (Optional)
OCR_API_KEY=your_key_here
OCR_CLOUD_ENABLED=false
OCR_LOCAL_ENABLED=true
```

## Migration from Hardcoded Values

If you have existing hardcoded values:

1. **Backend:** Already migrated - uses environment variables
2. **Frontend:** 
   - Old: Hardcoded IPs in `environment.prod.ts`
   - New: Placeholders replaced at build time
   - Action: Rebuild frontend after setting `.env`

3. **OCR API Key:**
   - Old: Hardcoded in `application.properties`
   - New: Uses `${OCR_API_KEY}` from `.env`
   - Action: Add `OCR_API_KEY` to `.env`

## Next Steps

1. ✅ Create `.env` from `.env.example`
2. ✅ Fill in all required values
3. ✅ Set proper file permissions: `chmod 600 .env`
4. ✅ Rebuild containers: `docker-compose build`
5. ✅ Start services: `docker-compose up -d`
6. ✅ Verify configuration: Check logs and test application

---

**Remember:** Never commit `.env` to version control! Always use `.env.example` as a template.

