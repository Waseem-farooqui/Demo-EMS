#!/bin/bash

# Fix Email SSL Configuration
# This script ensures the email configuration matches the port being used
# Port 465 (SSL): Requires socket factory, SSL enabled, STARTTLS disabled
# Port 587 (STARTTLS): Requires NO socket factory, SSL disabled, STARTTLS enabled

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

ENV_FILE="${1:-.env}"

if [ ! -f "$ENV_FILE" ]; then
    print_error ".env file not found: $ENV_FILE"
    exit 1
fi

print_info "Reading email configuration from $ENV_FILE..."

# Read current configuration
MAIL_PORT=$(grep "^MAIL_PORT=" "$ENV_FILE" | cut -d '=' -f2 | tr -d '"' | tr -d "'" | xargs || echo "")
MAIL_SMTP_SSL_ENABLE=$(grep "^MAIL_SMTP_SSL_ENABLE=" "$ENV_FILE" | cut -d '=' -f2 | tr -d '"' | tr -d "'" | xargs || echo "")
MAIL_SMTP_STARTTLS_ENABLE=$(grep "^MAIL_SMTP_STARTTLS_ENABLE=" "$ENV_FILE" | cut -d '=' -f2 | tr -d '"' | tr -d "'" | xargs || echo "")
MAIL_SMTP_STARTTLS_REQUIRED=$(grep "^MAIL_SMTP_STARTTLS_REQUIRED=" "$ENV_FILE" | cut -d '=' -f2 | tr -d '"' | tr -d "'" | xargs || echo "")
MAIL_SMTP_SSL_SOCKET_FACTORY_PORT=$(grep "^MAIL_SMTP_SSL_SOCKET_FACTORY_PORT=" "$ENV_FILE" | cut -d '=' -f2 | tr -d '"' | tr -d "'" | xargs || echo "")

if [ -z "$MAIL_PORT" ]; then
    print_error "MAIL_PORT is not set in $ENV_FILE"
    exit 1
fi

print_info "Current configuration:"
echo "  MAIL_PORT: $MAIL_PORT"
echo "  MAIL_SMTP_SSL_ENABLE: ${MAIL_SMTP_SSL_ENABLE:-not set}"
echo "  MAIL_SMTP_STARTTLS_ENABLE: ${MAIL_SMTP_STARTTLS_ENABLE:-not set}"
echo "  MAIL_SMTP_STARTTLS_REQUIRED: ${MAIL_SMTP_STARTTLS_REQUIRED:-not set}"
echo "  MAIL_SMTP_SSL_SOCKET_FACTORY_PORT: ${MAIL_SMTP_SSL_SOCKET_FACTORY_PORT:-not set}"

# Determine correct configuration based on port
if [ "$MAIL_PORT" = "465" ]; then
    print_info "Port 465 detected - configuring for SSL..."
    
    # Required settings for port 465
    SSL_ENABLE="true"
    STARTTLS_ENABLE="false"
    STARTTLS_REQUIRED="false"
    SOCKET_FACTORY_PORT="465"
    
    # Check if fixes are needed
    NEEDS_FIX=false
    
    if [ "$MAIL_SMTP_SSL_ENABLE" != "true" ]; then
        print_warning "MAIL_SMTP_SSL_ENABLE should be 'true' for port 465"
        NEEDS_FIX=true
    fi
    
    if [ "$MAIL_SMTP_STARTTLS_ENABLE" != "false" ]; then
        print_warning "MAIL_SMTP_STARTTLS_ENABLE should be 'false' for port 465"
        NEEDS_FIX=true
    fi
    
    if [ "$MAIL_SMTP_STARTTLS_REQUIRED" != "false" ]; then
        print_warning "MAIL_SMTP_STARTTLS_REQUIRED should be 'false' for port 465"
        NEEDS_FIX=true
    fi
    
    if [ "$MAIL_SMTP_SSL_SOCKET_FACTORY_PORT" != "465" ] && [ -n "$MAIL_SMTP_SSL_SOCKET_FACTORY_PORT" ]; then
        print_warning "MAIL_SMTP_SSL_SOCKET_FACTORY_PORT should be '465' for port 465"
        NEEDS_FIX=true
    fi
    
    if [ "$NEEDS_FIX" = "false" ]; then
        print_success "Configuration is correct for port 465"
        exit 0
    fi
    
elif [ "$MAIL_PORT" = "587" ]; then
    print_info "Port 587 detected - configuring for STARTTLS..."
    
    # Required settings for port 587
    SSL_ENABLE="false"
    STARTTLS_ENABLE="true"
    STARTTLS_REQUIRED="true"
    SOCKET_FACTORY_PORT=""  # Must be empty for port 587
    
    # Check if fixes are needed
    NEEDS_FIX=false
    
    if [ "$MAIL_SMTP_SSL_ENABLE" != "false" ]; then
        print_warning "MAIL_SMTP_SSL_ENABLE should be 'false' for port 587"
        NEEDS_FIX=true
    fi
    
    if [ "$MAIL_SMTP_STARTTLS_ENABLE" != "true" ]; then
        print_warning "MAIL_SMTP_STARTTLS_ENABLE should be 'true' for port 587"
        NEEDS_FIX=true
    fi
    
    if [ "$MAIL_SMTP_STARTTLS_REQUIRED" != "true" ]; then
        print_warning "MAIL_SMTP_STARTTLS_REQUIRED should be 'true' for port 587"
        NEEDS_FIX=true
    fi
    
    if [ -n "$MAIL_SMTP_SSL_SOCKET_FACTORY_PORT" ]; then
        print_warning "MAIL_SMTP_SSL_SOCKET_FACTORY_PORT should be empty for port 587"
        NEEDS_FIX=true
    fi
    
    if [ "$NEEDS_FIX" = "false" ]; then
        print_success "Configuration is correct for port 587"
        exit 0
    fi
