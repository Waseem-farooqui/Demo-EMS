#!/bin/bash
# fresh-deploy.sh - Fresh Deployment Script for Ubuntu 24.04
# Employee Management System - Complete automated installation

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
APP_DIR="/opt/EmployeeManagementSystem"
BACKUP_DIR="/backups/ems"
DB_NAME="employee_management_system"

echo -e "${CYAN}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║     Employee Management System - Fresh Deployment         ║${NC}"
echo -e "${CYAN}║              Ubuntu 24.04 LTS                              ║${NC}"
echo -e "${CYAN}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${YELLOW}This script will:${NC}"
echo "  ✓ Update system packages"
echo "  ✓ Install Docker & Docker Compose"
echo "  ✓ Clone application repository"
echo "  ✓ Configure environment variables"
echo "  ✓ Build and deploy containers"
echo "  ✓ Setup automated backups"
echo "  ✓ Configure firewall"
echo ""
read -p "Continue with fresh deployment? (y/n): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Deployment cancelled."
    exit 0
fi

# Function to print step
print_step() {
    echo ""
    echo -e "${BLUE}▶ $1${NC}"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
}

# Function to print success
print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

# Function to print error
print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Function to print warning
print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

# Function to print info
print_info() {
    echo -e "${CYAN}ℹ️  $1${NC}"
}

# Check if running as root
if [ "$EUID" -eq 0 ]; then
    print_error "Please do not run this script as root. Run as regular user with sudo privileges."
    exit 1
fi

# Check Ubuntu version
print_step "Step 1/15: Checking system requirements"
if [ -f /etc/os-release ]; then
    . /etc/os-release
    if [[ "$ID" == "ubuntu" && "$VERSION_ID" == "24.04" ]]; then
        print_success "Ubuntu 24.04 LTS detected"
    else
        print_warning "This script is designed for Ubuntu 24.04, but detected: $ID $VERSION_ID"
        read -p "Continue anyway? (y/n): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    fi
else
    print_error "Cannot detect OS version"
    exit 1
fi

# Update system
print_step "Step 2/15: Updating system packages"
sudo apt-get update
sudo apt-get upgrade -y
print_success "System packages updated"

# Install prerequisites
print_step "Step 3/15: Installing prerequisites"
sudo apt-get install -y \
    apt-transport-https \
    ca-certificates \
    curl \
    gnupg \
    lsb-release \
    git \
    vim \
    wget \
    software-properties-common \
    ufw
print_success "Prerequisites installed"

# Install Docker
print_step "Step 4/15: Installing Docker"
if command -v docker &> /dev/null; then
    print_warning "Docker already installed, skipping"
else
    # Add Docker's official GPG key
    sudo install -m 0755 -d /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    sudo chmod a+r /etc/apt/keyrings/docker.gpg

    # Add Docker repository
    echo \
        "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
        $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
        sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

    # Install Docker
    sudo apt-get update
    sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

    # Start and enable Docker
    sudo systemctl start docker
    sudo systemctl enable docker

    # Add current user to docker group
    sudo usermod -aG docker $USER

    print_success "Docker installed successfully"
    print_warning "You may need to log out and back in for Docker group changes to take effect"
fi

# Verify Docker installation
docker --version
docker compose version

# Install Docker Compose (standalone) if not available
print_step "Step 5/15: Checking Docker Compose"
if ! command -v docker-compose &> /dev/null; then
    print_info "Installing Docker Compose standalone..."
    sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    sudo chmod +x /usr/local/bin/docker-compose
    print_success "Docker Compose standalone installed"
else
    print_success "Docker Compose already available"
fi

# Create application directory
print_step "Step 6/15: Creating application directory"
sudo mkdir -p $APP_DIR
sudo chown -R $USER:$USER $APP_DIR
print_success "Application directory created: $APP_DIR"

# Clone repository
print_step "Step 7/15: Cloning application repository"
read -p "Enter Git repository URL: " REPO_URL
if [ -z "$REPO_URL" ]; then
    print_error "Repository URL cannot be empty"
    exit 1
fi

