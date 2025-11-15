#!/bin/bash
# Find MySQL Root Password Script

echo "=== Finding MySQL Root Password ==="
echo ""

# Common root passwords to try
PASSWORDS=("rootpassword" "root" "" "password" "admin" "123456")

echo "Trying common root passwords..."
echo ""

for pass in "${PASSWORDS[@]}"; do
    if [ -z "$pass" ]; then
        echo "Trying: (empty password)"
        if docker-compose exec -T mysql mysql -u root -e "SELECT 1;" > /dev/null 2>&1; then
            echo "✓ SUCCESS! Root password is: (empty)"
            echo ""
            echo "Use this command:"
            echo "  docker-compose exec mysql mysql -u root -e \"ALTER USER 'emsuser'@'%' IDENTIFIED BY 'wud19@WUD'; FLUSH PRIVILEGES;\""
            exit 0
        fi
    else
        echo "Trying: $pass"
        if docker-compose exec -T mysql mysql -u root -p"$pass" -e "SELECT 1;" > /dev/null 2>&1; then
            echo "✓ SUCCESS! Root password is: $pass"
            echo ""
            echo "Use this command:"
            echo "  docker-compose exec mysql mysql -u root -p$pass -e \"ALTER USER 'emsuser'@'%' IDENTIFIED BY 'wud19@WUD'; FLUSH PRIVILEGES;\""
            exit 0
        fi
    fi
done

echo ""
echo "✗ Could not find root password"
echo ""
echo "Options:"
echo "1. Check your .env file for DB_ROOT_PASSWORD"
echo "2. Connect interactively: docker-compose exec mysql mysql -u root -p"
echo "3. Reset MySQL container (WILL DELETE DATA):"
echo "   docker-compose stop mysql"
echo "   docker volume rm employeemanagementsystem_mysql_data"
echo "   docker-compose up -d mysql"

