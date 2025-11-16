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
-- Cleanup Leave Balances Table Structure
-- ===================================================================
-- Remove incorrect/unused columns from leave_balances table
-- The entity uses 'financial_year' not 'year'
-- The entity uses 'used_leaves' not 'used'
-- The entity uses 'remaining_leaves' not 'remaining'
-- The entity uses INT types, not DECIMAL
-- Using stored procedure approach for conditional logic

DELIMITER //

CREATE PROCEDURE IF NOT EXISTS fix_leave_balances_table()
BEGIN
    DECLARE table_exists INT DEFAULT 0;
    DECLARE column_exists INT DEFAULT 0;
    DECLARE column_type VARCHAR(100);
    
    -- Check if leave_balances table exists
    SELECT COUNT(*) INTO table_exists
    FROM INFORMATION_SCHEMA.TABLES 
    WHERE table_schema = 'employee_management_system' 
    AND table_name = 'leave_balances';
    
    IF table_exists > 0 THEN
        -- Drop 'year' column if it exists (entity uses 'financial_year' instead)
        SELECT COUNT(*) INTO column_exists
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE table_schema = 'employee_management_system' 
        AND table_name = 'leave_balances' 
        AND column_name = 'year';
        
        IF column_exists > 0 THEN
            SET @sql = 'ALTER TABLE leave_balances DROP COLUMN `year`';
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;
        
        -- Drop 'used' column if it exists (entity uses 'used_leaves' instead)
        SELECT COUNT(*) INTO column_exists
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE table_schema = 'employee_management_system' 
        AND table_name = 'leave_balances' 
        AND column_name = 'used';
        
        IF column_exists > 0 THEN
            SET @sql = 'ALTER TABLE leave_balances DROP COLUMN `used`';
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;
        
        -- Drop 'remaining' column if it exists (entity uses 'remaining_leaves' instead)
        SELECT COUNT(*) INTO column_exists
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE table_schema = 'employee_management_system' 
        AND table_name = 'leave_balances' 
        AND column_name = 'remaining';
        
        IF column_exists > 0 THEN
            SET @sql = 'ALTER TABLE leave_balances DROP COLUMN `remaining`';
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;
        
        -- Ensure financial_year exists and is VARCHAR(20) NOT NULL
        SELECT COUNT(*) INTO column_exists
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE table_schema = 'employee_management_system' 
        AND table_name = 'leave_balances' 
        AND column_name = 'financial_year';
        
        IF column_exists = 0 THEN
            SET @sql = 'ALTER TABLE leave_balances ADD COLUMN financial_year VARCHAR(20) NOT NULL';
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        ELSE
            -- Fix type if it's not VARCHAR(20)
            SELECT COLUMN_TYPE INTO column_type
            FROM INFORMATION_SCHEMA.COLUMNS 
            WHERE table_schema = 'employee_management_system' 
            AND table_name = 'leave_balances' 
            AND column_name = 'financial_year';
            
            IF column_type != 'varchar(20)' THEN
                SET @sql = 'ALTER TABLE leave_balances MODIFY COLUMN financial_year VARCHAR(20) NOT NULL';
                PREPARE stmt FROM @sql;
                EXECUTE stmt;
                DEALLOCATE PREPARE stmt;
            END IF;
        END IF;
        
        -- Ensure total_allocated is INT (not DECIMAL)
        SELECT COLUMN_TYPE INTO column_type
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE table_schema = 'employee_management_system' 
        AND table_name = 'leave_balances' 
        AND column_name = 'total_allocated';
        
        IF column_type IS NOT NULL AND column_type NOT LIKE '%int%' THEN
            SET @sql = 'ALTER TABLE leave_balances MODIFY COLUMN total_allocated INT NOT NULL DEFAULT 0';
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;
        
        -- Ensure used_leaves is INT (not DECIMAL)
        SELECT COLUMN_TYPE INTO column_type
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE table_schema = 'employee_management_system' 
        AND table_name = 'leave_balances' 
        AND column_name = 'used_leaves';
        
        IF column_type IS NOT NULL AND column_type NOT LIKE '%int%' THEN
            SET @sql = 'ALTER TABLE leave_balances MODIFY COLUMN used_leaves INT NOT NULL DEFAULT 0';
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;
        
        -- Ensure remaining_leaves is INT (not DECIMAL)
        SELECT COLUMN_TYPE INTO column_type
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE table_schema = 'employee_management_system' 
        AND table_name = 'leave_balances' 
        AND column_name = 'remaining_leaves';
        
        IF column_type IS NOT NULL AND column_type NOT LIKE '%int%' THEN
            SET @sql = 'ALTER TABLE leave_balances MODIFY COLUMN remaining_leaves INT NOT NULL DEFAULT 0';
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;
    END IF;
END //

DELIMITER ;

-- Execute the procedure
CALL fix_leave_balances_table();

-- Drop the procedure after use
DROP PROCEDURE IF EXISTS fix_leave_balances_table;

-- ===================================================================
-- Cleanup Leaves Table Structure
-- ===================================================================
-- Remove incorrect/unused columns from leaves table
-- The entity uses 'number_of_days' not 'days_taken'
-- The entity uses 'financial_year' VARCHAR(20) not VARCHAR(255)
-- Using stored procedure approach for conditional logic

DELIMITER //

