-- ===================================================================
-- Fix leaves table - Remove incorrect columns and fix types
-- ===================================================================
-- This script fixes the leaves table structure to match the Leave entity:
-- - Removes 'days_taken' column (entity uses 'number_of_days')
-- - Removes 'year' column if exists (entity uses 'financial_year')
-- - Removes 'approved_date' (DATETIME) if exists (entity uses 'approval_date' DATE)
-- - Fixes 'financial_year' from VARCHAR(255) to VARCHAR(20)
-- - Fixes 'approved_by' from BIGINT to VARCHAR(255)
-- - Ensures 'number_of_days' exists and is INT
-- - Ensures 'approval_date' exists and is DATE
-- ===================================================================

USE employee_management_system;

DELIMITER //

CREATE PROCEDURE IF NOT EXISTS fix_leaves_table_complete()
BEGIN
    DECLARE table_exists INT DEFAULT 0;
    DECLARE column_exists INT DEFAULT 0;
    DECLARE column_type VARCHAR(100);
    DECLARE data_type VARCHAR(50);
    
    -- Check if leaves table exists
    SELECT COUNT(*) INTO table_exists
    FROM INFORMATION_SCHEMA.TABLES 
    WHERE table_schema = 'employee_management_system' 
    AND table_name = 'leaves';
    
    IF table_exists > 0 THEN
        SELECT 'Starting leaves table fix...' AS status;
        
        -- ===================================================================
        -- 1. Remove 'days_taken' column (entity uses 'number_of_days')
        -- ===================================================================
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
            SELECT '✓ Dropped days_taken column' AS status;
        ELSE
            SELECT 'days_taken column does not exist' AS status;
        END IF;
        
        -- ===================================================================
        -- 2. Remove 'year' column if exists (entity uses 'financial_year')
        -- ===================================================================
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
            SELECT '✓ Dropped year column' AS status;
        END IF;
        
        -- ===================================================================
        -- 3. Remove 'approved_date' (DATETIME) if exists (entity uses 'approval_date' DATE)
        -- ===================================================================
        SELECT COUNT(*) INTO column_exists
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE table_schema = 'employee_management_system' 
        AND table_name = 'leaves' 
        AND column_name = 'approved_date';
        
        IF column_exists > 0 THEN
            -- Check if it's DATETIME type
            SELECT DATA_TYPE INTO data_type
            FROM INFORMATION_SCHEMA.COLUMNS 
            WHERE table_schema = 'employee_management_system' 
            AND table_name = 'leaves' 
            AND column_name = 'approved_date';
            
            -- Only drop if it's DATETIME, keep if it's DATE (as approval_date)
            IF data_type = 'datetime' THEN
                SET @sql = 'ALTER TABLE leaves DROP COLUMN `approved_date`';
                PREPARE stmt FROM @sql;
                EXECUTE stmt;
                DEALLOCATE PREPARE stmt;
                SELECT '✓ Dropped approved_date (DATETIME) column' AS status;
            END IF;
        END IF;
        
        -- ===================================================================
        -- 4. Ensure 'number_of_days' exists and is INT NOT NULL
        -- ===================================================================
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
            SELECT '✓ Added number_of_days column' AS status;
        ELSE
            -- Check if type is correct (should be INT)
            SELECT DATA_TYPE INTO data_type
            FROM INFORMATION_SCHEMA.COLUMNS 
            WHERE table_schema = 'employee_management_system' 
            AND table_name = 'leaves' 
            AND column_name = 'number_of_days';
            
            IF data_type != 'int' THEN
                SET @sql = 'ALTER TABLE leaves MODIFY COLUMN number_of_days INT NOT NULL';
                PREPARE stmt FROM @sql;
                EXECUTE stmt;
                DEALLOCATE PREPARE stmt;
                SELECT CONCAT('✓ Fixed number_of_days type to INT (was ', data_type, ')') AS status;
            END IF;
        END IF;
        
        -- ===================================================================
        -- 5. Fix 'financial_year' to VARCHAR(20) (not VARCHAR(255))
        -- ===================================================================
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
            SELECT '✓ Added financial_year column' AS status;
        ELSE
            -- Check if type is correct (should be VARCHAR(20))
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
                SELECT CONCAT('✓ Fixed financial_year type to VARCHAR(20) (was ', column_type, ')') AS status;
            END IF;
        END IF;
        
        -- ===================================================================
        -- 6. Fix 'approved_by' to VARCHAR(255) (not BIGINT)
        -- ===================================================================
        SELECT COUNT(*) INTO column_exists
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE table_schema = 'employee_management_system' 
        AND table_name = 'leaves' 
        AND column_name = 'approved_by';
        
        IF column_exists > 0 THEN
            SELECT DATA_TYPE INTO data_type
            FROM INFORMATION_SCHEMA.COLUMNS 
            WHERE table_schema = 'employee_management_system' 
            AND table_name = 'leaves' 
            AND column_name = 'approved_by';
            
            IF data_type = 'bigint' THEN
                SET @sql = 'ALTER TABLE leaves MODIFY COLUMN approved_by VARCHAR(255) NULL';
                PREPARE stmt FROM @sql;
                EXECUTE stmt;
                DEALLOCATE PREPARE stmt;
                SELECT '✓ Fixed approved_by type to VARCHAR(255) (was BIGINT)' AS status;
            END IF;
        END IF;
        
        -- ===================================================================
        -- 7. Ensure 'approval_date' exists and is DATE (not DATETIME)
        -- ===================================================================
        SELECT COUNT(*) INTO column_exists
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE table_schema = 'employee_management_system' 
        AND table_name = 'leaves' 
        AND column_name = 'approval_date';
        
        IF column_exists = 0 THEN
            SET @sql = 'ALTER TABLE leaves ADD COLUMN approval_date DATE NULL';
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
            SELECT '✓ Added approval_date column' AS status;
        ELSE
            -- Check if type is correct (should be DATE)
            SELECT DATA_TYPE INTO data_type
            FROM INFORMATION_SCHEMA.COLUMNS 
            WHERE table_schema = 'employee_management_system' 
            AND table_name = 'leaves' 
            AND column_name = 'approval_date';
            
            IF data_type != 'date' THEN
                SET @sql = 'ALTER TABLE leaves MODIFY COLUMN approval_date DATE NULL';
                PREPARE stmt FROM @sql;
                EXECUTE stmt;
                DEALLOCATE PREPARE stmt;
                SELECT CONCAT('✓ Fixed approval_date type to DATE (was ', data_type, ')') AS status;
            END IF;
        END IF;
        
        -- ===================================================================
        -- 8. Ensure 'organization_id' exists
        -- ===================================================================
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
            SELECT '✓ Added organization_id column' AS status;
        END IF;
        
        SELECT '✅ leaves table structure fixed successfully!' AS final_status;
        
    ELSE
        SELECT 'leaves table does not exist' AS status;
    END IF;
END //

DELIMITER ;

-- Execute the procedure
CALL fix_leaves_table_complete();

-- Drop the procedure after use
DROP PROCEDURE IF EXISTS fix_leaves_table_complete;

SELECT 'Fix complete! Please verify with: DESCRIBE leaves;' AS message;

