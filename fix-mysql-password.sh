#!/bin/bash
# Fix MySQL User Password Script
# This script fixes the emsuser password to match your .env file

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}=== Fixing MySQL User Password ===${NC}"
echo ""

# Check if .env file exists
if [ ! -f .env ]; then
    echo -e "${RED}ERROR: .env file not found!${NC}"
    echo "Please create .env file with DB_PASSWORD and DB_ROOT_PASSWORD"
    exit 1
fi

# Source .env file to get variables
set -a
source .env
set +a

# Get passwords from .env or use defaults
DB_PASSWORD=${DB_PASSWORD:-emspassword}
DB_ROOT_PASSWORD=${DB_ROOT_PASSWORD:-rootpassword}
DB_USERNAME=${DB_USERNAME:-emsuser}

# If root password fails, try common defaults
ROOT_PASSWORD_OPTIONS=("$DB_ROOT_PASSWORD" "rootpassword" "root" "")

echo -e "${YELLOW}Attempting to connect with root password from .env...${NC}"

echo -e "${GREEN}Using password from .env file${NC}"
echo "Username: $DB_USERNAME"
echo ""

# Check if MySQL container is running
if ! docker-compose ps mysql | grep -q "Up"; then
    echo -e "${RED}ERROR: MySQL container is not running!${NC}"
    echo "Start it with: docker-compose up -d mysql"
    exit 1
fi

echo "Resetting password for user '$DB_USERNAME'..."
echo ""

# Try to connect with root password
# If it fails, we'll provide manual instructions
if ! docker-compose exec -T mysql mysql -u root -p"$DB_ROOT_PASSWORD" -e "SELECT 1;" > /dev/null 2>&1; then
    echo -e "${RED}ERROR: Cannot connect with root password from .env${NC}"
    echo ""
    echo "Your DB_ROOT_PASSWORD in .env might be incorrect."
    echo ""
    echo "To fix this, you have two options:"
    echo ""
    echo "Option 1: Update .env with correct root password"
    echo "  Edit .env and set: DB_ROOT_PASSWORD=your_actual_root_password"
    echo ""
    echo "Option 2: Reset MySQL container (WILL DELETE DATA!)"
    echo "  docker-compose stop mysql"
    echo "  docker volume rm employeemanagementsystem_mysql_data"
    echo "  docker-compose up -d mysql"
    echo ""
    echo "Option 3: Connect manually and fix password"
    echo "  docker-compose exec mysql mysql -u root -p"
    echo "  Then run: ALTER USER 'emsuser'@'%' IDENTIFIED BY 'wud19@WUD';"
    echo "           FLUSH PRIVILEGES;"
    exit 1
fi

# Fix the password
docker-compose exec -T mysql mysql -u root -p"$DB_ROOT_PASSWORD" <<EOF
ALTER USER IF EXISTS '$DB_USERNAME'@'%' IDENTIFIED BY '$DB_PASSWORD';
CREATE USER IF NOT EXISTS '$DB_USERNAME'@'%' IDENTIFIED BY '$DB_PASSWORD';
GRANT ALL PRIVILEGES ON employee_management_system.* TO '$DB_USERNAME'@'%';
GRANT CREATE ON *.* TO '$DB_USERNAME'@'%';
FLUSH PRIVILEGES;
SELECT 'Password updated successfully' AS status;
EOF

if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}✅ Password updated successfully!${NC}"
    echo ""
    echo "Restarting backend container..."
    docker-compose restart backend
    echo ""
    echo -e "${GREEN}✅ Done! Check backend logs:${NC}"
    echo "   docker-compose logs -f backend"
else
    echo ""
    echo -e "${RED}❌ Failed to update password${NC}"
    exit 1
fi

