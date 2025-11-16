-- ===================================================================
-- IMMEDIATE FIX: Add employee_id to rota_schedules table
-- ===================================================================
-- Run this NOW to fix the "Field 'employee_id' doesn't have a default value" error
-- ===================================================================

USE employee_management_system;

-- Check if employee_id column exists
SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN 'Column employee_id already exists'
        ELSE 'Column employee_id is MISSING - will add it'
    END AS status
FROM INFORMATION_SCHEMA.COLUMNS
WHERE table_schema = 'employee_management_system'
  AND table_name = 'rota_schedules'
  AND column_name = 'employee_id';

-- Add employee_id column if missing (idempotent)
SET @dbname = DATABASE();
SET @tablename = 'rota_schedules';
SET @columnname = 'employee_id';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE (table_name = @tablename) AND (table_schema = @dbname) AND (column_name = @columnname)
  ) > 0,
  "SELECT 'Column employee_id already exists - no action needed' AS message",
  CONCAT("ALTER TABLE ", @tablename, " ADD COLUMN ", @columnname, " BIGINT NOT NULL AFTER rota_id")
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Add other missing columns
SET @columnname = 'employee_name';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE (table_name = @tablename) AND (table_schema = @dbname) AND (column_name = @columnname)
  ) > 0,
  "SELECT 'Column employee_name already exists' AS message",
  CONCAT("ALTER TABLE ", @tablename, " ADD COLUMN ", @columnname, " VARCHAR(255) NOT NULL AFTER employee_id")
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

SET @columnname = 'schedule_date';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE (table_name = @tablename) AND (table_schema = @dbname) AND (column_name = @columnname)
  ) > 0,
  "SELECT 'Column schedule_date already exists' AS message",
  CONCAT("ALTER TABLE ", @tablename, " ADD COLUMN ", @columnname, " DATE NOT NULL AFTER employee_name")
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Modify day_of_week if it's INT
SET @preparedStatement = (SELECT IF(
  (
    SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS
    WHERE (table_name = @tablename) AND (table_schema = @dbname) AND (column_name = 'day_of_week') AND (DATA_TYPE = 'int')
  ) IS NOT NULL,
  CONCAT("ALTER TABLE ", @tablename, " MODIFY COLUMN day_of_week VARCHAR(50) NOT NULL"),
  "SELECT 'Column day_of_week is already VARCHAR' AS message"
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

SET @columnname = 'duty';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE (table_name = @tablename) AND (table_schema = @dbname) AND (column_name = @columnname)
  ) > 0,
  "SELECT 'Column duty already exists' AS message",
  CONCAT("ALTER TABLE ", @tablename, " ADD COLUMN ", @columnname, " VARCHAR(255) NOT NULL AFTER end_time")
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

SET @columnname = 'is_off_day';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE (table_name = @tablename) AND (table_schema = @dbname) AND (column_name = @columnname)
  ) > 0,
  "SELECT 'Column is_off_day already exists' AS message",
  CONCAT("ALTER TABLE ", @tablename, " ADD COLUMN ", @columnname, " BOOLEAN DEFAULT FALSE AFTER duty")
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Add indexes
SET @indexname = 'idx_employee_id';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE (table_name = @tablename) AND (table_schema = @dbname) AND (index_name = @indexname)
  ) > 0,
  "SELECT 'Index idx_employee_id already exists' AS message",
  CONCAT("CREATE INDEX ", @indexname, " ON ", @tablename, " (employee_id)")
));
PREPARE createIndexIfNotExists FROM @preparedStatement;
EXECUTE createIndexIfNotExists;
DEALLOCATE PREPARE createIndexIfNotExists;

SET @indexname = 'idx_schedule_date';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE (table_name = @tablename) AND (table_schema = @dbname) AND (index_name = @indexname)
  ) > 0,
  "SELECT 'Index idx_schedule_date already exists' AS message",
  CONCAT("CREATE INDEX ", @indexname, " ON ", @tablename, " (schedule_date)")
));
PREPARE createIndexIfNotExists FROM @preparedStatement;
EXECUTE createIndexIfNotExists;
DEALLOCATE PREPARE createIndexIfNotExists;

-- Add foreign key
SET @constraint_exists = (
    SELECT COUNT(*) 
    FROM information_schema.TABLE_CONSTRAINTS 
    WHERE CONSTRAINT_SCHEMA = @dbname
    AND TABLE_NAME = @tablename
    AND CONSTRAINT_NAME = 'fk_rota_schedules_employee'
    AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);

SET @sql = IF(@constraint_exists = 0,
    CONCAT('ALTER TABLE ', @tablename, ' ADD CONSTRAINT fk_rota_schedules_employee FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE'),
    'SELECT "Foreign key constraint already exists" AS message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Verify fix
SELECT 
    'rota_schedules table structure:' AS status,
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE table_schema = 'employee_management_system'
  AND table_name = 'rota_schedules'
ORDER BY ORDINAL_POSITION;

SELECT '✅ rota_schedules table fixed successfully!' AS status;

