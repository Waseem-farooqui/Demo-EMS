-- ===================================================================
-- Fix leaves table - Remove incorrect 'year' column
-- ===================================================================
-- This script removes the incorrect 'year' column from the leaves table
-- The entity uses 'financial_year' not 'year'
-- Run this script to fix the "Field 'year' doesn't have a default value" error
-- ===================================================================

USE employee_management_system;

-- Check if 'year' column exists and remove it
SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE table_schema = 'employee_management_system' 
    AND table_name = 'leaves' 
    AND column_name = 'year'
);

SET @sql = IF(@column_exists > 0,
    'ALTER TABLE leaves DROP COLUMN `year`',
    'SELECT "year column does not exist in leaves table" AS status');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Ensure financial_year column exists (nullable)
SET @col_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE table_schema = 'employee_management_system' 
    AND table_name = 'leaves' 
    AND column_name = 'financial_year'
);

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE leaves ADD COLUMN financial_year VARCHAR(20) NULL',
    'SELECT "financial_year column already exists" AS status');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Ensure organization_id column exists
SET @col_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE table_schema = 'employee_management_system' 
    AND table_name = 'leaves' 
    AND column_name = 'organization_id'
);

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE leaves ADD COLUMN organization_id BIGINT NULL',
    'SELECT "organization_id column already exists" AS status');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'leaves table structure fixed' AS status;

