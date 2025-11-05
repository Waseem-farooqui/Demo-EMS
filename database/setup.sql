-- ===================================================================
-- MySQL Database Setup Script for Employee Management System
-- ===================================================================

-- Create the database
CREATE DATABASE IF NOT EXISTS employeedb
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Use the database
USE employeedb;

-- ===================================================================
-- Option 1: Create a dedicated application user (Recommended)
-- ===================================================================

-- Create application user with limited privileges
CREATE USER IF NOT EXISTS 'empuser'@'localhost' IDENTIFIED BY 'emppass123';

-- Grant all privileges on employeedb database
GRANT ALL PRIVILEGES ON employeedb.* TO 'empuser'@'localhost';

-- Apply the changes
FLUSH PRIVILEGES;

-- ===================================================================
-- Option 2: Use root user (For Development Only)
-- ===================================================================

-- If using root, just update application.properties:
-- spring.datasource.username=root
-- spring.datasource.password=your_root_password

-- ===================================================================
-- Verify Setup
-- ===================================================================

-- Show all databases
SHOW DATABASES;

-- Show current database
SELECT DATABASE();

-- Show users
SELECT User, Host FROM mysql.user WHERE User IN ('root', 'empuser');

-- ===================================================================
-- Tables will be created automatically by Hibernate
-- ===================================================================

-- When you start the Spring Boot application with:
-- spring.jpa.hibernate.ddl-auto=update
--
-- Hibernate will automatically create these tables:
-- 1. employees
-- 2. users
-- 3. user_roles

-- ===================================================================
-- Useful Queries (Run after application creates tables)
-- ===================================================================

-- View all tables in database
SHOW TABLES;

-- View employees table structure
DESCRIBE employees;

-- View users table structure
DESCRIBE users;

-- View user_roles table structure
DESCRIBE user_roles;

-- Count records in each table
SELECT 'employees' as table_name, COUNT(*) as count FROM employees
UNION ALL
SELECT 'users' as table_name, COUNT(*) as count FROM users
UNION ALL
SELECT 'user_roles' as table_name, COUNT(*) as count FROM user_roles;

-- View all employees
SELECT * FROM employees ORDER BY id DESC;

-- View all users with their roles
SELECT
    u.id,
    u.username,
    u.email,
    u.enabled,
    GROUP_CONCAT(ur.role) as roles
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id
GROUP BY u.id, u.username, u.email, u.enabled
ORDER BY u.id DESC;

-- ===================================================================
-- Backup and Restore Commands
-- ===================================================================

-- Export database (run from command line):
-- mysqldump -u root -p employeedb > employeedb_backup.sql

-- Import database (run from command line):
-- mysql -u root -p employeedb < employeedb_backup.sql

-- ===================================================================
-- Clean Database (Be Careful - Deletes All Data!)
-- ===================================================================

-- Drop all tables (if you want to start fresh)
-- DROP TABLE IF EXISTS user_roles;
-- DROP TABLE IF EXISTS users;
-- DROP TABLE IF EXISTS employees;

-- Or delete all data but keep tables
-- DELETE FROM user_roles;
-- DELETE FROM users;
-- DELETE FROM employees;

-- Reset auto-increment
-- ALTER TABLE employees AUTO_INCREMENT = 1;
-- ALTER TABLE users AUTO_INCREMENT = 1;

