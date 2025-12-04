#!/bin/bash
###############################################################################
# Restore Validate Mode Script
# Changes JPA_DDL_AUTO back to 'validate' after database is created
# This should be run after fresh-database-start.sh completes
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

ENV_FILE="${ENV_FILE:-.env}"

if [ ! -f "$ENV_FILE" ]; then
    print_error ".env file not found!"
    exit 1
fi

print_step "Restoring Validate Mode"

# Check current value
CURRENT_JPA_DDL=$(grep "^JPA_DDL_AUTO=" "$ENV_FILE" | cut -d '=' -f2 | tr -d '"' | tr -d "'" | xargs || echo "")

if [ "$CURRENT_JPA_DDL" = "validate" ]; then
    print_info "JPA_DDL_AUTO is already set to 'validate'"
    exit 0
fi

print_info "Current JPA_DDL_AUTO: $CURRENT_JPA_DDL"
print_info "Changing to 'validate'..."

# Update JPA_DDL_AUTO
if grep -q "^JPA_DDL_AUTO=" "$ENV_FILE"; then
    if [[ "$OSTYPE" == "darwin"* ]]; then
        sed -i '' "s/^JPA_DDL_AUTO=.*/JPA_DDL_AUTO=validate/" "$ENV_FILE"
    else
        sed -i "s/^JPA_DDL_AUTO=.*/JPA_DDL_AUTO=validate/" "$ENV_FILE"
    fi
    print_success "JPA_DDL_AUTO updated to 'validate'"
else
    print_error "JPA_DDL_AUTO not found in .env"
    exit 1
fi

# Re-enable Flyway if it was disabled
if grep -q "^SPRING_FLYWAY_ENABLED=" "$ENV_FILE"; then
    if [[ "$OSTYPE" == "darwin"* ]]; then
        sed -i '' "s/^SPRING_FLYWAY_ENABLED=.*/SPRING_FLYWAY_ENABLED=true/" "$ENV_FILE"
    else
        sed -i "s/^SPRING_FLYWAY_ENABLED=.*/SPRING_FLYWAY_ENABLED=true/" "$ENV_FILE"
    fi
    print_info "Flyway re-enabled"
fi

# Determine compose command
if docker compose version &> /dev/null 2>&1; then
    COMPOSE_CMD="docker compose"
else
    COMPOSE_CMD="docker-compose"
fi

# Restart backend
print_step "Restarting Backend"

print_info "Restarting backend to apply new configuration..."
$COMPOSE_CMD restart backend

print_info "Waiting for backend to restart (30 seconds)..."
sleep 30

# Check backend status
if $COMPOSE_CMD ps backend | grep -q "Up"; then
    print_success "Backend restarted successfully"
    print_info "JPA_DDL_AUTO is now set to 'validate'"
    print_info "Flyway will handle future migrations"
else
    print_error "Backend failed to restart"
    print_info "Check logs: $COMPOSE_CMD logs backend"
    exit 1
fi

print_success "Validate mode restored!"

