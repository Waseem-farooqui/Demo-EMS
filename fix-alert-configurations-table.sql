-- ===================================================================
-- Fix Alert Configurations Table Schema
-- ===================================================================
-- This script fixes the alert_configurations table to match the entity
-- Run this if you get "Field 'alert_type' doesn't have a default value" error
-- ===================================================================

USE employee_management_system;

-- Check if table exists with old schema
SET @table_exists = (
    SELECT COUNT(*) 
    FROM information_schema.tables 
    WHERE table_schema = 'employee_management_system' 
    AND table_name = 'alert_configurations'
);

SET @has_alert_type = (
    SELECT COUNT(*) 
    FROM information_schema.columns 
    WHERE table_schema = 'employee_management_system' 
    AND table_name = 'alert_configurations' 
    AND column_name = 'alert_type'
);

-- If table exists with old schema, drop it and recreate
-- WARNING: This will delete all existing alert configurations!
SET @sql = IF(@table_exists > 0 AND @has_alert_type > 0,
    'DROP TABLE IF EXISTS alert_configurations',
    'SELECT "Table does not exist or already has correct schema" AS message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Create table with correct schema
CREATE TABLE IF NOT EXISTS alert_configurations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    alert_days_before INT NOT NULL,
    alert_email VARCHAR(255),
    enabled BOOLEAN DEFAULT TRUE,
    alert_priority VARCHAR(50) NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    alert_frequency VARCHAR(50),
    repeat_until_resolved BOOLEAN DEFAULT FALSE,
    UNIQUE KEY uk_doc_priority (document_type, alert_priority),
    UNIQUE KEY uk_org_doc_priority (organization_id, document_type, alert_priority),
    INDEX idx_org_id (organization_id),
    INDEX idx_document_type (document_type),
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SELECT 'Alert configurations table schema fixed successfully' AS status;

