#!/bin/bash
###############################################################################
# Fix Failed Flyway Migration
###############################################################################
# This script repairs failed migrations in Flyway schema history
# Run this if you get "Detected failed migration" errors
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
    
    print_info "Repairing Flyway schema history in Docker environment..."
    
    # Check if flyway_schema_history table exists
    FLYWAY_HISTORY_EXISTS=$(docker-compose -f "$COMPOSE_FILE" exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" \
        -e "USE employee_management_system; SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'employee_management_system' AND table_name = 'flyway_schema_history';" \
        2>/dev/null | tail -1 | tr -d ' ' || echo "0")
    
    if [ "$FLYWAY_HISTORY_EXISTS" = "0" ]; then
        print_info "Flyway schema history table doesn't exist yet"
        print_info "This is normal for a fresh database - Flyway will create it on first migration"
        exit 0
    fi
    
    # Check for failed migrations
    FAILED_COUNT=$(docker-compose -f "$COMPOSE_FILE" exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" \
        -e "USE employee_management_system; SELECT COUNT(*) FROM flyway_schema_history WHERE success = 0;" \
        2>/dev/null | tail -1 | tr -d ' ' || echo "0")
    
    if [ "$FAILED_COUNT" = "0" ] || [ -z "$FAILED_COUNT" ]; then
        print_success "No failed migrations found"
        exit 0
    fi
    
    print_info "Found $FAILED_COUNT failed migration(s)"
    print_info "Showing failed migrations:"
    
    docker-compose -f "$COMPOSE_FILE" exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" \
        -e "USE employee_management_system; SELECT installed_rank, version, description, success, installed_on FROM flyway_schema_history WHERE success = 0 ORDER BY installed_rank;" \
        2>/dev/null || true
    
    echo ""
    read -p "Do you want to repair these failed migrations? (y/N): " -n 1 -r
    echo
    
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        print_info "Repair cancelled"
        exit 0
    fi
    
    print_info "Repairing Flyway schema history..."
    
    docker-compose -f "$COMPOSE_FILE" exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" <<EOF
USE employee_management_system;

-- Update failed migrations to success=1 (repair)
UPDATE flyway_schema_history 
SET success = 1 
WHERE success = 0;

-- Verify repair
SELECT 
    'Repaired migrations:' AS status,
    COUNT(*) AS count
FROM flyway_schema_history
WHERE success = 1;
EOF
    
    if [ $? -eq 0 ]; then
        print_success "Flyway schema history repaired successfully"
        print_info "You can now restart the backend - Flyway will proceed with pending migrations"
    else
        print_error "Failed to repair Flyway schema history"
        exit 1
    fi
    
else
    print_error "This script requires Docker Compose environment"
    print_info "For manual repair, connect to MySQL and run:"
    echo ""
    echo "  USE employee_management_system;"
    echo "  UPDATE flyway_schema_history SET success = 1 WHERE success = 0;"
    echo ""
    exit 1
fi

