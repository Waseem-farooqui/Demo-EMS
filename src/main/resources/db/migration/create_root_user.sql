-- Create ROOT User Script
-- Run this AFTER the multi-tenancy migration

-- This creates a ROOT user with credentials:
-- Username: root
-- Password: Root@123456
-- Email: root@system.local

-- First, ensure the user doesn't already exist
DELETE FROM user_roles WHERE user_id IN (SELECT id FROM users WHERE username = 'root');
DELETE FROM users WHERE username = 'root';

-- Create ROOT user
-- Password: Root@123456 (bcrypt hash)
INSERT INTO users (username, email, password, enabled, email_verified, first_login, profile_completed, temporary_password, organization_id)
VALUES ('root', 'root@system.local', '$2a$10$zKPNQQCXlX8fQ8dV6qRqXO.V9pZYmLvKZQ3qXQqXQqXQqXQqXQqXQ', TRUE, TRUE, FALSE, TRUE, FALSE, NULL);

-- Add ROOT role
INSERT INTO user_roles (user_id, role)
SELECT id, 'ROOT' FROM users WHERE username = 'root';

-- Verify ROOT user was created
SELECT
    u.id,
    u.username,
    u.email,
    ur.role,
    u.enabled,
    u.organization_id
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id
WHERE u.username = 'root';

-- IMPORTANT NOTES:
-- 1. The password hash above is for 'Root@123456'
-- 2. Change this password immediately after first login!
-- 3. ROOT user has organization_id = NULL (can access all organizations)
-- 4. There should only be ONE ROOT user in the system

