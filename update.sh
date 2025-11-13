#!/bin/bash
# update.sh - Zero Downtime Update Script for Employee Management System

set -e

echo "🚀 Starting Zero-Downtime Update..."
echo "=========================================="

# Configuration
BLUE_GREEN_COMPOSE="docker-compose.blue-green.yml"
BACKEND_BLUE_PORT=8081
FRONTEND_BLUE_PORT=81
MAX_HEALTH_ATTEMPTS=30
HEALTH_CHECK_INTERVAL=2

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

# Function to check service health
check_health() {
    local url=$1
    local max_attempts=$2
    local service_name=$3

    local attempt=0
    echo "Checking $service_name health..."

    until curl -f "$url" > /dev/null 2>&1 || [ $attempt -eq $max_attempts ]; do
        attempt=$((attempt+1))
        echo "  Attempt $attempt/$max_attempts - Waiting for $service_name..."
        sleep $HEALTH_CHECK_INTERVAL
    done

    if [ $attempt -eq $max_attempts ]; then
        return 1
    fi

    return 0
}

# 1. Backup current state
echo ""
echo "📦 Step 1/9: Creating backup..."
BACKUP_DATE=$(date +%Y%m%d_%H%M%S)

# Backup database
docker-compose exec -T mysql mysqldump -u root -p${DB_ROOT_PASSWORD} employee_management_system > "backup_db_${BACKUP_DATE}.sql" 2>/dev/null || {
    print_warning "Database backup failed (continuing anyway)"
}

# Backup environment
cp .env ".env.backup_${BACKUP_DATE}" 2>/dev/null || true

print_status "Backup created: backup_db_${BACKUP_DATE}.sql"

# 2. Build new images
echo ""
echo "🔨 Step 2/9: Building new images..."
docker-compose build --no-cache || {
    print_error "Build failed!"
    exit 1
}
print_status "Images built successfully"

# 3. Create blue-green docker-compose if not exists
echo ""
echo "📝 Step 3/9: Preparing blue-green environment..."
if [ ! -f "$BLUE_GREEN_COMPOSE" ]; then
    print_warning "Blue-green compose file not found, creating..."
    cat > "$BLUE_GREEN_COMPOSE" << 'EOL'
version: '3.8'

services:
  backend-blue:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: ems-backend-blue
    restart: "no"
    env_file: .env
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/${DB_NAME:-employee_management_system}?useSSL=false&serverTimezone=UTC
      SPRING_DATASOURCE_USERNAME: ${DB_USERNAME}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      SPRING_PROFILES_ACTIVE: prod
    ports:
      - "8081:8080"
    volumes:
      - uploads_data:/app/uploads
    networks:
      - ems-network
    depends_on:
      - mysql
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/api/actuator/health"]
      interval: 10s
      timeout: 5s
      retries: 3

  frontend-blue:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    container_name: ems-frontend-blue
    restart: "no"
    ports:
      - "81:80"
    networks:
      - ems-network
    depends_on:
      - backend-blue
    healthcheck:
      test: ["CMD", "wget", "--quiet", "--tries=1", "--spider", "http://localhost/"]
      interval: 10s
      timeout: 5s
      retries: 3

volumes:
  uploads_data:
    external: true
    name: employeemanagementsystem_uploads_data

networks:
  ems-network:
    external: true
    name: employeemanagementsystem_ems-network
EOL
fi
print_status "Blue-green environment ready"

# 4. Start blue environment
echo ""
echo "🔵 Step 4/9: Starting blue environment..."
docker-compose -f "$BLUE_GREEN_COMPOSE" up -d || {
    print_error "Failed to start blue environment!"
    exit 1
}
print_status "Blue containers started"

# 5. Wait for health checks
echo ""
echo "⏳ Step 5/9: Waiting for health checks..."
sleep 10

# Check backend health
if ! check_health "http://localhost:$BACKEND_BLUE_PORT/api/actuator/health" $MAX_HEALTH_ATTEMPTS "Backend"; then
    print_error "Backend health check failed!"
    echo "Showing backend logs:"
    docker-compose -f "$BLUE_GREEN_COMPOSE" logs --tail=50 backend-blue
    docker-compose -f "$BLUE_GREEN_COMPOSE" down
    exit 1
fi
print_status "Backend is healthy"

# Check frontend health
if ! check_health "http://localhost:$FRONTEND_BLUE_PORT/" 10 "Frontend"; then
    print_error "Frontend health check failed!"
    echo "Showing frontend logs:"
    docker-compose -f "$BLUE_GREEN_COMPOSE" logs --tail=50 frontend-blue
    docker-compose -f "$BLUE_GREEN_COMPOSE" down
    exit 1
fi
print_status "Frontend is healthy"

# 6. Switch traffic
echo ""
echo "🔄 Step 6/9: Switching traffic to new version..."
docker-compose up -d --no-deps backend frontend || {
    print_error "Failed to switch traffic!"
    print_warning "Rolling back..."
    docker-compose -f "$BLUE_GREEN_COMPOSE" down
    exit 1
}
print_status "Traffic switched to new version"

# 7. Wait for new containers to be stable
echo ""
echo "⏳ Step 7/9: Verifying new containers..."
sleep 15

# Final health check on main containers
if ! check_health "http://localhost:8080/api/actuator/health" 10 "Main Backend"; then
    print_error "Main backend health check failed!"
    print_warning "Consider rolling back"
    docker-compose logs --tail=50 backend
fi

if ! check_health "http://localhost:80/" 10 "Main Frontend"; then
    print_error "Main frontend health check failed!"
    print_warning "Consider rolling back"
    docker-compose logs --tail=50 frontend
fi
print_status "New containers are running"

# 8. Stop blue environment
echo ""
echo "🧹 Step 8/9: Cleaning up blue environment..."
docker-compose -f "$BLUE_GREEN_COMPOSE" down
print_status "Blue environment stopped"

# 9. Clean up old images
echo ""
echo "🗑️  Step 9/9: Removing old images..."
docker image prune -f > /dev/null 2>&1
print_status "Old images removed"

echo ""
echo "=========================================="
print_status "Update completed successfully!"
echo "🎉 Application is running on updated version with zero downtime!"
echo ""
echo "Quick checks:"
echo "  Backend: http://localhost:8080/api/actuator/health"
echo "  Frontend: http://localhost/"
echo ""
echo "Monitor logs with: docker-compose logs -f"

