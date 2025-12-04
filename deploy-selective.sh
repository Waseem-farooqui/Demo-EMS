#!/bin/bash
###############################################################################
# Production Deployment Script - Selective Container Deployment
# Interactive script to select which container(s) to deploy
###############################################################################

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="${COMPOSE_FILE:-compose.yaml}"
BACKUP_DIR="${BACKUP_DIR:-/backups/ems}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Load environment variables
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
fi

# Banner
echo -e "${CYAN}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║   Production Deployment - Selective Container            ║${NC}"
echo -e "${CYAN}║              Choose Container(s) to Deploy                ║${NC}"
echo -e "${CYAN}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Functions
print_step() {
    echo -e "\n${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${CYAN}$1${NC}"
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"
}

print_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

# Show menu
print_step "Select Container(s) to Deploy"

echo "Available containers:"
echo "  1) MySQL"
echo "  2) Backend"
echo "  3) Frontend"
echo "  4) Backend + Frontend"
echo "  5) All Containers (with backup/restore)"
echo "  6) Exit"
echo ""

read -p "Enter your choice (1-6): " choice

case $choice in
    1)
        CONTAINER="mysql"
        DEPLOY_MYSQL=true
        DEPLOY_BACKEND=false
        DEPLOY_FRONTEND=false
        USE_BACKUP=false
        ;;
    2)
        CONTAINER="backend"
        DEPLOY_MYSQL=false
        DEPLOY_BACKEND=true
        DEPLOY_FRONTEND=false
        USE_BACKUP=false
        ;;
    3)
        CONTAINER="frontend"
        DEPLOY_MYSQL=false
        DEPLOY_BACKEND=false
        DEPLOY_FRONTEND=true
        USE_BACKUP=false
        ;;
    4)
        CONTAINER="backend-frontend"
        DEPLOY_MYSQL=false
        DEPLOY_BACKEND=true
        DEPLOY_FRONTEND=true
        USE_BACKUP=false
        ;;
    5)
        CONTAINER="all"
        DEPLOY_MYSQL=true
        DEPLOY_BACKEND=true
        DEPLOY_FRONTEND=true
        USE_BACKUP=true
        ;;
    6)
        print_info "Exiting..."
        exit 0
        ;;
    *)
        print_error "Invalid choice!"
        exit 1
        ;;
esac

print_info "Selected: $CONTAINER"

# Backup if deploying all
if [ "$USE_BACKUP" = true ]; then
    print_step "Creating Backup"
    
    BACKUP_PATH=""
    BACKUP_CREATED=false
    
    if docker-compose -f "$COMPOSE_FILE" ps | grep -q "Up"; then
        print_info "Creating backup..."
        mkdir -p "$BACKUP_DIR"
        BACKUP_PATH="$BACKUP_DIR/deploy_$TIMESTAMP"
        mkdir -p "$BACKUP_PATH"
        
        # Backup database
        if docker-compose -f "$COMPOSE_FILE" ps mysql | grep -q "Up"; then
            print_info "Backing up MySQL database..."
            sleep 5
            
            if docker-compose -f "$COMPOSE_FILE" exec -T mysql mysqldump \
                -u root -p"${DB_ROOT_PASSWORD}" \
                --single-transaction \
                --routines \
                --triggers \
                --events \
                --add-drop-database \
                --databases employee_management_system > "$BACKUP_PATH/database.sql" 2>/dev/null; then
                
                if [ -s "$BACKUP_PATH/database.sql" ]; then
                    print_success "Database backup created"
                    BACKUP_CREATED=true
                fi
            fi
        fi
        
        # Backup uploads
        if docker-compose -f "$COMPOSE_FILE" ps backend | grep -q "Up"; then
            print_info "Backing up uploaded files..."
            
            if docker-compose -f "$COMPOSE_FILE" exec -T backend test -d /app/uploads 2>/dev/null; then
                mkdir -p "$BACKUP_PATH/uploads"
                docker cp ems-backend:/app/uploads/. "$BACKUP_PATH/uploads/" 2>/dev/null || true
                FILE_COUNT=$(find "$BACKUP_PATH/uploads" -type f 2>/dev/null | wc -l)
                if [ "$FILE_COUNT" -gt 0 ]; then
                    print_success "Uploads backup created: $FILE_COUNT files"
                    BACKUP_CREATED=true
                fi
            fi
        fi
        
        echo "$BACKUP_PATH" > "$BACKUP_DIR/latest_backup_path.txt" 2>/dev/null || true
    fi
