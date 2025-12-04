#!/bin/bash
###############################################################################
# Repair Flyway Migration V25
# Fixes the failed migration "Add Title To Next Of Kin"
###############################################################################

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

print_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

print_step() {
    echo ""
    echo -e "${BLUE}▶ $1${NC}"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
}

# Configuration
COMPOSE_FILE="${COMPOSE_FILE:-compose.yaml}"
DB_NAME="${DB_NAME:-employee_management_system}"
DB_ROOT_PASSWORD="${DB_ROOT_PASSWORD:-rootpassword}"

print_step "Repairing Flyway Migration V25"

# Check if MySQL container is running
if ! docker-compose -f "$COMPOSE_FILE" ps mysql | grep -q "Up"; then
    print_error "MySQL container is not running"
    print_info "Start it with: docker-compose up -d mysql"
    exit 1
fi

print_info "Checking if 'title' column exists in next_of_kin table..."

# Check if the column already exists
COLUMN_EXISTS=$(docker-compose -f "$COMPOSE_FILE" exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" \
    -e "USE ${DB_NAME}; SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = '${DB_NAME}' AND table_name = 'next_of_kin' AND column_name = 'title';" \
    2>/dev/null | tail -1 | tr -d ' ' || echo "0")

if [ "$COLUMN_EXISTS" = "1" ]; then
    print_success "Column 'title' already exists in next_of_kin table"
    print_info "The migration likely completed but was marked as failed"
    print_info "Marking migration V25 as successful..."
    
    # Mark the migration as successful
    docker-compose -f "$COMPOSE_FILE" exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" <<EOF 2>/dev/null
USE ${DB_NAME};

-- Update V25 migration to success=1
UPDATE flyway_schema_history 
SET success = 1 
WHERE version = '25' AND success = 0;

-- Verify
SELECT 'Migration V25 repaired:' AS status;
SELECT version, description, success, installed_on 
FROM flyway_schema_history 
WHERE version = '25';
EOF

    if [ $? -eq 0 ]; then
        print_success "Migration V25 marked as successful"
    else
        print_error "Failed to repair migration V25"
        exit 1
    fi
else
    print_warning "Column 'title' does not exist in next_of_kin table"
    print_info "The migration failed before completion"
    print_info "Removing failed migration record so Flyway can retry..."
    
    # Delete the failed migration record so Flyway can retry
    docker-compose -f "$COMPOSE_FILE" exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" <<EOF 2>/dev/null
USE ${DB_NAME};

-- Show failed migration before deletion
SELECT 'Failed migration to be removed:' AS status;
SELECT version, description, success, installed_on 
FROM flyway_schema_history 
WHERE version = '25' AND success = 0;

-- Delete failed migration record
DELETE FROM flyway_schema_history 
WHERE version = '25' AND success = 0;

-- Verify deletion
SELECT 'Migration V25 record removed. Flyway will retry on next startup.' AS status;
EOF

    if [ $? -eq 0 ]; then
        print_success "Failed migration V25 record removed"
        print_info "Flyway will automatically retry the migration on next backend startup"
    else
        print_error "Failed to remove migration V25 record"
        exit 1
    fi
fi

print_success "Flyway repair completed!"
print_info "You can now restart the backend container:"
print_info "  docker-compose restart backend"

