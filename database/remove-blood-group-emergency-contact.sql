-- ===================================================================
-- Script to Remove Blood Group and Emergency Contact Columns
-- Employee Management System - Production Database Migration
-- ===================================================================
-- 
-- This script removes the following columns from the employees table:
--   - blood_group
--   - emergency_contact_name
--   - emergency_contact_phone
--   - emergency_contact_relationship
--
-- IMPORTANT: 
--   1. BACKUP YOUR DATABASE BEFORE RUNNING THIS SCRIPT
--   2. Test this script on a staging/development environment first
--   3. Run during a maintenance window if possible
--   4. Verify application functionality after migration
--
-- Usage:
--   mysql -u [username] -p [database_name] < remove-blood-group-emergency-contact.sql
--   OR
--   mysql -u [username] -p
--   USE [database_name];
--   SOURCE remove-blood-group-emergency-contact.sql;
--
-- ===================================================================

USE employeedb;

-- Set SQL mode to handle potential issues
SET SQL_MODE = 'STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION';

-- Start transaction for safety (if supported)
-- Note: DDL statements in MySQL are auto-committed, but we'll use it for documentation
START TRANSACTION;

-- ===================================================================
-- Step 1: Verify columns exist before dropping
-- ===================================================================

SELECT 'Checking for columns to remove...' AS status;

-- Check if blood_group column exists
SET @blood_group_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'employees'
    AND COLUMN_NAME = 'blood_group'
);

-- Check if emergency_contact_name column exists
SET @emergency_name_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'employees'
    AND COLUMN_NAME = 'emergency_contact_name'
);

-- Check if emergency_contact_phone column exists
SET @emergency_phone_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'employees'
    AND COLUMN_NAME = 'emergency_contact_phone'
);

-- Check if emergency_contact_relationship column exists
SET @emergency_relationship_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'employees'
    AND COLUMN_NAME = 'emergency_contact_relationship'
);

-- Display status
SELECT 
    CASE WHEN @blood_group_exists > 0 THEN 'EXISTS' ELSE 'NOT FOUND' END AS blood_group_status,
    CASE WHEN @emergency_name_exists > 0 THEN 'EXISTS' ELSE 'NOT FOUND' END AS emergency_contact_name_status,
    CASE WHEN @emergency_phone_exists > 0 THEN 'EXISTS' ELSE 'NOT FOUND' END AS emergency_contact_phone_status,
    CASE WHEN @emergency_relationship_exists > 0 THEN 'EXISTS' ELSE 'NOT FOUND' END AS emergency_contact_relationship_status;

-- ===================================================================
-- Step 2: Drop columns if they exist
-- ===================================================================

-- Drop blood_group column
SET @sql = IF(@blood_group_exists > 0,
    'ALTER TABLE employees DROP COLUMN blood_group',
    'SELECT ''Column blood_group does not exist, skipping...'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT IF(@blood_group_exists > 0, '✓ Dropped blood_group column', '⊘ blood_group column not found') AS result;

-- Drop emergency_contact_name column
SET @sql = IF(@emergency_name_exists > 0,
    'ALTER TABLE employees DROP COLUMN emergency_contact_name',
    'SELECT ''Column emergency_contact_name does not exist, skipping...'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT IF(@emergency_name_exists > 0, '✓ Dropped emergency_contact_name column', '⊘ emergency_contact_name column not found') AS result;

-- Drop emergency_contact_phone column
SET @sql = IF(@emergency_phone_exists > 0,
    'ALTER TABLE employees DROP COLUMN emergency_contact_phone',
    'SELECT ''Column emergency_contact_phone does not exist, skipping...'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT IF(@emergency_phone_exists > 0, '✓ Dropped emergency_contact_phone column', '⊘ emergency_contact_phone column not found') AS result;

-- Drop emergency_contact_relationship column
SET @sql = IF(@emergency_relationship_exists > 0,
    'ALTER TABLE employees DROP COLUMN emergency_contact_relationship',
    'SELECT ''Column emergency_contact_relationship does not exist, skipping...'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT IF(@emergency_relationship_exists > 0, '✓ Dropped emergency_contact_relationship column', '⊘ emergency_contact_relationship column not found') AS result;

-- ===================================================================
-- Step 3: Verify columns have been removed
-- ===================================================================

SELECT 'Verifying columns have been removed...' AS status;

SELECT 
    CASE WHEN COUNT(*) > 0 THEN 'STILL EXISTS' ELSE 'REMOVED' END AS blood_group_status
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE()
AND TABLE_NAME = 'employees'
AND COLUMN_NAME = 'blood_group';

SELECT 
    CASE WHEN COUNT(*) > 0 THEN 'STILL EXISTS' ELSE 'REMOVED' END AS emergency_contact_name_status
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE()
AND TABLE_NAME = 'employees'
AND COLUMN_NAME = 'emergency_contact_name';

SELECT 
    CASE WHEN COUNT(*) > 0 THEN 'STILL EXISTS' ELSE 'REMOVED' END AS emergency_contact_phone_status
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE()
AND TABLE_NAME = 'employees'
AND COLUMN_NAME = 'emergency_contact_phone';

SELECT 
    CASE WHEN COUNT(*) > 0 THEN 'STILL EXISTS' ELSE 'REMOVED' END AS emergency_contact_relationship_status
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE()
AND TABLE_NAME = 'employees'
AND COLUMN_NAME = 'emergency_contact_relationship';

-- ===================================================================
-- Step 4: Show final table structure
-- ===================================================================

SELECT 'Final employees table structure (showing first 20 columns):' AS status;
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
AND TABLE_NAME = 'employees'
ORDER BY ORDINAL_POSITION
LIMIT 20;

-- Commit transaction (DDL statements auto-commit, but included for clarity)
COMMIT;

SELECT '========================================' AS '';
SELECT 'Migration completed successfully!' AS status;
SELECT 'All blood group and emergency contact columns have been removed.' AS message;
SELECT '========================================' AS '';

