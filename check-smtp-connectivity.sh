#!/bin/bash
###############################################################################
# SMTP Connectivity and Authentication Check Script
# Reads .env file, checks firewall, connectivity, and tests password authentication
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

# Find .env file
ENV_FILE=""
if [ -f ".env" ]; then
    ENV_FILE=".env"
    print_success "Found .env file"
elif [ -f "prod.env" ]; then
    ENV_FILE="prod.env"
    print_warning "Using prod.env (create .env from prod.env for production)"
else
    print_error "No .env or prod.env file found!"
    exit 1
fi

print_step "Reading Email Configuration from $ENV_FILE"

# Read email configuration from .env file
MAIL_HOST=$(grep "^MAIL_HOST=" "$ENV_FILE" | cut -d '=' -f2 | tr -d '"' | tr -d "'" | xargs || echo "")
MAIL_PORT=$(grep "^MAIL_PORT=" "$ENV_FILE" | cut -d '=' -f2 | tr -d '"' | tr -d "'" | xargs || echo "")
MAIL_USERNAME=$(grep "^MAIL_USERNAME=" "$ENV_FILE" | cut -d '=' -f2 | tr -d '"' | tr -d "'" | xargs || echo "")
MAIL_PASSWORD=$(grep "^MAIL_PASSWORD=" "$ENV_FILE" | cut -d '=' -f2 | tr -d '"' | tr -d "'" | xargs || echo "")
MAIL_SMTP_AUTH=$(grep "^MAIL_SMTP_AUTH=" "$ENV_FILE" | cut -d '=' -f2 | tr -d '"' | tr -d "'" | xargs || echo "true")
MAIL_SMTP_STARTTLS_ENABLE=$(grep "^MAIL_SMTP_STARTTLS_ENABLE=" "$ENV_FILE" | cut -d '=' -f2 | tr -d '"' | tr -d "'" | xargs || echo "")
MAIL_SMTP_STARTTLS_REQUIRED=$(grep "^MAIL_SMTP_STARTTLS_REQUIRED=" "$ENV_FILE" | cut -d '=' -f2 | tr -d '"' | tr -d "'" | xargs || echo "")
MAIL_SMTP_SSL_ENABLE=$(grep "^MAIL_SMTP_SSL_ENABLE=" "$ENV_FILE" | cut -d '=' -f2 | tr -d '"' | tr -d "'" | xargs || echo "")
MAIL_SMTP_SSL_TRUST=$(grep "^MAIL_SMTP_SSL_TRUST=" "$ENV_FILE" | cut -d '=' -f2 | tr -d '"' | tr -d "'" | xargs || echo "")

# Display configuration (hide password)
echo ""
echo "Email Configuration from $ENV_FILE:"
echo "  MAIL_HOST: ${MAIL_HOST:-not set}"
echo "  MAIL_PORT: ${MAIL_PORT:-not set}"
echo "  MAIL_USERNAME: ${MAIL_USERNAME:-not set}"
echo "  MAIL_PASSWORD: ${MAIL_PASSWORD:+***SET***}${MAIL_PASSWORD:-not set}"
echo "  MAIL_SMTP_AUTH: ${MAIL_SMTP_AUTH}"
echo "  MAIL_SMTP_STARTTLS_ENABLE: ${MAIL_SMTP_STARTTLS_ENABLE:-not set}"
echo "  MAIL_SMTP_STARTTLS_REQUIRED: ${MAIL_SMTP_STARTTLS_REQUIRED:-not set}"
echo "  MAIL_SMTP_SSL_ENABLE: ${MAIL_SMTP_SSL_ENABLE:-not set}"
echo "  MAIL_SMTP_SSL_TRUST: ${MAIL_SMTP_SSL_TRUST:-not set}"
echo ""

# Validate configuration
if [ -z "$MAIL_HOST" ]; then
    print_error "MAIL_HOST is not set in $ENV_FILE"
    exit 1
fi

if [ -z "$MAIL_PORT" ]; then
    print_error "MAIL_PORT is not set in $ENV_FILE"
    exit 1
