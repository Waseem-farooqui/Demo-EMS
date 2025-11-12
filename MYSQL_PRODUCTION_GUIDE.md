# MySQL Configuration Guide - Employee Management System

## ✅ MySQL Support Status

The Employee Management System **FULLY SUPPORTS MySQL 8.0+** for production deployment.

---

## 🔧 Configuration

### 1. Docker Deployment (Automatic)

When using Docker, MySQL is automatically configured:

```bash
docker compose up -d
```

MySQL 8.0 container starts with:
- Database: `employee_management_system`
- User: `emsuser` (configurable via .env)
- Password: Set in .env file
- Port: 3307 (mapped to host)
- Encoding: UTF-8 MB4
- Persistent storage via Docker volume

### 2. Manual MySQL Setup

If running without Docker:

```sql
-- 1. Create database
CREATE DATABASE employee_management_system
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 2. Create user
CREATE USER 'emsuser'@'localhost' IDENTIFIED BY 'your_secure_password';

-- 3. Grant privileges
GRANT ALL PRIVILEGES ON employee_management_system.* TO 'emsuser'@'localhost';
FLUSH PRIVILEGES;
```

### 3. Application Configuration

**For Production (application-prod.properties):**
```properties
# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/employee_management_system?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=emsuser
spring.datasource.password=your_secure_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA Configuration
spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect
spring.jpa.hibernate.ddl-auto=update
```

**Using Environment Variables:**
```bash
export SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/employee_management_system?useSSL=false&serverTimezone=UTC"
export SPRING_DATASOURCE_USERNAME="emsuser"
export SPRING_DATASOURCE_PASSWORD="your_secure_password"
```

---

## 🐳 Docker MySQL Configuration

The docker-compose.yaml includes MySQL 8.0 with:

```yaml
mysql:
  image: mysql:8.0
  environment:
    MYSQL_DATABASE: employee_management_system
    MYSQL_USER: emsuser
    MYSQL_PASSWORD: ${DB_PASSWORD}
    MYSQL_ROOT_PASSWORD: ${DB_ROOT_PASSWORD}
  volumes:
    - mysql_data:/var/lib/mysql  # Persistent storage
  healthcheck:
    test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
    interval: 10s
    timeout: 5s
    retries: 5
```

**Environment Variables (.env):**
```env
DB_NAME=employee_management_system
DB_USERNAME=emsuser
DB_PASSWORD=YourSecurePassword123!
DB_ROOT_PASSWORD=YourRootPassword456!
DB_PORT=3307
```

---

## 📊 Schema Management

### Auto-Schema Creation

The application uses Hibernate with `ddl-auto=update`, which:
- ✅ Automatically creates tables on first run
- ✅ Updates schema when entities change
- ✅ Preserves existing data
- ✅ No manual SQL scripts needed

### Initial Database State

On first startup, Hibernate creates:
- Users table with roles
- Organizations table
- Employees table with org isolation
- Departments table
- Documents table
- Leaves table with balance tracking
- Attendance records
- Rota schedules
- Notifications
- Alert configurations
- And all relationships

---

## 🔄 Migration from H2 to MySQL

### Development (H2)
```properties
spring.datasource.url=jdbc:h2:mem:employeedb
spring.jpa.hibernate.ddl-auto=update
```

### Production (MySQL)
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/employee_management_system
spring.jpa.hibernate.ddl-auto=update
```

**No data migration needed** - Start fresh in production or export/import if needed.

---

## 🛠️ Common Operations

### Connect to MySQL (Docker)
```bash
# Using docker compose
docker compose exec mysql mysql -uemsuser -p employee_management_system

# Direct connection
mysql -h localhost -P 3307 -uemsuser -p employee_management_system
```

### Backup Database
```bash
# Docker
docker compose exec -T mysql mysqldump -uemsuser -p${DB_PASSWORD} employee_management_system > backup.sql

# Manual
mysqldump -uemsuser -p employee_management_system > backup.sql
```

### Restore Database
```bash
# Docker
docker compose exec -T mysql mysql -uemsuser -p${DB_PASSWORD} employee_management_system < backup.sql

