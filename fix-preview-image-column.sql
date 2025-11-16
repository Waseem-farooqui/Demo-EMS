-- ===================================================================
-- Fix preview_image Column - Change from BLOB to MEDIUMBLOB
-- ===================================================================
-- BLOB has 64KB limit, but preview images can be up to 200KB
-- MEDIUMBLOB supports up to 16MB, providing safety margin
-- ===================================================================

USE employee_management_system;

-- Check current column type
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    COLUMN_TYPE
FROM information_schema.columns 
WHERE table_schema = 'employee_management_system' 
AND table_name = 'documents' 
AND column_name = 'preview_image';

-- Change column type from BLOB to MEDIUMBLOB
ALTER TABLE documents MODIFY COLUMN preview_image MEDIUMBLOB;

-- Verify the change
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    COLUMN_TYPE
FROM information_schema.columns 
WHERE table_schema = 'employee_management_system' 
AND table_name = 'documents' 
AND column_name = 'preview_image';

SELECT 'preview_image column changed to MEDIUMBLOB successfully' AS status;

