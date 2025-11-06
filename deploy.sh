#!/bin/bash

###############################################################################
# Employee Management System - Quick Deploy Script
# This script helps deploy the application quickly
###############################################################################

set -e  # Exit on error

echo "🚀 Employee Management System - Deployment Script"
echo "=================================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if running as root
if [ "$EUID" -ne 0 ]; then
    echo -e "${RED}❌ Please run as root (sudo)${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Running as root${NC}"
echo ""

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Check prerequisites
echo "📋 Checking prerequisites..."
echo ""

# Check Java
if command_exists java; then
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -ge 11 ]; then
        echo -e "${GREEN}✅ Java $JAVA_VERSION installed${NC}"
    else
        echo -e "${RED}❌ Java 11 or higher required${NC}"
        exit 1
    fi
else
    echo -e "${RED}❌ Java not found. Installing...${NC}"
    apt update
    apt install -y openjdk-11-jdk
fi

# Check Maven
if command_exists mvn; then
    echo -e "${GREEN}✅ Maven installed${NC}"
else
    echo -e "${RED}❌ Maven not found. Installing...${NC}"
    apt install -y maven
fi

# Check MySQL
if command_exists mysql; then
    echo -e "${GREEN}✅ MySQL installed${NC}"
else
    echo -e "${YELLOW}⚠️  MySQL not found. Installing...${NC}"
    apt install -y mysql-server
    mysql_secure_installation
fi

# Check Nginx
if command_exists nginx; then
    echo -e "${GREEN}✅ Nginx installed${NC}"
else
    echo -e "${YELLOW}⚠️  Nginx not found. Installing...${NC}"
    apt install -y nginx
fi

# Check Tesseract
if command_exists tesseract; then
    echo -e "${GREEN}✅ Tesseract OCR installed${NC}"
else
    echo -e "${YELLOW}⚠️  Tesseract not found. Installing...${NC}"
    apt install -y tesseract-ocr tesseract-ocr-eng
fi

echo ""
echo "📦 All prerequisites installed!"
echo ""

# Database setup
echo "🗄️  Setting up database..."
echo ""

read -p "Enter MySQL root password: " -s MYSQL_ROOT_PASSWORD
echo ""
read -p "Enter new database username [emp_admin]: " DB_USER
DB_USER=${DB_USER:-emp_admin}
read -p "Enter new database password: " -s DB_PASSWORD
echo ""

# Create database
mysql -u root -p"$MYSQL_ROOT_PASSWORD" <<EOF
CREATE DATABASE IF NOT EXISTS employee_management_system
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS '$DB_USER'@'localhost' IDENTIFIED BY '$DB_PASSWORD';
GRANT ALL PRIVILEGES ON employee_management_system.* TO '$DB_USER'@'localhost';
FLUSH PRIVILEGES;
EOF

echo -e "${GREEN}✅ Database created${NC}"
echo ""

# Environment variables
echo "🔐 Configuring environment variables..."
echo ""

JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n')

read -p "Enter email username: " MAIL_USERNAME
read -p "Enter email password: " -s MAIL_PASSWORD
echo ""
read -p "Enter application URL [http://localhost:8080]: " APP_URL
APP_URL=${APP_URL:-http://localhost:8080}

# Create .env file
cat > /opt/employee-management/.env <<EOF
DB_USERNAME=$DB_USER
DB_PASSWORD=$DB_PASSWORD
JWT_SECRET=$JWT_SECRET
MAIL_USERNAME=$MAIL_USERNAME
MAIL_PASSWORD=$MAIL_PASSWORD
APP_URL=$APP_URL
TESSERACT_DATA_PATH=/usr/share/tesseract-ocr/4.00/tessdata
EOF

echo -e "${GREEN}✅ Environment configured${NC}"
echo ""

# Build application
echo "🔨 Building application..."
echo ""

cd /opt/employee-management
mvn clean package -DskipTests -Pprod

echo -e "${GREEN}✅ Application built${NC}"
echo ""

# Create systemd service
echo "⚙️  Creating systemd service..."
echo ""

cat > /etc/systemd/system/employee-management.service <<'EOF'
[Unit]
Description=Employee Management System Backend
After=syslog.target network.target mysql.service

[Service]
Type=simple
User=www-data
WorkingDirectory=/opt/employee-management
EnvironmentFile=/opt/employee-management/.env
ExecStart=/usr/bin/java -jar \
    -Dspring.profiles.active=prod \
    -Xms512m -Xmx2g \
    /opt/employee-management/target/EmployeeManagementSystem-0.0.1-SNAPSHOT.jar

Restart=always
RestartSec=10

StandardOutput=journal
StandardError=journal
SyslogIdentifier=employee-management

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable employee-management
systemctl start employee-management

echo -e "${GREEN}✅ Service created and started${NC}"
echo ""

# Configure Nginx
echo "🌐 Configuring Nginx..."
echo ""

read -p "Enter your domain name [localhost]: " DOMAIN
DOMAIN=${DOMAIN:-localhost}

cat > /etc/nginx/sites-available/employee-management <<EOF
server {
    listen 80;
    server_name $DOMAIN;

    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;

        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";

        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    location / {
        root /var/www/employee-management/frontend;
        try_files \$uri \$uri/ /index.html;
    }

    client_max_body_size 10M;
}
EOF

ln -sf /etc/nginx/sites-available/employee-management /etc/nginx/sites-enabled/
nginx -t && systemctl reload nginx

echo -e "${GREEN}✅ Nginx configured${NC}"
echo ""

# Setup SSL (optional)
echo "🔒 Do you want to setup SSL with Let's Encrypt? (y/n)"
read -p "" SETUP_SSL

if [ "$SETUP_SSL" = "y" ]; then
    apt install -y certbot python3-certbot-nginx
    certbot --nginx -d "$DOMAIN"
    echo -e "${GREEN}✅ SSL configured${NC}"
fi

echo ""
echo "=============================================="
echo -e "${GREEN}✅ Deployment Complete!${NC}"
echo "=============================================="
echo ""
echo "📊 Application Status:"
systemctl status employee-management --no-pager | head -5
echo ""
echo "🌐 Access your application:"
echo "   URL: http://$DOMAIN"
echo "   API: http://$DOMAIN/api"
echo ""
echo "📋 Next Steps:"
echo "   1. Create ROOT user: curl -X POST http://$DOMAIN/api/auth/create-root"
echo "   2. Check logs: sudo journalctl -u employee-management -f"
echo "   3. Monitor health: curl http://$DOMAIN/api/actuator/health"
echo ""
echo "📖 Documentation:"
echo "   - Deployment Guide: /opt/employee-management/DEPLOYMENT_GUIDE.md"
echo "   - Checklist: /opt/employee-management/PRODUCTION_READINESS_CHECKLIST.md"
echo ""
echo -e "${GREEN}🎉 Enjoy your Employee Management System!${NC}"

