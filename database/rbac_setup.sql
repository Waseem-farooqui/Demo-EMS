-- ============================================
-- RBAC Setup Script - Create SUPER_ADMIN & Departments
-- ============================================

-- Step 1: Create SUPER_ADMIN User
-- Password: Admin@123 (BCrypt encoded)
INSERT INTO users (username, email, password, enabled, email_verified, first_login, profile_completed, temporary_password)
VALUES ('superadmin', 'superadmin@company.com', '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KsnmG', true, true, false, true, false);

-- Step 2: Assign SUPER_ADMIN role
INSERT INTO user_roles (user_id, role)
VALUES ((SELECT id FROM users WHERE username = 'superadmin'), 'SUPER_ADMIN');

-- ============================================
-- Create Sample Departments
-- ============================================

-- IT Department
INSERT INTO departments (name, description, code, is_active, created_at, updated_at)
VALUES ('Information Technology', 'IT Department - Software Development and Infrastructure', 'IT', true, NOW(), NOW());

-- HR Department
INSERT INTO departments (name, description, code, is_active, created_at, updated_at)
VALUES ('Human Resources', 'HR Department - Recruitment and Employee Management', 'HR', true, NOW(), NOW());

-- Finance Department
INSERT INTO departments (name, description, code, is_active, created_at, updated_at)
VALUES ('Finance', 'Finance Department - Accounting and Financial Planning', 'FIN', true, NOW(), NOW());

-- Operations Department
INSERT INTO departments (name, description, code, is_active, created_at, updated_at)
VALUES ('Operations', 'Operations Department - Business Operations', 'OPS', true, NOW(), NOW());

-- Marketing Department
INSERT INTO departments (name, description, code, is_active, created_at, updated_at)
VALUES ('Marketing', 'Marketing Department - Brand and Customer Engagement', 'MKT', true, NOW(), NOW());

-- ============================================
-- Sample: Create Department Manager (ADMIN)
-- ============================================

-- Create IT Manager Employee Profile
INSERT INTO employees (full_name, person_type, work_email, job_title, date_of_joining, working_timing, holiday_allowance, department_id)
VALUES ('John Doe', 'Employee', 'john.doe@company.com', 'IT Manager', '2025-01-01', '9:00 AM - 6:00 PM', 20,
        (SELECT id FROM departments WHERE code = 'IT'));

-- Create User Account for IT Manager
INSERT INTO users (username, email, password, enabled, email_verified, first_login, profile_completed, temporary_password)
VALUES ('johndoe', 'john.doe@company.com', '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KsnmG', true, true, false, true, false);

-- Assign ADMIN role
INSERT INTO user_roles (user_id, role)
VALUES ((SELECT id FROM users WHERE username = 'johndoe'), 'ADMIN');

-- Link employee to user
UPDATE employees
SET user_id = (SELECT id FROM users WHERE username = 'johndoe')
WHERE work_email = 'john.doe@company.com';

-- Set as department manager
UPDATE departments
SET manager_id = (SELECT id FROM employees WHERE work_email = 'john.doe@company.com')
WHERE code = 'IT';

-- ============================================
-- Sample: Create Regular User
-- ============================================

-- Create Regular Employee in IT Department
INSERT INTO employees (full_name, person_type, work_email, job_title, date_of_joining, working_timing, holiday_allowance, department_id)
VALUES ('Alice Smith', 'Employee', 'alice.smith@company.com', 'Software Developer', '2025-01-15', '9:00 AM - 6:00 PM', 20,
        (SELECT id FROM departments WHERE code = 'IT'));

-- Create User Account
INSERT INTO users (username, email, password, enabled, email_verified, first_login, profile_completed, temporary_password)
VALUES ('alicesmith', 'alice.smith@company.com', '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KsnmG', true, true, false, true, false);

-- Assign USER role (default)
INSERT INTO user_roles (user_id, role)
VALUES ((SELECT id FROM users WHERE username = 'alicesmith'), 'USER');

-- Link employee to user
UPDATE employees
SET user_id = (SELECT id FROM users WHERE username = 'alicesmith')
WHERE work_email = 'alice.smith@company.com';

-- ============================================
-- CREDENTIALS SUMMARY
-- ============================================

/*
SUPER_ADMIN:
  Username: superadmin
  Password: Admin@123
  Access: ALL departments, ALL employees

ADMIN (IT Manager):
  Username: johndoe
  Password: Admin@123
  Access: IT Department employees only

USER (Developer):
  Username: alicesmith
  Password: Admin@123
  Access: Own profile only

Note: Change passwords after first login!
*/

-- ============================================
-- Verification Queries
-- ============================================

-- Check users and roles
SELECT u.id, u.username, u.email, r.role
FROM users u
LEFT JOIN user_roles r ON u.id = r.user_id
ORDER BY u.id;

-- Check departments
SELECT d.id, d.name, d.code, e.full_name as manager_name,
       (SELECT COUNT(*) FROM employees WHERE department_id = d.id) as employee_count
FROM departments d
LEFT JOIN employees e ON d.manager_id = e.id
ORDER BY d.id;

-- Check employees with departments
SELECT e.id, e.full_name, e.job_title, d.name as department, u.username
FROM employees e
LEFT JOIN departments d ON e.department_id = d.id
LEFT JOIN users u ON e.user_id = u.id
ORDER BY e.id;

