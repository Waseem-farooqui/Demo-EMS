-- ===================================================================
-- Fix rota_schedules table - Add missing columns
-- ===================================================================
-- This script adds the missing columns to the rota_schedules table
-- to match the RotaSchedule entity
-- Run this script to fix the "Field 'employee_id' doesn't have a default value" error
-- 
-- Usage: Run this script manually or via production-deploy.sh
-- ===================================================================

USE employee_management_system;

-- Add employee_id column (MySQL 8.0.19+ supports IF NOT EXISTS)
ALTER TABLE rota_schedules 
ADD COLUMN IF NOT EXISTS employee_id BIGINT NOT NULL AFTER rota_id;

-- Add employee_name column
ALTER TABLE rota_schedules 
ADD COLUMN IF NOT EXISTS employee_name VARCHAR(255) NOT NULL AFTER employee_id;

-- Add schedule_date column
ALTER TABLE rota_schedules 
ADD COLUMN IF NOT EXISTS schedule_date DATE NOT NULL AFTER employee_name;

-- Modify day_of_week from INT to VARCHAR (if it's still INT)
-- This will work even if it's already VARCHAR
ALTER TABLE rota_schedules 
MODIFY COLUMN day_of_week VARCHAR(50) NOT NULL;

-- Add duty column
ALTER TABLE rota_schedules 
ADD COLUMN IF NOT EXISTS duty VARCHAR(255) NOT NULL AFTER end_time;

-- Add is_off_day column
ALTER TABLE rota_schedules 
ADD COLUMN IF NOT EXISTS is_off_day BOOLEAN DEFAULT FALSE AFTER duty;

-- Add indexes (MySQL 8.0+ supports IF NOT EXISTS)
CREATE INDEX IF NOT EXISTS idx_employee_id ON rota_schedules(employee_id);
CREATE INDEX IF NOT EXISTS idx_schedule_date ON rota_schedules(schedule_date);

-- Add foreign key constraint for employee_id
-- Note: Check if constraint exists first to avoid error
SET @constraint_exists = (
    SELECT COUNT(*) 
    FROM information_schema.TABLE_CONSTRAINTS 
    WHERE CONSTRAINT_SCHEMA = 'employee_management_system'
    AND TABLE_NAME = 'rota_schedules'
    AND CONSTRAINT_NAME = 'fk_rota_schedules_employee'
    AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);

SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE rota_schedules ADD CONSTRAINT fk_rota_schedules_employee FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE',
    'SELECT "Foreign key constraint already exists" AS message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'rota_schedules table migration completed successfully!' AS status;
