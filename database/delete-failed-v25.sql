-- Delete failed V25 migration record
-- This allows Flyway to retry the migration on next startup
USE employee_management_system;

-- Show failed migration before deletion
SELECT 'Deleting failed V25 migration record...' AS status;
SELECT version, description, success, installed_on 
FROM flyway_schema_history 
WHERE version = '25' AND success = 0;

-- Delete failed migration record
DELETE FROM flyway_schema_history 
WHERE version = '25' AND success = 0;

-- Verify deletion
SELECT 'Migration V25 record deleted. Flyway will retry on next startup.' AS status;
SELECT COUNT(*) AS remaining_v25_records
FROM flyway_schema_history 
WHERE version = '25';

