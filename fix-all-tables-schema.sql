-- ===================================================================
-- Fix All Tables Schema - Complete Schema Alignment
-- ===================================================================
-- This script fixes all schema mismatches between entities and database
-- Run this during production deployment to ensure consistency
-- ===================================================================

USE employee_management_system;

-- ===================================================================
-- 1. Fix employees table - Add department_id column if missing
-- ===================================================================
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns 
    WHERE table_schema = 'employee_management_system' 
    AND table_name = 'employees' 
    AND column_name = 'department_id');

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE employees ADD COLUMN department_id BIGINT NULL AFTER organization_uuid, ADD INDEX idx_department_id (department_id), ADD FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL',
    'SELECT "department_id column already exists" AS status');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ===================================================================
-- 2. Fix leave_balances table - Align column names and types
-- ===================================================================
-- Check if table has old schema (year, total_allocated DECIMAL)
SET @has_old_schema = (SELECT COUNT(*) FROM information_schema.columns 
    WHERE table_schema = 'employee_management_system' 
    AND table_name = 'leave_balances' 
    AND column_name = 'year');

SET @sql = IF(@has_old_schema > 0,
    'ALTER TABLE leave_balances 
     CHANGE COLUMN year financial_year VARCHAR(20) NOT NULL,
     CHANGE COLUMN total_allocated total_allocated INT NOT NULL DEFAULT 0,
     CHANGE COLUMN used used_leaves INT NOT NULL DEFAULT 0,
     CHANGE COLUMN remaining remaining_leaves INT NOT NULL DEFAULT 0,
     DROP INDEX IF EXISTS uk_employee_leave_year,
     ADD UNIQUE KEY uk_employee_leave_year (employee_id, financial_year, leave_type)',
    'SELECT "leave_balances table already has correct schema" AS status');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ===================================================================
-- 3. Fix leaves table - Align column names and add missing columns
-- ===================================================================
-- Check if days_taken exists (old schema)
SET @has_days_taken = (SELECT COUNT(*) FROM information_schema.columns 
    WHERE table_schema = 'employee_management_system' 
    AND table_name = 'leaves' 
    AND column_name = 'days_taken');

SET @sql = IF(@has_days_taken > 0,
    'ALTER TABLE leaves 
     CHANGE COLUMN days_taken number_of_days INT NOT NULL,
     CHANGE COLUMN approved_by approved_by VARCHAR(255) NULL',
    'SELECT "leaves table already has correct schema" AS status');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add rejection_reason if missing
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns 
    WHERE table_schema = 'employee_management_system' 
    AND table_name = 'leaves' 
    AND column_name = 'rejection_reason');

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE leaves ADD COLUMN rejection_reason TEXT NULL AFTER approval_date',
    'SELECT "rejection_reason column already exists" AS status');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ===================================================================
-- 4. Fix attendance table - Align column names
-- ===================================================================
-- Check if date column exists (old schema)
SET @has_date_col = (SELECT COUNT(*) FROM information_schema.columns 
    WHERE table_schema = 'employee_management_system' 
    AND table_name = 'attendance' 
    AND column_name = 'date');

SET @sql = IF(@has_date_col > 0,
    'ALTER TABLE attendance 
     CHANGE COLUMN date work_date DATE NOT NULL,
     CHANGE COLUMN check_in_time check_in_time DATETIME NOT NULL,
     CHANGE COLUMN check_out_time check_out_time DATETIME NULL,
     CHANGE COLUMN status work_location VARCHAR(50) NOT NULL,
     ADD COLUMN is_active BOOLEAN DEFAULT TRUE AFTER notes,
     ADD COLUMN created_at DATETIME NULL AFTER is_active,
     ADD COLUMN updated_at DATETIME NULL AFTER created_at,
     DROP INDEX IF EXISTS uk_employee_date,
     ADD UNIQUE KEY uk_employee_date (employee_id, work_date)',
    'SELECT "attendance table already has correct schema" AS status');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ===================================================================
-- 5. Fix notifications table - Align message length
-- ===================================================================
-- Check if message is TEXT (should be VARCHAR(500))
SET @msg_type = (SELECT DATA_TYPE FROM information_schema.columns 
    WHERE table_schema = 'employee_management_system' 
    AND table_name = 'notifications' 
    AND column_name = 'message');