CREATE PROCEDURE IF NOT EXISTS fix_leaves_table()
BEGIN
    DECLARE table_exists INT DEFAULT 0;
    DECLARE column_exists INT DEFAULT 0;
    DECLARE column_type VARCHAR(100);
    
    -- Check if leaves table exists
    SELECT COUNT(*) INTO table_exists
    FROM INFORMATION_SCHEMA.TABLES 
    WHERE table_schema = 'employee_management_system' 
    AND table_name = 'leaves';
    
    IF table_exists > 0 THEN
        -- Drop 'days_taken' column if it exists (entity uses 'number_of_days' instead)
        SELECT COUNT(*) INTO column_exists
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE table_schema = 'employee_management_system' 
        AND table_name = 'leaves' 
        AND column_name = 'days_taken';
        
        IF column_exists > 0 THEN
            SET @sql = 'ALTER TABLE leaves DROP COLUMN `days_taken`';
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;
        
        -- Drop 'approved_date' (DATETIME) column if it exists (entity uses 'approval_date' DATE instead)
        SELECT COUNT(*) INTO column_exists
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE table_schema = 'employee_management_system' 
        AND table_name = 'leaves' 
        AND column_name = 'approved_date';
        
        IF column_exists > 0 THEN
            -- Check if it's DATETIME type (entity uses approval_date DATE)
            SELECT DATA_TYPE INTO @data_type
            FROM INFORMATION_SCHEMA.COLUMNS 
            WHERE table_schema = 'employee_management_system' 
            AND table_name = 'leaves' 
            AND column_name = 'approved_date';
            
            -- Only drop if it's DATETIME, keep if it's DATE (as approval_date)
            IF @data_type = 'datetime' THEN
                SET @sql = 'ALTER TABLE leaves DROP COLUMN `approved_date`';
                PREPARE stmt FROM @sql;
                EXECUTE stmt;
                DEALLOCATE PREPARE stmt;
            END IF;
        END IF;
        
        -- Ensure number_of_days exists and is INT NOT NULL
        SELECT COUNT(*) INTO column_exists
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE table_schema = 'employee_management_system' 
        AND table_name = 'leaves' 
        AND column_name = 'number_of_days';
        
        IF column_exists = 0 THEN
            SET @sql = 'ALTER TABLE leaves ADD COLUMN number_of_days INT NOT NULL';
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;
        
        -- Ensure financial_year exists and is VARCHAR(20) (not VARCHAR(255))
        SELECT COUNT(*) INTO column_exists
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE table_schema = 'employee_management_system' 
        AND table_name = 'leaves' 
        AND column_name = 'financial_year';
        
        IF column_exists = 0 THEN
            SET @sql = 'ALTER TABLE leaves ADD COLUMN financial_year VARCHAR(20) NULL';
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        ELSE
            -- Fix type if it's not VARCHAR(20)
            SELECT COLUMN_TYPE INTO column_type
            FROM INFORMATION_SCHEMA.COLUMNS 
            WHERE table_schema = 'employee_management_system' 
            AND table_name = 'leaves' 
            AND column_name = 'financial_year';
            
            IF column_type != 'varchar(20)' THEN
                SET @sql = 'ALTER TABLE leaves MODIFY COLUMN financial_year VARCHAR(20) NULL';
                PREPARE stmt FROM @sql;
                EXECUTE stmt;
                DEALLOCATE PREPARE stmt;
            END IF;
        END IF;
        
        -- Ensure approval_date exists and is DATE (not DATETIME)
        SELECT COUNT(*) INTO column_exists
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE table_schema = 'employee_management_system' 
        AND table_name = 'leaves' 
        AND column_name = 'approval_date';
        
        IF column_exists = 0 THEN
            SET @sql = 'ALTER TABLE leaves ADD COLUMN approval_date DATE NULL';
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        ELSE
            -- Fix type if it's not DATE
            SELECT DATA_TYPE INTO @data_type
            FROM INFORMATION_SCHEMA.COLUMNS 
            WHERE table_schema = 'employee_management_system' 
            AND table_name = 'leaves' 
            AND column_name = 'approval_date';
            
            IF @data_type != 'date' THEN
                SET @sql = 'ALTER TABLE leaves MODIFY COLUMN approval_date DATE NULL';
                PREPARE stmt FROM @sql;
                EXECUTE stmt;
                DEALLOCATE PREPARE stmt;
            END IF;
        END IF;
        
        -- Fix approved_by type if it's BIGINT (entity uses String/VARCHAR)
        SELECT COUNT(*) INTO column_exists
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE table_schema = 'employee_management_system' 
        AND table_name = 'leaves' 
        AND column_name = 'approved_by';
        
        IF column_exists > 0 THEN
            SELECT DATA_TYPE INTO @data_type
            FROM INFORMATION_SCHEMA.COLUMNS 
            WHERE table_schema = 'employee_management_system' 
            AND table_name = 'leaves' 
            AND column_name = 'approved_by';
            
            IF @data_type = 'bigint' THEN
                SET @sql = 'ALTER TABLE leaves MODIFY COLUMN approved_by VARCHAR(255) NULL';
                PREPARE stmt FROM @sql;
                EXECUTE stmt;
                DEALLOCATE PREPARE stmt;
            END IF;
        END IF;
        
        -- Ensure organization_id exists
        SELECT COUNT(*) INTO column_exists
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE table_schema = 'employee_management_system' 
        AND table_name = 'leaves' 
        AND column_name = 'organization_id';
        
        IF column_exists = 0 THEN
            SET @sql = 'ALTER TABLE leaves ADD COLUMN organization_id BIGINT NULL';
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;
    END IF;
END //

DELIMITER ;

-- Execute the procedure
CALL fix_leaves_table();

-- Drop the procedure after use
DROP PROCEDURE IF EXISTS fix_leaves_table;

-- ===================================================================
-- Verify Setup
-- ===================================================================

-- Verify database creation
SELECT 'Database employee_management_system created successfully' AS status;

-- Verify user privileges (optional - for debugging)
-- SHOW GRANTS FOR 'emsuser'@'%';
-- SHOW GRANTS FOR 'emsuser'@'localhost';