cd /tmp
git clone $REPO_URL EmployeeManagementSystem-temp
sudo cp -r EmployeeManagementSystem-temp/* $APP_DIR/
sudo chown -R $USER:$USER $APP_DIR
rm -rf EmployeeManagementSystem-temp
cd $APP_DIR
print_success "Repository cloned successfully"

# Generate secure credentials
print_step "Step 8/15: Generating secure credentials"
JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n')
DB_ROOT_PASSWORD=$(openssl rand -base64 32 | tr -d '\n')
DB_PASSWORD=$(openssl rand -base64 32 | tr -d '\n')
print_success "Secure credentials generated"

# Configure environment variables
print_step "Step 9/15: Configuring environment variables"
read -p "Enter your domain (e.g., example.com): " DOMAIN
if [ -z "$DOMAIN" ]; then
    DOMAIN="localhost"
    print_warning "Using localhost as domain"
fi

read -p "Enter your email for notifications (optional, press Enter to skip): " EMAIL
if [ ! -z "$EMAIL" ]; then
    read -p "Enter Gmail app password (16 chars, optional): " EMAIL_PASSWORD
fi

# Create .env file
cat > .env << EOF
# Database Configuration
DB_NAME=${DB_NAME}
DB_USERNAME=emsuser
DB_PASSWORD=${DB_PASSWORD}
DB_ROOT_PASSWORD=${DB_ROOT_PASSWORD}
DB_PORT=3307

# JWT Configuration
JWT_SECRET=${JWT_SECRET}
JWT_EXPIRATION=86400000

# Email Configuration (Optional)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=${EMAIL}
MAIL_PASSWORD=${EMAIL_PASSWORD}
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=true
MAIL_SMTP_STARTTLS_REQUIRED=false
MAIL_SMTP_SSL_TRUST=*
EMAIL_FROM_NAME=Employee Management System
EMAIL_FROM_ADDRESS=${EMAIL}

# Application URLs
APP_URL=http://${DOMAIN}
CORS_ALLOWED_ORIGINS=http://${DOMAIN},https://${DOMAIN}

# Port Configuration
BACKEND_PORT=8080
FRONTEND_PORT=80
EOF

chmod 600 .env
print_success "Environment variables configured"
print_info "Configuration saved to: $APP_DIR/.env"

# Update frontend environment
print_step "Step 10/15: Configuring frontend"
if [ -f frontend/src/environments/environment.prod.ts ]; then
    cat > frontend/src/environments/environment.prod.ts << EOF
export const environment = {
  production: true,
  apiUrl: 'http://${DOMAIN}:8080/api',
  apiBaseUrl: 'http://${DOMAIN}:8080',
  frontendUrl: 'http://${DOMAIN}',
  maxFileSize: 10485760,
  allowedFileTypes: ['image/jpeg', 'image/png', 'image/jpg', 'application/pdf']
};
EOF
    print_success "Frontend environment configured"
else
    print_warning "Frontend environment file not found, skipping"
fi

# Configure firewall
print_step "Step 11/15: Configuring firewall"
read -p "Configure UFW firewall? (y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    sudo ufw --force enable
    sudo ufw default deny incoming
    sudo ufw default allow outgoing
    sudo ufw allow ssh
    sudo ufw allow 80/tcp
    sudo ufw allow 443/tcp
    sudo ufw allow 8080/tcp
    sudo ufw status
    print_success "Firewall configured"
else
    print_warning "Firewall configuration skipped"
fi

# Build and deploy
print_step "Step 12/15: Building Docker images"
docker compose build --no-cache
print_success "Docker images built"

# Start services
print_step "Step 13/15: Starting services"
docker compose up -d
print_success "Services started"

# Wait for services to be healthy
print_step "Step 14/15: Waiting for services to be healthy"
echo "This may take 1-2 minutes..."
sleep 30

# Check service health
MAX_ATTEMPTS=30
ATTEMPT=0

while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
    if curl -f http://localhost:8080/api/actuator/health > /dev/null 2>&1; then
        print_success "Backend is healthy"
        break
    fi
    ATTEMPT=$((ATTEMPT + 1))
    echo -n "."
    sleep 2
done

if [ $ATTEMPT -eq $MAX_ATTEMPTS ]; then
    print_error "Backend health check failed"
    echo "Checking logs..."
    docker compose logs backend --tail=50
    exit 1
fi

if curl -f http://localhost:80/ > /dev/null 2>&1; then
    print_success "Frontend is healthy"
else
    print_warning "Frontend may not be ready yet"
fi

# Setup automated backups
print_step "Step 15/15: Setting up automated backups"
read -p "Setup automated daily backups? (y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    sudo mkdir -p $BACKUP_DIR
    sudo chown -R $USER:$USER $BACKUP_DIR

    # Make backup script executable
    chmod +x backup.sh

    # Add to crontab (daily at 2 AM)
    (crontab -l 2>/dev/null | grep -v "backup.sh"; echo "0 2 * * * cd $APP_DIR && ./backup.sh >> /var/log/ems-backup.log 2>&1") | crontab -

    print_success "Automated backups configured (daily at 2 AM)"
    print_info "Backup location: $BACKUP_DIR"
else
    print_warning "Automated backups not configured"
fi

# Print deployment summary
echo ""
echo -e "${GREEN}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║         DEPLOYMENT COMPLETED SUCCESSFULLY!                 ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${CYAN}📊 Deployment Summary:${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${YELLOW}Application:${NC}"
echo "  • Location: $APP_DIR"
echo "  • Domain: $DOMAIN"
echo "  • Frontend: http://$DOMAIN"
echo "  • Backend: http://$DOMAIN:8080"
echo ""
echo -e "${YELLOW}Services:${NC}"
docker compose ps
echo ""
echo -e "${YELLOW}Database:${NC}"
echo "  • Name: $DB_NAME"
echo "  • Port: 3307"
echo "  • Username: emsuser"
echo "  • Password: (stored in .env)"
echo ""
echo -e "${YELLOW}Credentials File:${NC}"
echo "  • Location: $APP_DIR/.env"
echo "  • JWT Secret: (auto-generated, 64+ chars)"
echo "  • DB Password: (auto-generated, 32+ chars)"
echo ""
echo -e "${YELLOW}Next Steps:${NC}"
echo "  1. Access application: http://$DOMAIN"
echo "  2. Create ROOT account (first-time setup)"
echo "  3. Configure SSL/HTTPS (recommended)"
echo "  4. Review configuration guide: POST_INSTALL_CONFIG.md"
echo ""
echo -e "${YELLOW}Useful Commands:${NC}"
echo "  • View logs: docker compose logs -f"
echo "  • Restart: docker compose restart"
echo "  • Stop: docker compose down"
echo "  • Backup: ./backup.sh"
echo "  • Monitor: ./monitor.sh"
echo ""
echo -e "${YELLOW}Configuration:${NC}"
echo "  • Review: $APP_DIR/.env"
echo "  • Security: Run ./security-fixes.sh"
echo "  • SSL: See DOCKER_DEPLOYMENT_GUIDE.md"
echo ""
echo -e "${CYAN}📖 Documentation:${NC}"
echo "  • Deployment Guide: $APP_DIR/DOCKER_DEPLOYMENT_GUIDE.md"
echo "  • Configuration: $APP_DIR/POST_INSTALL_CONFIG.md"
echo "  • Security: $APP_DIR/SECURITY_AUDIT_REPORT.md"
echo ""

# Save deployment info
cat > deployment-info.txt << EOF
Employee Management System - Deployment Information
═══════════════════════════════════════════════════

Deployment Date: $(date)
Server: $(hostname)
OS: $(lsb_release -d | cut -f2)
User: $USER

Application Details:
  - Directory: $APP_DIR
  - Domain: $DOMAIN
  - Frontend URL: http://$DOMAIN
  - Backend URL: http://$DOMAIN:8080

Database:
  - Name: $DB_NAME
  - Port: 3307
  - Username: emsuser

Credentials:
  - All credentials stored in: $APP_DIR/.env
  - JWT Secret: Auto-generated (64+ chars)
  - DB Root Password: Auto-generated (32+ chars)
  - DB Password: Auto-generated (32+ chars)

Services:
$(docker compose ps)

Next Steps:
  1. Access: http://$DOMAIN
  2. Create ROOT account
  3. Configure SSL/HTTPS
  4. Review security settings
  5. Setup monitoring

Support:
  - Documentation: $APP_DIR/DOCKER_DEPLOYMENT_GUIDE.md
  - Configuration: $APP_DIR/POST_INSTALL_CONFIG.md
  - Security: $APP_DIR/SECURITY_AUDIT_REPORT.md
EOF

print_success "Deployment info saved to: $APP_DIR/deployment-info.txt"

echo ""
print_info "IMPORTANT: If you added user to docker group, logout and login again"
print_info "OR run: newgrp docker"
echo ""

# Offer to open application
if command -v xdg-open &> /dev/null; then
    read -p "Open application in browser? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        xdg-open "http://$DOMAIN" &> /dev/null &
    fi
fi

echo -e "${GREEN}✅ Fresh deployment completed successfully!${NC}"
echo ""

