-- ===================================================================
-- Repair Flyway Schema History
-- ===================================================================
-- This script repairs failed migrations in Flyway schema history
-- Run this if you get "Detected failed migration" errors
-- ===================================================================

USE employee_management_system;

-- Check for failed migrations
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
WHERE success = 0
ORDER BY installed_rank;

-- Repair failed migrations by marking them as successful
-- Note: Only do this if the migration actually completed successfully
-- but was marked as failed due to a transient error
UPDATE flyway_schema_history 
SET success = 1 
WHERE success = 0;

-- Verify repair
SELECT 
    'Repaired migrations:' AS status,
    COUNT(*) AS count
FROM flyway_schema_history
WHERE success = 1;

SELECT 'Flyway schema history repair completed!' AS status;

