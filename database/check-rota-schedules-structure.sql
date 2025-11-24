-- Check current structure of rota_schedules table
USE employee_management_system;

-- Show all columns in rota_schedules table
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT,
    COLUMN_KEY,
    EXTRA
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'employee_management_system'
  AND TABLE_NAME = 'rota_schedules'
ORDER BY ORDINAL_POSITION;

-- Show indexes
SHOW INDEXES FROM rota_schedules;

-- Show foreign keys
SELECT 
    CONSTRAINT_NAME,
    COLUMN_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME
FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = 'employee_management_system'
  AND TABLE_NAME = 'rota_schedules'
  AND REFERENCED_TABLE_NAME IS NOT NULL;

