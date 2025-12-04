#!/bin/bash
###############################################################################
# Fresh Database Start Script
# Drops existing database and creates fresh database with JPA_DDL_AUTO=CREATE
# WARNING: This will DELETE all existing data!
###############################################################################

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

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

print_step() {
    echo ""
    echo -e "${BLUE}▶ $1${NC}"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
}

# Configuration
COMPOSE_FILE="${COMPOSE_FILE:-compose.yaml}"
ENV_FILE="${ENV_FILE:-.env}"

# Check if .env exists
if [ ! -f "$ENV_FILE" ]; then
    print_error ".env file not found!"
    print_info "Create it from prod.env: cp prod.env .env"
    exit 1
fi

# Determine compose command
if docker compose version &> /dev/null 2>&1; then
    COMPOSE_CMD="docker compose"
else
    COMPOSE_CMD="docker-compose"
fi

# Warning
print_error "╔════════════════════════════════════════════════════════════╗"
print_error "║  WARNING: This will DELETE ALL DATABASE DATA!             ║"
print_error "║  All tables, records, and data will be permanently lost! ║"
print_error "╚════════════════════════════════════════════════════════════╝"
echo ""
print_warning "This script will:"
echo "  1. Stop all containers"
echo "  2. Remove MySQL data volume (deletes all data)"
echo "  3. Set JPA_DDL_AUTO=create in .env"
echo "  4. Start fresh database and backend"
echo ""

# Confirmation
read -p "Are you sure you want to continue? Type 'YES' to confirm: " CONFIRM
if [ "$CONFIRM" != "YES" ]; then
    print_info "Operation cancelled"
    exit 0
fi

# Step 1: Backup current .env
print_step "Step 1/7: Backing Up Current .env"

if [ -f "$ENV_FILE" ]; then
    BACKUP_ENV="${ENV_FILE}.backup.$(date +%Y%m%d_%H%M%S)"
    cp "$ENV_FILE" "$BACKUP_ENV"
    print_success ".env backed up to: $BACKUP_ENV"
else
    print_warning ".env file not found, skipping backup"
fi

# Step 2: Stop Containers
print_step "Step 2/7: Stopping Containers"

print_info "Stopping all containers..."
$COMPOSE_CMD -f "$COMPOSE_FILE" down 2>/dev/null || true
docker stop ems-backend ems-frontend ems-mysql 2>/dev/null || true
print_success "Containers stopped"

# Step 3: Remove MySQL Volume
print_step "Step 3/7: Removing MySQL Data Volume"

print_info "Removing MySQL data volume (this deletes all database data)..."
$COMPOSE_CMD -f "$COMPOSE_FILE" down -v 2>/dev/null || true

# Remove volume by name (in case compose down -v didn't work)
VOLUME_NAME=$(docker volume ls | grep -E "mysql_data|employeemanagementsystem_mysql_data" | awk '{print $2}' | head -1 || echo "")
if [ -n "$VOLUME_NAME" ]; then
    print_info "Removing volume: $VOLUME_NAME"
    docker volume rm "$VOLUME_NAME" 2>/dev/null || true
    print_success "MySQL data volume removed"
else
    print_info "No MySQL volume found (may already be removed)"
fi

# Step 4: Update .env for JPA_DDL_AUTO=create
print_step "Step 4/7: Updating .env for Fresh Database"

# Read current JPA_DDL_AUTO
CURRENT_JPA_DDL=$(grep "^JPA_DDL_AUTO=" "$ENV_FILE" | cut -d '=' -f2 | tr -d '"' | tr -d "'" | xargs || echo "")

if [ "$CURRENT_JPA_DDL" = "create" ]; then
    print_info "JPA_DDL_AUTO is already set to 'create'"
else
    print_info "Updating JPA_DDL_AUTO to 'create'..."
    
    # Update or add JPA_DDL_AUTO
    if grep -q "^JPA_DDL_AUTO=" "$ENV_FILE"; then
        # Update existing line
        if [[ "$OSTYPE" == "darwin"* ]]; then
            # macOS
            sed -i '' "s/^JPA_DDL_AUTO=.*/JPA_DDL_AUTO=create/" "$ENV_FILE"
        else
            # Linux
            sed -i "s/^JPA_DDL_AUTO=.*/JPA_DDL_AUTO=create/" "$ENV_FILE"
        fi
    else
        # Add new line
        echo "" >> "$ENV_FILE"
        echo "# JPA Configuration - Set to 'create' for fresh database" >> "$ENV_FILE"
        echo "JPA_DDL_AUTO=create" >> "$ENV_FILE"
    fi
    
    print_success "JPA_DDL_AUTO set to 'create'"
