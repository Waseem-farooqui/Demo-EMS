#!/bin/bash
# Fix Missing Tables - Create all tables manually

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

ROOT_PASSWORD="wuf27@1991"

echo -e "${BLUE}=== Fixing Missing Database Tables ===${NC}"
echo ""

# Check if create-tables.sql exists
if [ ! -f create-tables.sql ]; then
    echo -e "${RED}ERROR: create-tables.sql not found!${NC}"
    exit 1
fi

echo -e "${YELLOW}Creating all tables...${NC}"

# Execute the SQL script
sudo docker-compose exec -T mysql mysql -u root -p"$ROOT_PASSWORD" < create-tables.sql

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Tables created successfully!${NC}"
    
    echo ""
    echo -e "${YELLOW}Verifying tables...${NC}"
    
    # Check if documents table exists
    TABLE_COUNT=$(sudo docker-compose exec -T mysql mysql -u root -p"$ROOT_PASSWORD" -e "
        USE employee_management_system;
        SELECT COUNT(*) as count FROM information_schema.tables 
        WHERE table_schema = 'employee_management_system' 
        AND table_name IN ('documents', 'employees', 'users', 'organizations', 'departments');
    " | grep -v count | tail -1)
    
    echo "Found $TABLE_COUNT core tables"
    
    echo ""
    echo -e "${YELLOW}Restarting backend to pick up tables...${NC}"
    sudo docker-compose restart backend
    
    echo ""
    echo -e "${GREEN}✅ Done!${NC}"
    echo ""
    echo "Check logs:"
    echo "  sudo docker-compose logs -f backend"
else
    echo -e "${RED}✗ Failed to create tables${NC}"
    exit 1
fi

