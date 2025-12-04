#!/bin/bash
###############################################################################
# Let's Encrypt SSL Certificate Setup Script
# For production environments with a valid domain name
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
SSL_DIR="${SSL_DIR:-$SCRIPT_DIR/ssl}"
EMAIL="${EMAIL:-admin@example.com}"
DOMAIN="${DOMAIN:-vertexdigitalsystem.com}"
SERVER_IP="${SERVER_IP:-}"  # Optional: Server IP for certificate

echo -e "${CYAN}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║        Let's Encrypt SSL Certificate Setup                ║${NC}"
echo -e "${CYAN}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Function to print colored messages
print_step() {
    echo ""
    echo -e "${BLUE}▶ $1${NC}"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
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

# Check if running as root
if [ "$EUID" -ne 0 ]; then
    print_error "Please run as root (use sudo)"
    exit 1
fi

# Get domain name
if [ "$DOMAIN" = "vertexdigitalsystem.com" ] || [ -z "$DOMAIN" ]; then
    read -p "Enter your domain name: " DOMAIN_INPUT
    DOMAIN="${DOMAIN_INPUT:-$DOMAIN}"
fi

if [ -z "$DOMAIN" ] || [ "$DOMAIN" = "localhost" ]; then
    print_error "Domain name is required!"
    exit 1
fi

# Get email
if [ "$EMAIL" = "admin@example.com" ] || [ -z "$EMAIL" ]; then
    read -p "Enter your email address for Let's Encrypt notifications: " EMAIL_INPUT
    EMAIL="${EMAIL_INPUT:-$EMAIL}"
fi

if [ -z "$EMAIL" ]; then
    print_error "Email address is required!"
    exit 1
fi

# Get server IP if not set
if [ -z "$SERVER_IP" ]; then
    # Try to detect server IP automatically
    DETECTED_IP=$(hostname -I | awk '{print $1}' 2>/dev/null || curl -s ifconfig.me 2>/dev/null || echo "")
    if [ -n "$DETECTED_IP" ]; then
        print_info "Detected server IP: $DETECTED_IP"
        read -p "Enter your production server IP address (or press Enter to use $DETECTED_IP): " SERVER_IP_INPUT
        SERVER_IP="${SERVER_IP_INPUT:-$DETECTED_IP}"
    else
        read -p "Enter your production server IP address: " SERVER_IP_INPUT
        SERVER_IP="${SERVER_IP_INPUT:-}"
    fi
fi

if [ -z "$SERVER_IP" ]; then
    print_error "Server IP address is required!"
    exit 1
fi

# Pre-flight checks
print_step "Pre-flight Checks"

# Check 1: Verify domain DNS resolution
print_info "Checking DNS resolution for $DOMAIN..."
DOMAIN_IP=$(dig +short $DOMAIN @8.8.8.8 | tail -1 2>/dev/null || nslookup $DOMAIN 8.8.8.8 2>/dev/null | grep -A 1 "Name:" | tail -1 | awk '{print $2}' || echo "")

if [ -z "$DOMAIN_IP" ]; then
    print_error "Failed to resolve $DOMAIN - DNS not configured or domain not pointing to any IP"
    print_info "Please configure DNS in Hostinger:"
    print_info "  1. Log in to Hostinger hPanel"
    print_info "  2. Go to DNS Zone Editor"
    print_info "  3. Add A record: $DOMAIN -> $SERVER_IP"
    print_info "  4. Add A record: www.$DOMAIN -> $SERVER_IP"
    print_info "  5. Wait for DNS propagation (can take up to 24 hours, usually 1-2 hours)"
    exit 1
fi

print_info "Domain $DOMAIN resolves to: $DOMAIN_IP"

# Check 2: Verify domain points to this server
if [ "$DOMAIN_IP" != "$SERVER_IP" ]; then
    print_error "DNS mismatch detected!"
    print_error "  Domain $DOMAIN points to: $DOMAIN_IP"
    print_error "  This server IP is: $SERVER_IP"
    print_info ""
    print_info "Please update DNS in Hostinger:"
    print_info "  1. Log in to Hostinger hPanel (https://hpanel.hostinger.com)"
    print_info "  2. Select your domain: $DOMAIN"
    print_info "  3. Go to 'DNS / Name Servers' section"
    print_info "  4. Click 'Manage' next to DNS Zone"
    print_info "  5. Update A record for $DOMAIN to point to: $SERVER_IP"
    print_info "  6. Update A record for www.$DOMAIN to point to: $SERVER_IP"
    print_info "  7. Wait for DNS propagation (check with: dig $DOMAIN @8.8.8.8)"
    print_info ""
    print_info "Current DNS status:"
    print_info "  $DOMAIN -> $DOMAIN_IP (should be $SERVER_IP)"
    exit 1
fi

print_success "DNS is correctly configured: $DOMAIN -> $SERVER_IP"

# Check 3: Verify www subdomain
print_info "Checking www.$DOMAIN..."
WWW_IP=$(dig +short www.$DOMAIN @8.8.8.8 | tail -1 2>/dev/null || nslookup www.$DOMAIN 8.8.8.8 2>/dev/null | grep -A 1 "Name:" | tail -1 | awk '{print $2}' || echo "")

if [ -z "$WWW_IP" ] || [ "$WWW_IP" != "$SERVER_IP" ]; then
    print_warning "www.$DOMAIN is not configured or points to different IP"
    print_info "  www.$DOMAIN -> ${WWW_IP:-not configured} (should be $SERVER_IP)"
    print_info "  Certificate will still be generated, but www subdomain may not work"
    read -p "Continue anyway? (y/N): " CONTINUE_WWW
    if [[ ! $CONTINUE_WWW =~ ^[Yy]$ ]]; then
        print_info "Please configure www.$DOMAIN in Hostinger DNS first"
        exit 1
    fi
else
    print_success "www.$DOMAIN is correctly configured: www.$DOMAIN -> $WWW_IP"
fi

# Check 4: Verify port 80 is accessible
print_info "Checking if port 80 is accessible..."
if command -v nc &> /dev/null; then
    if nc -z localhost 80 2>/dev/null; then
        print_success "Port 80 is open on this server"
    else
        print_warning "Port 80 is not listening on this server"
        print_info "Make sure nginx or another web server is running on port 80"
    fi
else
    print_info "netcat not available, skipping port check"
fi

# Check 5: Verify firewall allows port 80
print_info "Checking firewall configuration..."
if command -v ufw &> /dev/null; then
    if ufw status | grep -q "80.*ALLOW"; then
        print_success "Firewall allows port 80"
    else
        print_warning "Port 80 may not be allowed in firewall"
        print_info "Run: sudo ufw allow 80/tcp"
    fi
elif command -v firewall-cmd &> /dev/null; then
    if firewall-cmd --list-ports 2>/dev/null | grep -q "80/tcp"; then
        print_success "Firewall allows port 80"
    else
        print_warning "Port 80 may not be allowed in firewall"
        print_info "Run: sudo firewall-cmd --permanent --add-port=80/tcp && sudo firewall-cmd --reload"
    fi
else
    print_info "Firewall tool not found, assuming port 80 is accessible"
fi

# Check 6: Verify domain is accessible from internet
print_info "Testing domain accessibility from external service..."
EXTERNAL_TEST=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "http://$DOMAIN/.well-known/acme-challenge/test" 2>/dev/null || echo "000")
if [ "$EXTERNAL_TEST" != "000" ] && [ "$EXTERNAL_TEST" != "404" ]; then
    print_success "Domain is accessible from internet (HTTP $EXTERNAL_TEST)"
