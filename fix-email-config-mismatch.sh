#!/bin/bash
###############################################################################
# Fix Email Configuration Mismatch
# Restarts backend container to pick up correct .env configuration
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

print_step "Fixing Email Configuration Mismatch"

# Check if .env exists
if [ ! -f ".env" ]; then
    print_error ".env file not found!"
    exit 1
fi

# Read current .env configuration
MAIL_PORT=$(grep "^MAIL_PORT=" .env | cut -d '=' -f2 | tr -d '"' | tr -d "'" | xargs || echo "")
MAIL_HOST=$(grep "^MAIL_HOST=" .env | cut -d '=' -f2 | tr -d '"' | tr -d "'" | xargs || echo "")

print_info "Current .env configuration:"
echo "  MAIL_HOST: $MAIL_HOST"
echo "  MAIL_PORT: $MAIL_PORT"

# Check if docker compose is available
if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    print_error "Docker Compose not found!"
    exit 1
fi

# Determine compose command
if docker compose version &> /dev/null; then
    COMPOSE_CMD="docker compose"
else
    COMPOSE_CMD="docker-compose"
fi

# Check backend container
if ! $COMPOSE_CMD ps backend 2>/dev/null | grep -q "Up"; then
    print_error "Backend container is not running!"
    print_info "Start it with: $COMPOSE_CMD up -d backend"
    exit 1
fi

# Get current container configuration
print_info "Checking current container configuration..."
CONTAINER_MAIL_PORT=$($COMPOSE_CMD exec -T backend env 2>/dev/null | grep "^MAIL_PORT=" | cut -d '=' -f2 || echo "")

if [ -n "$CONTAINER_MAIL_PORT" ]; then
    echo "  Container MAIL_PORT: $CONTAINER_MAIL_PORT"
    
    if [ "$CONTAINER_MAIL_PORT" != "$MAIL_PORT" ]; then
        print_warning "Configuration mismatch detected!"
        print_info "Container needs to be restarted to pick up .env changes"
    else
        print_success "Configuration matches - no restart needed"
        exit 0
    fi
fi

# Restart backend container
print_step "Restarting Backend Container"

print_info "Stopping backend container..."
$COMPOSE_CMD stop backend

print_info "Starting backend container with updated .env..."
$COMPOSE_CMD up -d backend

# Wait for container to be ready
print_info "Waiting for backend to start..."
sleep 5

# Verify new configuration
print_step "Verifying New Configuration"

if $COMPOSE_CMD ps backend | grep -q "Up"; then
    print_success "Backend container is running"
    
    # Check if configuration is updated
    NEW_MAIL_PORT=$($COMPOSE_CMD exec -T backend env 2>/dev/null | grep "^MAIL_PORT=" | cut -d '=' -f2 || echo "")
    
    if [ "$NEW_MAIL_PORT" = "$MAIL_PORT" ]; then
        print_success "Configuration updated successfully!"
        echo "  Container MAIL_PORT: $NEW_MAIL_PORT (matches .env)"
    else
        print_warning "Configuration might not have updated"
        echo "  Expected: $MAIL_PORT"
        echo "  Got: $NEW_MAIL_PORT"
        print_info "Check backend logs: $COMPOSE_CMD logs backend | tail -50"
    fi
else
    print_error "Backend container failed to start!"
    print_info "Check logs: $COMPOSE_CMD logs backend"
    exit 1
fi

# Check backend logs for email errors
print_step "Checking Backend Logs for Email Issues"

print_info "Recent backend logs (last 20 lines):"
$COMPOSE_CMD logs backend --tail 20 | grep -i "mail\|smtp\|email" || print_info "No email-related log entries found"

print_success "Fix completed!"
print_info "Monitor backend logs: $COMPOSE_CMD logs -f backend"

