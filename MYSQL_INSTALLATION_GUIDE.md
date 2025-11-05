# MySQL Installation and Configuration Guide

## Step 1: Download MySQL Server and Workbench

### Option A: MySQL Installer (Recommended - Includes Everything)

1. **Download MySQL Installer:**
   - Go to: https://dev.mysql.com/downloads/installer/
   - Download: **mysql-installer-community-8.0.x.x.msi** (Web installer or Full)
   - Choose the **Full** package (includes Server + Workbench + tools)

2. **Run the Installer:**
   - Double-click the downloaded `.msi` file
   - Choose **Developer Default** or **Custom** setup type
   - Select components:
     - ✅ MySQL Server 8.0.x
     - ✅ MySQL Workbench 8.0.x
     - ✅ MySQL Shell (optional)
     - ✅ Connector/J (Java connector - important!)

3. **Configure MySQL Server:**
   - **Port:** Leave default `3306`
   - **Root Password:** Set a strong password (e.g., `root123` for development)
   - **Authentication:** Use "Strong Password Encryption"
   - **Windows Service:** Check "Start MySQL Server at System Startup"
   - **Service Name:** `MySQL80` (default)

4. **Complete Installation:**
   - Click "Execute" to install all components
   - Wait for installation to complete
   - Test connection in the installer

### Option B: Manual Download (If installer doesn't work)

**MySQL Server:**
- URL: https://dev.mysql.com/downloads/mysql/
- Download: Windows (x86, 64-bit), ZIP Archive
- Extract and run `bin/mysqld.exe --initialize`

**MySQL Workbench:**
- URL: https://dev.mysql.com/downloads/workbench/
- Download: Windows (x86, 64-bit), MSI Installer
- Install normally

---

## Step 2: Verify MySQL Installation

### Check MySQL Service

Open Command Prompt as Administrator:

```cmd
sc query MySQL80
```

**Expected Output:**
```
SERVICE_NAME: MySQL80
STATE: 4 RUNNING
```

### Check MySQL Port

```cmd
netstat -ano | findstr :3306
```

**Expected Output:**
```
TCP    0.0.0.0:3306           0.0.0.0:0              LISTENING       xxxx
```

### Test MySQL Connection

```cmd
mysql -u root -p
```

Enter your root password. If successful, you'll see:
```
Welcome to the MySQL monitor...
mysql>
```

Type `exit` to quit.

---

## Step 3: Create Database for Your Application

### Using MySQL Command Line

```cmd
mysql -u root -p
```

Then run these commands:

```sql
-- Create database
CREATE DATABASE employeedb;

-- Create user (optional, for security)
CREATE USER 'empuser'@'localhost' IDENTIFIED BY 'emppass123';

-- Grant privileges
GRANT ALL PRIVILEGES ON employeedb.* TO 'empuser'@'localhost';

-- Apply changes
FLUSH PRIVILEGES;

-- Verify database
SHOW DATABASES;

-- Use the database
USE employeedb;

-- Exit
EXIT;
```

### Using MySQL Workbench (Easier)

1. **Open MySQL Workbench**
2. **Connect to MySQL:**
   - Click on "MySQL Connections" → "Local instance MySQL80"
   - Enter root password
   - Click "OK"

3. **Create Database:**
   - Click "Create a new schema" button (cylinder icon)
   - Schema Name: `employeedb`
   - Charset: `utf8mb4`
   - Collation: `utf8mb4_unicode_ci`
   - Click "Apply"

4. **Create User (Optional):**
   - Go to "Server" → "Users and Privileges"
   - Click "Add Account"
   - Login Name: `empuser`
   - Password: `emppass123`
   - Click "Schema Privileges" tab
   - Click "Add Entry"
   - Select: `employeedb`
   - Click "Select ALL" for privileges
   - Click "Apply"

---

## Step 4: Update Application Configuration

The application configuration has already been updated. Here's what changed:

### application.properties

**Changed from H2 to MySQL:**

```properties
# MySQL Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/employeedb?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=root123
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA Configuration
spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

### pom.xml

**MySQL Connector dependency added** (H2 removed from runtime scope)

---

## Step 5: Start Your Application

1. **Reload Maven Project** in IntelliJ IDEA
2. **Run the application**
3. **Check console** - you should see:
   ```
   HikariPool-1 - Starting...
   HikariPool-1 - Added connection com.mysql.cj.jdbc.ConnectionImpl@...
   ```

4. **Verify tables created:**
   ```sql
   USE employeedb;
   SHOW TABLES;
   ```
   
   You should see: `employees`, `users`, `user_roles`

---

## Step 6: Test the Application

### Using MySQL Workbench

```sql
USE employeedb;

-- View all tables
SHOW TABLES;

-- Check employees table structure
DESCRIBE employees;

