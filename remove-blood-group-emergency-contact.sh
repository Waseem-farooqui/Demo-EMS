#!/bin/bash

# ===================================================================
# Script to Remove Blood Group and Emergency Contact Columns
# Employee Management System - Production Database Migration
# ===================================================================
#
# This script safely removes blood group and emergency contact columns
# from the employees table in the production database.
#
# Prerequisites:
#   - MySQL client installed
#   - Database backup created
#   - Appropriate database credentials
#
# Usage:
#   chmod +x remove-blood-group-emergency-contact.sh
#   ./remove-blood-group-emergency-contact.sh
#
# ===================================================================

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration (modify these for your environment)
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-employeedb}"
DB_USER="${DB_USER:-empuser}"
SQL_FILE="database/remove-blood-group-emergency-contact.sql"

# Function to print colored messages
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if MySQL is available
check_mysql() {
    if ! command -v mysql &> /dev/null; then
        print_error "MySQL client is not installed or not in PATH"
        exit 1
    fi
    print_info "MySQL client found"
}

# Function to check if SQL file exists
check_sql_file() {
    if [ ! -f "$SQL_FILE" ]; then
        print_error "SQL file not found: $SQL_FILE"
        exit 1
    fi
    print_info "SQL file found: $SQL_FILE"
}

# Function to create database backup
create_backup() {
    print_warning "Creating database backup before migration..."
    
    BACKUP_DIR="backups"
    TIMESTAMP=$(date +%Y%m%d_%H%M%S)
    BACKUP_FILE="${BACKUP_DIR}/employeedb_backup_${TIMESTAMP}.sql"
    
    # Create backup directory if it doesn't exist
    mkdir -p "$BACKUP_DIR"
    
    print_info "Backing up database to: $BACKUP_FILE"
    
    if mysqldump -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p "$DB_NAME" > "$BACKUP_FILE" 2>/dev/null; then
        print_info "Backup created successfully: $BACKUP_FILE"
        return 0
    else
        print_error "Failed to create backup. Please create a manual backup before proceeding."
        read -p "Do you want to continue without backup? (yes/no): " -r
        if [[ ! $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
            print_info "Migration cancelled by user"
            exit 1
        fi
        return 1
    fi
}

# Function to verify database connection
verify_connection() {
    print_info "Verifying database connection..."
    
    if mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p -e "USE $DB_NAME;" 2>/dev/null; then
        print_info "Database connection successful"
        return 0
    else
        print_error "Failed to connect to database"
        print_info "Please check your database credentials and ensure the database exists"
        exit 1
    fi
}

# Function to check if columns exist
check_columns() {
    print_info "Checking if columns exist in employees table..."
    
    mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p "$DB_NAME" <<EOF
SELECT 
    CASE WHEN COUNT(*) > 0 THEN 'EXISTS' ELSE 'NOT FOUND' END AS blood_group_status
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = '$DB_NAME'
AND TABLE_NAME = 'employees'
AND COLUMN_NAME = 'blood_group';

SELECT 
    CASE WHEN COUNT(*) > 0 THEN 'EXISTS' ELSE 'NOT FOUND' END AS emergency_contact_name_status
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = '$DB_NAME'
AND TABLE_NAME = 'employees'
AND COLUMN_NAME = 'emergency_contact_name';

SELECT 
    CASE WHEN COUNT(*) > 0 THEN 'EXISTS' ELSE 'NOT FOUND' END AS emergency_contact_phone_status
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = '$DB_NAME'
AND TABLE_NAME = 'employees'
AND COLUMN_NAME = 'emergency_contact_phone';

SELECT 
    CASE WHEN COUNT(*) > 0 THEN 'EXISTS' ELSE 'NOT FOUND' END AS emergency_contact_relationship_status
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = '$DB_NAME'
AND TABLE_NAME = 'employees'
AND COLUMN_NAME = 'emergency_contact_relationship';
EOF
}

# Function to run migration
run_migration() {
    print_info "Running migration script..."
    
    if mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p "$DB_NAME" < "$SQL_FILE"; then
        print_info "Migration script executed successfully"
        return 0
    else
        print_error "Migration script failed"
        return 1
    fi
}

# Function to verify migration
verify_migration() {
    print_info "Verifying migration results..."
    
    mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p "$DB_NAME" <<EOF
SELECT 
    CASE WHEN COUNT(*) > 0 THEN 'STILL EXISTS - MIGRATION FAILED' ELSE 'REMOVED - SUCCESS' END AS blood_group_status
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = '$DB_NAME'
AND TABLE_NAME = 'employees'
AND COLUMN_NAME = 'blood_group';

SELECT 
    CASE WHEN COUNT(*) > 0 THEN 'STILL EXISTS - MIGRATION FAILED' ELSE 'REMOVED - SUCCESS' END AS emergency_contact_name_status
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = '$DB_NAME'
AND TABLE_NAME = 'employees'
AND COLUMN_NAME = 'emergency_contact_name';

SELECT 
    CASE WHEN COUNT(*) > 0 THEN 'STILL EXISTS - MIGRATION FAILED' ELSE 'REMOVED - SUCCESS' END AS emergency_contact_phone_status
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = '$DB_NAME'
AND TABLE_NAME = 'employees'
AND COLUMN_NAME = 'emergency_contact_phone';

SELECT 
    CASE WHEN COUNT(*) > 0 THEN 'STILL EXISTS - MIGRATION FAILED' ELSE 'REMOVED - SUCCESS' END AS emergency_contact_relationship_status
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = '$DB_NAME'
AND TABLE_NAME = 'employees'
AND COLUMN_NAME = 'emergency_contact_relationship';
EOF
}

# Main execution
main() {
    echo "=========================================="
    echo "  Database Migration Script"
    echo "  Remove Blood Group & Emergency Contact"
    echo "=========================================="
    echo ""
    
    # Pre-flight checks
    check_mysql
    check_sql_file
    
    # Confirm before proceeding
    print_warning "This script will remove the following columns from the employees table:"
    echo "  - blood_group"
    echo "  - emergency_contact_name"
    echo "  - emergency_contact_phone"
    echo "  - emergency_contact_relationship"
    echo ""
    read -p "Do you want to proceed? (yes/no): " -r
    if [[ ! $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
        print_info "Migration cancelled by user"
        exit 0
    fi
    
    # Create backup
    create_backup
    
    # Verify connection
    verify_connection
    
    # Check columns before migration
    print_info "Checking columns before migration..."
    check_columns
    
    # Run migration
    if run_migration; then
        print_info "Migration completed successfully"
        
        # Verify migration
        verify_migration
        
        echo ""
        print_info "=========================================="
        print_info "Migration completed successfully!"
        print_info "All columns have been removed."
        print_info "Please verify your application functionality."
        print_info "=========================================="
    else
        print_error "Migration failed. Please check the error messages above."
        print_warning "If you created a backup, you can restore it using:"
        print_warning "  mysql -h $DB_HOST -P $DB_PORT -u $DB_USER -p $DB_NAME < $BACKUP_FILE"
        exit 1
    fi
}

# Run main function
main

