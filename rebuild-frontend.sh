#!/bin/bash
###############################################################################
# Frontend Rebuild Script with Nginx Configuration Check
# This script rebuilds only the frontend container and verifies nginx config
###############################################################################

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Banner
echo -e "${CYAN}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║   Frontend Rebuild Script with Nginx Verification         ║${NC}"
echo -e "${CYAN}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Functions
print_step() {
    echo ""
    echo -e "${BLUE}▶ $1${NC}"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_info() {
    echo -e "${CYAN}ℹ️  $1${NC}"
}

# Check if running as root
if [ "$EUID" -eq 0 ]; then
    print_warning "Running as root. Some operations may need adjustment."
fi

# Check prerequisites
print_step "Step 1/10: Checking Prerequisites"

# Check Docker
if ! command -v docker &> /dev/null; then
    print_error "Docker is not installed"
    exit 1
else
    print_success "Docker is installed: $(docker --version)"
fi

# Check Docker Compose
if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    print_error "Docker Compose is not installed"
    exit 1
else
    print_success "Docker Compose is available"
fi

# Check if in project directory
if [ ! -f "compose.yaml" ] && [ ! -f "docker-compose.yml" ]; then
    print_error "compose.yaml or docker-compose.yml not found"
    print_info "Please run this script from the project root directory"
    exit 1
fi

COMPOSE_FILE="compose.yaml"
if [ ! -f "$COMPOSE_FILE" ]; then
    COMPOSE_FILE="docker-compose.yml"
fi

print_success "Using compose file: $COMPOSE_FILE"

# Check frontend directory
if [ ! -d "frontend" ]; then
    print_error "frontend directory not found"
    exit 1
fi

print_success "Frontend directory found"

# Check nginx.conf
if [ ! -f "frontend/nginx.conf" ]; then
    print_error "frontend/nginx.conf not found"
    exit 1
fi

print_success "nginx.conf found"

# Load environment variables
print_step "Step 2/10: Loading Environment Variables"

if [ -f .env ]; then
    print_success ".env file found"
    set -a
    source .env 2>/dev/null || true
    set +a
else
    print_warning ".env file not found, using defaults"
fi

# Verify nginx configuration
print_step "Step 3/10: Verifying Nginx Configuration"

NGINX_CONF="frontend/nginx.conf"

# Check if nginx.conf exists and is readable
if [ ! -f "$NGINX_CONF" ]; then
    print_error "nginx.conf not found at $NGINX_CONF"
    exit 1
fi

print_success "nginx.conf file found"

# Check nginx configuration syntax (if nginx is available locally)
if command -v nginx &> /dev/null; then
    print_info "Checking nginx configuration syntax..."
    if nginx -t -c "$(pwd)/$NGINX_CONF" 2>/dev/null; then
        print_success "Nginx configuration syntax is valid"
    else
        print_warning "Could not validate nginx config syntax (nginx binary not in PATH or config path issue)"
        print_info "Will validate during container build"
    fi
else
    print_info "nginx binary not found locally, will validate during container build"
fi

# Check for common security issues in nginx.conf
print_info "Checking for common security issues..."

SECURITY_ISSUES=0

# Check for server_tokens
if grep -q "server_tokens off" "$NGINX_CONF"; then
    print_success "server_tokens is disabled (good for security)"
else
    print_warning "server_tokens not explicitly disabled (may expose nginx version)"
    SECURITY_ISSUES=$((SECURITY_ISSUES + 1))
fi

# Check for X-Frame-Options
if grep -qi "X-Frame-Options" "$NGINX_CONF"; then
    print_success "X-Frame-Options header found"
else
    print_warning "X-Frame-Options header not found (recommended for security)"
    SECURITY_ISSUES=$((SECURITY_ISSUES + 1))
fi

# Check for X-Content-Type-Options
if grep -qi "X-Content-Type-Options" "$NGINX_CONF"; then
    print_success "X-Content-Type-Options header found"
else
    print_warning "X-Content-Type-Options header not found (recommended for security)"
    SECURITY_ISSUES=$((SECURITY_ISSUES + 1))
fi

# Check for Content-Security-Policy
if grep -qi "Content-Security-Policy" "$NGINX_CONF"; then
    print_success "Content-Security-Policy header found"
else
    print_warning "Content-Security-Policy header not found (recommended for security)"
    SECURITY_ISSUES=$((SECURITY_ISSUES + 1))
fi

# Check for try_files directive (required for Angular routing)
if grep -q "try_files" "$NGINX_CONF"; then
    print_success "try_files directive found (required for Angular routing)"
else
    print_error "try_files directive not found - Angular routing will not work!"
    SECURITY_ISSUES=$((SECURITY_ISSUES + 1))
fi

# Check for root directive
if grep -q "root /usr/share/nginx/html" "$NGINX_CONF"; then
    print_success "root directive points to correct location"
else
    print_warning "root directive may not point to /usr/share/nginx/html"
    SECURITY_ISSUES=$((SECURITY_ISSUES + 1))
fi

if [ $SECURITY_ISSUES -eq 0 ]; then
    print_success "No security issues found in nginx configuration"
else
    print_warning "Found $SECURITY_ISSUES potential security/configuration issues"
    print_info "Review nginx.conf and consider adding security headers"
fi

# Check frontend files
print_step "Step 4/10: Checking Frontend Source Files"

if [ ! -f "frontend/package.json" ]; then
    print_error "frontend/package.json not found"
    exit 1
fi

if [ ! -f "frontend/Dockerfile" ]; then
    print_error "frontend/Dockerfile not found"
    exit 1
fi

print_success "Frontend source files found"

# Check if frontend container is running
print_step "Step 5/10: Checking Current Frontend Container Status"

if docker-compose -f "$COMPOSE_FILE" ps frontend | grep -q "Up"; then
    print_info "Frontend container is currently running"
    CONTAINER_RUNNING=true
else
    print_info "Frontend container is not running"
    CONTAINER_RUNNING=false
fi

# Stop frontend container
print_step "Step 6/10: Stopping Frontend Container"

if [ "$CONTAINER_RUNNING" = true ]; then
    print_info "Stopping frontend container..."
    docker-compose -f "$COMPOSE_FILE" stop frontend
    print_success "Frontend container stopped"
else
    print_info "Frontend container was not running"
fi

# Remove old frontend container and image
print_step "Step 7/10: Removing Old Frontend Container and Image"

print_info "Removing frontend container..."
docker-compose -f "$COMPOSE_FILE" rm -f frontend 2>/dev/null || true

print_info "Removing old frontend image (if exists)..."
docker rmi ems-frontend 2>/dev/null || true
docker rmi "$(docker images | grep frontend | awk '{print $3}')" 2>/dev/null || true

print_success "Old frontend container and image removed"

# Rebuild frontend
print_step "Step 8/10: Rebuilding Frontend Container"

print_info "Building frontend image..."
print_info "This may take several minutes..."

# Build with no cache to ensure clean build
if docker-compose -f "$COMPOSE_FILE" build --no-cache frontend; then
    print_success "Frontend image built successfully"
else
    print_error "Failed to build frontend image"
    exit 1
fi

# Start frontend container
print_step "Step 9/10: Starting Frontend Container"

if docker-compose -f "$COMPOSE_FILE" up -d frontend; then
    print_success "Frontend container started"
else
    print_error "Failed to start frontend container"
    exit 1
fi

# Wait for container to be healthy
print_info "Waiting for frontend container to be healthy..."
sleep 5

# Verify frontend is running
print_step "Step 10/10: Verifying Frontend Deployment"

# Check container status
if docker-compose -f "$COMPOSE_FILE" ps frontend | grep -q "Up"; then
    print_success "Frontend container is running"
else
    print_error "Frontend container is not running"
    print_info "Check logs with: docker-compose logs frontend"
    exit 1
fi

# Check if nginx is serving content
print_info "Checking if nginx is serving content..."

# Get container IP or use localhost
FRONTEND_PORT=$(docker-compose -f "$COMPOSE_FILE" port frontend 80 2>/dev/null | cut -d: -f2 || echo "80")

# Check if index.html exists in container
if docker-compose -f "$COMPOSE_FILE" exec -T frontend test -f /usr/share/nginx/html/index.html 2>/dev/null; then
    print_success "index.html found in container"
else
    print_error "index.html NOT found in container - build may have failed!"
    print_info "Checking container logs..."
    docker-compose -f "$COMPOSE_FILE" logs --tail=50 frontend
    exit 1
fi

# Check nginx configuration in container
print_info "Validating nginx configuration in container..."
if docker-compose -f "$COMPOSE_FILE" exec -T frontend nginx -t 2>/dev/null; then
    print_success "Nginx configuration is valid in container"
else
    print_error "Nginx configuration is invalid in container!"
    docker-compose -f "$COMPOSE_FILE" exec -T frontend nginx -t
    exit 1
fi

# Test HTTP response
print_info "Testing HTTP response..."
if curl -s -o /dev/null -w "%{http_code}" "http://localhost:${FRONTEND_PORT}" | grep -q "200"; then
    print_success "Frontend is responding with HTTP 200"
else
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:${FRONTEND_PORT}" || echo "000")
    if [ "$HTTP_CODE" = "000" ]; then
        print_warning "Could not connect to frontend (may still be starting)"
        print_info "Wait a few seconds and check: curl http://localhost:${FRONTEND_PORT}"
    else
        print_error "Frontend returned HTTP $HTTP_CODE (expected 200)"
        print_info "Check logs: docker-compose logs frontend"
    fi
fi

# Check for default nginx page
print_info "Checking if default nginx page is being served..."
RESPONSE=$(curl -s "http://localhost:${FRONTEND_PORT}" | head -20)
if echo "$RESPONSE" | grep -qi "Welcome to nginx"; then
    print_error "Default nginx page is being served - application files not deployed!"
    print_info "This indicates the build failed or files were not copied correctly"
    print_info "Checking container contents..."
    docker-compose -f "$COMPOSE_FILE" exec -T frontend ls -la /usr/share/nginx/html/
    exit 1
elif echo "$RESPONSE" | grep -qi "app-root"; then
    print_success "Angular application is being served (found app-root)"
elif echo "$RESPONSE" | grep -qi "<!DOCTYPE html"; then
    print_success "HTML content is being served"
else
    print_warning "Could not verify application content (may need manual check)"
fi

# Display container logs
print_step "Recent Frontend Container Logs"
docker-compose -f "$COMPOSE_FILE" logs --tail=20 frontend

# Summary
echo ""
echo -e "${CYAN}════════════════════════════════════════════════════════════${NC}"
echo -e "${CYAN}Summary:${NC}"
echo ""
echo -e "Frontend rebuild ${GREEN}completed${NC}"
echo ""
echo -e "Container Status:"
docker-compose -f "$COMPOSE_FILE" ps frontend
echo ""
echo -e "Useful commands:"
echo -e "  ${GREEN}docker-compose logs frontend${NC} - View frontend logs"
echo -e "  ${GREEN}docker-compose exec frontend nginx -t${NC} - Test nginx config"
echo -e "  ${GREEN}docker-compose exec frontend ls -la /usr/share/nginx/html/${NC} - List files"
echo -e "  ${GREEN}curl http://localhost:${FRONTEND_PORT}${NC} - Test frontend"
echo ""