SET @sql = IF(@msg_type = 'text',
    'ALTER TABLE notifications MODIFY COLUMN message VARCHAR(500) NOT NULL',
    'SELECT "notifications.message already has correct type" AS status');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add reference_id and reference_type if missing
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns 
    WHERE table_schema = 'employee_management_system' 
    AND table_name = 'notifications' 
    AND column_name = 'reference_id');

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE notifications 
     ADD COLUMN reference_id BIGINT NULL AFTER message,
     ADD COLUMN reference_type VARCHAR(50) NULL AFTER reference_id',
    'SELECT "reference_id and reference_type columns already exist" AS status');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ===================================================================
-- 6. Fix rotas table - Add missing columns
-- ===================================================================
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns 
    WHERE table_schema = 'employee_management_system' 
    AND table_name = 'rotas' 
    AND column_name = 'hotel_name');

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE rotas 
     ADD COLUMN hotel_name VARCHAR(255) NOT NULL AFTER id,
     ADD COLUMN department VARCHAR(255) NOT NULL AFTER hotel_name,
     ADD COLUMN file_name VARCHAR(255) NOT NULL AFTER department,
     ADD COLUMN file_path VARCHAR(500) NOT NULL AFTER file_name,
     ADD COLUMN file_data LONGBLOB NULL AFTER file_path,
     ADD COLUMN extracted_text TEXT NULL AFTER file_data,
     ADD COLUMN uploaded_by BIGINT NOT NULL AFTER uploaded_date,
     ADD COLUMN uploaded_by_name VARCHAR(255) NOT NULL AFTER uploaded_by,
     DROP COLUMN IF EXISTS status,
     DROP COLUMN IF EXISTS created_by,
     DROP COLUMN IF EXISTS created_at,
     DROP COLUMN IF EXISTS updated_at,
     DROP COLUMN IF EXISTS week_start_date,
     DROP COLUMN IF EXISTS week_end_date',
    'SELECT "rotas table already has correct schema" AS status');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ===================================================================
-- 7. Add missing indexes for performance
-- ===================================================================

-- Documents table indexes
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics 
    WHERE table_schema = 'employee_management_system' 
    AND table_name = 'documents' 
    AND index_name = 'idx_org_id');

SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE documents ADD INDEX idx_org_id (employee_id)',
    'SELECT "idx_org_id already exists on documents" AS status');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics 
    WHERE table_schema = 'employee_management_system' 
    AND table_name = 'documents' 
    AND index_name = 'idx_uploaded_date');

SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE documents ADD INDEX idx_uploaded_date (uploaded_date)',
    'SELECT "idx_uploaded_date already exists on documents" AS status');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics 
    WHERE table_schema = 'employee_management_system' 
    AND table_name = 'documents' 
    AND index_name = 'idx_file_hash');

SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE documents ADD INDEX idx_file_hash (file_hash)',
    'SELECT "idx_file_hash already exists on documents" AS status');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Leaves table indexes
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics 
    WHERE table_schema = 'employee_management_system' 
    AND table_name = 'leaves' 
    AND index_name = 'idx_applied_date');

SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE leaves ADD INDEX idx_applied_date (applied_date)',
    'SELECT "idx_applied_date already exists on leaves" AS status');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Attendance table indexes
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics 
    WHERE table_schema = 'employee_management_system' 
    AND table_name = 'attendance' 
    AND index_name = 'idx_work_date');

SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE attendance ADD INDEX idx_work_date (work_date)',
    'SELECT "idx_work_date already exists on attendance" AS status');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Notifications table indexes
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics 
    WHERE table_schema = 'employee_management_system' 
    AND table_name = 'notifications' 
    AND index_name = 'idx_reference_id');

SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE notifications ADD INDEX idx_reference_id (reference_id)',
    'SELECT "idx_reference_id already exists on notifications" AS status');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ===================================================================
-- 8. Fix documents table - Change preview_image from BLOB to MEDIUMBLOB
-- ===================================================================
-- BLOB has 64KB limit, but preview images can be up to 200KB
-- MEDIUMBLOB supports up to 16MB, providing safety margin
SET @col_type = (SELECT DATA_TYPE FROM information_schema.columns 
    WHERE table_schema = 'employee_management_system' 
    AND table_name = 'documents' 
    AND column_name = 'preview_image');

SET @sql = IF(@col_type = 'blob',
    'ALTER TABLE documents MODIFY COLUMN preview_image MEDIUMBLOB',
    'SELECT "preview_image already has correct type (MEDIUMBLOB or not exists)" AS status');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'All table schema fixes completed successfully' AS status;