fi

if [ -z "$MAIL_USERNAME" ]; then
    print_error "MAIL_USERNAME is not set in $ENV_FILE"
    exit 1
fi

if [ -z "$MAIL_PASSWORD" ] || [ "$MAIL_PASSWORD" = "CHANGE_THIS"* ]; then
    print_error "MAIL_PASSWORD is not set or still has placeholder value!"
    print_info "Update MAIL_PASSWORD in $ENV_FILE with your actual email password"
    exit 1
fi

print_success "Email configuration loaded from $ENV_FILE"

# Check 1: Firewall Status
print_step "Checking Firewall Status"

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
        if ufw status | grep -q "${MAIL_PORT}"; then
            print_info "SMTP port ${MAIL_PORT} found in firewall rules:"
            ufw status | grep "${MAIL_PORT}"
        else
            print_warning "SMTP port ${MAIL_PORT} not explicitly allowed"
            print_info "This is OK if 'default allow outgoing' is set"
        fi
    else
        print_warning "Firewall is not active"
    fi
elif command -v firewall-cmd &> /dev/null; then
    print_info "Using firewalld (firewall-cmd)"
    if firewall-cmd --state 2>/dev/null | grep -q "running"; then
        print_info "Firewalld is running"
    fi
else
    print_warning "No firewall tool found (ufw or firewalld)"
fi

# Check 2: DNS Resolution
print_step "Testing DNS Resolution"

print_info "Resolving $MAIL_HOST..."
if host "$MAIL_HOST" &> /dev/null || nslookup "$MAIL_HOST" &> /dev/null; then
    SMTP_IP=$(host "$MAIL_HOST" 2>/dev/null | grep "has address" | awk '{print $4}' | head -1 || \
              nslookup "$MAIL_HOST" 2>/dev/null | grep -A 1 "Name:" | tail -1 | awk '{print $2}' || echo "")
    if [ -n "$SMTP_IP" ]; then
        print_success "DNS resolved: $MAIL_HOST -> $SMTP_IP"
    else
        print_warning "DNS resolved but couldn't extract IP"
    fi
else
    print_error "Cannot resolve $MAIL_HOST"
    exit 1
fi

# Check 3: Test SMTP Connectivity
print_step "Testing SMTP Connectivity"

print_info "Testing connection to $MAIL_HOST:${MAIL_PORT}..."
if timeout 5 bash -c "echo > /dev/tcp/$MAIL_HOST/${MAIL_PORT}" 2>/dev/null; then
    print_success "Port ${MAIL_PORT} is reachable"
else
    print_error "Cannot connect to $MAIL_HOST:${MAIL_PORT}"
    print_info "This could indicate:"
    print_info "  - Firewall blocking outbound connection"
    print_info "  - Network issue"
    print_info "  - SMTP server issue"
    exit 1
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
        if docker exec "$BACKEND_CONTAINER" timeout 5 bash -c "echo > /dev/tcp/$MAIL_HOST/${MAIL_PORT}" 2>/dev/null; then
            print_success "Backend container can reach $MAIL_HOST:${MAIL_PORT}"
        else
            print_error "Backend container cannot reach $MAIL_HOST:${MAIL_PORT}"
            print_info "This could indicate Docker network restrictions"
        fi
        
        # Check environment variables in container
        print_info "Checking environment variables in $BACKEND_CONTAINER..."
        echo ""
        echo "Email Configuration in Container:"
        docker exec "$BACKEND_CONTAINER" env | grep -E "MAIL_|EMAIL_" | sed 's/PASSWORD=.*/PASSWORD=***HIDDEN***/' || print_warning "No MAIL_ variables found"
        echo ""
        
        # Compare .env with container
        CONTAINER_MAIL_HOST=$(docker exec "$BACKEND_CONTAINER" env | grep "^MAIL_HOST=" | cut -d '=' -f2 || echo "")
        CONTAINER_MAIL_PORT=$(docker exec "$BACKEND_CONTAINER" env | grep "^MAIL_PORT=" | cut -d '=' -f2 || echo "")
        CONTAINER_MAIL_USERNAME=$(docker exec "$BACKEND_CONTAINER" env | grep "^MAIL_USERNAME=" | cut -d '=' -f2 || echo "")
        
        if [ -n "$CONTAINER_MAIL_HOST" ]; then
            if [ "$CONTAINER_MAIL_HOST" != "$MAIL_HOST" ]; then
                print_warning "MAIL_HOST mismatch: .env=$MAIL_HOST, container=$CONTAINER_MAIL_HOST"
            fi
            if [ "$CONTAINER_MAIL_PORT" != "$MAIL_PORT" ]; then
                print_warning "MAIL_PORT mismatch: .env=$MAIL_PORT, container=$CONTAINER_MAIL_PORT"
            fi
            if [ "$CONTAINER_MAIL_USERNAME" != "$MAIL_USERNAME" ]; then
                print_warning "MAIL_USERNAME mismatch: .env=$MAIL_USERNAME, container=$CONTAINER_MAIL_USERNAME"
            fi
        fi
    else
        print_warning "Backend container not running"
    fi
