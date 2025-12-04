-- ===================================================================
-- Repair Flyway Migration V25 - Add Title To Next Of Kin
-- ===================================================================
-- This script repairs the failed migration V25
-- Run this if you get "Detected failed migration to version 25" errors
-- ===================================================================

USE employee_management_system;

-- Check if the 'title' column already exists
SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN 'Column exists - migration completed but marked as failed'
        ELSE 'Column does not exist - migration failed before completion'
    END AS status
FROM information_schema.columns 
WHERE table_schema = 'employee_management_system' 
  AND table_name = 'next_of_kin' 
  AND column_name = 'title';

-- Show current V25 migration status
SELECT 'Current V25 migration status:' AS info;
SELECT 
    installed_rank,
    version,
    description,
    type,
    script,
    installed_on,
    execution_time,
    success
FROM flyway_schema_history
WHERE version = '25'
ORDER BY installed_rank;

-- Option 1: If column exists, mark migration as successful
-- Uncomment the following if the column already exists:
/*
UPDATE flyway_schema_history 
SET success = 1 
WHERE version = '25' AND success = 0;
*/

-- Option 2: If column does NOT exist, delete failed migration record
-- This allows Flyway to retry the migration on next startup
-- Uncomment the following if the column does NOT exist:
/*
DELETE FROM flyway_schema_history 
WHERE version = '25' AND success = 0;
*/

-- Verify repair
SELECT 'After repair - V25 migration status:' AS info;
SELECT 
    version,
    description,
    success,
    installed_on
FROM flyway_schema_history
WHERE version = '25';

SELECT 'Repair completed! Restart the backend container.' AS status;

