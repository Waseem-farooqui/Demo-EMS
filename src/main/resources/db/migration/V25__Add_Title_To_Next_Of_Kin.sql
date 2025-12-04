-- Migration: Add title column to next_of_kin table
-- Description: Adds a title field (Mr, Mrs, Miss, etc.) to next of kin entries,
--              matching the same functionality as in previous employment and personal information sections.
--              Also creates the table if it doesn't exist (for cases where JPA didn't create it).

-- Create table if it doesn't exist
CREATE TABLE IF NOT EXISTS next_of_kin (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(50) NULL COMMENT 'Title: Mr, Mrs, Miss, Ms, Dr, Prof, etc.',
    name VARCHAR(255) NOT NULL,
    contact VARCHAR(255),
    address TEXT,
    relationship VARCHAR(255),
    employee_id BIGINT NOT NULL,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    INDEX idx_employee_id (employee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add title column if table exists but column doesn't (for existing tables)
-- This handles the case where the table was created by JPA without the title column
SET @column_exists = (
    SELECT COUNT(*) 
    FROM information_schema.columns 
    WHERE table_schema = DATABASE() 
      AND table_name = 'next_of_kin' 
      AND column_name = 'title'
);

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE next_of_kin ADD COLUMN title VARCHAR(50) NULL COMMENT ''Title: Mr, Mrs, Miss, Ms, Dr, Prof, etc.''',
    'SELECT ''Column title already exists, skipping'' AS message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

