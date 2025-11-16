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
-- Cleanup Incorrect Table Structures (if tables were created incorrectly)
-- ===================================================================
-- This section removes incorrect columns from rotas table if they exist
-- These columns don't belong to rotas table and should be removed

-- Check if rotas table exists and has incorrect columns
SET @rotas_table_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.TABLES 
    WHERE table_schema = 'employee_management_system' 
    AND table_name = 'rotas'
);

-- Remove employee_id from rotas table if it exists (belongs to rota_schedules)
-- First drop foreign key constraint if it exists
SET @fk_name = (
    SELECT CONSTRAINT_NAME 
    FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE 
    WHERE table_schema = 'employee_management_system' 
    AND table_name = 'rotas' 
    AND column_name = 'employee_id'
    AND referenced_table_name IS NOT NULL
    LIMIT 1
);

SET @sql = IF(@rotas_table_exists > 0 AND @fk_name IS NOT NULL,
    CONCAT('ALTER TABLE rotas DROP FOREIGN KEY ', @fk_name),
    'SELECT "No foreign key constraint on employee_id" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Drop index on employee_id if it exists
SET @index_name = (
    SELECT DISTINCT index_name 
    FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE table_schema = 'employee_management_system' 
    AND table_name = 'rotas' 
    AND column_name = 'employee_id'
    AND index_name != 'PRIMARY'
    LIMIT 1
);

SET @sql = IF(@rotas_table_exists > 0 AND @index_name IS NOT NULL,
    CONCAT('ALTER TABLE rotas DROP INDEX ', @index_name),
    'SELECT "No index on employee_id" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Drop employee_id column if it exists
SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE table_schema = 'employee_management_system' 
    AND table_name = 'rotas' 
    AND column_name = 'employee_id'
);

SET @sql = IF(@rotas_table_exists > 0 AND @column_exists > 0,
    'ALTER TABLE rotas DROP COLUMN employee_id',
    'SELECT "Column employee_id does not exist in rotas table" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Remove other incorrect columns (week_start_date, week_end_date, status, created_by, created_at, updated_at)
SET @columns_to_drop = (
    SELECT GROUP_CONCAT(column_name) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE table_schema = 'employee_management_system' 
    AND table_name = 'rotas' 
    AND column_name IN ('week_start_date', 'week_end_date', 'status', 'created_by', 'created_at', 'updated_at')
);

-- Drop week_start_date
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE table_schema = 'employee_management_system' AND table_name = 'rotas' AND column_name = 'week_start_date');
SET @sql = IF(@rotas_table_exists > 0 AND @column_exists > 0, 'ALTER TABLE rotas DROP COLUMN week_start_date', 'SELECT "week_start_date does not exist" AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Drop week_end_date
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE table_schema = 'employee_management_system' AND table_name = 'rotas' AND column_name = 'week_end_date');
SET @sql = IF(@rotas_table_exists > 0 AND @column_exists > 0, 'ALTER TABLE rotas DROP COLUMN week_end_date', 'SELECT "week_end_date does not exist" AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Drop status
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE table_schema = 'employee_management_system' AND table_name = 'rotas' AND column_name = 'status');
SET @sql = IF(@rotas_table_exists > 0 AND @column_exists > 0, 'ALTER TABLE rotas DROP COLUMN status', 'SELECT "status does not exist" AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Drop created_by (check for FK first)
SET @fk_name = (SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE WHERE table_schema = 'employee_management_system' AND table_name = 'rotas' AND column_name = 'created_by' AND referenced_table_name IS NOT NULL LIMIT 1);
SET @sql = IF(@rotas_table_exists > 0 AND @fk_name IS NOT NULL, CONCAT('ALTER TABLE rotas DROP FOREIGN KEY ', @fk_name), 'SELECT "No FK on created_by" AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE table_schema = 'employee_management_system' AND table_name = 'rotas' AND column_name = 'created_by');
SET @sql = IF(@rotas_table_exists > 0 AND @column_exists > 0, 'ALTER TABLE rotas DROP COLUMN created_by', 'SELECT "created_by does not exist" AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Drop created_at
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE table_schema = 'employee_management_system' AND table_name = 'rotas' AND column_name = 'created_at');
SET @sql = IF(@rotas_table_exists > 0 AND @column_exists > 0, 'ALTER TABLE rotas DROP COLUMN created_at', 'SELECT "created_at does not exist" AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Drop updated_at
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE table_schema = 'employee_management_system' AND table_name = 'rotas' AND column_name = 'updated_at');
SET @sql = IF(@rotas_table_exists > 0 AND @column_exists > 0, 'ALTER TABLE rotas DROP COLUMN updated_at', 'SELECT "updated_at does not exist" AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ===================================================================
-- Cleanup Attendance Table Structure
-- ===================================================================
-- Remove incorrect/unused columns from attendance table
-- The entity uses 'work_date' not 'date', and 'is_active' instead of 'status'
-- Using stored procedure approach for conditional logic

DELIMITER //

CREATE PROCEDURE IF NOT EXISTS fix_attendance_table()
BEGIN
    DECLARE table_exists INT DEFAULT 0;
    DECLARE column_exists INT DEFAULT 0;
    DECLARE column_nullable VARCHAR(3);
    
    -- Check if attendance table exists
    SELECT COUNT(*) INTO table_exists
    FROM INFORMATION_SCHEMA.TABLES 
    WHERE table_schema = 'employee_management_system' 
    AND table_name = 'attendance';
    
    IF table_exists > 0 THEN
        -- Drop 'date' column if it exists (entity uses 'work_date' instead)
        SELECT COUNT(*) INTO column_exists
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE table_schema = 'employee_management_system' 
        AND table_name = 'attendance' 
        AND column_name = 'date';
        
        IF column_exists > 0 THEN
            SET @sql = 'ALTER TABLE attendance DROP COLUMN `date`';
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;
        
        -- Drop 'status' column if it exists (entity uses 'is_active' instead)
        SELECT COUNT(*) INTO column_exists
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE table_schema = 'employee_management_system' 
        AND table_name = 'attendance' 
        AND column_name = 'status';
        
        IF column_exists > 0 THEN
            SET @sql = 'ALTER TABLE attendance DROP COLUMN `status`';
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;
        
        -- Ensure work_date is NOT NULL
        SELECT IS_NULLABLE INTO column_nullable
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE table_schema = 'employee_management_system' 
        AND table_name = 'attendance' 
        AND column_name = 'work_date';
        
        IF column_nullable = 'YES' THEN
            SET @sql = 'ALTER TABLE attendance MODIFY COLUMN work_date DATE NOT NULL';
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;
        
        -- Ensure work_location is NOT NULL
        SELECT IS_NULLABLE INTO column_nullable
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE table_schema = 'employee_management_system' 
        AND table_name = 'attendance' 
        AND column_name = 'work_location';
        
        IF column_nullable = 'YES' THEN
            SET @sql = 'ALTER TABLE attendance MODIFY COLUMN work_location VARCHAR(255) NOT NULL';
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;
    END IF;
END //

DELIMITER ;

-- Execute the procedure
CALL fix_attendance_table();

-- Drop the procedure after use
DROP PROCEDURE IF EXISTS fix_attendance_table;

-- ===================================================================
-- Verify Setup
-- ===================================================================

-- Verify database creation
SELECT 'Database employee_management_system created successfully' AS status;

-- Verify user privileges (optional - for debugging)
-- SHOW GRANTS FOR 'emsuser'@'%';
-- SHOW GRANTS FOR 'emsuser'@'localhost';

