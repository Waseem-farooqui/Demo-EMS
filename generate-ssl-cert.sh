#!/bin/bash
###############################################################################
# SSL Certificate Generation Script
# Generates self-signed certificates for development/testing
# For production, use Let's Encrypt (see setup-letsencrypt.sh)
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
DOMAIN="${DOMAIN:-localhost}"
DAYS_VALID="${DAYS_VALID:-365}"

echo -e "${CYAN}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║        SSL Certificate Generation Script                  ║${NC}"
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

# Check if OpenSSL is installed
if ! command -v openssl &> /dev/null; then
    print_error "OpenSSL is not installed!"
    print_info "Install it with: sudo apt-get install openssl"
    exit 1
fi

# Create SSL directory
print_info "Creating SSL directory: $SSL_DIR"
mkdir -p "$SSL_DIR"
chmod 700 "$SSL_DIR"

# Check if certificates already exist
if [ -f "$SSL_DIR/$DOMAIN.crt" ] && [ -f "$SSL_DIR/$DOMAIN.key" ]; then
    print_warning "SSL certificates already exist for $DOMAIN"
    read -p "Do you want to regenerate them? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        print_info "Keeping existing certificates"
        exit 0
    fi
    print_info "Backing up existing certificates..."
    mv "$SSL_DIR/$DOMAIN.crt" "$SSL_DIR/$DOMAIN.crt.backup.$(date +%Y%m%d_%H%M%S)" 2>/dev/null || true
    mv "$SSL_DIR/$DOMAIN.key" "$SSL_DIR/$DOMAIN.key.backup.$(date +%Y%m%d_%H%M%S)" 2>/dev/null || true
fi

# Generate private key
print_info "Generating private key (2048 bits)..."
openssl genrsa -out "$SSL_DIR/$DOMAIN.key" 2048
chmod 600 "$SSL_DIR/$DOMAIN.key"
print_success "Private key generated"

# Generate certificate signing request
print_info "Generating certificate signing request..."
openssl req -new -key "$SSL_DIR/$DOMAIN.key" -out "$SSL_DIR/$DOMAIN.csr" \
    -subj "/C=US/ST=State/L=City/O=Organization/CN=$DOMAIN" \
    -addext "subjectAltName=DNS:$DOMAIN,DNS:*.$DOMAIN,IP:127.0.0.1,IP:::1"

print_success "CSR generated"

# Generate self-signed certificate
print_info "Generating self-signed certificate (valid for $DAYS_VALID days)..."
openssl x509 -req -days "$DAYS_VALID" \
    -in "$SSL_DIR/$DOMAIN.csr" \
    -signkey "$SSL_DIR/$DOMAIN.key" \
    -out "$SSL_DIR/$DOMAIN.crt" \
    -extensions v3_req \
    -extfile <(
        echo "[v3_req]"
        echo "keyUsage = keyEncipherment, dataEncipherment"
        echo "extendedKeyUsage = serverAuth"
        echo "subjectAltName = @alt_names"
        echo "[alt_names]"
        echo "DNS.1 = $DOMAIN"
        echo "DNS.2 = *.$DOMAIN"
        echo "IP.1 = 127.0.0.1"
        echo "IP.2 = ::1"
    )

chmod 644 "$SSL_DIR/$DOMAIN.crt"
print_success "Certificate generated"

# Generate fullchain (for Let's Encrypt compatibility)
if [ -f "$SSL_DIR/$DOMAIN.crt" ]; then
    cp "$SSL_DIR/$DOMAIN.crt" "$SSL_DIR/$DOMAIN-fullchain.crt" 2>/dev/null || true
fi

# Generate DH parameters for better security (optional, takes time)
print_info "Generating DH parameters (this may take a few minutes)..."
if [ ! -f "$SSL_DIR/dhparam.pem" ]; then
    openssl dhparam -out "$SSL_DIR/dhparam.pem" 2048 2>/dev/null || {
        print_warning "DH parameter generation failed or took too long"
        print_info "You can generate it later with: openssl dhparam -out $SSL_DIR/dhparam.pem 2048"
    }
    if [ -f "$SSL_DIR/dhparam.pem" ]; then
        chmod 600 "$SSL_DIR/dhparam.pem"
        print_success "DH parameters generated"
    fi
else
    print_info "DH parameters already exist, skipping"
fi

# Display certificate information
print_info "Certificate details:"
openssl x509 -in "$SSL_DIR/$DOMAIN.crt" -text -noout | grep -E "Subject:|Issuer:|Not Before|Not After|DNS:|IP Address:" | head -10

echo ""
print_success "SSL certificates generated successfully!"
echo ""
print_info "Certificate files:"
echo "  - Private Key: $SSL_DIR/$DOMAIN.key"
echo "  - Certificate: $SSL_DIR/$DOMAIN.crt"
echo "  - Full Chain: $SSL_DIR/$DOMAIN-fullchain.crt"
echo "  - CSR: $SSL_DIR/$DOMAIN.csr"
if [ -f "$SSL_DIR/dhparam.pem" ]; then
    echo "  - DH Params: $SSL_DIR/dhparam.pem"
fi
echo ""
print_warning "These are self-signed certificates for development/testing"
print_warning "For production, use Let's Encrypt certificates (see setup-letsencrypt.sh)"
echo ""
print_info "Next steps:"
echo "  1. Update compose.yaml to mount SSL certificates"
echo "  2. Update nginx.conf to use HTTPS"
echo "  3. Restart containers: docker-compose up -d"
echo ""

