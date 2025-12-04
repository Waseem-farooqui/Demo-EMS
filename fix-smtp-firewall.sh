#!/bin/bash
###############################################################################
# Fix SMTP Firewall Rules
# Ensures outbound SMTP connections are allowed
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

# Check if running as root
if [ "$EUID" -ne 0 ]; then
    print_error "This script must be run as root"
    echo "Usage: sudo $0"
    exit 1
fi

print_step "Fixing SMTP Firewall Rules"

# Check if UFW is installed
if ! command -v ufw &> /dev/null; then
    print_error "UFW is not installed"
    print_info "Install with: sudo apt-get install ufw"
    exit 1
fi

# Check current status
print_info "Current firewall status:"
ufw status verbose | head -5

# Ensure outgoing is allowed
print_info "Ensuring outgoing connections are allowed..."
ufw default allow outgoing
print_success "Outgoing connections allowed"

# Explicitly allow SMTP ports (optional, but good for documentation)
print_info "Adding explicit SMTP port rules (for documentation)..."
ufw allow out 465/tcp comment 'SMTP SSL (Hostinger)' 2>/dev/null || true
ufw allow out 587/tcp comment 'SMTP TLS (Hostinger)' 2>/dev/null || true
print_success "SMTP port rules added"

# Reload firewall
print_info "Reloading firewall..."
ufw --force enable
print_success "Firewall reloaded"

# Show final status
print_info "Final firewall status:"
ufw status | grep -E "Status|465|587|outgoing" || ufw status verbose | head -10

print_success "SMTP firewall rules configured!"
print_info "You may need to restart the backend container:"
print_info "  docker compose restart backend"

