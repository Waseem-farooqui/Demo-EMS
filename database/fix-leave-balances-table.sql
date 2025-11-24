-- ===================================================================
-- Fix leave_balances table - Remove incorrect columns and fix types
-- ===================================================================
-- This script removes incorrect columns from the leave_balances table:
-- - Removes 'year' column (entity uses 'financial_year')
-- - Removes 'used' column (entity uses 'used_leaves')
-- - Removes 'remaining' column (entity uses 'remaining_leaves')
-- - Fixes column types: DECIMAL -> INT for numeric fields
-- - Ensures financial_year is VARCHAR(20) NOT NULL
-- ===================================================================

USE employee_management_system;

DELIMITER //

CREATE PROCEDURE IF NOT EXISTS fix_leave_balances_table()
BEGIN
    DECLARE table_exists INT DEFAULT 0;
    DECLARE column_exists INT DEFAULT 0;
    DECLARE column_type VARCHAR(100);
    
    -- Check if leave_balances table exists
    SELECT COUNT(*) INTO table_exists
    FROM INFORMATION_SCHEMA.TABLES 
    WHERE table_schema = 'employee_management_system' 
    AND table_name = 'leave_balances';
    
    IF table_exists > 0 THEN
        -- Drop 'year' column if it exists
        SELECT COUNT(*) INTO column_exists
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE table_schema = 'employee_management_system' 
        AND table_name = 'leave_balances' 
        AND column_name = 'year';
        
        IF column_exists > 0 THEN
            SET @sql = 'ALTER TABLE leave_balances DROP COLUMN `year`';
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
            SELECT 'Dropped year column' AS status;
        END IF;
        
        -- Drop 'used' column if it exists
        SELECT COUNT(*) INTO column_exists
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE table_schema = 'employee_management_system' 
        AND table_name = 'leave_balances' 
        AND column_name = 'used';
        
        IF column_exists > 0 THEN
            SET @sql = 'ALTER TABLE leave_balances DROP COLUMN `used`';
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
            SELECT 'Dropped used column' AS status;
        END IF;
        
        -- Drop 'remaining' column if it exists
        SELECT COUNT(*) INTO column_exists
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE table_schema = 'employee_management_system' 
        AND table_name = 'leave_balances' 
        AND column_name = 'remaining';
        
        IF column_exists > 0 THEN
            SET @sql = 'ALTER TABLE leave_balances DROP COLUMN `remaining`';
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
            SELECT 'Dropped remaining column' AS status;
        END IF;
        
        -- Ensure financial_year exists and is VARCHAR(20) NOT NULL
        SELECT COUNT(*) INTO column_exists
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE table_schema = 'employee_management_system' 
        AND table_name = 'leave_balances' 
        AND column_name = 'financial_year';
        
        IF column_exists = 0 THEN
            SET @sql = 'ALTER TABLE leave_balances ADD COLUMN financial_year VARCHAR(20) NOT NULL';
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
            SELECT 'Added financial_year column' AS status;
        ELSE
            -- Fix type if it's not VARCHAR(20)
            SELECT COLUMN_TYPE INTO column_type
            FROM INFORMATION_SCHEMA.COLUMNS 
            WHERE table_schema = 'employee_management_system' 
            AND table_name = 'leave_balances' 
            AND column_name = 'financial_year';
            
            IF column_type != 'varchar(20)' THEN
                SET @sql = 'ALTER TABLE leave_balances MODIFY COLUMN financial_year VARCHAR(20) NOT NULL';
                PREPARE stmt FROM @sql;
                EXECUTE stmt;
                DEALLOCATE PREPARE stmt;
                SELECT CONCAT('Fixed financial_year type from ', column_type, ' to VARCHAR(20)') AS status;
            END IF;
        END IF;
        
        -- Ensure total_allocated is INT (not DECIMAL)
        SELECT COLUMN_TYPE INTO column_type
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE table_schema = 'employee_management_system' 
        AND table_name = 'leave_balances' 
        AND column_name = 'total_allocated';
        
        IF column_type IS NOT NULL AND column_type NOT LIKE '%int%' THEN
            SET @sql = 'ALTER TABLE leave_balances MODIFY COLUMN total_allocated INT NOT NULL DEFAULT 0';
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
            SELECT CONCAT('Fixed total_allocated type from ', column_type, ' to INT') AS status;
        END IF;
        
        -- Ensure used_leaves is INT (not DECIMAL)
        SELECT COLUMN_TYPE INTO column_type
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE table_schema = 'employee_management_system' 
        AND table_name = 'leave_balances' 
        AND column_name = 'used_leaves';
        
        IF column_type IS NOT NULL AND column_type NOT LIKE '%int%' THEN
            SET @sql = 'ALTER TABLE leave_balances MODIFY COLUMN used_leaves INT NOT NULL DEFAULT 0';
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
            SELECT CONCAT('Fixed used_leaves type from ', column_type, ' to INT') AS status;
        END IF;
        
        -- Ensure remaining_leaves is INT (not DECIMAL)
        SELECT COLUMN_TYPE INTO column_type
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE table_schema = 'employee_management_system' 
        AND table_name = 'leave_balances' 
        AND column_name = 'remaining_leaves';
        
        IF column_type IS NOT NULL AND column_type NOT LIKE '%int%' THEN
            SET @sql = 'ALTER TABLE leave_balances MODIFY COLUMN remaining_leaves INT NOT NULL DEFAULT 0';
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
            SELECT CONCAT('Fixed remaining_leaves type from ', column_type, ' to INT') AS status;
        END IF;
        
        SELECT 'leave_balances table structure fixed successfully' AS status;
    ELSE
        SELECT 'leave_balances table does not exist' AS status;
    END IF;
END //

DELIMITER ;

-- Execute the procedure
CALL fix_leave_balances_table();

-- Drop the procedure after use
DROP PROCEDURE IF EXISTS fix_leave_balances_table;

SELECT 'Fix complete! Please verify the table structure with: DESCRIBE leave_balances;' AS message;

