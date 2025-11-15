#!/bin/bash
# Verify and Fix MySQL Password Script
# This script helps diagnose and fix password mismatches

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}=== MySQL Password Verification and Fix ===${NC}"
echo ""

# Check .env file
if [ ! -f .env ]; then
    echo -e "${RED}ERROR: .env file not found!${NC}"
    exit 1
fi

# Source .env
set -a
source .env
set +a

DB_PASSWORD=${DB_PASSWORD:-emspassword}
DB_ROOT_PASSWORD=${DB_ROOT_PASSWORD:-rootpassword}
DB_USERNAME=${DB_USERNAME:-emsuser}

echo -e "${YELLOW}Configuration from .env:${NC}"
echo "  DB_USERNAME: $DB_USERNAME"
echo "  DB_PASSWORD: $DB_PASSWORD"
echo "  DB_ROOT_PASSWORD: [hidden]"
echo ""

# Check if MySQL is running
if ! docker-compose ps mysql | grep -q "Up"; then
    echo -e "${RED}ERROR: MySQL container is not running!${NC}"
    exit 1
fi

echo -e "${YELLOW}Step 1: Testing root password...${NC}"

# Try to connect as root
ROOT_CONNECTED=false
for root_pass in "$DB_ROOT_PASSWORD" "rootpassword" "root" ""; do
    if docker-compose exec -T mysql mysql -u root -p"$root_pass" -e "SELECT 1;" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Root password works: ${root_pass:-'(empty)'}${NC}"
        ACTUAL_ROOT_PASS="$root_pass"
        ROOT_CONNECTED=true
        break
    fi
done

if [ "$ROOT_CONNECTED" = false ]; then
    echo -e "${RED}✗ Cannot connect with root password${NC}"
    echo ""
    echo "Please try connecting manually:"
    echo "  docker-compose exec mysql mysql -u root -p"
    echo ""
    echo "Or reset MySQL container (WILL DELETE DATA):"
    echo "  docker-compose stop mysql"
    echo "  docker volume rm employeemanagementsystem_mysql_data"
    echo "  docker-compose up -d mysql"
    exit 1
fi

echo ""
echo -e "${YELLOW}Step 2: Checking current emsuser password...${NC}"

# Try to connect as emsuser with the password from .env
if docker-compose exec -T mysql mysql -u "$DB_USERNAME" -p"$DB_PASSWORD" -e "SELECT 1;" > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Password is correct! User can connect.${NC}"
    echo ""
    echo "If backend still fails, check:"
    echo "  1. Backend environment variables: docker-compose exec backend env | grep DB_PASSWORD"
    echo "  2. Backend logs: docker-compose logs backend | tail -20"
    exit 0
else
    echo -e "${RED}✗ Password is incorrect or user doesn't exist${NC}"
fi

echo ""
echo -e "${YELLOW}Step 3: Fixing emsuser password...${NC}"

# Fix the password
docker-compose exec -T mysql mysql -u root -p"$ACTUAL_ROOT_PASS" <<EOF
ALTER USER IF EXISTS '$DB_USERNAME'@'%' IDENTIFIED BY '$DB_PASSWORD';
CREATE USER IF NOT EXISTS '$DB_USERNAME'@'%' IDENTIFIED BY '$DB_PASSWORD';
GRANT ALL PRIVILEGES ON employee_management_system.* TO '$DB_USERNAME'@'%';
GRANT CREATE ON *.* TO '$DB_USERNAME'@'%';
FLUSH PRIVILEGES;
SELECT 'Password updated successfully' AS status;
EOF

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Password updated successfully!${NC}"
    
    echo ""
    echo -e "${YELLOW}Step 4: Verifying connection...${NC}"
    if docker-compose exec -T mysql mysql -u "$DB_USERNAME" -p"$DB_PASSWORD" -e "SELECT 1;" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Verification successful! User can now connect.${NC}"
    else
        echo -e "${RED}✗ Verification failed. Please check manually.${NC}"
    fi
    
    echo ""
    echo -e "${YELLOW}Step 5: Restarting backend...${NC}"
    docker-compose restart backend
    
    echo ""
    echo -e "${GREEN}✅ Done! Check backend logs:${NC}"
    echo "   docker-compose logs -f backend"
else
    echo -e "${RED}✗ Failed to update password${NC}"
    exit 1
fi