fi

# Also disable Flyway temporarily (optional, but recommended when using JPA create)
print_info "Checking Flyway configuration..."
if grep -q "^SPRING_FLYWAY_ENABLED=" "$ENV_FILE"; then
    if [[ "$OSTYPE" == "darwin"* ]]; then
        sed -i '' "s/^SPRING_FLYWAY_ENABLED=.*/SPRING_FLYWAY_ENABLED=false/" "$ENV_FILE"
    else
        sed -i "s/^SPRING_FLYWAY_ENABLED=.*/SPRING_FLYWAY_ENABLED=false/" "$ENV_FILE"
    fi
    print_info "Flyway disabled (will be re-enabled after database is created)"
else
    # Add Flyway disable (optional)
    print_info "Note: Consider disabling Flyway when using JPA_DDL_AUTO=create"
fi

# Step 5: Create Fresh Volumes
print_step "Step 5/7: Creating Fresh Volumes"

print_info "Creating fresh MySQL data volume..."
docker volume create employeemanagementsystem_mysql_data 2>/dev/null || true
print_success "Volumes created"

# Step 6: Start MySQL
print_step "Step 6/7: Starting MySQL"

print_info "Starting MySQL container..."
$COMPOSE_CMD -f "$COMPOSE_FILE" up -d mysql

print_info "Waiting for MySQL to be ready (30 seconds)..."
sleep 30

# Wait for MySQL to be healthy
MAX_ATTEMPTS=30
ATTEMPT=0
DB_ROOT_PASSWORD=$(grep "^DB_ROOT_PASSWORD=" "$ENV_FILE" | cut -d '=' -f2 | tr -d '"' | tr -d "'" | xargs || echo "rootpassword")

while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
    if $COMPOSE_CMD -f "$COMPOSE_FILE" exec mysql mysqladmin ping -h localhost -u root -p"${DB_ROOT_PASSWORD}" --silent 2>/dev/null; then
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
    $COMPOSE_CMD -f "$COMPOSE_FILE" logs mysql | tail -20
    exit 1
fi

# Step 7: Start Backend with JPA_DDL_AUTO=create
print_step "Step 7/7: Starting Backend (JPA will create tables)"

print_info "Starting backend container..."
print_info "Backend will create all tables automatically using JPA_DDL_AUTO=create"
$COMPOSE_CMD -f "$COMPOSE_FILE" up -d backend

print_info "Waiting for backend to start and create tables (60 seconds)..."
sleep 60

# Check backend logs
print_info "Checking backend startup logs..."
$COMPOSE_CMD -f "$COMPOSE_FILE" logs backend --tail 30 | grep -i "started\|error\|exception\|table" || true

# Verify backend is running
if $COMPOSE_CMD -f "$COMPOSE_FILE" ps backend | grep -q "Up"; then
    print_success "Backend is running"
else
    print_error "Backend failed to start"
    print_info "Check logs: $COMPOSE_CMD -f $COMPOSE_FILE logs backend"
    exit 1
fi

# Final Status
print_step "Fresh Database Setup Complete!"

print_info "Container Status:"
$COMPOSE_CMD -f "$COMPOSE_FILE" ps

echo ""
print_success "Fresh database created successfully!"
print_info "JPA_DDL_AUTO is set to 'create' - tables will be created automatically"
print_warning "IMPORTANT: After tables are created, change JPA_DDL_AUTO back to 'validate'"
print_info "Edit .env and change: JPA_DDL_AUTO=validate"
print_info "Then restart backend: $COMPOSE_CMD restart backend"

echo ""
print_info "To verify database was created:"
echo "  $COMPOSE_CMD exec mysql mysql -u root -p\${DB_ROOT_PASSWORD} -e 'SHOW TABLES;' employee_management_system"

print_success "Done!"

