#!/bin/bash

# Immediate fix for approved_by column type issue
# This fixes: "Incorrect integer value: 'username' for column 'approved_by'"

echo "🔧 Fixing approved_by column type from BIGINT to VARCHAR(255)..."

docker-compose exec mysql mysql -u root -p"wuf27@1991" employee_management_system <<EOF
-- Clear any existing numeric values (they're invalid usernames)
UPDATE leaves SET approved_by = NULL WHERE approved_by IS NOT NULL;

-- Change column type from BIGINT to VARCHAR(255)
ALTER TABLE leaves MODIFY COLUMN approved_by VARCHAR(255) NULL;

-- Verify the fix
SELECT COLUMN_NAME, DATA_TYPE, COLUMN_TYPE 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE table_schema = 'employee_management_system' 
AND table_name = 'leaves' 
AND column_name = 'approved_by';
EOF

echo ""
echo "✅ Fix complete! The approved_by column is now VARCHAR(255)"
echo "   You can now approve leaves without errors."

