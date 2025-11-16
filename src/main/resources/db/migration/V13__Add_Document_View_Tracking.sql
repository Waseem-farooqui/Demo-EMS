-- ===================================================================
-- Flyway Migration: Add Document View Tracking
-- Version: V13
-- Description: Adds document view tracking columns to documents table
--              This migration is idempotent - safe to run multiple times
-- ===================================================================

-- Add document view tracking columns (idempotent)
SET @dbname = DATABASE();
SET @tablename = 'documents';

-- Add last_viewed_at column if it doesn't exist
SET @columnname = 'last_viewed_at';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE (table_name = @tablename) AND (table_schema = @dbname) AND (column_name = @columnname)
  ) > 0,
  "SELECT 'Column last_viewed_at already exists.' AS message",
  CONCAT("ALTER TABLE ", @tablename, " ADD COLUMN ", @columnname, " DATETIME NULL")
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Add last_viewed_by column if it doesn't exist
SET @columnname = 'last_viewed_by';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE (table_name = @tablename) AND (table_schema = @dbname) AND (column_name = @columnname)
  ) > 0,
  "SELECT 'Column last_viewed_by already exists.' AS message",
  CONCAT("ALTER TABLE ", @tablename, " ADD COLUMN ", @columnname, " VARCHAR(255) NULL")
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Add indexes if they don't exist
SET @indexname = 'idx_documents_last_viewed';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE (table_name = @tablename) AND (table_schema = @dbname) AND (index_name = @indexname)
  ) > 0,
  "SELECT 'Index idx_documents_last_viewed already exists.' AS message",
  CONCAT("CREATE INDEX ", @indexname, " ON ", @tablename, " (last_viewed_at)")
));
PREPARE createIndexIfNotExists FROM @preparedStatement;
EXECUTE createIndexIfNotExists;
DEALLOCATE PREPARE createIndexIfNotExists;

SET @indexname = 'idx_documents_last_viewed_by';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE (table_name = @tablename) AND (table_schema = @dbname) AND (index_name = @indexname)
  ) > 0,
  "SELECT 'Index idx_documents_last_viewed_by already exists.' AS message",
  CONCAT("CREATE INDEX ", @indexname, " ON ", @tablename, " (last_viewed_by)")
));
PREPARE createIndexIfNotExists FROM @preparedStatement;
EXECUTE createIndexIfNotExists;
DEALLOCATE PREPARE createIndexIfNotExists;

