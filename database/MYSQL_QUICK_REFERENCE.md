# MySQL Quick Reference for Employee Management System

## 🚀 Quick Start Commands

### Connect to MySQL
```cmd
mysql -u root -p
```
Enter password when prompted: `root123`

### Create Database
```sql
CREATE DATABASE employeedb;
USE employeedb;
```

### Exit MySQL
```sql
EXIT;
```

---

## 📊 Database Operations

### Show All Databases
```sql
SHOW DATABASES;
```

### Use Database
```sql
USE employeedb;
```

### Show All Tables
```sql
SHOW TABLES;
```

### Show Table Structure
```sql
DESCRIBE employees;
DESCRIBE users;
DESCRIBE user_roles;
```

---

## 👥 View Data

### View All Employees
```sql
SELECT * FROM employees;
```

### View Employees with Formatting
```sql
SELECT 
    id,
    full_name,
    person_type,
    work_email,
    job_title,
    date_of_joining
FROM employees
ORDER BY id DESC;
```

### Count Employees
```sql
SELECT COUNT(*) as total FROM employees;
```

### View All Users
```sql
SELECT 
    u.id,
    u.username,
    u.email,
    GROUP_CONCAT(ur.role) as roles
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id
GROUP BY u.id
ORDER BY u.id DESC;
```

### Find Employee by Email
```sql
SELECT * FROM employees WHERE work_email = 'john@example.com';
```

### Find Employee by Name
```sql
SELECT * FROM employees WHERE full_name LIKE '%John%';
```

---

## ✏️ Modify Data

### Update Employee
```sql
UPDATE employees 
SET job_title = 'Senior Software Engineer'
WHERE id = 1;
```

### Delete Employee
```sql
DELETE FROM employees WHERE id = 1;
```

### Delete All Employees (Be Careful!)
```sql
DELETE FROM employees;
```

### Reset Auto-Increment
```sql
ALTER TABLE employees AUTO_INCREMENT = 1;
```

---

## 🔐 User Management

### Create Application User
```sql
CREATE USER 'empuser'@'localhost' IDENTIFIED BY 'emppass123';
GRANT ALL PRIVILEGES ON employeedb.* TO 'empuser'@'localhost';
FLUSH PRIVILEGES;
```

### Show All Users
```sql
SELECT User, Host FROM mysql.user;
```

### Change User Password
```sql
ALTER USER 'root'@'localhost' IDENTIFIED BY 'newpassword';
FLUSH PRIVILEGES;
```

---

## 📈 Statistics and Monitoring

### Count Records by Table
```sql
SELECT 
    (SELECT COUNT(*) FROM employees) as employees,
    (SELECT COUNT(*) FROM users) as users,
    (SELECT COUNT(*) FROM user_roles) as roles;
```

### Show Table Sizes
```sql
SELECT 
    table_name,
    table_rows,
    ROUND(data_length/1024/1024, 2) as 'Size (MB)'
FROM information_schema.tables
WHERE table_schema = 'employeedb';
```

### Show Recent Employees
```sql
SELECT * FROM employees 
ORDER BY date_of_joining DESC 
LIMIT 10;
```

---

## 💾 Backup and Restore

### Export Database (Command Line)
```cmd
mysqldump -u root -p employeedb > backup_2025_10_29.sql
```

### Import Database (Command Line)
```cmd
mysql -u root -p employeedb < backup_2025_10_29.sql
```

### Export Single Table
```cmd
mysqldump -u root -p employeedb employees > employees_backup.sql
```

---

## 🔍 Troubleshooting Queries

### Check MySQL Version
```sql
SELECT VERSION();
```

### Show Current User
```sql
SELECT USER();
```

### Show Current Database
```sql
SELECT DATABASE();
```

### Check Character Set
```sql
SHOW VARIABLES LIKE 'character_set%';
```

### Check Connections
```sql
SHOW PROCESSLIST;
```

### Check Table Status
```sql
SHOW TABLE STATUS FROM employeedb;
```

---

## 🧹 Maintenance

### Optimize Tables
```sql
OPTIMIZE TABLE employees;
OPTIMIZE TABLE users;
OPTIMIZE TABLE user_roles;
```

### Repair Tables
```sql
REPAIR TABLE employees;
```

### Check Tables
```sql
CHECK TABLE employees;
```

---

## 🎯 Common Tasks

### Clear All Data (Keep Tables)
```sql
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE user_roles;
TRUNCATE TABLE users;
TRUNCATE TABLE employees;
SET FOREIGN_KEY_CHECKS = 1;
```

### Drop All Tables
```sql
DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS employees;
```

### Recreate Tables
Just restart your Spring Boot application with `spring.jpa.hibernate.ddl-auto=update`

---

## 📱 MySQL Workbench Shortcuts

- **Execute Query:** `Ctrl + Enter`
- **Execute All:** `Ctrl + Shift + Enter`
- **New Query Tab:** `Ctrl + T`
- **Format Query:** `Ctrl + B`
- **Comment Line:** `Ctrl + /`

---

## 🔗 Useful Links

- **MySQL Documentation:** https://dev.mysql.com/doc/
- **MySQL Workbench:** https://www.mysql.com/products/workbench/
- **Spring Data JPA:** https://spring.io/projects/spring-data-jpa

---

## ⚠️ Important Notes

### For Development
- Username: `root`
- Password: `root123` (or your chosen password)
- Database: `employeedb`
- Port: `3306`

### For Production
- Create dedicated user (not root)
- Use strong passwords
- Enable SSL
- Regular backups
- Restrict access by IP

### Application Configuration
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/employeedb
spring.datasource.username=root
spring.datasource.password=root123
spring.jpa.hibernate.ddl-auto=update
```

---

## 🚨 Emergency Commands

### Stop MySQL Service
```cmd
net stop MySQL80
```

### Start MySQL Service
```cmd
net start MySQL80
```

### Check MySQL Service Status
```cmd
sc query MySQL80
```

### Kill MySQL Connections
```sql
SHOW PROCESSLIST;
KILL [process_id];
```

---

**Keep this reference handy for quick MySQL operations!**