else
    print_warning "Docker not found"
fi

# Check 5: Test SMTP Authentication
print_step "Testing SMTP Authentication"

print_info "Testing authentication with $MAIL_USERNAME on $MAIL_HOST:${MAIL_PORT}..."

# Check if swaks (Swiss Army Knife for SMTP) is available
if command -v swaks &> /dev/null; then
    print_info "Using swaks to test SMTP authentication..."
    
    if [ "$MAIL_PORT" = "465" ]; then
        # Port 465 uses SSL
        SWAKS_CMD="swaks --server $MAIL_HOST --port $MAIL_PORT --from $MAIL_USERNAME --to $MAIL_USERNAME --auth LOGIN --auth-user $MAIL_USERNAME --auth-password '$MAIL_PASSWORD' --tls"
    else
        # Port 587 uses STARTTLS
        SWAKS_CMD="swaks --server $MAIL_HOST --port $MAIL_PORT --from $MAIL_USERNAME --to $MAIL_USERNAME --auth LOGIN --auth-user $MAIL_USERNAME --auth-password '$MAIL_PASSWORD' --tls"
    fi
    
    if eval "$SWAKS_CMD" 2>&1 | grep -q "250\|200\|Authentication succeeded"; then
        print_success "SMTP authentication successful!"
    else
        AUTH_RESULT=$(eval "$SWAKS_CMD" 2>&1 | tail -5)
        print_error "SMTP authentication failed!"
        echo ""
        echo "Authentication test output:"
        echo "$AUTH_RESULT"
        echo ""
        print_info "This indicates:"
        print_info "  - Password is incorrect"
        print_info "  - Username format is wrong"
        print_info "  - Account might be locked or restricted"
    fi
elif command -v python3 &> /dev/null; then
    print_info "Using Python to test SMTP authentication..."
    
    # Create temporary Python script to test authentication
    cat > /tmp/test_smtp_auth.py << EOF
import smtplib
import sys
from email.mime.text import MIMEText

host = "$MAIL_HOST"
port = int("$MAIL_PORT")
username = "$MAIL_USERNAME"
password = "$MAIL_PASSWORD"
use_ssl = "$MAIL_SMTP_SSL_ENABLE" == "true"
use_starttls = "$MAIL_SMTP_STARTTLS_ENABLE" == "true"

try:
    if use_ssl or port == 465:
        # SSL connection
        server = smtplib.SMTP_SSL(host, port, timeout=10)
    else:
        # Regular connection with STARTTLS
        server = smtplib.SMTP(host, port, timeout=10)
        if use_starttls:
            server.starttls()
    
    server.login(username, password)
    print("SUCCESS: Authentication successful!")
    server.quit()
    sys.exit(0)
except smtplib.SMTPAuthenticationError as e:
    print(f"FAILED: Authentication failed - {e}")
    sys.exit(1)
