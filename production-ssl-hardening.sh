#!/bin/bash
###############################################################################
# Production SSL & System Hardening Script
# Combines SSL certificate generation and Ubuntu server hardening
# For production deployment on Ubuntu 24.04 LTS
###############################################################################

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SSL_DIR="${SSL_DIR:-$SCRIPT_DIR/ssl}"
DOMAIN="${DOMAIN:-localhost}"
EMAIL="${EMAIL:-admin@example.com}"
USE_LETSENCRYPT="${USE_LETSENCRYPT:-false}"

# Banner
echo -e "${CYAN}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║   Production SSL & System Hardening Script               ║${NC}"
echo -e "${CYAN}║              Ubuntu 24.04 LTS - Complete Setup            ║${NC}"
echo -e "${CYAN}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Functions
print_step() {
    echo -e "\n${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${CYAN}$1${NC}"
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"
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

# Step 1: SSL Certificate Setup
print_step "Step 1/2: Setting Up SSL Certificates"

# Get domain name
if [ "$DOMAIN" = "localhost" ]; then
    read -p "Enter your domain name (or 'localhost' for self-signed): " DOMAIN_INPUT
    DOMAIN="${DOMAIN_INPUT:-localhost}"
fi

# Get email for Let's Encrypt
if [ "$USE_LETSENCRYPT" = "true" ] || [ "$DOMAIN" != "localhost" ]; then
    if [ "$EMAIL" = "admin@example.com" ]; then
        read -p "Enter your email for Let's Encrypt notifications: " EMAIL_INPUT
        EMAIL="${EMAIL_INPUT:-$EMAIL}"
    fi
fi

# Create SSL directory
mkdir -p "$SSL_DIR"
chmod 700 "$SSL_DIR"

# Generate SSL certificates
if [ "$DOMAIN" = "localhost" ] || [ "$USE_LETSENCRYPT" != "true" ]; then
    print_info "Generating self-signed certificates for $DOMAIN..."
    
    if [ -f "$SCRIPT_DIR/generate-ssl-cert.sh" ]; then
        chmod +x "$SCRIPT_DIR/generate-ssl-cert.sh"
        DOMAIN="$DOMAIN" "$SCRIPT_DIR/generate-ssl-cert.sh" || {
            print_error "SSL certificate generation failed!"
            exit 1
        }
    else
        print_error "generate-ssl-cert.sh not found!"
        exit 1
    fi
else
    print_info "Setting up Let's Encrypt certificates for $DOMAIN..."
    
    # Install certbot if not installed
    if ! command -v certbot &> /dev/null; then
        print_info "Installing certbot..."
        apt-get update
        apt-get install -y certbot
    fi
    
    # Stop nginx temporarily
    systemctl stop nginx 2>/dev/null || docker-compose stop frontend 2>/dev/null || true
    
    # Generate certificate
    certbot certonly --standalone \
        --non-interactive \
        --agree-tos \
        --email "$EMAIL" \
        -d "$DOMAIN" || {
        print_error "Let's Encrypt certificate generation failed!"
        print_info "Make sure:"
        print_info "  1. Domain points to this server's IP"
        print_info "  2. Port 80 is accessible from internet"
        print_info "  3. Firewall allows port 80"
        exit 1
    }
    
    # Copy certificates
    cp "/etc/letsencrypt/live/$DOMAIN/fullchain.pem" "$SSL_DIR/$DOMAIN-fullchain.crt" 2>/dev/null || true
    cp "/etc/letsencrypt/live/$DOMAIN/privkey.pem" "$SSL_DIR/$DOMAIN.key" 2>/dev/null || true
    cp "/etc/letsencrypt/live/$DOMAIN/cert.pem" "$SSL_DIR/$DOMAIN.crt" 2>/dev/null || true
    
    chmod 644 "$SSL_DIR/$DOMAIN-fullchain.crt" 2>/dev/null || true
    chmod 600 "$SSL_DIR/$DOMAIN.key" 2>/dev/null || true
    chmod 644 "$SSL_DIR/$DOMAIN.crt" 2>/dev/null || true
fi

# Prepare certificates for nginx
if [ -f "$SSL_DIR/$DOMAIN.crt" ] && [ -f "$SSL_DIR/$DOMAIN.key" ]; then
    cp "$SSL_DIR/$DOMAIN.crt" "$SSL_DIR/cert.crt" 2>/dev/null || true
    cp "$SSL_DIR/$DOMAIN.key" "$SSL_DIR/cert.key" 2>/dev/null || true
    print_success "SSL certificates prepared"
else
    print_error "SSL certificates not found!"
    exit 1
fi

# Step 2: System Hardening
print_step "Step 2/2: System Hardening"

# Check if hardening script exists
if [ -f "$SCRIPT_DIR/ubuntu-server-security-hardening.sh" ]; then
    print_info "Running Ubuntu server security hardening..."
    chmod +x "$SCRIPT_DIR/ubuntu-server-security-hardening.sh"
    "$SCRIPT_DIR/ubuntu-server-security-hardening.sh" || {
        print_warning "Some hardening steps may have failed, but continuing..."
    }
else
    print_warning "ubuntu-server-security-hardening.sh not found, skipping hardening"
fi

print_success "SSL & System Hardening Complete!"
echo ""
print_info "SSL certificates location: $SSL_DIR"
print_info "Domain: $DOMAIN"
if [ "$DOMAIN" != "localhost" ] && [ "$USE_LETSENCRYPT" = "true" ]; then
    print_info "Let's Encrypt certificates are valid for 90 days"
    print_info "Set up auto-renewal: sudo certbot renew --dry-run"
fi
echo ""