else
    print_warning "Could not verify domain accessibility from internet"
    print_info "This is normal if the domain was just configured"
fi

print_success "Pre-flight checks completed"
echo ""

# Create SSL directory
print_info "Creating SSL directory: $SSL_DIR"
mkdir -p "$SSL_DIR"
chmod 700 "$SSL_DIR"

# Check if certbot is installed
if ! command -v certbot &> /dev/null; then
    print_info "Certbot is not installed. Installing..."
    apt-get update
    apt-get install -y certbot
fi

# Stop nginx temporarily for certificate generation
print_info "Stopping nginx/docker containers temporarily for certificate generation..."
print_warning "This will stop your web server temporarily (usually 1-2 minutes)"

# Track what was running
NGINX_WAS_RUNNING=false
DOCKER_WAS_RUNNING=false

# Stop system nginx if running
if systemctl is-active --quiet nginx 2>/dev/null; then
    print_info "Stopping system nginx..."
    systemctl stop nginx
    NGINX_WAS_RUNNING=true
fi

# Stop docker frontend container if running
if command -v docker-compose &> /dev/null && [ -f compose.yaml ]; then
    if docker-compose ps frontend 2>/dev/null | grep -q "Up"; then
        print_info "Stopping Docker frontend container..."
        docker-compose stop frontend 2>/dev/null || true
        DOCKER_WAS_RUNNING=true
    fi