else
    print_error "Unsupported port: $MAIL_PORT (supported: 465 or 587)"
    exit 1
fi

# Apply fixes
print_info "Applying fixes to $ENV_FILE..."

# Backup
cp "$ENV_FILE" "${ENV_FILE}.backup.$(date +%Y%m%d_%H%M%S)"
print_info "Backup created: ${ENV_FILE}.backup.$(date +%Y%m%d_%H%M%S)"

# Update SSL_ENABLE
if grep -q "^MAIL_SMTP_SSL_ENABLE=" "$ENV_FILE"; then
    if [[ "$OSTYPE" == "darwin"* ]]; then
        sed -i '' "s/^MAIL_SMTP_SSL_ENABLE=.*/MAIL_SMTP_SSL_ENABLE=$SSL_ENABLE/" "$ENV_FILE"
    else
        sed -i "s/^MAIL_SMTP_SSL_ENABLE=.*/MAIL_SMTP_SSL_ENABLE=$SSL_ENABLE/" "$ENV_FILE"
    fi
else
    echo "MAIL_SMTP_SSL_ENABLE=$SSL_ENABLE" >> "$ENV_FILE"
fi

# Update STARTTLS_ENABLE
if grep -q "^MAIL_SMTP_STARTTLS_ENABLE=" "$ENV_FILE"; then
    if [[ "$OSTYPE" == "darwin"* ]]; then
        sed -i '' "s/^MAIL_SMTP_STARTTLS_ENABLE=.*/MAIL_SMTP_STARTTLS_ENABLE=$STARTTLS_ENABLE/" "$ENV_FILE"
    else
        sed -i "s/^MAIL_SMTP_STARTTLS_ENABLE=.*/MAIL_SMTP_STARTTLS_ENABLE=$STARTTLS_ENABLE/" "$ENV_FILE"
    fi
else
    echo "MAIL_SMTP_STARTTLS_ENABLE=$STARTTLS_ENABLE" >> "$ENV_FILE"
fi

# Update STARTTLS_REQUIRED
if grep -q "^MAIL_SMTP_STARTTLS_REQUIRED=" "$ENV_FILE"; then
    if [[ "$OSTYPE" == "darwin"* ]]; then
        sed -i '' "s/^MAIL_SMTP_STARTTLS_REQUIRED=.*/MAIL_SMTP_STARTTLS_REQUIRED=$STARTTLS_REQUIRED/" "$ENV_FILE"
    else
        sed -i "s/^MAIL_SMTP_STARTTLS_REQUIRED=.*/MAIL_SMTP_STARTTLS_REQUIRED=$STARTTLS_REQUIRED/" "$ENV_FILE"
    fi
else
    echo "MAIL_SMTP_STARTTLS_REQUIRED=$STARTTLS_REQUIRED" >> "$ENV_FILE"
fi

# Update SOCKET_FACTORY_PORT
if [ -n "$SOCKET_FACTORY_PORT" ]; then
    if grep -q "^MAIL_SMTP_SSL_SOCKET_FACTORY_PORT=" "$ENV_FILE"; then
        if [[ "$OSTYPE" == "darwin"* ]]; then
            sed -i '' "s/^MAIL_SMTP_SSL_SOCKET_FACTORY_PORT=.*/MAIL_SMTP_SSL_SOCKET_FACTORY_PORT=$SOCKET_FACTORY_PORT/" "$ENV_FILE"
        else
            sed -i "s/^MAIL_SMTP_SSL_SOCKET_FACTORY_PORT=.*/MAIL_SMTP_SSL_SOCKET_FACTORY_PORT=$SOCKET_FACTORY_PORT/" "$ENV_FILE"
        fi
    else
        echo "MAIL_SMTP_SSL_SOCKET_FACTORY_PORT=$SOCKET_FACTORY_PORT" >> "$ENV_FILE"
    fi
else
    # Remove or comment out SOCKET_FACTORY_PORT for port 587
    if grep -q "^MAIL_SMTP_SSL_SOCKET_FACTORY_PORT=" "$ENV_FILE"; then
        if [[ "$OSTYPE" == "darwin"* ]]; then
            sed -i '' "/^MAIL_SMTP_SSL_SOCKET_FACTORY_PORT=/d" "$ENV_FILE"
        else
            sed -i "/^MAIL_SMTP_SSL_SOCKET_FACTORY_PORT=/d" "$ENV_FILE"
        fi
    fi
fi

print_success "Configuration updated!"
print_info "Updated settings:"
echo "  MAIL_SMTP_SSL_ENABLE=$SSL_ENABLE"
echo "  MAIL_SMTP_STARTTLS_ENABLE=$STARTTLS_ENABLE"
echo "  MAIL_SMTP_STARTTLS_REQUIRED=$STARTTLS_REQUIRED"
if [ -n "$SOCKET_FACTORY_PORT" ]; then
    echo "  MAIL_SMTP_SSL_SOCKET_FACTORY_PORT=$SOCKET_FACTORY_PORT"
else
    echo "  MAIL_SMTP_SSL_SOCKET_FACTORY_PORT=(removed for port 587)"
fi

print_info "Next steps:"
echo "  1. Restart the backend: docker compose restart backend"
echo "  2. Check logs: docker compose logs backend | grep -i mail"
echo "  3. Test email sending from the application"

