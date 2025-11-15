# Database URL Configuration Guide

## Overview

The database connection URL is now fully configurable via environment variables, with a Docker-friendly default that uses the MySQL container name.

## Default Behavior (Docker)

**When running with Docker Compose (default):**
- Database URL automatically uses `mysql` as the hostname (container service name)
- No configuration needed in `.env` file
- Works out of the box with Docker networking

**Default URL:**
```
jdbc:mysql://mysql:3306/employee_management_system?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true
```

## Configuration Options

### Option 1: Use Default (Recommended for Docker)

**Leave `SPRING_DATASOURCE_URL` unset in `.env`**

The system will automatically use:
- Host: `mysql` (Docker service name)
- Port: `3306` (internal container port)
- Database: `${DB_NAME}` or `employee_management_system`

**In `compose.yaml`:**
```yaml
SPRING_DATASOURCE_URL: ${SPRING_DATASOURCE_URL:jdbc:mysql://mysql:3306/${DB_NAME:-employee_management_system}?...}
```

**In `application-prod.properties`:**
```properties
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:mysql://mysql:3306/${DB_NAME:employee_management_system}?...}
```

### Option 2: Override for Non-Docker Deployment

**If running backend outside Docker or connecting to external database:**

Add to `.env`:
```bash
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/employee_management_system?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
```

Or for remote database:
```bash
SPRING_DATASOURCE_URL=jdbc:mysql://your-db-host:3306/employee_management_system?useSSL=true&serverTimezone=UTC
```

### Option 3: Custom Database Host/Port

**If using different database configuration:**

Add to `.env`:
```bash
# Custom database host
SPRING_DATASOURCE_URL=jdbc:mysql://custom-host:3307/employee_management_system?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true

# Or use separate variables
DB_HOST=custom-host
DB_PORT=3307
# Then construct URL in compose.yaml or application-prod.properties
```

## How It Works

### Configuration Flow

1. **`.env` file** (optional)
   ```bash
   SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/employee_management_system?...
   ```

2. **`compose.yaml`** reads from `.env`
   ```yaml
   environment:
     SPRING_DATASOURCE_URL: ${SPRING_DATASOURCE_URL:jdbc:mysql://mysql:3306/...}
   ```

3. **`application-prod.properties`** uses environment variable
   ```properties
   spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:mysql://mysql:3306/...}
   ```

4. **Spring Boot** reads the property and connects to database

### Priority Order

1. **Environment variable** `SPRING_DATASOURCE_URL` (highest priority)
2. **Default in compose.yaml** (if env var not set)
3. **Default in application-prod.properties** (fallback)

## Examples

### Docker Deployment (Default)
```bash
# .env file - No SPRING_DATASOURCE_URL needed
DB_NAME=employee_management_system
DB_USERNAME=emsuser
DB_PASSWORD=mypassword
```

**Result:** Uses `jdbc:mysql://mysql:3306/employee_management_system?...`

### Local Development (Non-Docker)
```bash
# .env file
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/employee_management_system?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
DB_USERNAME=root
DB_PASSWORD=mypassword
```

**Result:** Uses `localhost` instead of `mysql` container name

### Remote Database
```bash
# .env file
SPRING_DATASOURCE_URL=jdbc:mysql://db.example.com:3306/employee_management_system?useSSL=true&serverTimezone=UTC
DB_USERNAME=remote_user
DB_PASSWORD=remote_password
```

**Result:** Connects to remote database server

## Verification

### Check Current Configuration

```bash
# Check environment variable in container
docker-compose exec backend env | grep SPRING_DATASOURCE_URL

# Check Spring Boot property
docker-compose logs backend | grep -i "datasource.url"

# Test database connection
docker-compose exec backend curl http://localhost:8080/api/actuator/health
```

### Verify Database Connection

```bash
# Check if backend can connect to database
docker-compose logs backend | grep -i "database\|mysql\|connection"

# Should see: "HikariPool-1 - Starting..." and "HikariPool-1 - Start completed"
```

## Troubleshooting

### Connection Refused

**Problem:** Backend can't connect to database

**Solutions:**
1. **Check container name:**
   ```bash
   docker-compose ps mysql
   # Should show 'ems-mysql' container running
   ```

2. **Verify network:**
   ```bash
   docker-compose exec backend ping mysql
   # Should resolve to MySQL container IP
   ```

3. **Check database is ready:**
   ```bash
   docker-compose logs mysql | grep -i "ready\|started"
   ```

### Wrong Hostname

**Problem:** Using `localhost` instead of `mysql`

**Solution:** Don't set `SPRING_DATASOURCE_URL` in `.env`, or set it to use `mysql`:
```bash
SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/employee_management_system?...
```

### External Database

**Problem:** Need to connect to database outside Docker network

**Solution:** Set full URL in `.env`:
```bash
SPRING_DATASOURCE_URL=jdbc:mysql://your-external-host:3306/employee_management_system?useSSL=true&serverTimezone=UTC
```

## Best Practices

1. **For Docker deployments:** Don't set `SPRING_DATASOURCE_URL` - use default
2. **For non-Docker:** Always set `SPRING_DATASOURCE_URL` with `localhost`
3. **For production:** Use SSL in connection string: `useSSL=true`
4. **For security:** Never hardcode database URLs in source code
5. **For flexibility:** Use environment variables for all database configuration

## Summary

✅ **Default:** Uses `mysql` container name (Docker-friendly)  
✅ **Configurable:** Can override via `SPRING_DATASOURCE_URL` in `.env`  
✅ **Flexible:** Works for Docker, local, and remote databases  
✅ **Secure:** No hardcoded credentials or URLs

---

**The database URL now defaults to using the `mysql` container name, making Docker deployments work out of the box!** 🎉

