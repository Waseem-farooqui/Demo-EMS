#!/bin/bash
###############################################################################
# Production Deployment Script - All Containers
# Deploys all containers (MySQL, Backend, Frontend) with backup/restore
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
SSL_DIR="${SSL_DIR:-$SCRIPT_DIR/ssl}"
DOMAIN="${DOMAIN:-vertexdigitalsystem.com}"
SERVER_IP="${SERVER_IP:-}"  # Production server IP
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Load environment variables
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
fi

# Banner
echo -e "${CYAN}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║   Production Deployment - All Containers                  ║${NC}"
echo -e "${CYAN}║              With Backup & Restore                         ║${NC}"
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

# Step 1: Create Backup
print_step "Step 1/8: Creating Backup"

BACKUP_PATH=""
BACKUP_CREATED=false

if docker-compose -f "$COMPOSE_FILE" ps | grep -q "Up"; then
    print_info "Existing containers detected, creating backup..."
    
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
            --databases employee_management_system > "$BACKUP_PATH/database.sql" 2>"$BACKUP_PATH/database_backup.log"; then
            
            if [ -s "$BACKUP_PATH/database.sql" ]; then
                DB_SIZE=$(du -h "$BACKUP_PATH/database.sql" | cut -f1)
                print_success "Database backup created: $DB_SIZE"
                BACKUP_CREATED=true
            else
                print_warning "Database backup file is empty"
                rm -f "$BACKUP_PATH/database.sql"
            fi
        else
            print_warning "Database backup failed - check $BACKUP_PATH/database_backup.log"
        fi
    else
        print_info "MySQL container not running, skipping database backup"
    fi
    
    # Backup uploads
    if docker-compose -f "$COMPOSE_FILE" ps backend | grep -q "Up"; then
        print_info "Backing up uploaded files..."
        
        if docker-compose -f "$COMPOSE_FILE" exec -T backend test -d /app/uploads 2>/dev/null; then
            mkdir -p "$BACKUP_PATH/uploads"
            
            if docker cp ems-backend:/app/uploads/. "$BACKUP_PATH/uploads/" 2>"$BACKUP_PATH/uploads_backup.log"; then
                FILE_COUNT=$(find "$BACKUP_PATH/uploads" -type f 2>/dev/null | wc -l)
                if [ "$FILE_COUNT" -gt 0 ]; then
                    UPLOADS_SIZE=$(du -sh "$BACKUP_PATH/uploads" 2>/dev/null | cut -f1)
                    print_success "Uploads backup created: $FILE_COUNT files ($UPLOADS_SIZE)"
                    BACKUP_CREATED=true
                else
                    print_info "Uploads directory is empty"
                    rm -rf "$BACKUP_PATH/uploads"
                fi
            else
                print_warning "Uploads backup failed"
            fi
        fi
    else
        print_info "Backend container not running, skipping uploads backup"
    fi
    
    # Save backup path
    echo "$BACKUP_PATH" > "$BACKUP_DIR/latest_backup_path.txt" 2>/dev/null || true
    
    if [ "$BACKUP_CREATED" = true ]; then
        print_success "Backup created: $BACKUP_PATH"
    else
        print_warning "No data backed up (fresh deployment or containers not running)"
    fi
else
    print_info "No existing containers to backup"
fi

# Step 2: Stop Containers
print_step "Step 2/8: Stopping Containers"

print_info "Stopping all containers..."
docker-compose -f "$COMPOSE_FILE" down 2>/dev/null || true
docker stop ems-backend ems-frontend ems-mysql 2>/dev/null || true
print_success "Containers stopped"

# Step 3: Build Images
print_step "Step 3/8: Building Docker Images"

print_info "Building backend image..."
export DOCKER_BUILDKIT=1
docker-compose -f "$COMPOSE_FILE" build --no-cache backend || {
    print_error "Backend build failed!"
    exit 1
}

print_info "Building frontend image..."
print_info "Using host network for better DNS resolution during build..."
docker-compose -f "$COMPOSE_FILE" build --no-cache frontend || {
    print_error "Frontend build failed!"
    print_info "Troubleshooting DNS issues..."
    print_info "1. Check internet connectivity: ping -c 3 registry.npmjs.org"
    print_info "2. Check Docker DNS: docker run --rm node:18-alpine nslookup registry.npmjs.org"
    print_info "3. Try building with custom DNS: docker build --dns 8.8.8.8 --dns 8.8.4.4 -t test-frontend -f frontend/Dockerfile frontend/"
    exit 1
}

print_success "All images built"

# Step 4: Create Volumes
print_step "Step 4/8: Creating Volumes"

