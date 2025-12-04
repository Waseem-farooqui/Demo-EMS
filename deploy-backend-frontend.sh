#!/bin/bash
###############################################################################
# Production Deployment Script - Backend & Frontend Only
# Deploys only backend and frontend containers (keeps MySQL running)
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

# Load environment variables
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
fi

# Banner
echo -e "${CYAN}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║   Production Deployment - Backend & Frontend Only         ║${NC}"
echo -e "${CYAN}║              MySQL Container Unchanged                     ║${NC}"
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

# Check MySQL is running
print_step "Step 1/4: Checking MySQL"

if ! docker-compose -f "$COMPOSE_FILE" ps mysql | grep -q "Up"; then
    print_error "MySQL container is not running!"
    print_info "Please start MySQL first or use deploy-all.sh to deploy all containers"
    exit 1
fi

print_success "MySQL is running"

# Step 2: Stop Backend and Frontend
print_step "Step 2/4: Stopping Backend & Frontend"

print_info "Stopping backend and frontend containers..."
docker-compose -f "$COMPOSE_FILE" stop backend frontend 2>/dev/null || true
docker stop ems-backend ems-frontend 2>/dev/null || true
print_success "Containers stopped"

# Step 3: Build Images
print_step "Step 3/4: Building Docker Images"

print_info "Building backend image..."
export DOCKER_BUILDKIT=1
docker-compose -f "$COMPOSE_FILE" build --no-cache backend || {
    print_error "Backend build failed!"
    exit 1
}

print_info "Building frontend image..."
docker-compose -f "$COMPOSE_FILE" build --no-cache frontend || {
    print_error "Frontend build failed!"
    exit 1
}

print_success "Images built"

# Step 4: Start Backend and Frontend
print_step "Step 4/4: Starting Backend & Frontend"

print_info "Starting backend container..."
docker-compose -f "$COMPOSE_FILE" up -d backend

print_info "Waiting for backend to start (60 seconds)..."
sleep 60

print_info "Starting frontend container..."
docker-compose -f "$COMPOSE_FILE" up -d frontend

print_info "Waiting for frontend to start (10 seconds)..."
sleep 10

# Final Status
print_step "Deployment Complete!"

print_info "Container Status:"
docker-compose -f "$COMPOSE_FILE" ps

echo ""
print_success "Backend and Frontend deployed successfully!"
print_info "Backend: http://localhost:${BACKEND_PORT:-8080}"
print_info "Frontend: http://localhost:${FRONTEND_PORT:-80} or https://localhost:${FRONTEND_HTTPS_PORT:-443}"
print_info "MySQL: Running (unchanged)"
echo ""

