-- ===================================================================
-- Flyway Migration: Fix rotas table structure
-- Version: V16
-- Description: Remove incorrect fields from rotas table that belong to rota_schedules
--              Ensure rotas table has only the correct fields
--              This migration is idempotent and safe to run multiple times
-- ===================================================================

-- Note: We don't use USE statement in Flyway migrations
-- Flyway automatically uses the database specified in the connection URL

-- ===================================================================
-- Fix rotas table - Remove incorrect fields
-- ===================================================================

-- Check if rotas table exists first
SET @table_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.TABLES 
    WHERE table_schema = DATABASE() 
    AND table_name = 'rotas'
);

-- Only proceed if table exists
SET @sql = IF(@table_exists > 0,
    'SELECT "rotas table exists, proceeding with cleanup" AS status',
    'SELECT "rotas table does not exist, skipping migration" AS status'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Remove fields that don't belong to rotas table (only if table exists)
-- These fields belong to rota_schedules or are unused

-- Remove employee_id (belongs to rota_schedules) - with error handling
-- First, we need to drop any foreign key constraints that reference this column
SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE table_schema = DATABASE() 
    AND table_name = 'rotas' 
    AND column_name = 'employee_id'
);

-- Check for foreign key constraints on employee_id
SET @fk_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE 
    WHERE table_schema = DATABASE() 
    AND table_name = 'rotas' 
    AND column_name = 'employee_id'
    AND referenced_table_name IS NOT NULL
);

-- Drop foreign key constraint if it exists
SET @fk_name = (
    SELECT CONSTRAINT_NAME 
    FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE 
    WHERE table_schema = DATABASE() 
    AND table_name = 'rotas' 
    AND column_name = 'employee_id'
    AND referenced_table_name IS NOT NULL
    LIMIT 1
);

SET @sql = IF(@table_exists > 0 AND @fk_exists > 0 AND @fk_name IS NOT NULL,
    CONCAT('ALTER TABLE rotas DROP FOREIGN KEY ', @fk_name),
    'SELECT "No foreign key constraint found on employee_id" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Also drop any index on employee_id
SET @index_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE table_schema = DATABASE() 
    AND table_name = 'rotas' 
    AND column_name = 'employee_id'
    AND index_name != 'PRIMARY'
);

SET @index_name = (
    SELECT DISTINCT index_name 
    FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE table_schema = DATABASE() 
    AND table_name = 'rotas' 
    AND column_name = 'employee_id'
    AND index_name != 'PRIMARY'
    LIMIT 1
);

SET @sql = IF(@table_exists > 0 AND @index_exists > 0 AND @index_name IS NOT NULL,
    CONCAT('ALTER TABLE rotas DROP INDEX ', @index_name),
    'SELECT "No index found on employee_id" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Now drop the column
SET @sql = IF(@table_exists > 0 AND @column_exists > 0,
    'ALTER TABLE rotas DROP COLUMN employee_id',
    'SELECT "Column employee_id does not exist or table does not exist" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Remove week_start_date (unused)
SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE table_schema = DATABASE() 
    AND table_name = 'rotas' 
    AND column_name = 'week_start_date'
);

SET @sql = IF(@table_exists > 0 AND @column_exists > 0,
    'ALTER TABLE rotas DROP COLUMN week_start_date',
    'SELECT "Column week_start_date does not exist or table does not exist" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Remove week_end_date (unused)
SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE table_schema = DATABASE() 
    AND table_name = 'rotas' 
    AND column_name = 'week_end_date'
);

SET @sql = IF(@table_exists > 0 AND @column_exists > 0,
    'ALTER TABLE rotas DROP COLUMN week_end_date',
    'SELECT "Column week_end_date does not exist or table does not exist" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Remove status (unused)
SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE table_schema = DATABASE() 
    AND table_name = 'rotas' 
    AND column_name = 'status'
);

SET @sql = IF(@table_exists > 0 AND @column_exists > 0,
    'ALTER TABLE rotas DROP COLUMN status',
    'SELECT "Column status does not exist or table does not exist" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Remove created_by (unused - we have uploaded_by)
-- First check for foreign key constraints
SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE table_schema = DATABASE() 
    AND table_name = 'rotas' 
    AND column_name = 'created_by'
);

SET @fk_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE 
    WHERE table_schema = DATABASE() 
    AND table_name = 'rotas' 
    AND column_name = 'created_by'
    AND referenced_table_name IS NOT NULL
);

