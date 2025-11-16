#!/bin/bash
###############################################################################
# IMMEDIATE FIX: rota_schedules table - Add employee_id column
###############################################################################
# Run this script to immediately fix the "Field 'employee_id' doesn't have 
# a default value" error
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
    
    print_info "Fixing rota_schedules table structure..."
    
    # Check if table exists
    TABLE_EXISTS=$(docker-compose -f "$COMPOSE_FILE" exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" \
        -e "USE employee_management_system; SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'employee_management_system' AND table_name = 'rota_schedules';" \
        2>/dev/null | tail -1 | tr -d ' ' || echo "0")
    
    if [ "$TABLE_EXISTS" = "0" ]; then
        print_info "rota_schedules table doesn't exist yet"
        print_info "It will be created by JPA or Flyway migration V14"
        exit 0
    fi
    
    # Check if employee_id column exists
    HAS_EMPLOYEE_ID=$(docker-compose -f "$COMPOSE_FILE" exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" \
        -e "USE employee_management_system; SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'employee_management_system' AND table_name = 'rota_schedules' AND column_name = 'employee_id';" \
        2>/dev/null | tail -1 | tr -d ' ' || echo "0")
    
    if [ "$HAS_EMPLOYEE_ID" = "1" ]; then
        print_success "rota_schedules table already has employee_id column"
        print_info "Table structure is correct"
        exit 0
    fi
    
    print_warning "rota_schedules table is missing employee_id column"
    print_info "Applying fix..."
    
    # Apply the fix
    if [ -f "database/fix-rota-schedules-now.sql" ]; then
        docker-compose -f "$COMPOSE_FILE" exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" < database/fix-rota-schedules-now.sql 2>/dev/null || {
            print_error "Failed to apply fix via SQL file, trying inline SQL..."
            
            # Fallback: inline SQL
            docker-compose -f "$COMPOSE_FILE" exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" <<'INLINESQL'
USE employee_management_system;

SET @dbname = DATABASE();
SET @tablename = 'rota_schedules';

-- Add employee_id
SET @columnname = 'employee_id';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE (table_name = @tablename) AND (table_schema = @dbname) AND (column_name = @columnname)) > 0,
  "SELECT 'Column employee_id already exists' AS message",
  CONCAT("ALTER TABLE ", @tablename, " ADD COLUMN ", @columnname, " BIGINT NOT NULL AFTER rota_id")
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Add employee_name
SET @columnname = 'employee_name';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE (table_name = @tablename) AND (table_schema = @dbname) AND (column_name = @columnname)) > 0,
  "SELECT 'Column employee_name already exists' AS message",
  CONCAT("ALTER TABLE ", @tablename, " ADD COLUMN ", @columnname, " VARCHAR(255) NOT NULL AFTER employee_id")
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Add schedule_date
SET @columnname = 'schedule_date';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE (table_name = @tablename) AND (table_schema = @dbname) AND (column_name = @columnname)) > 0,
  "SELECT 'Column schedule_date already exists' AS message",
  CONCAT("ALTER TABLE ", @tablename, " ADD COLUMN ", @columnname, " DATE NOT NULL AFTER employee_name")
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Modify day_of_week
SET @preparedStatement = (SELECT IF(
  (SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE (table_name = @tablename) AND (table_schema = @dbname) AND (column_name = 'day_of_week') AND (DATA_TYPE = 'int')) IS NOT NULL,
  CONCAT("ALTER TABLE ", @tablename, " MODIFY COLUMN day_of_week VARCHAR(50) NOT NULL"),
  "SELECT 'Column day_of_week is already VARCHAR' AS message"
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Add duty
SET @columnname = 'duty';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE (table_name = @tablename) AND (table_schema = @dbname) AND (column_name = @columnname)) > 0,
  "SELECT 'Column duty already exists' AS message",
  CONCAT("ALTER TABLE ", @tablename, " ADD COLUMN ", @columnname, " VARCHAR(255) NOT NULL AFTER end_time")
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Add is_off_day
SET @columnname = 'is_off_day';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE (table_name = @tablename) AND (table_schema = @dbname) AND (column_name = @columnname)) > 0,
  "SELECT 'Column is_off_day already exists' AS message",
  CONCAT("ALTER TABLE ", @tablename, " ADD COLUMN ", @columnname, " BOOLEAN DEFAULT FALSE AFTER duty")
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

SELECT 'rota_schedules table fixed!' AS status;
INLINESQL
        }
        
        if [ $? -eq 0 ]; then
            print_success "rota_schedules table structure fixed successfully"
            print_info "The employee_id column has been added"
            print_info "You can now retry the manual ROTA creation"
        else
            print_error "Failed to fix rota_schedules table"
            exit 1
        fi
    else
        print_error "fix-rota-schedules-now.sql not found"
        exit 1
    fi
    
else
    print_error "This script requires Docker Compose environment"
    exit 1
fi

