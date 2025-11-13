#!/bin/bash
# cleanup-vm.sh - Complete VM Cleanup and Reset Script
# This script removes all Employee Management System installations and data

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${RED}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${RED}║         COMPLETE VM CLEANUP - REMOVE ALL DATA             ║${NC}"
echo -e "${RED}║          Employee Management System                       ║${NC}"
echo -e "${RED}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${YELLOW}⚠️  WARNING: This will PERMANENTLY DELETE:${NC}"
echo "  • All Docker containers, images, and volumes"
echo "  • All application data and databases"
echo "  • All uploaded documents"
echo "  • All configuration files"
echo "  • All logs and backups"
echo "  • Docker and Docker Compose (optional)"
echo "  • Application source code (optional)"
echo ""
echo -e "${RED}THIS CANNOT BE UNDONE!${NC}"
echo ""
read -p "Are you absolutely sure you want to continue? Type 'DELETE EVERYTHING' to proceed: " confirmation

if [ "$confirmation" != "DELETE EVERYTHING" ]; then
    echo -e "${GREEN}Cleanup cancelled. No changes made.${NC}"
    exit 0
fi

echo ""
echo -e "${BLUE}Starting complete cleanup...${NC}"
echo ""

# Function to print step
print_step() {
    echo -e "${BLUE}▶ $1${NC}"
}

# Function to print success
print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

# Function to print warning
print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

# Step 1: Stop all running containers
print_step "Step 1/12: Stopping all Docker containers..."
if command -v docker &> /dev/null; then
    docker stop $(docker ps -aq) 2>/dev/null || true
    print_success "All containers stopped"
else
    print_warning "Docker not installed, skipping"
fi

# Step 2: Remove Employee Management System containers
print_step "Step 2/12: Removing EMS containers..."
if command -v docker &> /dev/null; then
    docker rm -f ems-backend ems-frontend ems-mysql 2>/dev/null || true
    docker rm -f ems-backend-blue ems-frontend-blue 2>/dev/null || true
    print_success "EMS containers removed"
fi

# Step 3: Remove all Docker containers
print_step "Step 3/12: Removing all Docker containers..."
if command -v docker &> /dev/null; then
    docker rm -f $(docker ps -aq) 2>/dev/null || true
    print_success "All containers removed"
fi

# Step 4: Remove Docker images
print_step "Step 4/12: Removing Docker images..."
if command -v docker &> /dev/null; then
    # Remove EMS images
    docker rmi -f $(docker images | grep -E 'ems-|employeemanagementsystem' | awk '{print $3}') 2>/dev/null || true

    read -p "Remove ALL Docker images? (y/N): " remove_all_images
    if [[ $remove_all_images =~ ^[Yy]$ ]]; then
        docker rmi -f $(docker images -aq) 2>/dev/null || true
        print_success "All Docker images removed"
    else
        print_success "EMS Docker images removed"
    fi
fi

# Step 5: Remove Docker volumes
print_step "Step 5/12: Removing Docker volumes..."
if command -v docker &> /dev/null; then
    # Remove EMS volumes
    docker volume rm employeemanagementsystem_mysql_data 2>/dev/null || true
    docker volume rm employeemanagementsystem_uploads_data 2>/dev/null || true

    read -p "Remove ALL Docker volumes? (y/N): " remove_all_volumes
    if [[ $remove_all_volumes =~ ^[Yy]$ ]]; then
        docker volume prune -f 2>/dev/null || true
        print_success "All Docker volumes removed"
    else
        print_success "EMS Docker volumes removed"
    fi
fi

# Step 6: Remove Docker networks
print_step "Step 6/12: Removing Docker networks..."
if command -v docker &> /dev/null; then
    docker network rm employeemanagementsystem_ems-network 2>/dev/null || true
    docker network prune -f 2>/dev/null || true
    print_success "Docker networks removed"
fi

# Step 7: Clean Docker system
print_step "Step 7/12: Cleaning Docker system..."
if command -v docker &> /dev/null; then
    read -p "Run full Docker system prune? (y/N): " docker_prune
    if [[ $docker_prune =~ ^[Yy]$ ]]; then
        docker system prune -a --volumes -f
        print_success "Docker system cleaned"
    else
        print_warning "Skipped Docker system prune"
    fi
fi

# Step 8: Remove application directory
print_step "Step 8/12: Removing application files..."
read -p "Remove application source code directory? (y/N): " remove_source
if [[ $remove_source =~ ^[Yy]$ ]]; then
    APP_DIR="/opt/EmployeeManagementSystem"
    read -p "Enter application directory path (default: $APP_DIR): " custom_dir
    if [ ! -z "$custom_dir" ]; then
        APP_DIR="$custom_dir"
    fi

    if [ -d "$APP_DIR" ]; then
        rm -rf "$APP_DIR"
        print_success "Application directory removed: $APP_DIR"
    else
        print_warning "Directory not found: $APP_DIR"
    fi