fi

# Wait a moment for ports to be released
sleep 2

# Verify port 80 is free
if command -v netstat &> /dev/null; then
    PORT_80_USAGE=$(netstat -tuln | grep ":80 " || echo "")
    if [ -n "$PORT_80_USAGE" ]; then
        print_warning "Port 80 is still in use:"
        echo "$PORT_80_USAGE"
        print_info "Trying to free port 80..."
        sleep 3
    fi
fi

# Generate certificate using standalone mode
print_info "Generating Let's Encrypt certificate for $DOMAIN..."
print_warning "Make sure port 80 is accessible from the internet!"

# Prepare certbot command
CERTBOT_CMD="certbot certonly --standalone --non-interactive --agree-tos --email $EMAIL -d $DOMAIN"

# Add www subdomain if domain doesn't start with www
if [[ ! "$DOMAIN" =~ ^www\. ]]; then
    CERTBOT_CMD="$CERTBOT_CMD -d www.$DOMAIN"
    print_info "Including www.$DOMAIN in certificate"
fi

# Execute certbot
print_info "Starting Certbot standalone server on port 80..."
print_warning "Certbot will temporarily use port 80 for domain verification"

$CERTBOT_CMD || {
    print_error "Certificate generation failed!"
    echo ""
    print_info "════════════════════════════════════════════════════════════"
    print_info "Hostinger DNS Configuration Guide"
    print_info "════════════════════════════════════════════════════════════"
    echo ""
    print_info "The error indicates DNS is not properly configured in Hostinger."
    echo ""
    print_info "Step-by-step Hostinger DNS setup:"
    echo ""
    print_info "1. Log in to Hostinger hPanel:"
    print_info "   https://hpanel.hostinger.com"
    echo ""
    print_info "2. Select your domain: $DOMAIN"
    echo ""
    print_info "3. Navigate to DNS / Name Servers section"
    echo ""
    print_info "4. Click 'Manage' next to 'DNS Zone' or 'DNS Records'"
    echo ""
    print_info "5. Add/Update A records:"
    print_info "   - Type: A"
    print_info "   - Name: @ (or leave blank for root domain)"
    print_info "   - Points to: $SERVER_IP"
    print_info "   - TTL: 3600 (or default)"
    echo ""
    print_info "   - Type: A"
    print_info "   - Name: www"
    print_info "   - Points to: $SERVER_IP"
    print_info "   - TTL: 3600 (or default)"
    echo ""
    print_info "6. Remove any conflicting A records pointing to different IPs"
    echo ""
    print_info "7. Save changes and wait for DNS propagation:"
    print_info "   - Usually takes 1-2 hours"
    print_info "   - Can take up to 24 hours"
    print_info "   - Check with: dig $DOMAIN @8.8.8.8"
    echo ""
    print_info "8. Verify DNS before retrying:"
    print_info "   dig $DOMAIN @8.8.8.8"
    print_info "   # Should show: $DOMAIN -> $SERVER_IP"
    echo ""
    print_info "════════════════════════════════════════════════════════════"
    print_info "Additional Troubleshooting"
    print_info "════════════════════════════════════════════════════════════"
    echo ""
    print_info "1. Verify port 80 is accessible:"
    print_info "   sudo ufw allow 80/tcp"
    print_info "   sudo netstat -tulpn | grep :80"
    echo ""
    print_info "2. Check Certbot logs for details:"
    print_info "   sudo cat /var/log/letsencrypt/letsencrypt.log | tail -50"
    echo ""
    print_info "3. Test domain accessibility:"
    print_info "   curl -I http://$DOMAIN"
    print_info "   # Should connect to this server, not Hostinger default page"
    echo ""
    
    # Restart services if they were running
    if [ "$NGINX_WAS_RUNNING" = true ]; then
        print_info "Restarting nginx..."
        systemctl start nginx 2>/dev/null || true
    fi
    if [ "$DOCKER_WAS_RUNNING" = true ]; then
        print_info "Restarting Docker frontend..."
        docker-compose start frontend 2>/dev/null || true
    fi
    
    exit 1
}

