#!/bin/bash

# ===================================================================
# Quick Fix Script: Fix approved_by column type in leaves table
# ===================================================================
# This script fixes the approved_by column from BIGINT to VARCHAR(255)
# to match the entity which stores username strings, not user IDs
# ===================================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

print_info() {
    echo -e "${GREEN}ℹ️  $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

# Check if .env file exists
if [ ! -f .env ]; then
    print_error ".env file not found!"
    exit 1
fi

# Load environment variables
source .env

# Get database root password
DB_ROOT_PASSWORD="${DB_ROOT_PASSWORD:-wuf27@1991}"

COMPOSE_FILE="compose.yaml"

print_info "Fixing approved_by column type in leaves table..."

# Check if MySQL container is running
if ! docker-compose -f "$COMPOSE_FILE" ps mysql | grep -q "Up"; then
    print_error "MySQL container is not running!"
    exit 1
fi

# Run the fix script
print_info "Executing fix-leaves-table.sql..."
docker-compose -f "$COMPOSE_FILE" exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" employee_management_system < database/fix-leaves-table.sql

if [ $? -eq 0 ]; then
    print_success "Leaves table structure fixed successfully!"
    
    # Verify the fix
    print_info "Verifying approved_by column type..."
    COLUMN_TYPE=$(docker-compose -f "$COMPOSE_FILE" exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" \
        -e "USE employee_management_system; SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE table_schema = 'employee_management_system' AND table_name = 'leaves' AND column_name = 'approved_by';" \
        2>/dev/null | tail -1 | tr -d ' ' || echo "")
    
    if [ "$COLUMN_TYPE" = "varchar" ]; then
        print_success "approved_by column is now VARCHAR(255) ✓"
    else
        print_warning "Column type verification: $COLUMN_TYPE (expected: varchar)"
    fi
    
    print_info "You can now approve leaves without errors!"
else
    print_error "Failed to fix leaves table structure"
    exit 1
fi

