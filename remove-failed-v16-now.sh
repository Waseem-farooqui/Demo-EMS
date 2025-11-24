#!/bin/bash
###############################################################################
# IMMEDIATE FIX: Remove Failed V16 Migration Record
###############################################################################
# This script removes the failed V16 migration record from Flyway schema history
# Run this to fix the "Detected failed migration to version 16" error
###############################################################################

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

print_info() {
    echo -e "${YELLOW}ℹ️  $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Check if running in Docker Compose environment
if [ -f "compose.yaml" ] || [ -f "docker-compose.yml" ]; then
    COMPOSE_FILE="compose.yaml"
    if [ ! -f "$COMPOSE_FILE" ]; then
        COMPOSE_FILE="docker-compose.yml"
    fi
    
    # Load environment variables
    if [ -f .env ]; then
        set -a
        source .env 2>/dev/null || true
        set +a
    fi
    
    DB_ROOT_PASSWORD=${DB_ROOT_PASSWORD:-rootpassword}
    
    print_info "Removing failed V16 migration record from Flyway schema history..."
    
    # Check if MySQL is running
    if ! docker-compose -f "$COMPOSE_FILE" ps mysql | grep -q "Up"; then
        print_error "MySQL container is not running"
        print_info "Starting MySQL..."
        docker-compose -f "$COMPOSE_FILE" up -d mysql
        sleep 5
    fi
    
    # Remove failed V16 migration and drop FK constraint
    docker-compose -f "$COMPOSE_FILE" exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" <<'SQL' 2>/dev/null || true
USE employee_management_system;

-- Drop foreign key constraint on employee_id if it exists
SET @fk_name = (
    SELECT CONSTRAINT_NAME 
    FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE 
    WHERE table_schema = 'employee_management_system' 
    AND table_name = 'rotas' 
    AND column_name = 'employee_id'
    AND referenced_table_name IS NOT NULL
    LIMIT 1
);

SET @sql = IF(@fk_name IS NOT NULL,
    CONCAT('ALTER TABLE rotas DROP FOREIGN KEY ', @fk_name),
    'SELECT "No foreign key constraint found" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Delete the failed V16 migration record
DELETE FROM flyway_schema_history 
WHERE version = '16' AND success = 0;

-- Verify deletion
SELECT 'V16 migration record removed' AS status;
SELECT COUNT(*) AS remaining_failed_migrations 
FROM flyway_schema_history 
WHERE success = 0;
SQL
    
    if [ $? -eq 0 ]; then
        print_success "Failed V16 migration record removed successfully"
        print_info "You can now restart the backend"
        print_info "Schema fixes will be handled automatically by DatabaseSchemaFixer on startup"
    else
        print_error "Failed to remove V16 migration record"
        exit 1
    fi
    
else
    print_error "This script requires Docker Compose environment"
    exit 1
fi

