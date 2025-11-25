-- Create Alert Configurations table for document expiry alerts
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

