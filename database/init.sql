-- ===================================================================
-- Employee Management System - Database Initialization Script
-- ===================================================================
-- This script is executed automatically when MySQL container starts
-- It creates the database and ensures proper character set
-- Tables will be created by JPA/Hibernate with ddl-auto=update
-- This script checks for existence before creating/modifying anything
-- ===================================================================

-- Create database if it doesn't exist
CREATE DATABASE IF NOT EXISTS employee_management_system
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Use the database
USE employee_management_system;

-- ===================================================================
-- Grant Privileges to Application User (emsuser)
-- ===================================================================
-- The user 'emsuser' is created by Docker via MYSQL_USER environment variable
-- (default: DB_USERNAME=emsuser in .env file)
-- Docker Compose creates the user with host '%' (any host)
-- 
-- IMPORTANT: If you change DB_USERNAME in .env to a different value,
-- you must update 'emsuser' in the statements below to match your username.

-- Ensure the user exists and password is set correctly
-- Note: The password should match MYSQL_PASSWORD from compose.yaml
-- IMPORTANT: Update this password to match your .env DB_PASSWORD value
-- Current password: wud19@WUD

-- Check if user exists, if not create it
SET @user_exists = (SELECT COUNT(*) FROM mysql.user WHERE User = 'emsuser' AND Host = '%');
SET @sql = IF(@user_exists > 0,
    'ALTER USER ''emsuser''@''%'' IDENTIFIED BY ''wud19@WUD''',
    'CREATE USER ''emsuser''@''%'' IDENTIFIED BY ''wud19@WUD'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Grant all privileges on the database to emsuser
-- This includes: SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, ALTER, INDEX, etc.
GRANT ALL PRIVILEGES ON employee_management_system.* TO 'emsuser'@'%';

-- Grant CREATE DATABASE privilege (needed if createDatabaseIfNotExist=true in JDBC URL)
GRANT CREATE ON *.* TO 'emsuser'@'%';

-- Apply privilege changes
FLUSH PRIVILEGES;

-- ===================================================================
-- Add Emergency Contact Columns to Employees Table (if not exists)
-- ===================================================================
-- Check and add emergency_contact_name column
SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE table_schema = 'employee_management_system' 
    AND table_name = 'employees' 
    AND column_name = 'emergency_contact_name'
);

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE employees ADD COLUMN emergency_contact_name VARCHAR(255) NULL',
    'SELECT ''Column emergency_contact_name already exists'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Check and add emergency_contact_phone column
SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE table_schema = 'employee_management_system' 
    AND table_name = 'employees' 
    AND column_name = 'emergency_contact_phone'
);

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE employees ADD COLUMN emergency_contact_phone VARCHAR(50) NULL',
    'SELECT ''Column emergency_contact_phone already exists'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Check and add emergency_contact_relationship column
SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE table_schema = 'employee_management_system' 
    AND table_name = 'employees' 
    AND column_name = 'emergency_contact_relationship'
);

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE employees ADD COLUMN emergency_contact_relationship VARCHAR(100) NULL',
    'SELECT ''Column emergency_contact_relationship already exists'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ===================================================================
-- Note: Other tables are created automatically by Spring Boot JPA
-- with ddl-auto=update configuration
-- 
-- The following tables are created by JPA entities:
-- 
-- 1. organizations
-- 2. users
-- 3. user_roles
-- 4. employees (with emergency contact columns added above)
-- 5. departments
-- 6. documents
-- 7. leaves
-- 8. leave_balances
-- 9. attendance
-- 10. rotas
-- 11. rota_schedules
-- 12. rota_change_logs
-- 13. employment_records
-- 14. notifications
-- 15. alert_configurations
-- 16. smtp_configuration
-- 17. verification_tokens
--
-- If JPA fails to create tables, ensure:
-- - spring.jpa.hibernate.ddl-auto=update (in application-prod.properties)
-- - Database connection is working
-- - User has CREATE TABLE privileges
-- ===================================================================

-- ===================================================================
-- Verify Setup
-- ===================================================================

-- Verify database creation
SELECT 'Database employee_management_system initialized successfully' AS status;
