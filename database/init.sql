-- ===================================================================
-- Employee Management System - Database Initialization Script
-- ===================================================================
-- This script is executed automatically when MySQL container starts
-- It creates the database and ensures proper character set
-- Tables will be created by JPA/Hibernate with ddl-auto=update
-- ===================================================================

-- Create database if it doesn't exist
CREATE DATABASE IF NOT EXISTS employee_management_system
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Use the database
USE employee_management_system;

-- Note: Tables are created automatically by Spring Boot JPA
-- with ddl-auto=update configuration
-- 
-- If you need to create tables manually, uncomment the sections below
-- or use Flyway migrations instead

-- ===================================================================
-- Core Tables (Created by JPA automatically)
-- ===================================================================
-- The following tables are created by JPA entities:
-- 
-- 1. organizations
-- 2. users
-- 3. user_roles
-- 4. employees
-- 5. departments
-- 6. documents
-- 7. leaves
-- 8. leave_balances
-- 9. attendance
-- 10. rotas
-- 11. rota_schedules
-- 12. rota_change_logs
-- 13. notifications
-- 14. alert_configurations
-- 15. verification_tokens
--
-- If JPA fails to create tables, ensure:
-- - spring.jpa.hibernate.ddl-auto=update (in application-prod.properties)
-- - Database connection is working
-- - User has CREATE TABLE privileges
-- ===================================================================

-- Grant privileges to the application user (created by Docker)
-- This is handled by MySQL's initialization process

-- Verify database creation
SELECT 'Database employee_management_system created successfully' AS status;

