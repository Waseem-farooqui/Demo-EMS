#!/bin/bash
# Quick Password Fix - Using known root password

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

ROOT_PASSWORD="wuf27@1991"
EMSUSER_PASSWORD="wud19@WUD"

echo -e "${YELLOW}=== Fixing MySQL Password ===${NC}"
echo ""

# Fix emsuser password
echo "Updating emsuser password..."
sudo docker-compose exec -T mysql mysql -u root -p"$ROOT_PASSWORD" <<EOF
ALTER USER 'emsuser'@'%' IDENTIFIED BY '$EMSUSER_PASSWORD';
GRANT ALL PRIVILEGES ON employee_management_system.* TO 'emsuser'@'%';
GRANT CREATE ON *.* TO 'emsuser'@'%';
FLUSH PRIVILEGES;
SELECT 'Password updated successfully' AS status;
EOF

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Password updated!${NC}"
    
    # Update .env file
    echo ""
    echo "Updating .env file..."
    if ! grep -q "^SPRING_DATASOURCE_PASSWORD=" .env 2>/dev/null; then
        echo "SPRING_DATASOURCE_PASSWORD=$EMSUSER_PASSWORD" >> .env
    else
        sed -i "s/^SPRING_DATASOURCE_PASSWORD=.*/SPRING_DATASOURCE_PASSWORD=$EMSUSER_PASSWORD/" .env
    fi
    
    if ! grep -q "^DB_PASSWORD=" .env 2>/dev/null; then
        echo "DB_PASSWORD=$EMSUSER_PASSWORD" >> .env
    else
        sed -i "s/^DB_PASSWORD=.*/DB_PASSWORD=$EMSUSER_PASSWORD/" .env
    fi
    
    if ! grep -q "^DB_ROOT_PASSWORD=" .env 2>/dev/null; then
        echo "DB_ROOT_PASSWORD=$ROOT_PASSWORD" >> .env
    else
        sed -i "s|^DB_ROOT_PASSWORD=.*|DB_ROOT_PASSWORD=$ROOT_PASSWORD|" .env
    fi
    
    echo -e "${GREEN}✓ .env file updated${NC}"
    
    # Restart backend
    echo ""
    echo "Restarting backend..."
    sudo docker-compose stop backend
    sudo docker-compose rm -f backend
    sudo docker-compose up -d backend
    
    echo ""
    echo -e "${GREEN}✅ Done!${NC}"
    echo ""
    echo "Check logs:"
    echo "  sudo docker-compose logs -f backend"
else
    echo -e "${RED}✗ Failed${NC}"
    exit 1
fi

