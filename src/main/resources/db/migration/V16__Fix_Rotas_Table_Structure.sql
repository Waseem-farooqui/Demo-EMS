-- ===================================================================
-- Flyway Migration: Fix rotas table structure
-- Version: V16
-- Description: Remove incorrect fields from rotas table that belong to rota_schedules
--              Ensure rotas table has only the correct fields
-- ===================================================================

USE employee_management_system;

-- ===================================================================
-- Fix rotas table - Remove incorrect fields
-- ===================================================================

-- Remove fields that don't belong to rotas table
-- These fields belong to rota_schedules or are unused

-- Remove employee_id (belongs to rota_schedules)
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
   WHERE table_schema = DATABASE() 
   AND table_name = 'rotas' 
   AND column_name = 'employee_id') > 0,
  'ALTER TABLE rotas DROP COLUMN employee_id',
  'SELECT "Column employee_id does not exist in rotas table" AS message'
));
PREPARE stmt FROM @preparedStatement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Remove week_start_date (unused)
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
   WHERE table_schema = DATABASE() 
   AND table_name = 'rotas' 
   AND column_name = 'week_start_date') > 0,
  'ALTER TABLE rotas DROP COLUMN week_start_date',
  'SELECT "Column week_start_date does not exist in rotas table" AS message'
));
PREPARE stmt FROM @preparedStatement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Remove week_end_date (unused)
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
   WHERE table_schema = DATABASE() 
   AND table_name = 'rotas' 
   AND column_name = 'week_end_date') > 0,
  'ALTER TABLE rotas DROP COLUMN week_end_date',
  'SELECT "Column week_end_date does not exist in rotas table" AS message'
));
PREPARE stmt FROM @preparedStatement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Remove status (unused)
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
   WHERE table_schema = DATABASE() 
   AND table_name = 'rotas' 
   AND column_name = 'status') > 0,
  'ALTER TABLE rotas DROP COLUMN status',
  'SELECT "Column status does not exist in rotas table" AS message'
));
PREPARE stmt FROM @preparedStatement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Remove created_by (unused - we have uploaded_by)
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
   WHERE table_schema = DATABASE() 
   AND table_name = 'rotas' 
   AND column_name = 'created_by') > 0,
  'ALTER TABLE rotas DROP COLUMN created_by',
  'SELECT "Column created_by does not exist in rotas table" AS message'
));
PREPARE stmt FROM @preparedStatement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Remove created_at (unused - we have uploaded_date)
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
   WHERE table_schema = DATABASE() 
   AND table_name = 'rotas' 
   AND column_name = 'created_at') > 0,
  'ALTER TABLE rotas DROP COLUMN created_at',
  'SELECT "Column created_at does not exist in rotas table" AS message'
));
PREPARE stmt FROM @preparedStatement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Remove updated_at (unused)
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
   WHERE table_schema = DATABASE() 
   AND table_name = 'rotas' 
   AND column_name = 'updated_at') > 0,
  'ALTER TABLE rotas DROP COLUMN updated_at',
  'SELECT "Column updated_at does not exist in rotas table" AS message'
));
PREPARE stmt FROM @preparedStatement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Ensure file_path is VARCHAR(500) not VARCHAR(255) to match entity
SET @preparedStatement = (SELECT IF(
  (SELECT CHARACTER_MAXIMUM_LENGTH FROM INFORMATION_SCHEMA.COLUMNS 
   WHERE table_schema = DATABASE() 
   AND table_name = 'rotas' 
   AND column_name = 'file_path' 
   AND CHARACTER_MAXIMUM_LENGTH < 500) IS NOT NULL,
  'ALTER TABLE rotas MODIFY COLUMN file_path VARCHAR(500) NOT NULL',
  'SELECT "Column file_path is already correct or does not exist" AS message'
));
PREPARE stmt FROM @preparedStatement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Ensure uploaded_date is DATETIME(6) to match entity (for microsecond precision)
SET @preparedStatement = (SELECT IF(
  (SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS 
   WHERE table_schema = DATABASE() 
   AND table_name = 'rotas' 
   AND column_name = 'uploaded_date' 
   AND DATA_TYPE = 'datetime' 
   AND DATETIME_PRECISION < 6) IS NOT NULL,
  'ALTER TABLE rotas MODIFY COLUMN uploaded_date DATETIME(6) NOT NULL',
  'SELECT "Column uploaded_date is already correct or does not exist" AS message'
));
PREPARE stmt FROM @preparedStatement;
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