-- Check users table structure  
DESCRIBE users;

-- View data (after registering users and adding employees)
SELECT * FROM employees;
SELECT * FROM users;
SELECT * FROM user_roles;
```

### Using Application

1. **Start frontend:** `cd frontend && npm start`
2. **Open browser:** http://localhost:4200
3. **Register a user**
4. **Login**
5. **Add employees**
6. **Check MySQL Workbench** - data should be persisted!

---

## Troubleshooting

### Issue: "Access denied for user 'root'@'localhost'"

**Solution:**
1. Reset MySQL root password:
   ```cmd
   mysql -u root --skip-password
   ALTER USER 'root'@'localhost' IDENTIFIED BY 'root123';
   FLUSH PRIVILEGES;
   EXIT;
   ```

2. Update `application.properties` with correct password

### Issue: "Communications link failure"

**Solution:**
1. Check MySQL service is running:
   ```cmd
   sc query MySQL80
   ```

2. If not running:
   ```cmd
   net start MySQL80
   ```

3. Verify port 3306 is listening:
   ```cmd
   netstat -ano | findstr :3306
   ```

### Issue: "Public Key Retrieval is not allowed"

**Solution:**
Add to JDBC URL: `allowPublicKeyRetrieval=true`

Already included in the updated configuration.

### Issue: "Unknown database 'employeedb'"

**Solution:**
Create the database:
```sql
mysql -u root -p
CREATE DATABASE employeedb;
EXIT;
```

### Issue: "Driver class not found"

**Solution:**
1. Reload Maven project to download MySQL connector
2. Verify `mysql-connector-j` is in dependencies
3. Clean and rebuild project

---

## Comparison: H2 vs MySQL

### H2 (Before)
- ✅ In-memory, fast for development
- ✅ No installation needed
- ✅ H2 Console built-in
- ❌ Data lost on restart
- ❌ Not for production

### MySQL (Now)
- ✅ Persistent data storage
- ✅ Production-ready
- ✅ Better performance for large datasets
- ✅ Industry standard
- ✅ Advanced features (replication, clustering)
- ❌ Requires installation
- ❌ Needs separate database server

---

## MySQL Workbench Tips

### Useful Queries

**Check all employees:**
```sql
SELECT * FROM employees ORDER BY id DESC;
```

**Count employees:**
```sql
SELECT COUNT(*) as total_employees FROM employees;
```

**Find employee by email:**
```sql
SELECT * FROM employees WHERE work_email = 'john@example.com';
```

**Check users and roles:**
```sql
SELECT u.id, u.username, u.email, r.role 
FROM users u 
LEFT JOIN user_roles r ON u.id = r.user_id;
```

**Delete all data (be careful!):**
```sql
DELETE FROM employees;
DELETE FROM user_roles;
DELETE FROM users;
```

### Export Data

1. Right-click on table → "Table Data Export Wizard"
2. Choose format (CSV, JSON, SQL)
3. Select file location
4. Export

### Import Data

1. Right-click on table → "Table Data Import Wizard"
2. Select file
3. Map columns
4. Import

---

## Security Best Practices

### For Production

1. **Don't use root user in application:**
   ```sql
   CREATE USER 'appuser'@'%' IDENTIFIED BY 'SecurePassword123!';
   GRANT SELECT, INSERT, UPDATE, DELETE ON employeedb.* TO 'appuser'@'%';
   ```

2. **Use environment variables:**
   ```properties
   spring.datasource.username=${DB_USERNAME:root}
   spring.datasource.password=${DB_PASSWORD:root123}
   ```

3. **Enable SSL:**
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/employeedb?useSSL=true
   ```

4. **Use connection pooling** (already configured via HikariCP)

5. **Regular backups:**
   ```cmd
   mysqldump -u root -p employeedb > backup.sql
   ```

---

## Next Steps

1. ✅ Install MySQL Server + Workbench
2. ✅ Create database: `employeedb`
3. ✅ Update `application.properties` (already done)
4. ✅ Reload Maven project
5. ✅ Start application
6. ✅ Test with Workbench
7. ✅ Register users and add employees
8. ✅ Verify data persists after restart

---

## Summary

**Installation:**
- Download: https://dev.mysql.com/downloads/installer/
- Install: MySQL Server 8.0 + Workbench
- Create database: `employeedb`
- Default port: 3306
- Set root password

**Configuration Changes:**
- ✅ Updated `application.properties`
- ✅ MySQL connector in `pom.xml`
- ✅ Hibernate dialect changed to MySQL8Dialect
- ✅ Removed H2 console configuration

**Benefits:**
- ✅ Persistent data storage
- ✅ Production-ready
- ✅ Professional database management
- ✅ Data survives application restarts
- ✅ Better query performance

**Your data is now stored in MySQL database! 🎉**

