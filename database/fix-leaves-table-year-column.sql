-- ===================================================================
-- Fix leaves table - Remove incorrect columns and fix types
-- ===================================================================
-- This script removes incorrect columns from the leaves table:
-- - Removes 'year' column (entity uses 'financial_year')
-- - Removes 'days_taken' column (entity uses 'number_of_days')
-- - Fixes 'financial_year' to VARCHAR(20) instead of VARCHAR(255)
-- ===================================================================

USE employee_management_system;

DELIMITER //

CREATE PROCEDURE IF NOT EXISTS fix_leaves_table()
BEGIN
    DECLARE table_exists INT DEFAULT 0;
    DECLARE column_exists INT DEFAULT 0;
    DECLARE column_type VARCHAR(100);
    
    -- Check if leaves table exists
    SELECT COUNT(*) INTO table_exists
    FROM INFORMATION_SCHEMA.TABLES 
    WHERE table_schema = 'employee_management_system' 
    AND table_name = 'leaves';
    
    IF table_exists > 0 THEN
        -- Drop 'year' column if it exists
        SELECT COUNT(*) INTO column_exists
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE table_schema = 'employee_management_system' 
        AND table_name = 'leaves' 
        AND column_name = 'year';
        
        IF column_exists > 0 THEN
            SET @sql = 'ALTER TABLE leaves DROP COLUMN `year`';
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;
        
        -- Drop 'days_taken' column if it exists (entity uses 'number_of_days')
        SELECT COUNT(*) INTO column_exists
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE table_schema = 'employee_management_system' 
        AND table_name = 'leaves' 
        AND column_name = 'days_taken';
        
        IF column_exists > 0 THEN
            SET @sql = 'ALTER TABLE leaves DROP COLUMN `days_taken`';
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;
        
        -- Ensure number_of_days exists and is INT NOT NULL
        SELECT COUNT(*) INTO column_exists
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE table_schema = 'employee_management_system' 
        AND table_name = 'leaves' 
        AND column_name = 'number_of_days';
        
        IF column_exists = 0 THEN
            SET @sql = 'ALTER TABLE leaves ADD COLUMN number_of_days INT NOT NULL';
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;
        
        -- Ensure financial_year exists and is VARCHAR(20) (not VARCHAR(255))
        SELECT COUNT(*) INTO column_exists
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE table_schema = 'employee_management_system' 
        AND table_name = 'leaves' 
        AND column_name = 'financial_year';
        
        IF column_exists = 0 THEN
            SET @sql = 'ALTER TABLE leaves ADD COLUMN financial_year VARCHAR(20) NULL';
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        ELSE
            -- Fix type if it's not VARCHAR(20)
            SELECT COLUMN_TYPE INTO column_type
            FROM INFORMATION_SCHEMA.COLUMNS 
            WHERE table_schema = 'employee_management_system' 
            AND table_name = 'leaves' 
            AND column_name = 'financial_year';
            
            IF column_type != 'varchar(20)' THEN
                SET @sql = 'ALTER TABLE leaves MODIFY COLUMN financial_year VARCHAR(20) NULL';
                PREPARE stmt FROM @sql;
                EXECUTE stmt;
                DEALLOCATE PREPARE stmt;
            END IF;
        END IF;
        
        -- Ensure organization_id exists
        SELECT COUNT(*) INTO column_exists
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE table_schema = 'employee_management_system' 
        AND table_name = 'leaves' 
        AND column_name = 'organization_id';
        
        IF column_exists = 0 THEN
            SET @sql = 'ALTER TABLE leaves ADD COLUMN organization_id BIGINT NULL';
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;
        
        SELECT 'leaves table structure fixed successfully' AS status;
    ELSE
        SELECT 'leaves table does not exist' AS status;
    END IF;
END //

DELIMITER ;

-- Execute the procedure
CALL fix_leaves_table();

-- Drop the procedure after use
DROP PROCEDURE IF EXISTS fix_leaves_table;

SELECT 'Fix complete! Please verify the table structure with: DESCRIBE leaves;' AS message;
