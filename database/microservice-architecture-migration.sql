-- =====================================================
-- Microservice Architecture - Multi-System Support
-- Add system_type to organizations and create inventory tables
-- =====================================================

-- Step 1: Add system_type column to organizations table
ALTER TABLE organizations
ADD COLUMN IF NOT EXISTS system_type VARCHAR(50) NOT NULL DEFAULT 'EMPLOYEE_MANAGEMENT';

-- Step 2: Add index for system_type
CREATE INDEX IF NOT EXISTS idx_org_system_type ON organizations(system_type);

-- Step 3: Create inventory_categories table
CREATE TABLE IF NOT EXISTS inventory_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    organization_id BIGINT NOT NULL,
    organization_uuid VARCHAR(36) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_category_code_org (code, organization_uuid),
    INDEX idx_category_org (organization_uuid),
    INDEX idx_category_active (is_active)
);

-- Step 4: Create inventory_items table
CREATE TABLE IF NOT EXISTS inventory_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_code VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    category_id BIGINT,
    quantity INT DEFAULT 0,
    reorder_level INT DEFAULT 10,
    unit_price DECIMAL(10, 2),
    unit VARCHAR(50),
    supplier VARCHAR(100),
    barcode VARCHAR(100),
    image_path VARCHAR(500),
    image_data LONGBLOB,
    organization_id BIGINT NOT NULL,
    organization_uuid VARCHAR(36) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,

    UNIQUE KEY uk_item_code_org (item_code, organization_uuid),
    INDEX idx_item_org (organization_uuid),
    INDEX idx_item_category (category_id),
    INDEX idx_item_active (is_active),
    INDEX idx_item_reorder (organization_uuid, quantity, reorder_level),
    FOREIGN KEY (category_id) REFERENCES inventory_categories(id) ON DELETE SET NULL
);

-- Step 5: Create inventory_transactions table
CREATE TABLE IF NOT EXISTS inventory_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id BIGINT NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10, 2),
    total_amount DECIMAL(10, 2),
    reference_number VARCHAR(100),
    remarks VARCHAR(1000),
    organization_id BIGINT NOT NULL,
    organization_uuid VARCHAR(36) NOT NULL,
    performed_by BIGINT NOT NULL,
    transaction_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_transaction_item (item_id),
    INDEX idx_transaction_org (organization_uuid),
    INDEX idx_transaction_date (transaction_date),
    INDEX idx_transaction_type (transaction_type),
    FOREIGN KEY (item_id) REFERENCES inventory_items(id) ON DELETE CASCADE
);

-- Step 6: Insert default inventory categories for existing organizations with INVENTORY_MANAGEMENT or HYBRID type
INSERT INTO inventory_categories (code, name, description, organization_id, organization_uuid, is_active)
SELECT
    'GEN',
    'General',
    'General inventory items',
    o.id,
    o.organization_uuid,
    TRUE
FROM organizations o
WHERE o.system_type IN ('INVENTORY_MANAGEMENT', 'HYBRID')
AND NOT EXISTS (
    SELECT 1 FROM inventory_categories ic
    WHERE ic.organization_uuid = o.organization_uuid AND ic.code = 'GEN'
);

INSERT INTO inventory_categories (code, name, description, organization_id, organization_uuid, is_active)
SELECT
    'RAW',
    'Raw Materials',
    'Raw materials for production',
    o.id,
    o.organization_uuid,
    TRUE
FROM organizations o
WHERE o.system_type IN ('INVENTORY_MANAGEMENT', 'HYBRID')
AND NOT EXISTS (
    SELECT 1 FROM inventory_categories ic
    WHERE ic.organization_uuid = o.organization_uuid AND ic.code = 'RAW'
);

INSERT INTO inventory_categories (code, name, description, organization_id, organization_uuid, is_active)
SELECT
    'FIN',
    'Finished Goods',
    'Finished products ready for sale',
    o.id,
    o.organization_uuid,
    TRUE
FROM organizations o
WHERE o.system_type IN ('INVENTORY_MANAGEMENT', 'HYBRID')
AND NOT EXISTS (
    SELECT 1 FROM inventory_categories ic
    WHERE ic.organization_uuid = o.organization_uuid AND ic.code = 'FIN'
);

-- Step 7: Add comments to tables
ALTER TABLE organizations
MODIFY COLUMN system_type VARCHAR(50) NOT NULL DEFAULT 'EMPLOYEE_MANAGEMENT'
COMMENT 'EMPLOYEE_MANAGEMENT, INVENTORY_MANAGEMENT, or HYBRID';

-- Verification queries
SELECT 'Organizations table updated' AS status;
SELECT COUNT(*) AS org_count, system_type FROM organizations GROUP BY system_type;
SELECT 'Inventory tables created' AS status;
SELECT COUNT(*) AS category_count FROM inventory_categories;

-- Success message
SELECT '✅ Microservice architecture migration completed successfully!' AS message;

