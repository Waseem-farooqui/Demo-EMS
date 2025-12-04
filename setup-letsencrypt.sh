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
DOMAIN="${DOMAIN:-}"

echo -e "${CYAN}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║        Let's Encrypt SSL Certificate Setup                ║${NC}"
echo -e "${CYAN}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Function to print colored messages
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

# Check if certbot is installed
if ! command -v certbot &> /dev/null; then
    print_info "Certbot is not installed. Installing..."
    sudo apt-get update
    sudo apt-get install -y certbot
fi

# Get domain name
if [ -z "$DOMAIN" ]; then
    read -p "Enter your domain name (e.g., example.com): " DOMAIN
fi

if [ -z "$DOMAIN" ]; then
    print_error "Domain name is required!"
    exit 1
fi

# Get email
if [ -z "$EMAIL" ] || [ "$EMAIL" = "admin@example.com" ]; then
    read -p "Enter your email address for Let's Encrypt notifications: " EMAIL
fi

if [ -z "$EMAIL" ]; then
    print_error "Email address is required!"
    exit 1
fi

# Create SSL directory
print_info "Creating SSL directory: $SSL_DIR"
mkdir -p "$SSL_DIR"
chmod 700 "$SSL_DIR"

# Stop nginx temporarily for certificate generation
print_info "Stopping nginx temporarily..."
sudo systemctl stop nginx 2>/dev/null || docker-compose stop frontend 2>/dev/null || true

# Generate certificate using standalone mode
print_info "Generating Let's Encrypt certificate for $DOMAIN..."
print_warning "Make sure port 80 is accessible from the internet!"

# Use certbot standalone mode
sudo certbot certonly --standalone \
    --non-interactive \
    --agree-tos \
    --email "$EMAIL" \
    -d "$DOMAIN" \
    --cert-path "$SSL_DIR" \
    --key-path "$SSL_DIR" \
    --fullchain-path "$SSL_DIR" \
    --preferred-challenges http || {
    
    print_error "Certificate generation failed!"
    print_info "Common issues:"
    print_info "  1. Port 80 must be accessible from the internet"
    print_info "  2. Domain must point to this server's IP address"
    print_info "  3. Firewall must allow port 80"
    exit 1
}

# Copy certificates to SSL directory
print_info "Copying certificates..."
sudo cp "/etc/letsencrypt/live/$DOMAIN/fullchain.pem" "$SSL_DIR/$DOMAIN-fullchain.crt" 2>/dev/null || true
sudo cp "/etc/letsencrypt/live/$DOMAIN/privkey.pem" "$SSL_DIR/$DOMAIN.key" 2>/dev/null || true
sudo cp "/etc/letsencrypt/live/$DOMAIN/cert.pem" "$SSL_DIR/$DOMAIN.crt" 2>/dev/null || true

# Set proper permissions
sudo chmod 644 "$SSL_DIR/$DOMAIN-fullchain.crt" 2>/dev/null || true
sudo chmod 600 "$SSL_DIR/$DOMAIN.key" 2>/dev/null || true
sudo chmod 644 "$SSL_DIR/$DOMAIN.crt" 2>/dev/null || true

# Generate DH parameters
print_info "Generating DH parameters..."
if [ ! -f "$SSL_DIR/dhparam.pem" ]; then
    sudo openssl dhparam -out "$SSL_DIR/dhparam.pem" 2048
    sudo chmod 600 "$SSL_DIR/dhparam.pem"
fi

print_success "Let's Encrypt certificates generated successfully!"
echo ""
print_info "Certificate files:"
echo "  - Full Chain: $SSL_DIR/$DOMAIN-fullchain.crt"
echo "  - Certificate: $SSL_DIR/$DOMAIN.crt"
echo "  - Private Key: $SSL_DIR/$DOMAIN.key"
echo "  - DH Params: $SSL_DIR/dhparam.pem"
echo ""
print_info "Certificates are valid for 90 days"
print_info "Set up auto-renewal with: sudo certbot renew --dry-run"
echo ""
print_info "To renew certificates manually:"
echo "  sudo certbot renew"
echo "  docker-compose restart frontend"
echo ""

