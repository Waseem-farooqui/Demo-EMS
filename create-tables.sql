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
    department_id BIGINT,
    UNIQUE KEY uk_work_email_org (work_email, organization_id),
    INDEX idx_org_id (organization_id),
    INDEX idx_user_id (user_id),
    INDEX idx_department_id (department_id),
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL
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
    file_hash VARCHAR(32),
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
    INDEX idx_uploaded_date (uploaded_date),
    INDEX idx_file_hash (file_hash),
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Leaves table
CREATE TABLE IF NOT EXISTS leaves (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    leave_type VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    number_of_days INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    reason VARCHAR(500),
    applied_date DATE NOT NULL,
    approved_by VARCHAR(255),
    approval_date DATE,
    remarks TEXT,
    rejection_reason TEXT,
    medical_certificate LONGBLOB,
    certificate_file_name VARCHAR(255),
    certificate_content_type VARCHAR(100),
    financial_year VARCHAR(20),
    organization_id BIGINT,
    INDEX idx_employee_id (employee_id),
    INDEX idx_status (status),
    INDEX idx_dates (start_date, end_date),
    INDEX idx_applied_date (applied_date),
    INDEX idx_org_id (organization_id),
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Leave balances table
CREATE TABLE IF NOT EXISTS leave_balances (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    financial_year VARCHAR(20) NOT NULL,
    leave_type VARCHAR(50) NOT NULL,
    total_allocated INT NOT NULL DEFAULT 0,
    used_leaves INT NOT NULL DEFAULT 0,
    remaining_leaves INT NOT NULL DEFAULT 0,
    organization_id BIGINT,
    UNIQUE KEY uk_employee_leave_year (employee_id, financial_year, leave_type),
    INDEX idx_employee_id (employee_id),
    INDEX idx_org_id (organization_id),
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Attendance table
CREATE TABLE IF NOT EXISTS attendance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    check_in_time DATETIME NOT NULL,
    check_out_time DATETIME,
    work_date DATE NOT NULL,
    work_location VARCHAR(50) NOT NULL,
    hours_worked DOUBLE,
    notes VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE,
    created_at DATETIME,
    updated_at DATETIME,
    organization_id BIGINT,
    UNIQUE KEY uk_employee_date (employee_id, work_date),
    INDEX idx_work_date (work_date),
    INDEX idx_org_id (organization_id),
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Rotas table
CREATE TABLE IF NOT EXISTS rotas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hotel_name VARCHAR(255) NOT NULL,
    department VARCHAR(255) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    uploaded_date DATETIME NOT NULL,
    uploaded_by BIGINT NOT NULL,
    uploaded_by_name VARCHAR(255) NOT NULL,
    organization_id BIGINT,
    INDEX idx_start_date (start_date),
    INDEX idx_end_date (end_date),
    INDEX idx_org_id (organization_id),
    FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE CASCADE
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
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message VARCHAR(500) NOT NULL,
    reference_id BIGINT,
    reference_type VARCHAR(50),
    is_read BOOLEAN DEFAULT FALSE,
    created_at DATETIME,
    read_at DATETIME,
    organization_id BIGINT,
    INDEX idx_user_id (user_id),
    INDEX idx_is_read (is_read),
    INDEX idx_created_at (created_at),
    INDEX idx_reference_id (reference_id),
    INDEX idx_org_id (organization_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Alert configurations table
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