SET @fk_name = (
    SELECT CONSTRAINT_NAME 
    FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE 
    WHERE table_schema = DATABASE() 
    AND table_name = 'rotas' 
    AND column_name = 'created_by'
    AND referenced_table_name IS NOT NULL
    LIMIT 1
);

-- Drop foreign key if exists
SET @sql = IF(@table_exists > 0 AND @fk_exists > 0 AND @fk_name IS NOT NULL,
    CONCAT('ALTER TABLE rotas DROP FOREIGN KEY ', @fk_name),
    'SELECT "No foreign key constraint found on created_by" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Now drop the column
SET @sql = IF(@table_exists > 0 AND @column_exists > 0,
    'ALTER TABLE rotas DROP COLUMN created_by',
    'SELECT "Column created_by does not exist or table does not exist" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Remove created_at (unused - we have uploaded_date)
SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE table_schema = DATABASE() 
    AND table_name = 'rotas' 
    AND column_name = 'created_at'
);

SET @sql = IF(@table_exists > 0 AND @column_exists > 0,
    'ALTER TABLE rotas DROP COLUMN created_at',
    'SELECT "Column created_at does not exist or table does not exist" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Remove updated_at (unused)
SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE table_schema = DATABASE() 
    AND table_name = 'rotas' 
    AND column_name = 'updated_at'
);

SET @sql = IF(@table_exists > 0 AND @column_exists > 0,
    'ALTER TABLE rotas DROP COLUMN updated_at',
    'SELECT "Column updated_at does not exist or table does not exist" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Ensure file_path is VARCHAR(500) not VARCHAR(255) to match entity
SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE table_schema = DATABASE() 
    AND table_name = 'rotas' 
    AND column_name = 'file_path'
);

SET @current_length = (
    SELECT CHARACTER_MAXIMUM_LENGTH 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE table_schema = DATABASE() 
    AND table_name = 'rotas' 
    AND column_name = 'file_path'
);

SET @sql = IF(@table_exists > 0 AND @column_exists > 0 AND (@current_length IS NULL OR @current_length < 500),
    'ALTER TABLE rotas MODIFY COLUMN file_path VARCHAR(500) NOT NULL',
    'SELECT "Column file_path is already correct or does not exist" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Ensure uploaded_date is DATETIME(6) to match entity (for microsecond precision)
SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE table_schema = DATABASE() 
    AND table_name = 'rotas' 
    AND column_name = 'uploaded_date'
);

SET @current_precision = (
    SELECT DATETIME_PRECISION 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE table_schema = DATABASE() 
    AND table_name = 'rotas' 
    AND column_name = 'uploaded_date'
);

SET @sql = IF(@table_exists > 0 AND @column_exists > 0 AND (@current_precision IS NULL OR @current_precision < 6),
    'ALTER TABLE rotas MODIFY COLUMN uploaded_date DATETIME(6) NOT NULL',
    'SELECT "Column uploaded_date is already correct or does not exist" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ===================================================================
-- Verify rotas table structure
-- ===================================================================

SELECT 
    'rotas table structure after cleanup:' AS status,
    COLUMN_NAME,
    DATA_TYPE,
    CHARACTER_MAXIMUM_LENGTH,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE table_schema = DATABASE()
  AND table_name = 'rotas'
ORDER BY ORDINAL_POSITION;

SELECT '✅ rotas table structure fixed successfully!' AS status;

