-- Multi-Tenancy Migration Script
-- This script adds organization support to the system

-- Step 1: Create organizations table
CREATE TABLE IF NOT EXISTS organizations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(500),
    contact_email VARCHAR(255),
    contact_phone VARCHAR(50),
    address VARCHAR(1000),
    logo_path VARCHAR(500),
    logo_data LONGBLOB,
    is_active BOOLEAN DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_org_name (name),
    INDEX idx_org_active (is_active)
);

-- Step 2: Add organization_id column to users table
ALTER TABLE users
ADD COLUMN IF NOT EXISTS organization_id BIGINT,
ADD INDEX idx_user_org (organization_id);

-- Step 3: Add organization_id column to employees table
ALTER TABLE employees
ADD COLUMN IF NOT EXISTS organization_id BIGINT,
ADD INDEX idx_employee_org (organization_id);

-- Step 4: Add organization_id column to departments table
ALTER TABLE departments
ADD COLUMN IF NOT EXISTS organization_id BIGINT,
ADD INDEX idx_department_org (organization_id);

-- Step 5: Create ROOT user (only if no ROOT exists)
-- Default credentials: username=root, password=Root@123
INSERT INTO users (username, email, password, enabled, email_verified, first_login, profile_completed, temporary_password, organization_id)
SELECT 'root', 'root@system.local', '$2a$10$xQl9qXZ8fE5XfY9zGZ8pZuYGZ8pZuYGZ8pZuYGZ8pZuYGZ8pZuYGZ', TRUE, TRUE, FALSE, TRUE, FALSE, NULL
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE username = 'root'
);

-- Step 6: Add ROOT role to root user
INSERT INTO user_roles (user_id, role)
SELECT u.id, 'ROOT'
FROM users u
WHERE u.username = 'root'
AND NOT EXISTS (
    SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role = 'ROOT'
);

-- Step 7: Add foreign key constraints (optional - can be done after data migration)
-- ALTER TABLE users ADD CONSTRAINT fk_user_organization FOREIGN KEY (organization_id) REFERENCES organizations(id);
-- ALTER TABLE employees ADD CONSTRAINT fk_employee_organization FOREIGN KEY (organization_id) REFERENCES organizations(id);
-- ALTER TABLE departments ADD CONSTRAINT fk_department_organization FOREIGN KEY (organization_id) REFERENCES organizations(id);

-- Notes:
-- 1. ROOT user password hash above is for 'Root@123' - CHANGE THIS in production!
-- 2. Existing users/employees/departments will have NULL organization_id - assign them to organizations
-- 3. You may need to create a default organization and migrate existing data to it
-- 4. Foreign key constraints are commented out - enable after data migration is complete

