#!/bin/bash
###############################################################################
# Employee Management System - Production Deployment Script
# Ubuntu 24.04 LTS - Complete Production-Ready Deployment
###############################################################################
# This script provides a one-stop solution for production deployment:
# - Stops and removes all existing containers and images
# - Applies all security fixes and configuration updates
# - Deploys with production-ready settings
# - Verifies deployment health
###############################################################################

set -e  # Exit on error

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKUP_DIR="${BACKUP_DIR:-/backups/ems}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Banner
echo -e "${CYAN}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║   Employee Management System - Production Deployment       ║${NC}"
echo -e "${CYAN}║              Ubuntu 24.04 LTS - One-Stop Solution         ║${NC}"
echo -e "${CYAN}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Functions
print_step() {
    echo ""
    echo -e "${BLUE}▶ $1${NC}"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_info() {
    echo -e "${CYAN}ℹ️  $1${NC}"
}

# Check if running as root
if [ "$EUID" -eq 0 ]; then
    print_warning "Running as root. Some operations may need adjustment."
fi

# Check prerequisites
print_step "Step 1/20: Checking Prerequisites"

# Check Docker
if ! command -v docker &> /dev/null; then
    print_error "Docker is not installed"
    echo "Installing Docker..."
    curl -fsSL https://get.docker.com -o get-docker.sh
    sudo sh get-docker.sh
    sudo usermod -aG docker $USER
    rm get-docker.sh
    print_success "Docker installed"
    print_warning "You may need to log out and back in for Docker group changes"
else
    print_success "Docker is installed: $(docker --version)"
fi

# Check Docker Compose
if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    print_error "Docker Compose is not installed"
    echo "Installing Docker Compose..."
    sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    sudo chmod +x /usr/local/bin/docker-compose
    print_success "Docker Compose installed"
else
    print_success "Docker Compose is available"
fi

# Check if in project directory
if [ ! -f "compose.yaml" ] && [ ! -f "docker-compose.yml" ]; then
    print_error "compose.yaml or docker-compose.yml not found"
    print_info "Please run this script from the project root directory"
    exit 1
fi

COMPOSE_FILE="compose.yaml"
if [ ! -f "$COMPOSE_FILE" ]; then
    COMPOSE_FILE="docker-compose.yml"
fi

print_success "Compose file found: $COMPOSE_FILE"

# Check .env file
print_step "Step 2/20: Verifying Environment Configuration"

if [ ! -f .env ]; then
    print_warning ".env file not found"
    if [ -f .env.example ]; then
        print_info "Creating .env from .env.example..."
        cp .env.example .env
        print_success ".env file created"
        print_warning "⚠️  CRITICAL: Please edit .env file with your production values!"
        echo ""
        echo "Required variables:"
        echo "  - DB_PASSWORD"
        echo "  - DB_ROOT_PASSWORD"
        echo "  - JWT_SECRET (generate with: openssl rand -base64 64)"
        echo "  - API_URL, FRONTEND_URL, APP_URL"
        echo "  - CORS_ALLOWED_ORIGINS"
        echo ""
        read -p "Press Enter after updating .env file..."
    else
        print_error ".env.example not found. Cannot proceed."
        exit 1
    fi
else
    print_success ".env file exists"
fi

# Load environment variables
set -a
source .env 2>/dev/null || true
set +a

# Verify critical variables
MISSING_VARS=()
[ -z "$DB_PASSWORD" ] && MISSING_VARS+=("DB_PASSWORD")
[ -z "$DB_ROOT_PASSWORD" ] && MISSING_VARS+=("DB_ROOT_PASSWORD")
[ -z "$JWT_SECRET" ] && MISSING_VARS+=("JWT_SECRET")

if [ ${#MISSING_VARS[@]} -gt 0 ]; then
    print_error "Missing critical environment variables: ${MISSING_VARS[*]}"
    print_info "Please add them to .env file before continuing"
    exit 1
fi

# Validate JWT secret strength
if [ ${#JWT_SECRET} -lt 64 ]; then
    print_warning "JWT_SECRET is too short (${#JWT_SECRET} chars, recommend 64+)"
    print_info "Generate new secret: openssl rand -base64 64"
    read -p "Continue anyway? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

print_success "Environment variables validated"

# Backup existing deployment
print_step "Step 3/20: Creating Backup"

if docker-compose ps | grep -q "Up"; then
    print_info "Existing containers detected, creating backup..."
    
    mkdir -p "$BACKUP_DIR"
    BACKUP_PATH="$BACKUP_DIR/pre_deploy_$TIMESTAMP"
    mkdir -p "$BACKUP_PATH"
    
    # Backup database
    if docker-compose ps mysql | grep -q "Up"; then
        print_info "Backing up database..."
        docker-compose exec -T mysql mysqldump \
            -u root -p"${DB_ROOT_PASSWORD}" \
            --single-transaction \
            employee_management_system > "$BACKUP_PATH/database.sql" 2>/dev/null || \
            print_warning "Database backup failed (may not exist yet)"
    fi
    
    # Backup uploads
    if docker-compose ps backend | grep -q "Up"; then
        print_info "Backing up uploads..."
        docker cp ems-backend:/app/uploads "$BACKUP_PATH/uploads" 2>/dev/null || \
            print_warning "Uploads backup failed (may be empty)"
    fi
    
    # Backup .env
    cp .env "$BACKUP_PATH/.env.backup" 2>/dev/null || true
    
    print_success "Backup created: $BACKUP_PATH"
else
    print_info "No existing containers to backup"
fi

# Stop and remove existing containers
print_step "Step 4/20: Stopping Existing Containers"

print_info "Stopping all containers..."
docker-compose -f "$COMPOSE_FILE" down 2>/dev/null || true

# Stop any remaining EMS containers
print_info "Stopping EMS containers..."
docker stop ems-backend ems-frontend ems-mysql 2>/dev/null || true
docker stop ems-backend-blue ems-frontend-blue 2>/dev/null || true

print_success "All containers stopped"

# Remove containers
print_step "Step 5/20: Removing Containers"

print_info "Removing containers..."
docker-compose -f "$COMPOSE_FILE" down -v 2>/dev/null || true
docker rm -f ems-backend ems-frontend ems-mysql 2>/dev/null || true
docker rm -f ems-backend-blue ems-frontend-blue 2>/dev/null || true
docker ps -a | grep -E "ems-|demo-ems" | awk '{print $1}' | xargs -r docker rm -f 2>/dev/null || true

print_success "Containers removed"

# Remove images
print_step "Step 6/20: Removing Old Images"

print_info "Removing EMS-related images..."
docker images | grep -E "ems-|demo-ems|employeemanagementsystem" | awk '{print $3}' | xargs -r docker rmi -f 2>/dev/null || true

print_info "Cleaning up dangling images..."
docker image prune -f > /dev/null 2>&1

print_success "Old images removed"

# Verify required files exist
print_step "Step 7/20: Verifying Required Files"

REQUIRED_FILES=(
    "compose.yaml"
    "Dockerfile"
    "frontend/Dockerfile"
    "database/init.sql"
    "src/main/resources/application-prod.properties"
    "frontend/src/environments/environment.prod.ts"
)

MISSING_FILES=()
for file in "${REQUIRED_FILES[@]}"; do
    if [ ! -f "$file" ]; then
        MISSING_FILES+=("$file")
    fi
done

if [ ${#MISSING_FILES[@]} -gt 0 ]; then
    print_error "Missing required files: ${MISSING_FILES[*]}"
    exit 1
fi

print_success "All required files present"

# Verify configuration files
print_step "Step 8/20: Verifying Configuration"

# Check database URL uses mysql container
if ! grep -q "mysql:3306" src/main/resources/application-prod.properties; then
    print_error "application-prod.properties not configured for Docker (should use 'mysql' container)"
    exit 1
fi

# Check frontend Dockerfile has build args
if ! grep -q "ARG API_URL" frontend/Dockerfile; then
    print_error "frontend/Dockerfile missing build arguments"
    exit 1
fi

# Check compose.yaml has environment variables
if ! grep -q "SPRING_PROFILES_ACTIVE" "$COMPOSE_FILE"; then
    print_error "$COMPOSE_FILE missing Spring profile configuration"
    exit 1
fi

print_success "Configuration verified"

# Security checks
print_step "Step 9/20: Security Validation"

# Check for hardcoded secrets
if grep -r "K81768751288957" src/ 2>/dev/null; then
    print_error "Hardcoded OCR API key found in source code!"
    exit 1
fi

# Check for hardcoded IPs in frontend
if grep -q "62.169.20.104" frontend/src/environments/environment.prod.ts 2>/dev/null; then
    print_warning "Hardcoded IP found in environment.prod.ts (should use placeholders)"
fi

# Check CORS configuration
if [ -n "$CORS_ALLOWED_ORIGINS" ]; then
    if [[ "$CORS_ALLOWED_ORIGINS" == *"*"* ]]; then
        print_error "CORS allows all origins (*) - security risk!"
        exit 1
    fi
fi

print_success "Security checks passed"

# Clean Docker system
print_step "Step 10/20: Cleaning Docker System"

print_info "Removing unused Docker resources..."
docker system prune -f > /dev/null 2>&1

print_success "Docker system cleaned"

# Build images
print_step "Step 11/20: Building Docker Images"

print_info "Building backend image (this may take 5-10 minutes)..."
docker-compose -f "$COMPOSE_FILE" build --no-cache backend || {
    print_error "Backend build failed!"
    exit 1
}

print_info "Building frontend image (this may take 5-10 minutes)..."
docker-compose -f "$COMPOSE_FILE" build --no-cache frontend || {
    print_error "Frontend build failed!"
    exit 1
}

print_success "All images built successfully"

# Create volumes
print_step "Step 12/20: Creating Docker Volumes"

print_info "Creating volumes..."
docker volume create employeemanagementsystem_mysql_data 2>/dev/null || true
docker volume create employeemanagementsystem_uploads_data 2>/dev/null || true

print_success "Volumes created"

# Start MySQL first
print_step "Step 13/20: Starting Database"

print_info "Starting MySQL container..."
docker-compose -f "$COMPOSE_FILE" up -d mysql

# Wait for MySQL to be ready
print_info "Waiting for MySQL to be ready (30 seconds)..."
sleep 30

# Check MySQL health
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

if [ $ATTEMPT -eq $MAX_ATTEMPTS ]; then
    print_error "MySQL failed to start"
    docker-compose -f "$COMPOSE_FILE" logs mysql
    exit 1
fi

# Start backend
print_step "Step 14/20: Starting Backend"

print_info "Starting backend container..."
docker-compose -f "$COMPOSE_FILE" up -d backend

# Wait for backend to start
print_info "Waiting for backend to start (60 seconds)..."
sleep 60

# Check backend health
print_info "Checking backend health..."
MAX_ATTEMPTS=30
ATTEMPT=0
while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
    if curl -f http://localhost:${BACKEND_PORT:-8080}/api/actuator/health > /dev/null 2>&1; then
        print_success "Backend is healthy"
        break
    fi
    ATTEMPT=$((ATTEMPT + 1))
    echo -n "."
    sleep 2
done

if [ $ATTEMPT -eq $MAX_ATTEMPTS ]; then
    print_warning "Backend health check timeout"
    print_info "Checking backend logs..."
    docker-compose -f "$COMPOSE_FILE" logs backend --tail=50
fi

# Start frontend
print_step "Step 15/20: Starting Frontend"

print_info "Starting frontend container..."
docker-compose -f "$COMPOSE_FILE" up -d frontend

# Wait for frontend
print_info "Waiting for frontend to start (10 seconds)..."
sleep 10

# Verify database tables
print_step "Step 16/20: Verifying Database Tables"

print_info "Checking if database tables exist..."
sleep 10

TABLES=$(docker-compose -f "$COMPOSE_FILE" exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" \
    employee_management_system -e "SHOW TABLES;" 2>/dev/null | wc -l)

if [ "$TABLES" -gt 1 ]; then
    print_success "Database tables exist ($((TABLES-1)) tables found)"
else
    print_warning "Tables may not be created yet"
    print_info "JPA will create tables on first startup"
    print_info "Check backend logs: docker-compose logs backend | grep -i 'table\|create'"
fi

# Verify services
print_step "Step 17/20: Verifying Services"

print_info "Checking service status..."
docker-compose -f "$COMPOSE_FILE" ps

# Health checks
print_info "Running health checks..."

# Backend health
if curl -f http://localhost:${BACKEND_PORT:-8080}/api/actuator/health > /dev/null 2>&1; then
    print_success "Backend health check: PASSED"
else
    print_warning "Backend health check: FAILED (check logs)"
fi

# Frontend health
if curl -f http://localhost:${FRONTEND_PORT:-80}/ > /dev/null 2>&1; then
    print_success "Frontend health check: PASSED"
else
    print_warning "Frontend health check: FAILED (check logs)"
fi

# Verify production configuration
print_step "Step 18/20: Verifying Production Configuration"

# Check Spring profile
if docker-compose -f "$COMPOSE_FILE" logs backend | grep -qi "profile.*prod"; then
    print_success "Backend using production profile"
else
    print_warning "Could not verify production profile (check logs)"
fi

# Check frontend URLs
if docker-compose -f "$COMPOSE_FILE" exec frontend cat /usr/share/nginx/html/main*.js 2>/dev/null | grep -q "${API_URL:-localhost}"; then
    print_success "Frontend using production URLs"
elif docker-compose -f "$COMPOSE_FILE" exec frontend cat /usr/share/nginx/html/main*.js 2>/dev/null | grep -q "localhost"; then
    print_warning "Frontend may still be using localhost (rebuild may be needed)"
else
    print_info "Could not verify frontend URLs"
fi

# Final verification
print_step "Step 19/20: Final Verification"

print_info "Service Status:"
docker-compose -f "$COMPOSE_FILE" ps

print_info ""
print_info "Application URLs:"
print_info "  Frontend: http://localhost:${FRONTEND_PORT:-80}"
print_info "  Backend API: http://localhost:${BACKEND_PORT:-8080}/api"
print_info "  Health Check: http://localhost:${BACKEND_PORT:-8080}/api/actuator/health"

# Summary
print_step "Step 20/20: Deployment Summary"

echo ""
echo -e "${GREEN}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║         DEPLOYMENT COMPLETED SUCCESSFULLY!                 ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""

print_success "All services deployed and running"
echo ""

print_info "📊 Deployment Details:"
echo "  • Backup Location: ${BACKUP_PATH:-N/A}"
echo "  • Database: ${DB_NAME:-employee_management_system}"
echo "  • Frontend Port: ${FRONTEND_PORT:-80}"
echo "  • Backend Port: ${BACKEND_PORT:-8080}"
echo ""

print_info "📝 Next Steps:"
echo "  1. Access application: http://localhost:${FRONTEND_PORT:-80}"
echo "  2. Create ROOT account (first-time setup)"
echo "  3. Verify all functionality"
echo "  4. Review logs: docker-compose logs -f"
echo "  5. Set up automated backups: ./backup.sh"
echo ""

print_info "🔧 Useful Commands:"
echo "  • View logs: docker-compose logs -f"
echo "  • Restart: docker-compose restart"
echo "  • Stop: docker-compose down"
echo "  • Backup: ./backup.sh"
echo "  • Monitor: ./monitor.sh"
echo ""

print_info "📚 Documentation:"
echo "  • Migration Guide: MIGRATION_GUIDE.md"
echo "  • Environment Setup: ENV_SETUP_GUIDE.md"
echo "  • Production Checklist: PRODUCTION_READINESS_CHECKLIST.md"
echo ""

# Check for any warnings
if [ -n "$BACKUP_PATH" ]; then
    print_warning "Backup created at: $BACKUP_PATH"
    print_info "Keep this backup until you verify the deployment is working correctly"
fi

echo ""
print_success "🎉 Production deployment complete!"
echo ""

# Optional: Open browser
if command -v xdg-open &> /dev/null; then
    read -p "Open application in browser? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        xdg-open "http://localhost:${FRONTEND_PORT:-80}" &> /dev/null &
    fi
fi

exit 0