# Copy certificates to SSL directory
print_info "Copying certificates..."
cp "/etc/letsencrypt/live/$DOMAIN/fullchain.pem" "$SSL_DIR/$DOMAIN-fullchain.crt" 2>/dev/null || true
cp "/etc/letsencrypt/live/$DOMAIN/privkey.pem" "$SSL_DIR/$DOMAIN.key" 2>/dev/null || true
cp "/etc/letsencrypt/live/$DOMAIN/cert.pem" "$SSL_DIR/$DOMAIN.crt" 2>/dev/null || true

# Set proper permissions
chmod 644 "$SSL_DIR/$DOMAIN-fullchain.crt" 2>/dev/null || true
chmod 600 "$SSL_DIR/$DOMAIN.key" 2>/dev/null || true
chmod 644 "$SSL_DIR/$DOMAIN.crt" 2>/dev/null || true

# Create symlinks for nginx
cp "$SSL_DIR/$DOMAIN.crt" "$SSL_DIR/cert.crt" 2>/dev/null || true
cp "$SSL_DIR/$DOMAIN.key" "$SSL_DIR/cert.key" 2>/dev/null || true

# Generate DH parameters
print_info "Generating DH parameters..."
if [ ! -f "$SSL_DIR/dhparam.pem" ]; then
    openssl dhparam -out "$SSL_DIR/dhparam.pem" 2048
    chmod 600 "$SSL_DIR/dhparam.pem"
    print_success "DH parameters generated"
else
    print_info "DH parameters already exist, skipping"
fi

print_success "Let's Encrypt certificates generated successfully!"
echo ""
print_info "Certificate files:"
echo "  - Full Chain: $SSL_DIR/$DOMAIN-fullchain.crt"
echo "  - Certificate: $SSL_DIR/$DOMAIN.crt"
echo "  - Private Key: $SSL_DIR/$DOMAIN.key"
echo "  - Nginx cert: $SSL_DIR/cert.crt"
echo "  - Nginx key: $SSL_DIR/cert.key"
echo "  - DH Params: $SSL_DIR/dhparam.pem"
echo ""
print_info "Certificates are valid for 90 days"
print_info "Set up auto-renewal with: sudo certbot renew --dry-run"
echo ""
print_info "To renew certificates manually:"
echo "  sudo certbot renew"
echo "  docker-compose restart frontend"
echo ""
print_info "To set up automatic renewal, add to crontab:"
echo "  sudo crontab -e"
echo "  # Add this line:"
echo "  0 0 * * * certbot renew --quiet && docker-compose -f /path/to/compose.yaml restart frontend"
echo ""

