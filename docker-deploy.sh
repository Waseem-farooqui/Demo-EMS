#!/bin/bash

###############################################################################
# Employee Management System - Docker Production Deployment Script
###############################################################################

set -e  # Exit on error

echo "🚀 Employee Management System - Docker Deployment"
echo "=================================================="
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker is not installed. Please install Docker first.${NC}"
    exit 1
fi

# Check if Docker Compose is installed
if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    echo -e "${RED}❌ Docker Compose is not installed. Please install Docker Compose first.${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Docker and Docker Compose are installed${NC}"
echo ""

# Check if .env file exists
if [ ! -f .env ]; then
    echo -e "${YELLOW}⚠️  .env file not found. Creating from template...${NC}"

    if [ -f .env.example ]; then
        cp .env.example .env
        echo -e "${BLUE}📝 .env file created from template${NC}"
        echo -e "${YELLOW}⚠️  Please edit .env file with your production values before continuing!${NC}"
        echo ""
        echo "Required changes:"
        echo "  1. Update database passwords (DB_PASSWORD, DB_ROOT_PASSWORD)"
        echo "  2. Generate secure JWT_SECRET (use: openssl rand -base64 64)"
        echo "  3. Configure email settings (MAIL_USERNAME, MAIL_PASSWORD)"
        echo "  4. Set your domain/URL (APP_URL, CORS_ALLOWED_ORIGINS)"
        echo ""
        read -p "Press Enter after updating .env file..."
    else
        echo -e "${RED}❌ .env.example file not found${NC}"
        exit 1
    fi
fi

echo -e "${GREEN}✅ .env file exists${NC}"
echo ""

# Load environment variables
source .env

# Function to deploy
deploy() {
    echo -e "${BLUE}🔨 Building and starting containers...${NC}"
    echo ""

    # Build images
    docker compose build --no-cache

    # Start services
    docker compose up -d

    echo ""
    echo -e "${GREEN}✅ Deployment completed successfully!${NC}"
    echo ""
    echo "Services status:"
    docker compose ps
    echo ""
    echo -e "${BLUE}📊 Application URLs:${NC}"
    echo "  Frontend: http://localhost:${FRONTEND_PORT:-80}"
    echo "  Backend API: http://localhost:${BACKEND_PORT:-8080}/api"
    echo "  Backend Health: http://localhost:${BACKEND_PORT:-8080}/api/actuator/health"
    echo ""
    echo -e "${BLUE}📝 Useful commands:${NC}"
    echo "  View logs: docker compose logs -f"
    echo "  View backend logs: docker compose logs -f backend"
    echo "  View frontend logs: docker compose logs -f frontend"
    echo "  Stop services: docker compose down"
    echo "  Restart services: docker compose restart"
    echo ""
}

# Function to show logs
show_logs() {
    echo -e "${BLUE}📋 Showing logs (Ctrl+C to exit)...${NC}"
    docker compose logs -f
}

# Function to stop
stop() {
    echo -e "${YELLOW}⏸️  Stopping services...${NC}"
    docker compose down
    echo -e "${GREEN}✅ Services stopped${NC}"
}

# Function to clean
clean() {
    echo -e "${RED}⚠️  This will remove all containers, volumes, and images!${NC}"
    read -p "Are you sure? (yes/no): " confirm

    if [ "$confirm" = "yes" ]; then
        echo -e "${YELLOW}🧹 Cleaning up...${NC}"
        docker compose down -v --rmi all
        echo -e "${GREEN}✅ Cleanup completed${NC}"
    else
        echo -e "${BLUE}Cleanup cancelled${NC}"
    fi
}

# Function to backup database
backup_db() {
    echo -e "${BLUE}💾 Creating database backup...${NC}"
    BACKUP_FILE="backup_$(date +%Y%m%d_%H%M%S).sql"
    docker compose exec -T mysql mysqldump -u${DB_USERNAME:-emsuser} -p${DB_PASSWORD:-emspassword} ${DB_NAME:-employee_management_system} > "$BACKUP_FILE"
    echo -e "${GREEN}✅ Database backup created: $BACKUP_FILE${NC}"
}

# Main menu
echo -e "${BLUE}Select an option:${NC}"
echo "  1) Deploy (build and start)"
echo "  2) Show logs"
echo "  3) Stop services"
echo "  4) Restart services"
echo "  5) Backup database"
echo "  6) Clean (remove all)"
echo "  7) Exit"
echo ""
read -p "Enter option (1-7): " option

case $option in
    1)
        deploy
        ;;
    2)
        show_logs
        ;;
    3)
        stop
        ;;
    4)
        echo -e "${BLUE}🔄 Restarting services...${NC}"
        docker compose restart
        echo -e "${GREEN}✅ Services restarted${NC}"
        ;;
    5)
        backup_db
        ;;
    6)
        clean
        ;;
    7)
        echo -e "${BLUE}Goodbye!${NC}"
        exit 0
        ;;
    *)
        echo -e "${RED}Invalid option${NC}"
        exit 1
        ;;
esac

