#!/bin/bash
###############################################################################
# SMTP Connectivity and Firewall Check Script
# Checks firewall rules and tests SMTP connectivity for Hostinger
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

print_step "SMTP Connectivity and Firewall Check"

# Check 1: Firewall Status
print_info "Checking firewall status..."
if command -v ufw &> /dev/null; then
    FIREWALL_STATUS=$(ufw status | head -1)
    echo "$FIREWALL_STATUS"
    
    if echo "$FIREWALL_STATUS" | grep -q "Status: active"; then
        print_info "Firewall is active"
        
        # Check default outgoing policy
        OUTGOING_POLICY=$(ufw status verbose | grep "Default:" | grep -o "deny (outgoing)" || echo "")
        if [ -n "$OUTGOING_POLICY" ]; then
            print_error "Firewall is blocking outgoing connections!"
            print_info "Fix: sudo ufw default allow outgoing"
        else
            print_success "Outgoing connections are allowed (default policy)"
        fi
        
        # Check if SMTP ports are explicitly allowed
        if ufw status | grep -q "465\|587"; then
            print_info "SMTP ports found in firewall rules:"
            ufw status | grep -E "465|587"
        else
            print_warning "SMTP ports (465/587) not explicitly allowed"
            print_info "This is OK if 'default allow outgoing' is set"
        fi
    else
        print_warning "Firewall is not active"
    fi
elif command -v firewall-cmd &> /dev/null; then
    print_info "Using firewalld (firewall-cmd)"
    if firewall-cmd --state 2>/dev/null | grep -q "running"; then
        print_info "Firewalld is running"
        # Check zones
        firewall-cmd --list-all-zones | grep -A 10 "public\|default"
    fi
else
    print_warning "No firewall tool found (ufw or firewalld)"
fi

# Check 2: Test SMTP Connectivity
print_step "Testing SMTP Connectivity"

# Test port 587 (TLS/STARTTLS)
print_info "Testing connection to smtp.hostinger.com:587..."
if command -v nc &> /dev/null || command -v telnet &> /dev/null; then
    if timeout 5 bash -c "echo > /dev/tcp/smtp.hostinger.com/587" 2>/dev/null; then
        print_success "Port 587 is reachable"
    else
        print_error "Cannot connect to smtp.hostinger.com:587"
        print_info "This could indicate:"
        print_info "  - Firewall blocking outbound connection"
        print_info "  - Network issue"
        print_info "  - Hostinger SMTP server issue"
    fi
else
    print_warning "nc or telnet not available, skipping connectivity test"
    print_info "Install with: sudo apt-get install netcat-openbsd"
fi

# Test port 465 (SSL)
print_info "Testing connection to smtp.hostinger.com:465..."
if timeout 5 bash -c "echo > /dev/tcp/smtp.hostinger.com/465" 2>/dev/null; then
    print_success "Port 465 is reachable"
else
    print_error "Cannot connect to smtp.hostinger.com:465"
fi

# Check 3: DNS Resolution
print_info "Testing DNS resolution for smtp.hostinger.com..."
if host smtp.hostinger.com &> /dev/null || nslookup smtp.hostinger.com &> /dev/null; then
    SMTP_IP=$(host smtp.hostinger.com | grep "has address" | awk '{print $4}' | head -1)
    print_success "DNS resolved: smtp.hostinger.com -> $SMTP_IP"
else
    print_error "Cannot resolve smtp.hostinger.com"
fi

# Check 4: Docker Network
print_step "Checking Docker Network Configuration"

if command -v docker &> /dev/null; then
    print_info "Checking if backend container can reach SMTP..."
    
    # Check if backend container exists
    if docker ps --format '{{.Names}}' | grep -q "backend\|ems-backend"; then
        BACKEND_CONTAINER=$(docker ps --format '{{.Names}}' | grep -E "backend|ems-backend" | head -1)
        print_info "Found backend container: $BACKEND_CONTAINER"
        
        # Test connectivity from container
        if docker exec "$BACKEND_CONTAINER" timeout 5 bash -c "echo > /dev/tcp/smtp.hostinger.com/587" 2>/dev/null; then
            print_success "Backend container can reach smtp.hostinger.com:587"
        else
            print_error "Backend container cannot reach smtp.hostinger.com:587"
            print_info "This could indicate Docker network restrictions"
        fi
    else
        print_warning "Backend container not running"
    fi
else
    print_warning "Docker not found"
fi

# Check 5: Environment Variables
print_step "Checking Email Configuration in Backend Container"

if command -v docker &> /dev/null; then
    if docker ps --format '{{.Names}}' | grep -q "backend\|ems-backend"; then
        BACKEND_CONTAINER=$(docker ps --format '{{.Names}}' | grep -E "backend|ems-backend" | head -1)
        print_info "Checking environment variables in $BACKEND_CONTAINER..."
        
        echo ""
        echo "Email Configuration:"
        docker exec "$BACKEND_CONTAINER" env | grep -E "MAIL_|EMAIL_" | sed 's/PASSWORD=.*/PASSWORD=***HIDDEN***/' || print_warning "No MAIL_ variables found"
    fi
fi

# Recommendations
print_step "Recommendations"

echo ""
print_info "If SMTP connectivity tests fail:"
echo "  1. Check firewall: sudo ufw status verbose"
echo "  2. Allow outgoing SMTP: sudo ufw default allow outgoing (if not already set)"
echo "  3. Test from server: telnet smtp.hostinger.com 587"
echo ""
print_info "If connectivity works but authentication fails:"
echo "  1. Verify password in .env file matches Hostinger email password"
echo "  2. Test webmail login: https://webmail.hostinger.com"
echo "  3. Check if email account requires IP whitelisting in Hostinger"
echo "  4. Try resetting email password in Hostinger hPanel"
echo ""
print_info "For port 587 (TLS/STARTTLS), ensure .env has:"
echo "  MAIL_PORT=587"
echo "  MAIL_SMTP_STARTTLS_ENABLE=true"
echo "  MAIL_SMTP_STARTTLS_REQUIRED=true"
echo "  MAIL_SMTP_SSL_ENABLE=false"
echo "  MAIL_SMTP_SSL_TRUST=smtp.hostinger.com"

print_success "Diagnostics completed!"

