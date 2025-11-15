-- ===================================================================
-- Create All Tables Manually
-- ===================================================================
-- This script creates all tables if they don't exist
-- Run this if JPA/Hibernate fails to create tables automatically
-- ===================================================================

USE employee_management_system;

-- Organizations table
CREATE TABLE IF NOT EXISTS organizations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_uuid VARCHAR(36) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL UNIQUE,
    logo_path VARCHAR(500),
    logo_data LONGBLOB,
    created_at DATETIME,
    updated_at DATETIME,
    is_active BOOLEAN DEFAULT TRUE,
    description VARCHAR(500),
    contact_email VARCHAR(255),
    contact_phone VARCHAR(50),
    address VARCHAR(1000),
    INDEX idx_org_uuid (organization_uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    email_verified BOOLEAN DEFAULT FALSE,
    first_login BOOLEAN DEFAULT TRUE,
    profile_completed BOOLEAN DEFAULT FALSE,
    temporary_password BOOLEAN DEFAULT TRUE,
    organization_id BIGINT,
    organization_uuid VARCHAR(36),
    UNIQUE KEY uk_username_org (username, organization_id),
    UNIQUE KEY uk_email_org (email, organization_id),
    INDEX idx_org_id (organization_id),
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- User roles table
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Employees table
CREATE TABLE IF NOT EXISTS employees (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    person_type VARCHAR(50) NOT NULL,
    work_email VARCHAR(255) NOT NULL,
    personal_email VARCHAR(255),
    phone_number VARCHAR(50),
    date_of_birth DATE,
    nationality VARCHAR(100),
    address VARCHAR(500),
    job_title VARCHAR(255) NOT NULL,
    reference VARCHAR(255),
    date_of_joining DATE NOT NULL,
    employment_status VARCHAR(50),
    contract_type VARCHAR(50),
    working_timing VARCHAR(100),
    holiday_allowance INT,
    user_id BIGINT,
    organization_id BIGINT,
    organization_uuid VARCHAR(36),
    UNIQUE KEY uk_work_email_org (work_email, organization_id),
    INDEX idx_org_id (organization_id),
    INDEX idx_user_id (user_id),
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Departments table
CREATE TABLE IF NOT EXISTS departments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    code VARCHAR(50),
    manager_id BIGINT,
    is_active BOOLEAN DEFAULT TRUE,
    organization_id BIGINT,
    created_at DATETIME,
    updated_at DATETIME,
    UNIQUE KEY uk_code_org (code, organization_id),
    UNIQUE KEY uk_name_org (name, organization_id),
    INDEX idx_org_id (organization_id),
    INDEX idx_manager_id (manager_id),
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    FOREIGN KEY (manager_id) REFERENCES employees(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Documents table
CREATE TABLE IF NOT EXISTS documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    document_number VARCHAR(255),
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_type VARCHAR(50),
    file_size BIGINT,
    file_hash VARCHAR(32),
    preview_image BLOB,
    extracted_text TEXT,
    issue_date DATE,
    expiry_date DATE,
    issuing_country VARCHAR(100),
    full_name VARCHAR(255),
    date_of_birth DATE,
    nationality VARCHAR(100),
    company_name VARCHAR(255),
    date_of_check DATE,
    reference_number VARCHAR(255),
    contract_date DATE,
    place_of_work VARCHAR(255),
    contract_between VARCHAR(500),
    job_title_contract VARCHAR(255),
    uploaded_date DATETIME NOT NULL,
    last_alert_sent DATETIME,
    alert_sent_count INT DEFAULT 0,
    last_viewed_at DATETIME,
    last_viewed_by VARCHAR(255),
    INDEX idx_employee_id (employee_id),
    INDEX idx_document_type (document_type),
    INDEX idx_expiry_date (expiry_date),
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Leaves table
CREATE TABLE IF NOT EXISTS leaves (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    leave_type VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    days_taken DECIMAL(5,2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    reason TEXT,
    applied_date DATETIME NOT NULL,
    approved_by BIGINT,
    approved_date DATETIME,
    rejection_reason TEXT,
    organization_id BIGINT,
    INDEX idx_employee_id (employee_id),
    INDEX idx_status (status),
    INDEX idx_dates (start_date, end_date),
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    FOREIGN KEY (approved_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Leave balances table
CREATE TABLE IF NOT EXISTS leave_balances (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    leave_type VARCHAR(50) NOT NULL,
    total_allocated DECIMAL(5,2) NOT NULL DEFAULT 0,
    used DECIMAL(5,2) NOT NULL DEFAULT 0,
    remaining DECIMAL(5,2) NOT NULL DEFAULT 0,
    year INT NOT NULL,
    organization_id BIGINT,
    UNIQUE KEY uk_employee_leave_year (employee_id, leave_type, year),
    INDEX idx_employee_id (employee_id),
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Attendance table
CREATE TABLE IF NOT EXISTS attendance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    date DATE NOT NULL,
    check_in_time DATETIME,
    check_out_time DATETIME,
    status VARCHAR(50),
    hours_worked DECIMAL(5,2),
    notes TEXT,
    organization_id BIGINT,
    UNIQUE KEY uk_employee_date (employee_id, date),
    INDEX idx_date (date),
    INDEX idx_status (status),
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Rotas table
CREATE TABLE IF NOT EXISTS rotas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    week_start_date DATE NOT NULL,
    week_end_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_by BIGINT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    organization_id BIGINT,
    INDEX idx_employee_id (employee_id),
    INDEX idx_week_dates (week_start_date, week_end_date),
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Rota schedules table
CREATE TABLE IF NOT EXISTS rota_schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rota_id BIGINT NOT NULL,
    day_of_week INT NOT NULL,
    start_time TIME,
    end_time TIME,
    break_duration INT,
    notes TEXT,
    INDEX idx_rota_id (rota_id),
    FOREIGN KEY (rota_id) REFERENCES rotas(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Rota change logs table
CREATE TABLE IF NOT EXISTS rota_change_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rota_id BIGINT NOT NULL,
    changed_by BIGINT NOT NULL,
    change_type VARCHAR(50) NOT NULL,
    old_value TEXT,
    new_value TEXT,
    changed_at DATETIME NOT NULL,
    reason TEXT,
    INDEX idx_rota_id (rota_id),
    INDEX idx_changed_at (changed_at),
    FOREIGN KEY (rota_id) REFERENCES rotas(id) ON DELETE CASCADE,
    FOREIGN KEY (changed_by) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Notifications table
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50),
    is_read BOOLEAN DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    read_at DATETIME,
    organization_id BIGINT,
    INDEX idx_user_id (user_id),
    INDEX idx_is_read (is_read),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Alert configurations table
CREATE TABLE IF NOT EXISTS alert_configurations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    alert_type VARCHAR(50) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    days_before INT,
    notification_method VARCHAR(50),
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    UNIQUE KEY uk_org_alert_type (organization_id, alert_type),
    INDEX idx_org_id (organization_id),
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Verification tokens table
CREATE TABLE IF NOT EXISTS verification_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    token_type VARCHAR(50) NOT NULL,
    expiry_date DATETIME NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    INDEX idx_token (token),
    INDEX idx_user_id (user_id),
    INDEX idx_expiry_date (expiry_date),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SELECT 'All tables created successfully' AS status;

