#!/bin/bash

# ===================================================================
# Script to Remove Blood Group and Emergency Contact Columns
# from employees table in Production Database
# ===================================================================
#
# This script safely removes the following columns from the employees table:
# - blood_group
# - emergency_contact_name
# - emergency_contact_phone
# - emergency_contact_relationship
#
# IMPORTANT: 
# 1. Backup your database before running this script
# 2. Test on a staging environment first
# 3. These columns will be permanently deleted
# 4. Any existing data in these columns will be lost
#
# Usage:
#   chmod +x remove-blood-group-emergency-contact-columns.sh
#   ./remove-blood-group-emergency-contact-columns.sh
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
DB_USER="${DB_USER:-root}"
SQL_FILE="remove-blood-group-emergency-contact-columns.sql"

echo -e "${YELLOW}========================================${NC}"
echo -e "${YELLOW}Database Column Removal Script${NC}"
echo -e "${YELLOW}========================================${NC}"
echo ""

# Check if SQL file exists
if [ ! -f "$SQL_FILE" ]; then
    echo -e "${RED}Error: SQL file '$SQL_FILE' not found!${NC}"
    exit 1
fi

# Prompt for database password
echo -e "${YELLOW}Database Configuration:${NC}"
echo "  Host: $DB_HOST"
echo "  Port: $DB_PORT"
echo "  Database: $DB_NAME"
echo "  User: $DB_USER"
echo ""

# Safety confirmation
echo -e "${RED}WARNING: This will permanently delete the following columns:${NC}"
echo "  - blood_group"
echo "  - emergency_contact_name"
echo "  - emergency_contact_phone"
echo "  - emergency_contact_relationship"
echo ""
echo -e "${RED}All data in these columns will be lost!${NC}"
echo ""
read -p "Have you backed up your database? (yes/no): " backup_confirm

if [ "$backup_confirm" != "yes" ]; then
    echo -e "${RED}Please backup your database before proceeding. Exiting...${NC}"
    exit 1
fi

read -p "Are you sure you want to proceed? (yes/no): " confirm

if [ "$confirm" != "yes" ]; then
    echo -e "${YELLOW}Operation cancelled.${NC}"
    exit 0
fi

echo ""
echo -e "${YELLOW}Executing SQL script...${NC}"
echo ""

# Execute SQL script
if mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p "$DB_NAME" < "$SQL_FILE"; then
    echo ""
    echo -e "${GREEN}✅ Script executed successfully!${NC}"
    echo ""
    echo -e "${GREEN}Verifying column removal...${NC}"
    
    # Verify columns are removed
    mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p "$DB_NAME" -e "
        SELECT 
            CASE 
                WHEN COUNT(*) = 0 THEN '✅ All columns successfully removed!'
                ELSE CONCAT('⚠️ Warning: ', COUNT(*), ' column(s) still exist')
            END AS result
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = '$DB_NAME'
        AND TABLE_NAME = 'employees'
        AND COLUMN_NAME IN ('blood_group', 'emergency_contact_name', 'emergency_contact_phone', 'emergency_contact_relationship');
    "
    
    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}Column removal completed!${NC}"
    echo -e "${GREEN}========================================${NC}"
else
    echo ""
    echo -e "${RED}❌ Error executing SQL script!${NC}"
    echo -e "${RED}Please check the error messages above.${NC}"
    exit 1
fi