except Exception as e:
    print(f"ERROR: {e}")
    sys.exit(1)
EOF
    
    AUTH_RESULT=$(python3 /tmp/test_smtp_auth.py 2>&1)
    rm -f /tmp/test_smtp_auth.py
    
    if echo "$AUTH_RESULT" | grep -q "SUCCESS"; then
        print_success "SMTP authentication successful!"
    else
        print_error "SMTP authentication failed!"
        echo ""
        echo "Authentication test output:"
        echo "$AUTH_RESULT"
        echo ""
        print_info "Common causes:"
        print_info "  - Password is incorrect"
        print_info "  - Username should be full email address: $MAIL_USERNAME"
        print_info "  - Account might require IP whitelisting"
    fi
else
    print_warning "swaks and python3 not available, skipping authentication test"
    print_info "Install swaks: sudo apt-get install swaks"
    print_info "Or install python3: sudo apt-get install python3"
fi

# Configuration Validation
print_step "Validating SMTP Configuration"

CONFIG_VALID=true

# Check port 587 configuration
if [ "$MAIL_PORT" = "587" ]; then
    if [ "$MAIL_SMTP_STARTTLS_ENABLE" != "true" ]; then
        print_error "For port 587, MAIL_SMTP_STARTTLS_ENABLE should be 'true'"
        CONFIG_VALID=false
    fi
    if [ "$MAIL_SMTP_STARTTLS_REQUIRED" != "true" ]; then
        print_error "For port 587, MAIL_SMTP_STARTTLS_REQUIRED should be 'true'"
        CONFIG_VALID=false
    fi
    if [ "$MAIL_SMTP_SSL_ENABLE" = "true" ]; then
        print_error "For port 587, MAIL_SMTP_SSL_ENABLE should be 'false'"
        CONFIG_VALID=false
    fi
fi

# Check port 465 configuration
if [ "$MAIL_PORT" = "465" ]; then
    if [ "$MAIL_SMTP_SSL_ENABLE" != "true" ]; then
        print_error "For port 465, MAIL_SMTP_SSL_ENABLE should be 'true'"
        CONFIG_VALID=false
    fi
    if [ "$MAIL_SMTP_STARTTLS_ENABLE" = "true" ]; then
        print_error "For port 465, MAIL_SMTP_STARTTLS_ENABLE should be 'false'"
        CONFIG_VALID=false
    fi
fi

if [ "$CONFIG_VALID" = true ]; then
    print_success "SMTP configuration is valid for port ${MAIL_PORT}"
else
    print_warning "SMTP configuration has issues - check the errors above"
fi

# Summary
print_step "Summary and Recommendations"

echo ""
if [ "$MAIL_PORT" = "587" ]; then
    print_info "Current configuration: Port 587 (TLS/STARTTLS)"
    print_info "Required settings:"
    echo "  MAIL_SMTP_STARTTLS_ENABLE=true"
    echo "  MAIL_SMTP_STARTTLS_REQUIRED=true"
    echo "  MAIL_SMTP_SSL_ENABLE=false"
elif [ "$MAIL_PORT" = "465" ]; then
    print_info "Current configuration: Port 465 (SSL)"
    print_info "Required settings:"
    echo "  MAIL_SMTP_SSL_ENABLE=true"
    echo "  MAIL_SMTP_STARTTLS_ENABLE=false"
    echo "  MAIL_SMTP_SSL_SOCKET_FACTORY_PORT=465"
fi

echo ""
print_info "If authentication failed:"
echo "  1. Verify password in Hostinger webmail: https://webmail.hostinger.com"
echo "  2. Reset password in Hostinger hPanel if needed"
echo "  3. Update .env file with correct password"
echo "  4. Restart backend: docker compose restart backend"
echo ""
print_info "If connectivity failed:"
echo "  1. Check firewall: sudo ufw status verbose"
echo "  2. Allow outgoing: sudo ufw default allow outgoing"
echo "  3. Test manually: telnet $MAIL_HOST $MAIL_PORT"

print_success "Diagnostics completed!"
