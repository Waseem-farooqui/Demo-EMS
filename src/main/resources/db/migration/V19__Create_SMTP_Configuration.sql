-- Create SMTP Configuration table for organizations
CREATE TABLE IF NOT EXISTS smtp_configuration (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    provider VARCHAR(50) NOT NULL DEFAULT 'GMAIL', -- GMAIL, OUTLOOK, CUSTOM
    host VARCHAR(255) NOT NULL,
    port INT NOT NULL DEFAULT 587,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(500) NOT NULL, -- Encrypted password/token
    from_email VARCHAR(255) NOT NULL,
    from_name VARCHAR(255) DEFAULT 'Employee Management System',
    enabled BOOLEAN DEFAULT TRUE,
    use_default BOOLEAN DEFAULT FALSE, -- If true, use env variables instead
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT, -- User ID who created this config
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    UNIQUE KEY unique_org_smtp (organization_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add index for faster lookups
CREATE INDEX idx_smtp_org_id ON smtp_configuration(organization_id);

