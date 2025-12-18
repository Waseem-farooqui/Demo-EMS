-- ===================================================================
-- Script to Remove Blood Group and Emergency Contact Columns
-- from employees table in Production Database
-- ===================================================================
-- 
-- This script removes the following columns from the employees table:
-- - blood_group
-- - emergency_contact_name
-- - emergency_contact_phone
-- - emergency_contact_relationship
--
-- IMPORTANT: 
-- 1. Backup your database before running this script
-- 2. Test on a staging environment first
-- 3. These columns will be permanently deleted
-- 4. Any existing data in these columns will be lost
--
-- Usage:
--   mysql -u [username] -p [database_name] < remove-blood-group-emergency-contact-columns.sql
--   OR
--   Execute this script through your database management tool
--
-- ===================================================================

USE employeedb;

-- ===================================================================
-- Remove Blood Group Column
-- ===================================================================

SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'employees'
    AND COLUMN_NAME = 'blood_group'
);

SET @sql = IF(@column_exists > 0,
    'ALTER TABLE employees DROP COLUMN blood_group',
    'SELECT ''Column blood_group does not exist, skipping...'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ===================================================================
-- Remove Emergency Contact Name Column
-- ===================================================================

SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'employees'
    AND COLUMN_NAME = 'emergency_contact_name'
);

SET @sql = IF(@column_exists > 0,
    'ALTER TABLE employees DROP COLUMN emergency_contact_name',
    'SELECT ''Column emergency_contact_name does not exist, skipping...'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ===================================================================
-- Remove Emergency Contact Phone Column
-- ===================================================================

SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'employees'
    AND COLUMN_NAME = 'emergency_contact_phone'
);

SET @sql = IF(@column_exists > 0,
    'ALTER TABLE employees DROP COLUMN emergency_contact_phone',
    'SELECT ''Column emergency_contact_phone does not exist, skipping...'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ===================================================================
-- Remove Emergency Contact Relationship Column
-- ===================================================================

SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'employees'
    AND COLUMN_NAME = 'emergency_contact_relationship'
);

SET @sql = IF(@column_exists > 0,
    'ALTER TABLE employees DROP COLUMN emergency_contact_relationship',
    'SELECT ''Column emergency_contact_relationship does not exist, skipping...'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ===================================================================
-- Verify Removal
-- ===================================================================

SELECT 
    'Verification: Checking remaining columns...' AS status;

SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
AND TABLE_NAME = 'employees'
AND COLUMN_NAME IN ('blood_group', 'emergency_contact_name', 'emergency_contact_phone', 'emergency_contact_relationship');

-- If no rows are returned, all columns have been successfully removed
SELECT 
    CASE 
        WHEN COUNT(*) = 0 THEN '✅ All columns successfully removed!'
        ELSE CONCAT('⚠️ Warning: ', COUNT(*), ' column(s) still exist')
    END AS result
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
AND TABLE_NAME = 'employees'
AND COLUMN_NAME IN ('blood_group', 'emergency_contact_name', 'emergency_contact_phone', 'emergency_contact_relationship');

-- ===================================================================
-- Script Complete
-- ===================================================================

SELECT 'Script execution completed. Please verify the results above.' AS final_status;

