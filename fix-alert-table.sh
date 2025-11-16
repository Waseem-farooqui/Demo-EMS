#!/bin/bash
# Fix Alert Configurations Table Schema

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}=== Fixing Alert Configurations Table Schema ===${NC}"
echo ""

# Check if .env exists
if [ ! -f .env ]; then
    echo -e "${RED}ERROR: .env file not found!${NC}"
    exit 1
fi

# Source .env
set -a
source .env
set +a

DB_ROOT_PASSWORD=${DB_ROOT_PASSWORD:-wuf27@1991}

echo -e "${YELLOW}Dropping and recreating alert_configurations table...${NC}"
echo -e "${YELLOW}⚠️  WARNING: This will delete all existing alert configurations!${NC}"
echo ""
read -p "Continue? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Cancelled."
    exit 0
fi

# Drop and recreate the table
docker-compose exec -T mysql mysql -u root -p"$DB_ROOT_PASSWORD" <<EOF
USE employee_management_system;

-- Drop table if exists
DROP TABLE IF EXISTS alert_configurations;

-- Create table with correct schema
CREATE TABLE alert_configurations (
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

SELECT 'Alert configurations table recreated successfully' AS status;
EOF

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Table schema fixed!${NC}"
    echo ""
    echo "The table now matches the entity structure."
    echo "Alert configurations will be created automatically when organizations are created."
else
    echo -e "${RED}✗ Failed to fix table${NC}"
    exit 1
fi