fi

# Stop selected containers
print_step "Stopping Selected Containers"

if [ "$DEPLOY_MYSQL" = true ]; then
    print_info "Stopping MySQL..."
    docker-compose -f "$COMPOSE_FILE" stop mysql 2>/dev/null || true
fi

if [ "$DEPLOY_BACKEND" = true ]; then
    print_info "Stopping Backend..."
    docker-compose -f "$COMPOSE_FILE" stop backend 2>/dev/null || true
fi

if [ "$DEPLOY_FRONTEND" = true ]; then
    print_info "Stopping Frontend..."
    docker-compose -f "$COMPOSE_FILE" stop frontend 2>/dev/null || true
fi

print_success "Containers stopped"

# Build selected containers
print_step "Building Selected Containers"

if [ "$DEPLOY_BACKEND" = true ]; then
    print_info "Building backend image..."
    export DOCKER_BUILDKIT=1
    docker-compose -f "$COMPOSE_FILE" build --no-cache backend || {
        print_error "Backend build failed!"
        exit 1
    }
fi

if [ "$DEPLOY_FRONTEND" = true ]; then
    print_info "Building frontend image..."
    docker-compose -f "$COMPOSE_FILE" build --no-cache frontend || {
        print_error "Frontend build failed!"
        exit 1
    }
fi

print_success "Builds complete"

# Start selected containers
print_step "Starting Selected Containers"

if [ "$DEPLOY_MYSQL" = true ]; then
    print_info "Starting MySQL..."
    docker-compose -f "$COMPOSE_FILE" up -d mysql
    print_info "Waiting for MySQL (30 seconds)..."
    sleep 30
    
    MAX_ATTEMPTS=30
    ATTEMPT=0
    while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
        if docker-compose -f "$COMPOSE_FILE" exec mysql mysqladmin ping -h localhost -u root -p"${DB_ROOT_PASSWORD}" --silent 2>/dev/null; then
            print_success "MySQL is ready"
            break
        fi
        ATTEMPT=$((ATTEMPT + 1))
        sleep 2
    done
    
    # Restore database if backup exists
    if [ "$USE_BACKUP" = true ] && [ -n "$BACKUP_PATH" ] && [ -f "$BACKUP_PATH/database.sql" ]; then
        print_info "Restoring database..."
        docker-compose -f "$COMPOSE_FILE" exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" < "$BACKUP_PATH/database.sql" 2>/dev/null || true
        print_success "Database restored"
    fi
fi

if [ "$DEPLOY_BACKEND" = true ]; then
    print_info "Starting Backend..."
    docker-compose -f "$COMPOSE_FILE" up -d backend
    print_info "Waiting for backend (60 seconds)..."
    sleep 60
    
    # Restore uploads if backup exists
    if [ "$USE_BACKUP" = true ] && [ -n "$BACKUP_PATH" ] && [ -d "$BACKUP_PATH/uploads" ]; then
        print_info "Restoring uploaded files..."
        docker-compose -f "$COMPOSE_FILE" exec -T backend mkdir -p /app/uploads 2>/dev/null || true
        docker cp "$BACKUP_PATH/uploads/." ems-backend:/app/uploads/ 2>/dev/null || true
        print_success "Uploads restored"
    fi
fi

if [ "$DEPLOY_FRONTEND" = true ]; then
    print_info "Starting Frontend..."
    docker-compose -f "$COMPOSE_FILE" up -d frontend
    print_info "Waiting for frontend (10 seconds)..."
    sleep 10
fi

# Final Status
print_step "Deployment Complete!"

print_info "Container Status:"
docker-compose -f "$COMPOSE_FILE" ps

echo ""
print_success "Selected container(s) deployed successfully!"
if [ "$USE_BACKUP" = true ] && [ -n "$BACKUP_PATH" ]; then
    print_info "Backup location: $BACKUP_PATH"
fi
echo ""

