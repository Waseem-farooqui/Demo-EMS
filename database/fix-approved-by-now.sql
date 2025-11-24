-- ===================================================================
-- IMMEDIATE FIX: Change approved_by from BIGINT to VARCHAR(255)
-- ===================================================================
-- This fixes the error: "Incorrect integer value: 'username' for column 'approved_by'"
-- The entity stores username strings, not user IDs
-- ===================================================================

USE employee_management_system;

-- Step 1: Clear any existing BIGINT values (they're invalid anyway)
-- Set to NULL since we can't convert user IDs to usernames retroactively
UPDATE leaves SET approved_by = NULL WHERE approved_by IS NOT NULL AND approved_by REGEXP '^[0-9]+$';

-- Step 2: Change column type from BIGINT to VARCHAR(255)
ALTER TABLE leaves MODIFY COLUMN approved_by VARCHAR(255) NULL;

SELECT '✅ approved_by column fixed to VARCHAR(255)' AS status;

