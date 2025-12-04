#!/bin/bash
###############################################################################
# Quick Fix: Disable Flyway for JPA_DDL_AUTO=create
# Use this if backend fails to start due to Flyway validation errors
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

ENV_FILE="${ENV_FILE:-.env}"

if [ ! -f "$ENV_FILE" ]; then
    print_error ".env file not found!"
    exit 1
fi

print_info "Disabling Flyway in $ENV_FILE..."

# Update or add SPRING_FLYWAY_ENABLED=false
if grep -q "^SPRING_FLYWAY_ENABLED=" "$ENV_FILE"; then
    if [[ "$OSTYPE" == "darwin"* ]]; then
        sed -i '' "s/^SPRING_FLYWAY_ENABLED=.*/SPRING_FLYWAY_ENABLED=false/" "$ENV_FILE"
    else
        sed -i "s/^SPRING_FLYWAY_ENABLED=.*/SPRING_FLYWAY_ENABLED=false/" "$ENV_FILE"
    fi
    print_success "SPRING_FLYWAY_ENABLED set to false"
else
    echo "" >> "$ENV_FILE"
    echo "# Flyway Configuration" >> "$ENV_FILE"
    echo "SPRING_FLYWAY_ENABLED=false" >> "$ENV_FILE"
    print_success "SPRING_FLYWAY_ENABLED=false added to .env"
fi

# Also ensure JPA_DDL_AUTO=create
if grep -q "^JPA_DDL_AUTO=" "$ENV_FILE"; then
    CURRENT_JPA=$(grep "^JPA_DDL_AUTO=" "$ENV_FILE" | cut -d '=' -f2 | tr -d '"' | tr -d "'" | xargs)
    if [ "$CURRENT_JPA" != "create" ]; then
        print_info "Updating JPA_DDL_AUTO to 'create'..."
        if [[ "$OSTYPE" == "darwin"* ]]; then
            sed -i '' "s/^JPA_DDL_AUTO=.*/JPA_DDL_AUTO=create/" "$ENV_FILE"
        else
            sed -i "s/^JPA_DDL_AUTO=.*/JPA_DDL_AUTO=create/" "$ENV_FILE"
        fi
        print_success "JPA_DDL_AUTO set to 'create'"
    fi
else
    echo "JPA_DDL_AUTO=create" >> "$ENV_FILE"
    print_success "JPA_DDL_AUTO=create added to .env"
fi

# Determine compose command
if docker compose version &> /dev/null 2>&1; then
    COMPOSE_CMD="docker compose"
else
    COMPOSE_CMD="docker-compose"
fi

print_info "Restarting backend to apply changes..."
$COMPOSE_CMD restart backend

print_success "Flyway disabled and backend restarted!"
print_info "Backend will now use JPA_DDL_AUTO=create to create tables"
print_info "Check logs: $COMPOSE_CMD logs backend | tail -50"

