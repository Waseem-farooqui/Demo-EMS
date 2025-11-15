-- ===================================================================
-- Employee Management System - Database Initialization Script
-- ===================================================================
-- This script is executed automatically when MySQL container starts
-- It creates the database and ensures proper character set
-- Tables will be created by JPA/Hibernate with ddl-auto=update
-- ===================================================================

-- Create database if it doesn't exist
CREATE DATABASE IF NOT EXISTS employee_management_system
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Use the database
USE employee_management_system;

-- Note: Tables are created automatically by Spring Boot JPA
-- with ddl-auto=update configuration
-- 
-- If you need to create tables manually, uncomment the sections below
-- or use Flyway migrations instead

-- ===================================================================
-- Core Tables (Created by JPA automatically)
-- ===================================================================
-- The following tables are created by JPA entities:
-- 
-- 1. organizations
-- 2. users
-- 3. user_roles
-- 4. employees
-- 5. departments
-- 6. documents
-- 7. leaves
-- 8. leave_balances
-- 9. attendance
-- 10. rotas
-- 11. rota_schedules
-- 12. rota_change_logs
-- 13. notifications
-- 14. alert_configurations
-- 15. verification_tokens
--
-- If JPA fails to create tables, ensure:
-- - spring.jpa.hibernate.ddl-auto=update (in application-prod.properties)
-- - Database connection is working
-- - User has CREATE TABLE privileges
-- ===================================================================

-- ===================================================================
-- Grant Privileges to Application User (emsuser)
-- ===================================================================
-- The user 'emsuser' is created by Docker via MYSQL_USER environment variable
-- (default: DB_USERNAME=emsuser in .env file)
-- Docker Compose creates the user with host '%' (any host)
-- 
-- IMPORTANT: If you change DB_USERNAME in .env to a different value,
-- you must update 'emsuser' in the statements below to match your username.
--
-- We need to explicitly set the password and grant privileges to ensure
-- JPA/Hibernate can create and modify tables, indexes, etc.

-- Ensure the user exists and password is set correctly
-- Note: The password should match MYSQL_PASSWORD from compose.yaml
-- IMPORTANT: Update this password to match your .env DB_PASSWORD value
-- Current password: wud19@WUD
ALTER USER IF EXISTS 'emsuser'@'%' IDENTIFIED BY 'wud19@WUD';

-- If user doesn't exist (shouldn't happen as Docker creates it), create it
CREATE USER IF NOT EXISTS 'emsuser'@'%' IDENTIFIED BY 'wud19@WUD';

-- Grant all privileges on the database to emsuser
-- This includes: SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, ALTER, INDEX, etc.
-- Note: Only grant to '%' host as that's what Docker Compose creates
GRANT ALL PRIVILEGES ON employee_management_system.* TO 'emsuser'@'%';

-- Grant CREATE DATABASE privilege (needed if createDatabaseIfNotExist=true in JDBC URL)
-- This allows the user to create the database if it doesn't exist
GRANT CREATE ON *.* TO 'emsuser'@'%';

-- Apply privilege changes
FLUSH PRIVILEGES;

-- ===================================================================
-- Verify Setup
-- ===================================================================

-- Verify database creation
SELECT 'Database employee_management_system created successfully' AS status;

-- Verify user privileges (optional - for debugging)
-- SHOW GRANTS FOR 'emsuser'@'%';
-- SHOW GRANTS FOR 'emsuser'@'localhost';

