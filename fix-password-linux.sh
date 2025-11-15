#!/bin/bash
# Fix MySQL Password - Linux Version
# Run this on your Linux server

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}=== MySQL Password Fix Tool ===${NC}"
echo ""

# Check if .env exists
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

echo -e "${YELLOW}From .env file:${NC}"
echo "  DB_ROOT_PASSWORD: [checking...]"
echo "  DB_PASSWORD: $DB_PASSWORD"
echo ""

# Try to find working root password
echo -e "${YELLOW}Trying to find root password...${NC}"

ROOT_PASS=""
PASSWORDS=("$DB_ROOT_PASSWORD" "rootpassword" "root" "" "password")

for pass in "${PASSWORDS[@]}"; do
    if [ -z "$pass" ]; then
        if sudo docker-compose exec -T mysql mysql -u root -e "SELECT 1;" > /dev/null 2>&1; then
            ROOT_PASS=""
            echo -e "${GREEN}✓ Found: (empty password)${NC}"
            break
        fi
    else
        if sudo docker-compose exec -T mysql mysql -u root -p"$pass" -e "SELECT 1;" > /dev/null 2>&1; then
            ROOT_PASS="$pass"
            echo -e "${GREEN}✓ Found: $pass${NC}"
            break
        fi
    fi
done

if [ -z "$ROOT_PASS" ] && ! sudo docker-compose exec -T mysql mysql -u root -e "SELECT 1;" > /dev/null 2>&1; then
    echo -e "${RED}✗ Cannot find working root password${NC}"
    echo ""
    echo "Please try connecting interactively:"
    echo "  sudo docker-compose exec mysql mysql -u root -p"
    echo ""
    echo "Or check your .env file:"
    echo "  cat .env | grep DB_ROOT_PASSWORD"
    exit 1
fi

echo ""
echo -e "${YELLOW}Fixing emsuser password...${NC}"

# Fix the password
if [ -z "$ROOT_PASS" ]; then
    sudo docker-compose exec -T mysql mysql -u root <<EOF
ALTER USER '$DB_USERNAME'@'%' IDENTIFIED BY '$DB_PASSWORD';
GRANT ALL PRIVILEGES ON employee_management_system.* TO '$DB_USERNAME'@'%';
GRANT CREATE ON *.* TO '$DB_USERNAME'@'%';
FLUSH PRIVILEGES;
SELECT 'Password updated' AS status;
EOF
else
    sudo docker-compose exec -T mysql mysql -u root -p"$ROOT_PASS" <<EOF
ALTER USER '$DB_USERNAME'@'%' IDENTIFIED BY '$DB_PASSWORD';
GRANT ALL PRIVILEGES ON employee_management_system.* TO '$DB_USERNAME'@'%';
GRANT CREATE ON *.* TO '$DB_USERNAME'@'%';
FLUSH PRIVILEGES;
SELECT 'Password updated' AS status;
EOF
fi

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Password updated successfully!${NC}"
    
    echo ""
    echo -e "${YELLOW}Updating .env file...${NC}"
    # Ensure SPRING_DATASOURCE_PASSWORD is set
    if ! grep -q "^SPRING_DATASOURCE_PASSWORD=" .env 2>/dev/null; then
        echo "SPRING_DATASOURCE_PASSWORD=$DB_PASSWORD" >> .env
        echo -e "${GREEN}✓ Added SPRING_DATASOURCE_PASSWORD to .env${NC}"
    else
        # Update existing line
        sed -i "s/^SPRING_DATASOURCE_PASSWORD=.*/SPRING_DATASOURCE_PASSWORD=$DB_PASSWORD/" .env
        echo -e "${GREEN}✓ Updated SPRING_DATASOURCE_PASSWORD in .env${NC}"
    fi
    
    echo ""
    echo -e "${YELLOW}Restarting backend...${NC}"
    sudo docker-compose stop backend
    sudo docker-compose rm -f backend
    sudo docker-compose up -d backend
    
    echo ""
    echo -e "${GREEN}✅ Done!${NC}"
    echo ""
    echo "Check logs:"
    echo "  sudo docker-compose logs -f backend"
else
    echo -e "${RED}✗ Failed to update password${NC}"
    exit 1
fi

