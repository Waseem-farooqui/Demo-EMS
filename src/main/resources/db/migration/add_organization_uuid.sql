-- Add organization_uuid column to organizations table
ALTER TABLE organizations
ADD COLUMN organization_uuid VARCHAR(36) UNIQUE;

-- Generate UUIDs for existing organizations
UPDATE organizations
SET organization_uuid = UUID()
WHERE organization_uuid IS NULL;

-- Make organization_uuid NOT NULL after populating
ALTER TABLE organizations
MODIFY COLUMN organization_uuid VARCHAR(36) NOT NULL;

-- Add index for faster lookups
CREATE INDEX idx_organizations_uuid ON organizations(organization_uuid);

-- Add organization_uuid to users table
ALTER TABLE users
ADD COLUMN organization_uuid VARCHAR(36);

-- Update existing users with their organization's UUID
UPDATE users u
INNER JOIN organizations o ON u.organization_id = o.id
SET u.organization_uuid = o.organization_uuid
WHERE u.organization_id IS NOT NULL;

-- Add index for faster lookups
CREATE INDEX idx_users_org_uuid ON users(organization_uuid);

-- Add organization_uuid to employees table
ALTER TABLE employees
ADD COLUMN organization_uuid VARCHAR(36);

-- Update existing employees with their organization's UUID
UPDATE employees e
INNER JOIN organizations o ON e.organization_id = o.id
SET e.organization_uuid = o.organization_uuid
WHERE e.organization_id IS NOT NULL;

-- Add index for faster lookups
CREATE INDEX idx_employees_org_uuid ON employees(organization_uuid);

-- Verify the migration
SELECT 'Organizations with UUID' as description, COUNT(*) as count
FROM organizations
WHERE organization_uuid IS NOT NULL
UNION ALL
SELECT 'Users with organization UUID', COUNT(*)
FROM users
WHERE organization_uuid IS NOT NULL
UNION ALL
SELECT 'Employees with organization UUID', COUNT(*)
FROM employees
WHERE organization_uuid IS NOT NULL;