fi

# Step 9: Remove backups
print_step "Step 9/12: Removing backups..."
read -p "Remove all backups? (y/N): " remove_backups
if [[ $remove_backups =~ ^[Yy]$ ]]; then
    BACKUP_DIR="/backups/ems"
    read -p "Enter backup directory path (default: $BACKUP_DIR): " custom_backup
    if [ ! -z "$custom_backup" ]; then
        BACKUP_DIR="$custom_backup"
    fi

    if [ -d "$BACKUP_DIR" ]; then
        rm -rf "$BACKUP_DIR"
        print_success "Backup directory removed: $BACKUP_DIR"
    fi

    # Remove backup files in current directory
    rm -f backup_*.sql* 2>/dev/null || true
    rm -rf uploads_backup_* 2>/dev/null || true
    rm -f .env.backup_* 2>/dev/null || true
    print_success "Local backup files removed"
fi

# Step 10: Remove logs
print_step "Step 10/12: Removing logs..."
rm -rf /var/log/ems* 2>/dev/null || true
rm -rf logs/ 2>/dev/null || true
print_success "Logs removed"

# Step 11: Remove cron jobs
print_step "Step 11/12: Removing cron jobs..."
read -p "Remove EMS-related cron jobs? (y/N): " remove_cron
if [[ $remove_cron =~ ^[Yy]$ ]]; then
    crontab -l 2>/dev/null | grep -v "backup.sh\|monitor.sh\|ems" | crontab - 2>/dev/null || true
    print_success "Cron jobs removed"
fi

# Step 12: Uninstall Docker (optional)
print_step "Step 12/12: Docker removal..."
read -p "Uninstall Docker and Docker Compose? (y/N): " uninstall_docker
if [[ $uninstall_docker =~ ^[Yy]$ ]]; then
    if command -v docker &> /dev/null; then
        # Detect OS and uninstall accordingly
        if [ -f /etc/debian_version ]; then
            # Debian/Ubuntu
            sudo apt-get purge -y docker-ce docker-ce-cli containerd.io docker-compose-plugin 2>/dev/null || true
            sudo apt-get autoremove -y
            sudo rm -rf /var/lib/docker
            sudo rm -rf /var/lib/containerd
            print_success "Docker removed (Debian/Ubuntu)"
        elif [ -f /etc/redhat-release ]; then
            # RHEL/CentOS
            sudo yum remove -y docker-ce docker-ce-cli containerd.io docker-compose-plugin 2>/dev/null || true
            sudo rm -rf /var/lib/docker
            sudo rm -rf /var/lib/containerd
            print_success "Docker removed (RHEL/CentOS)"
        else
            print_warning "Unknown OS, manual Docker removal may be required"
        fi
    fi

    # Remove Docker Compose
    sudo rm -f /usr/local/bin/docker-compose 2>/dev/null || true
    sudo rm -f /usr/bin/docker-compose 2>/dev/null || true
    print_success "Docker Compose removed"
fi

# Additional cleanup
print_step "Additional cleanup..."

# Remove environment files
rm -f .env 2>/dev/null || true
rm -f .env.* 2>/dev/null || true

# Remove node_modules (if exists)
rm -rf frontend/node_modules 2>/dev/null || true

# Remove Maven target
rm -rf target 2>/dev/null || true

# Remove IDE files
rm -rf .idea 2>/dev/null || true
rm -rf .vscode 2>/dev/null || true
rm -f *.iml 2>/dev/null || true

print_success "Additional cleanup complete"

echo ""
echo -e "${GREEN}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║              CLEANUP COMPLETED SUCCESSFULLY                ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo "Summary of actions taken:"
echo "  ✅ Docker containers stopped and removed"
echo "  ✅ Docker images removed"
echo "  ✅ Docker volumes removed (all data deleted)"
echo "  ✅ Docker networks removed"
echo "  ✅ Application files removed"
echo "  ✅ Backups removed"
echo "  ✅ Logs removed"
echo "  ✅ Cron jobs removed"
echo ""

if [[ $uninstall_docker =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}Docker and Docker Compose have been uninstalled.${NC}"
    echo ""
fi

echo -e "${BLUE}Your VM has been restored to a clean state.${NC}"
echo ""
echo "To verify cleanup:"
echo "  docker ps -a          # Should show no containers"
echo "  docker images         # Should show no/few images"
echo "  docker volume ls      # Should show no/few volumes"
echo "  df -h                 # Check freed disk space"
echo ""
echo "To reinstall the application:"
echo "  1. Install Docker and Docker Compose"
echo "  2. Clone the repository"
echo "  3. Follow the deployment guide"
echo ""

