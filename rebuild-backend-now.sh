#!/bin/bash
###############################################################################
# IMMEDIATE FIX: Rebuild Backend with Latest Code
###############################################################################
# This script rebuilds the backend container with the latest code changes
# Run this after making code changes to ensure the container uses the new code
###############################################################################

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

print_info() {
    echo -e "${YELLOW}ℹ️  $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Check if running in Docker Compose environment
if [ -f "compose.yaml" ] || [ -f "docker-compose.yml" ]; then
    COMPOSE_FILE="compose.yaml"
    if [ ! -f "$COMPOSE_FILE" ]; then
        COMPOSE_FILE="docker-compose.yml"
    fi
    
    print_info "Rebuilding backend container with latest code..."
    print_warning "This will stop the backend, rebuild the image, and restart it"
    
    # Stop backend
    print_info "Stopping backend container..."
    docker-compose -f "$COMPOSE_FILE" stop backend 2>/dev/null || true
    
    # Remove old container
    print_info "Removing old backend container..."
    docker-compose -f "$COMPOSE_FILE" rm -f backend 2>/dev/null || true
    
    # Rebuild backend image (no cache to ensure fresh build)
    print_info "Rebuilding backend image (this may take 5-10 minutes)..."
    docker-compose -f "$COMPOSE_FILE" build --no-cache backend || {
        print_error "Backend build failed!"
        exit 1
    }
    
    # Start backend
    print_info "Starting backend container..."
    docker-compose -f "$COMPOSE_FILE" up -d backend || {
        print_error "Failed to start backend!"
        exit 1
    }
    
    print_success "Backend container rebuilt and restarted successfully"
    print_info "Waiting for backend to be ready (30 seconds)..."
    sleep 30
    
    # Check backend health
    print_info "Checking backend health..."
    if docker-compose -f "$COMPOSE_FILE" exec backend curl -f http://localhost:8080/api/actuator/health 2>/dev/null; then
        print_success "Backend is healthy and ready!"
    else
        print_warning "Backend health check failed - check logs with: docker-compose logs backend"
    fi
    
    print_info "View backend logs: docker-compose logs -f backend"
    
else
    print_error "This script requires Docker Compose environment"
    exit 1
fi

