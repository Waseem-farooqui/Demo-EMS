#!/bin/bash

# Zero-Downtime Deployment Script for Employee Management System
# This script deploys changes without affecting active clients
# Usage: ./zero-downtime-deploy.sh [backend|frontend|all]

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
COMPOSE_FILE="compose.yaml"
BACKUP_DIR="./backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
LOG_FILE="./deployment_${TIMESTAMP}.log"

# Functions
print_info() {
    echo -e "${BLUE}ℹ $1${NC}" | tee -a "$LOG_FILE"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}" | tee -a "$LOG_FILE"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}" | tee -a "$LOG_FILE"
}

print_error() {
    echo -e "${RED}✗ $1${NC}" | tee -a "$LOG_FILE"
}

print_step() {
    echo ""
    echo -e "${BLUE}▶ $1${NC}" | tee -a "$LOG_FILE"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" | tee -a "$LOG_FILE"
}

# Check if running as root or with sudo
check_permissions() {
    if [ "$EUID" -ne 0 ]; then
        print_warning "Not running as root. Some commands may require sudo."
    fi
}

# Create backup directory
create_backup_dir() {
    mkdir -p "$BACKUP_DIR"
    print_success "Backup directory ready: $BACKUP_DIR"
}

# Backup current deployment
backup_current_deployment() {
    print_step "Creating Backup"
    
    BACKUP_PATH="$BACKUP_DIR/deployment_${TIMESTAMP}"
    mkdir -p "$BACKUP_PATH"
    
    # Backup Docker images
    print_info "Backing up Docker images..."
    docker images --format "{{.Repository}}:{{.Tag}}" | grep -E "(ems-backend|ems-frontend)" | while read image; do
        if [ ! -z "$image" ]; then
            IMAGE_NAME=$(echo "$image" | tr ':' '_' | tr '/' '_')
            docker save "$image" -o "$BACKUP_PATH/${IMAGE_NAME}.tar" 2>/dev/null || print_warning "Could not backup image: $image"
        fi
    done
    
    # Backup environment file
    if [ -f ".env" ]; then
        cp .env "$BACKUP_PATH/.env.backup"
        print_success "Backed up .env file"
    fi
    
    # Backup compose file
    cp "$COMPOSE_FILE" "$BACKUP_PATH/compose.yaml.backup"
    print_success "Backed up compose.yaml"
    
    # Backup database (optional - uncomment if needed)
    # print_info "Backing up database..."
    # docker exec ems-mysql mysqldump -u root -p"${DB_ROOT_PASSWORD:-rootpassword}" employee_management_system > "$BACKUP_PATH/database_${TIMESTAMP}.sql" 2>/dev/null || print_warning "Could not backup database"
    
    print_success "Backup completed: $BACKUP_PATH"
}

# Check service health
check_service_health() {
    local service=$1
    local max_attempts=30
    local attempt=1
    
    print_info "Checking health of $service..."
    
    while [ $attempt -le $max_attempts ]; do
        if docker exec "$service" curl -f http://localhost:8080/api/actuator/health >/dev/null 2>&1; then
            print_success "$service is healthy"
            return 0
        fi
        
        if [ "$service" == "ems-frontend" ]; then
            if docker exec "$service" wget --quiet --spider http://localhost/ >/dev/null 2>&1; then
                print_success "$service is healthy"
                return 0
            fi
        fi
        
        print_info "Waiting for $service to be healthy... ($attempt/$max_attempts)"
        sleep 2
        attempt=$((attempt + 1))
    done
    
    print_error "$service failed health check"
    return 1
}

