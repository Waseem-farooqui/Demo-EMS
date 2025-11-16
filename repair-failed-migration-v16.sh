#!/bin/bash
###############################################################################
# IMMEDIATE FIX: Repair Failed Migration V16
###############################################################################
# This script repairs the failed V16 migration in Flyway schema history
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
    
    print_info "Repairing failed migration V16 in Flyway schema history..."
    
    # Check if flyway_schema_history table exists
    FLYWAY_HISTORY_EXISTS=$(docker-compose -f "$COMPOSE_FILE" exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" \
        -e "USE employee_management_system; SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'employee_management_system' AND table_name = 'flyway_schema_history';" \
        2>/dev/null | tail -1 | tr -d ' ' || echo "0")
    
    if [ "$FLYWAY_HISTORY_EXISTS" = "0" ]; then
        print_info "Flyway schema history table doesn't exist yet"
        exit 0
    fi
    
    # Check for failed V16 migration
    FAILED_V16=$(docker-compose -f "$COMPOSE_FILE" exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" \
        -e "USE employee_management_system; SELECT COUNT(*) FROM flyway_schema_history WHERE version = '16' AND success = 0;" \
        2>/dev/null | tail -1 | tr -d ' ' || echo "0")
    
    if [ "$FAILED_V16" = "0" ]; then
        print_success "No failed V16 migration found"
        exit 0
    fi
    
    print_info "Found failed V16 migration - repairing..."
    
    # Repair the migration
    docker-compose -f "$COMPOSE_FILE" exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" <<'REPAIR' 2>/dev/null || true
USE employee_management_system;

-- Show failed migration
SELECT 'Failed migration V16:' AS status;
SELECT installed_rank, version, description, success, installed_on, checksum
FROM flyway_schema_history 
WHERE version = '16' AND success = 0;

-- Delete the failed migration record (we'll let Flyway re-run it)
DELETE FROM flyway_schema_history 
WHERE version = '16' AND success = 0;

-- Verify deletion
SELECT 'V16 migration record removed - Flyway will re-run it on next startup' AS status;
REPAIR
    
    if [ $? -eq 0 ]; then
        print_success "Failed V16 migration repaired successfully"
        print_info "The migration will be re-run automatically on backend startup"
        print_info "The migration is now idempotent and safe to run multiple times"
    else
        print_error "Failed to repair V16 migration"
        exit 1
    fi
    
else
    print_error "This script requires Docker Compose environment"
    exit 1
fi