docker volume create employeemanagementsystem_mysql_data 2>/dev/null || true
docker volume create employeemanagementsystem_uploads_data 2>/dev/null || true
print_success "Volumes created"

# Step 5: Start MySQL
print_step "Step 5/8: Starting MySQL"

print_info "Starting MySQL container..."
docker-compose -f "$COMPOSE_FILE" up -d mysql

print_info "Waiting for MySQL to be ready (30 seconds)..."
sleep 30

MAX_ATTEMPTS=30
ATTEMPT=0
while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
    if docker-compose -f "$COMPOSE_FILE" exec mysql mysqladmin ping -h localhost -u root -p"${DB_ROOT_PASSWORD}" --silent 2>/dev/null; then
        print_success "MySQL is ready"
        break
    fi
    ATTEMPT=$((ATTEMPT + 1))
    echo -n "."
    sleep 2
done
echo ""

if [ $ATTEMPT -eq $MAX_ATTEMPTS ]; then
    print_error "MySQL failed to start"
    exit 1
fi

# Step 6: Restore Database
print_step "Step 6/8: Restoring Database"

# Load backup path if not set
if [ -z "$BACKUP_PATH" ] && [ -f "$BACKUP_DIR/latest_backup_path.txt" ]; then
    BACKUP_PATH=$(cat "$BACKUP_DIR/latest_backup_path.txt" 2>/dev/null || echo "")
fi

if [ -n "$BACKUP_PATH" ] && [ -f "$BACKUP_PATH/database.sql" ] && [ -s "$BACKUP_PATH/database.sql" ]; then
    print_info "Restoring database from backup..."
    
    if docker-compose -f "$COMPOSE_FILE" exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" < "$BACKUP_PATH/database.sql" 2>"$BACKUP_PATH/database_restore.log"; then
        print_success "Database restored successfully"
    else
        print_warning "Database restore had errors, but continuing..."
        print_info "Check: $BACKUP_PATH/database_restore.log"
    fi
else
    print_info "No database backup found, using fresh database"
    print_info "Database will be initialized by application"
fi

# Step 7: Start Backend
print_step "Step 7/8: Starting Backend"

print_info "Starting backend container..."
docker-compose -f "$COMPOSE_FILE" up -d backend

print_info "Waiting for backend to start (60 seconds)..."
sleep 60

# Restore uploads
if [ -n "$BACKUP_PATH" ] && [ -d "$BACKUP_PATH/uploads" ]; then
    print_info "Restoring uploaded files..."
    
    MAX_WAIT=30
    WAIT_COUNT=0
    while [ $WAIT_COUNT -lt $MAX_WAIT ]; do
        if docker-compose -f "$COMPOSE_FILE" exec -T backend test -d /app/uploads 2>/dev/null; then
            break
        fi
        WAIT_COUNT=$((WAIT_COUNT + 1))
        sleep 1
    done
    
    docker-compose -f "$COMPOSE_FILE" exec -T backend mkdir -p /app/uploads 2>/dev/null || true
    
    FILE_COUNT=$(find "$BACKUP_PATH/uploads" -type f 2>/dev/null | wc -l)
    if [ "$FILE_COUNT" -gt 0 ]; then
        print_info "Restoring $FILE_COUNT files..."
        if docker cp "$BACKUP_PATH/uploads/." ems-backend:/app/uploads/ 2>/dev/null; then
            print_success "Uploaded files restored"
        else
            print_warning "Failed to restore some files"
        fi
    fi
fi

# Step 8: Start Frontend
print_step "Step 8/8: Starting Frontend"

print_info "Starting frontend container..."
docker-compose -f "$COMPOSE_FILE" up -d frontend

print_info "Waiting for frontend to start (10 seconds)..."
sleep 10

# Final Status
print_step "Deployment Complete!"

print_info "Container Status:"
docker-compose -f "$COMPOSE_FILE" ps

echo ""
print_success "All containers deployed successfully!"
print_info "Frontend (Domain): https://vertexdigitalsystem.com"
if [ -n "$SERVER_IP" ]; then
    print_info "Frontend (IP): https://$SERVER_IP (or http://$SERVER_IP)"
fi
print_info "Backend API (Domain): https://vertexdigitalsystem.com/api"
if [ -n "$SERVER_IP" ]; then
    print_info "Backend API (IP): https://$SERVER_IP/api"
fi
print_info "Health Check: https://vertexdigitalsystem.com/api/actuator/health"
if [ -n "$BACKUP_PATH" ]; then
    print_info "Backup location: $BACKUP_PATH"
fi
echo ""