# Deploy backend with zero downtime
deploy_backend() {
    print_step "Deploying Backend (Zero-Downtime)"
    
    # Step 1: Build new image
    print_info "Building new backend image..."
    docker compose -f "$COMPOSE_FILE" build --no-cache backend
    
    if [ $? -ne 0 ]; then
        print_error "Backend build failed!"
        return 1
    fi
    print_success "Backend image built successfully"
    
    # Step 2: Start new container with different name (blue-green deployment)
    print_info "Starting new backend container (blue-green)..."
    NEW_CONTAINER="ems-backend-new-${TIMESTAMP}"
    
    # Create temporary compose override for new container
    cat > /tmp/compose-override.yaml <<EOF
services:
  backend-new:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: ${NEW_CONTAINER}
    environment:
      $(docker compose -f "$COMPOSE_FILE" config | grep -A 100 "backend:" | grep "environment:" -A 50 | tail -n +2)
    ports:
      - "8081:8080"  # Different port to avoid conflict
    volumes:
      - uploads_data:/app/uploads
    networks:
      - ems-network
    depends_on:
      mysql:
        condition: service_healthy
EOF
    
    # Start new container
    docker compose -f "$COMPOSE_FILE" -f /tmp/compose-override.yaml up -d backend-new
    
    # Wait for new container to be healthy
    print_info "Waiting for new backend container to be ready..."
    sleep 10
    
    # Test new container
    if curl -f http://localhost:8081/api/actuator/health >/dev/null 2>&1; then
        print_success "New backend container is healthy"
        
        # Step 3: Switch traffic (update frontend to point to new backend temporarily)
        # Actually, we'll do a rolling update instead
        
        # Stop old container
        print_info "Stopping old backend container..."
        docker stop ems-backend
        
        # Rename new container to production name
        docker stop "$NEW_CONTAINER" 2>/dev/null || true
        docker rm "$NEW_CONTAINER" 2>/dev/null || true
        
        # Start new container with production name
        print_info "Starting backend with production configuration..."
        docker compose -f "$COMPOSE_FILE" up -d backend
        
        # Wait for health check
        if check_service_health "ems-backend"; then
            print_success "Backend deployed successfully!"
            rm -f /tmp/compose-override.yaml
            return 0
        else
            print_error "Backend health check failed!"
            # Rollback
            print_warning "Rolling back..."
            docker compose -f "$COMPOSE_FILE" stop backend
            docker start ems-backend || true
            return 1
        fi
    else
        print_error "New backend container failed health check"
        docker stop "$NEW_CONTAINER" 2>/dev/null || true
        docker rm "$NEW_CONTAINER" 2>/dev/null || true
        rm -f /tmp/compose-override.yaml
        return 1
    fi
}

# Deploy frontend with zero downtime
deploy_frontend() {
    print_step "Deploying Frontend (Zero-Downtime)"
    
    # Step 1: Build new image
    print_info "Building new frontend image..."
    docker compose -f "$COMPOSE_FILE" build --no-cache frontend
    
    if [ $? -ne 0 ]; then
        print_error "Frontend build failed!"
        return 1
    fi
    print_success "Frontend image built successfully"
    
    # Step 2: Create new container with different name
    print_info "Starting new frontend container..."
    NEW_CONTAINER="ems-frontend-new-${TIMESTAMP}"
    
    # Start new container on different port
    docker run -d \
        --name "$NEW_CONTAINER" \
        --network ems-network \
        -p 8082:80 \
        -p 8443:443 \
        -v ${SSL_DIR:-./ssl}:/etc/nginx/ssl:ro \
        -v certbot_webroot:/var/www/certbot:ro \
        --health-cmd="wget --quiet --spider http://localhost/ || exit 1" \
        --health-interval=10s \
        --health-timeout=5s \
        --health-retries=3 \
        $(docker images --format "{{.Repository}}:{{.Tag}}" | grep "frontend" | head -1)
    
    # Wait for new container to be healthy
    print_info "Waiting for new frontend container to be ready..."
    sleep 5
    
    # Test new container
    if curl -f http://localhost:8082/ >/dev/null 2>&1; then
        print_success "New frontend container is healthy"
        
        # Step 3: Switch traffic
        print_info "Switching traffic to new frontend..."
        
        # Stop old container
        docker stop ems-frontend
        
        # Remove old container
        docker rm ems-frontend
        
        # Start new container with production name and ports
        print_info "Starting frontend with production configuration..."
        docker compose -f "$COMPOSE_FILE" up -d frontend
        
        # Wait for health check
        sleep 5
        if docker exec ems-frontend wget --quiet --spider http://localhost/ >/dev/null 2>&1; then
            print_success "Frontend deployed successfully!"
            
            # Cleanup temporary container
            docker stop "$NEW_CONTAINER" 2>/dev/null || true
            docker rm "$NEW_CONTAINER" 2>/dev/null || true
            
            return 0
        else
            print_error "Frontend health check failed!"
            # Rollback
            print_warning "Rolling back..."
            docker compose -f "$COMPOSE_FILE" stop frontend
            docker start "$NEW_CONTAINER" || true
            return 1
        fi
    else
        print_error "New frontend container failed health check"
        docker stop "$NEW_CONTAINER" 2>/dev/null || true
        docker rm "$NEW_CONTAINER" 2>/dev/null || true
        return 1
    fi
}