# Manual
mysql -uemsuser -p employee_management_system < backup.sql
```

### View Database Size
```sql
SELECT 
    table_schema AS 'Database',
    ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) AS 'Size (MB)'
FROM information_schema.tables
WHERE table_schema = 'employee_management_system'
GROUP BY table_schema;
```

### Reset Database
```sql
DROP DATABASE employee_management_system;
CREATE DATABASE employee_management_system
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
```

---

## 🔍 Troubleshooting

### Connection Refused
```bash
# Check MySQL is running
docker compose ps mysql

# Check logs
docker compose logs mysql

# Wait for health check
docker compose ps  # Look for "healthy" status
```

### Authentication Failed
```bash
# Verify credentials in .env
cat .env | grep DB_

# Reset password
docker compose exec mysql mysql -uroot -p${DB_ROOT_PASSWORD}
ALTER USER 'emsuser'@'%' IDENTIFIED BY 'new_password';
FLUSH PRIVILEGES;
```

### Character Encoding Issues
```sql
-- Verify encoding
SHOW VARIABLES LIKE 'character_set%';
SHOW VARIABLES LIKE 'collation%';

-- Should show utf8mb4 for all
```

### Performance Issues
```sql
-- Check slow queries
SHOW FULL PROCESSLIST;

-- Analyze table
ANALYZE TABLE employees;
ANALYZE TABLE documents;
```

---

## 📈 Performance Optimization

### Connection Pooling (Already Configured)
```properties
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.max-lifetime=1200000
```

### Indexes
Hibernate automatically creates indexes for:
- Primary keys
- Foreign keys
- Unique constraints

### Query Optimization
- Use pagination for large lists
- Implement caching where appropriate
- Monitor slow query log

---

## 🔐 Security Best Practices

### Database User
```sql
-- Create limited user (not root)
CREATE USER 'emsuser'@'localhost' IDENTIFIED BY 'strong_password_here';
GRANT SELECT, INSERT, UPDATE, DELETE ON employee_management_system.* TO 'emsuser'@'localhost';

-- No SUPER, CREATE USER, or other admin privileges
```

### Connection Security
```properties
# For production with SSL
spring.datasource.url=jdbc:mysql://localhost:3306/employee_management_system?useSSL=true&requireSSL=true

# SSL certificates
spring.datasource.hikari.data-source-properties.useSSL=true
spring.datasource.hikari.data-source-properties.requireSSL=true
```

### Regular Backups
```bash
# Automated daily backup
0 2 * * * /path/to/backup-script.sh
```

---

## 📋 MySQL vs H2 Comparison

| Feature | H2 (Development) | MySQL (Production) |
|---------|------------------|-------------------|
| Use Case | Development/Testing | Production |
| Persistence | In-memory | Disk-based |
| Performance | Fast (in-memory) | Optimized for production |
| Scalability | Limited | High |
| Backup | Not needed | Required |
| Multi-user | Single user | Multiple connections |
| Configuration | None | Required |

---

## ✅ Verification Checklist

After setup, verify:

- [ ] MySQL container is running and healthy
- [ ] Can connect with credentials
- [ ] Database is created with correct encoding
- [ ] Backend connects successfully
- [ ] Health endpoint returns UP
- [ ] Tables are auto-created
- [ ] Data persists after container restart
- [ ] Backups work correctly

---

## 🎉 Conclusion

**MySQL 8.0 is fully configured and ready for production!**

Features:
- ✅ Full MySQL 8.0 support
- ✅ Automatic schema management
- ✅ Docker integration
- ✅ UTF-8 MB4 encoding
- ✅ Connection pooling
- ✅ Health checks
- ✅ Persistent storage
- ✅ Backup/restore procedures

**No additional configuration needed for basic deployment!**

---

For more information:
- Docker Deployment: `DOCKER_DEPLOYMENT.md`
- Production Ready: `PRODUCTION_READY.md`
- Quick Reference: `DOCKER_QUICK_REFERENCE.md`

