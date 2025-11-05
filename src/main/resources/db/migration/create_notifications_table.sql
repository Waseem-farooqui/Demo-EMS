-- Notification System Migration Script
-- Creates notifications table for leave request alerts

CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message VARCHAR(500) NOT NULL,
    reference_id BIGINT,
    reference_type VARCHAR(50),
    is_read BOOLEAN DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    read_at DATETIME,
    organization_id BIGINT,

    INDEX idx_notification_user (user_id),
    INDEX idx_notification_read (user_id, is_read),
    INDEX idx_notification_created (created_at),
    INDEX idx_notification_reference (reference_type, reference_id),
    INDEX idx_notification_org (organization_id)
);

-- Sample data (optional - for testing)
-- INSERT INTO notifications (user_id, type, title, message, reference_id, reference_type, is_read, created_at, organization_id)
-- VALUES (1, 'LEAVE_REQUEST', 'New Leave Request', 'John Doe has requested ANNUAL leave from 2025-11-10 to 2025-11-15', 1, 'LEAVE', FALSE, NOW(), 1);

