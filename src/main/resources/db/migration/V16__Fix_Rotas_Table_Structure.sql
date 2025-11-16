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
SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE table_schema = DATABASE() 
    AND table_name = 'rotas' 
    AND column_name = 'employee_id'
);

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
SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE table_schema = DATABASE() 
    AND table_name = 'rotas' 
    AND column_name = 'created_by'
);

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

