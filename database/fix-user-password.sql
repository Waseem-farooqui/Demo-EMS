-- Fix User Password Script
-- Run this if you get "Access denied" errors
-- This script resets the emsuser password to match your .env file

-- Note: Replace 'your_password_here' with the actual password from your .env file
-- Or use: ALTER USER 'emsuser'@'%' IDENTIFIED BY 'your_password_here';

-- First, ensure the user exists and has the correct password
ALTER USER IF EXISTS 'emsuser'@'%' IDENTIFIED BY 'emspassword';

-- If the user doesn't exist, create it
CREATE USER IF NOT EXISTS 'emsuser'@'%' IDENTIFIED BY 'emspassword';

-- Grant all privileges
GRANT ALL PRIVILEGES ON employee_management_system.* TO 'emsuser'@'%';
GRANT CREATE ON *.* TO 'emsuser'@'%';

-- Apply changes
FLUSH PRIVILEGES;

-- Verify
SELECT 'User emsuser password updated successfully' AS status;
SHOW GRANTS FOR 'emsuser'@'%';