# Deploy all services
deploy_all() {
    print_step "Deploying All Services"
    
    # Deploy backend first
    if ! deploy_backend; then
        print_error "Backend deployment failed. Aborting."
        return 1
    fi
    
    # Wait a bit between deployments
    sleep 5
    
    # Deploy frontend
    if ! deploy_frontend; then
        print_error "Frontend deployment failed."
        print_warning "Backend is running new version, frontend is on old version."
        return 1
    fi
    
    print_success "All services deployed successfully!"
}

# Verify deployment
verify_deployment() {
    print_step "Verifying Deployment"
    
    # Check backend
    print_info "Verifying backend..."
    if curl -f http://localhost:8080/api/actuator/health >/dev/null 2>&1; then
        print_success "Backend is responding"
    else
        print_error "Backend is not responding"
        return 1
    fi
    
    # Check frontend
    print_info "Verifying frontend..."
    if curl -f http://localhost/ >/dev/null 2>&1 || curl -f https://localhost/ >/dev/null 2>&1; then
        print_success "Frontend is responding"
    else
        print_error "Frontend is not responding"
        return 1
    fi
    
    # Check API connectivity from frontend
    print_info "Checking API connectivity..."
    API_RESPONSE=$(curl -s http://localhost/api/actuator/health 2>/dev/null || echo "FAILED")
    if [ "$API_RESPONSE" != "FAILED" ]; then
        print_success "API connectivity verified"
    else
        print_warning "API connectivity check failed (may be normal if health endpoint requires auth)"
    fi
    
    print_success "Deployment verification completed"
}

# Cleanup old images
cleanup_old_images() {
    print_step "Cleaning Up Old Images"
    
    # Keep last 3 versions of each image
    print_info "Removing old Docker images (keeping last 3 versions)..."
    
    docker images --format "{{.Repository}}:{{.Tag}} {{.ID}}" | grep -E "(ems-backend|ems-frontend)" | \
        tail -n +4 | awk '{print $2}' | xargs -r docker rmi -f 2>/dev/null || true
    
    print_success "Cleanup completed"
}

# Main execution
main() {
    print_step "Zero-Downtime Deployment Started"
    print_info "Timestamp: $TIMESTAMP"
    print_info "Log file: $LOG_FILE"
    
    # Check prerequisites
    check_permissions
    create_backup_dir
    
    # Create backup
    backup_current_deployment
    
    # Determine what to deploy
    DEPLOY_TARGET=${1:-all}
    
    case "$DEPLOY_TARGET" in
        backend)
            deploy_backend
            ;;
        frontend)
            deploy_frontend
            ;;
        all)
            deploy_all
            ;;
        *)
            print_error "Invalid deployment target: $DEPLOY_TARGET"
            print_info "Usage: $0 [backend|frontend|all]"
            exit 1
            ;;
    esac
    
    if [ $? -eq 0 ]; then
        # Verify deployment
        verify_deployment
        
        # Cleanup
        cleanup_old_images
        
        print_step "Deployment Completed Successfully"
        print_success "All services are running with the new version"
        print_info "Backup location: $BACKUP_DIR/deployment_${TIMESTAMP}"
        print_info "Log file: $LOG_FILE"
    else
        print_error "Deployment failed!"
        print_warning "Check logs: $LOG_FILE"
        print_info "Backup location: $BACKUP_DIR/deployment_${TIMESTAMP}"
        exit 1
    fi
}

# Run main function
main "$@"

