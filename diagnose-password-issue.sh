#!/bin/bash
# Comprehensive Password Diagnostic and Fix Script

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}=== MySQL Password Diagnostic Tool ===${NC}"
echo ""

# Step 1: Check .env file
echo -e "${YELLOW}[1] Checking .env file...${NC}"
if [ ! -f .env ]; then
    echo -e "${RED}✗ .env file not found!${NC}"
    exit 1
fi

# Source .env
set -a
source .env
set +a

DB_PASSWORD=${DB_PASSWORD:-emspassword}
DB_ROOT_PASSWORD=${DB_ROOT_PASSWORD:-rootpassword}
DB_USERNAME=${DB_USERNAME:-emsuser}

echo -e "${GREEN}✓ .env file found${NC}"
echo "  DB_USERNAME: $DB_USERNAME"
echo "  DB_PASSWORD: $DB_PASSWORD"
echo "  DB_ROOT_PASSWORD: [hidden]"
echo ""

# Step 2: Check what backend container sees
echo -e "${YELLOW}[2] Checking backend environment variables...${NC}"
BACKEND_DB_PASSWORD=$(docker-compose exec -T backend sh -c 'echo $DB_PASSWORD' 2>/dev/null || echo "NOT_SET")
BACKEND_SPRING_PASSWORD=$(docker-compose exec -T backend sh -c 'echo $SPRING_DATASOURCE_PASSWORD' 2>/dev/null || echo "NOT_SET")

echo "  Backend sees DB_PASSWORD: ${BACKEND_DB_PASSWORD:-NOT_SET}"
echo "  Backend sees SPRING_DATASOURCE_PASSWORD: ${BACKEND_SPRING_PASSWORD:-NOT_SET}"
echo ""

# Step 3: Find working root password
echo -e "${YELLOW}[3] Finding MySQL root password...${NC}"
ROOT_PASS=""
for pass in "$DB_ROOT_PASSWORD" "rootpassword" "root" ""; do
    if docker-compose exec -T mysql mysql -u root -p"$pass" -e "SELECT 1;" > /dev/null 2>&1; then
        ROOT_PASS="$pass"
        echo -e "${GREEN}✓ Root password found${NC}"
        break
    fi
done

if [ -z "$ROOT_PASS" ]; then
    echo -e "${RED}✗ Cannot find working root password${NC}"
    echo ""
    echo "Please connect manually:"
    echo "  docker-compose exec mysql mysql -u root -p"
    echo "  Then run the SQL commands from QUICK_PASSWORD_FIX.md"
    exit 1
fi

# Step 4: Check current MySQL user password
echo -e "${YELLOW}[4] Checking MySQL user configuration...${NC}"
MYSQL_USER_INFO=$(docker-compose exec -T mysql mysql -u root -p"$ROOT_PASS" -e "
SELECT User, Host FROM mysql.user WHERE User='$DB_USERNAME';
" 2>/dev/null || echo "ERROR")

if echo "$MYSQL_USER_INFO" | grep -q "$DB_USERNAME"; then
    echo -e "${GREEN}✓ User exists in MySQL${NC}"
else
    echo -e "${RED}✗ User does not exist!${NC}"
fi

# Step 5: Test current password
echo -e "${YELLOW}[5] Testing current password...${NC}"
if docker-compose exec -T mysql mysql -u "$DB_USERNAME" -p"$DB_PASSWORD" -e "SELECT 1;" > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Password is correct!${NC}"
    echo ""
    echo "If backend still fails, the issue might be:"
    echo "  1. Backend not reading .env correctly"
    echo "  2. Backend needs to be restarted"
    echo "  3. Environment variable not passed correctly"
    echo ""
    echo "Try: docker-compose down && docker-compose up -d"
    exit 0
else
    echo -e "${RED}✗ Password is incorrect${NC}"
fi

# Step 6: Fix the password
echo ""
echo -e "${YELLOW}[6] Fixing password...${NC}"
docker-compose exec -T mysql mysql -u root -p"$ROOT_PASS" <<EOF
ALTER USER IF EXISTS '$DB_USERNAME'@'%' IDENTIFIED BY '$DB_PASSWORD';
CREATE USER IF NOT EXISTS '$DB_USERNAME'@'%' IDENTIFIED BY '$DB_PASSWORD';
GRANT ALL PRIVILEGES ON employee_management_system.* TO '$DB_USERNAME'@'%';
GRANT CREATE ON *.* TO '$DB_USERNAME'@'%';
FLUSH PRIVILEGES;
SELECT 'Password fixed' AS status;
EOF

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Password updated in MySQL${NC}"
else
    echo -e "${RED}✗ Failed to update password${NC}"
    exit 1
fi

# Step 7: Verify
echo -e "${YELLOW}[7] Verifying...${NC}"
if docker-compose exec -T mysql mysql -u "$DB_USERNAME" -p"$DB_PASSWORD" -e "SELECT 1;" > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Verification successful!${NC}"
else
    echo -e "${RED}✗ Verification failed${NC}"
    exit 1
fi

# Step 8: Restart backend
echo ""
echo -e "${YELLOW}[8] Restarting backend...${NC}"
docker-compose stop backend
docker-compose up -d backend

echo ""
echo -e "${GREEN}✅ Done!${NC}"
echo ""
echo "Check backend logs:"
echo "  docker-compose logs -f backend"
echo ""
echo "You should see 'HikariPool-1 - Start completed' (not errors)"

