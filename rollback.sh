#!/bin/bash
# rollback.sh - Rollback Script for Employee Management System

set -e

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "⏪ Employee Management System - Rollback"
echo "=========================================="

# Check if backup exists
LATEST_BACKUP=$(ls -t backup_db_*.sql 2>/dev/null | head -1)

if [ -z "$LATEST_BACKUP" ]; then
    echo -e "${RED}❌ No backup found!${NC}"
    echo "Cannot perform rollback without a backup."
    exit 1
fi

echo "Found backup: $LATEST_BACKUP"
echo ""
echo -e "${YELLOW}⚠️  WARNING: This will rollback to previous version${NC}"
echo "This action will:"
echo "  1. Stop current containers"
echo "  2. Restore database from backup"
echo "  3. Restore previous code version (git)"
echo "  4. Rebuild and restart services"
echo ""
read -p "Are you sure you want to continue? (yes/no): " -r
echo ""

if [[ ! $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
    echo "Rollback cancelled."
    exit 0
fi

# 1. Stop current services
echo "🛑 Step 1/5: Stopping current services..."
docker-compose down
echo -e "${GREEN}✅ Services stopped${NC}"

# 2. Git rollback
echo ""
echo "📦 Step 2/5: Rolling back code to previous version..."
git log --oneline -5
echo ""
read -p "Enter the commit hash to rollback to (or press Enter for previous commit): " COMMIT_HASH

if [ -z "$COMMIT_HASH" ]; then
    COMMIT_HASH="HEAD~1"
fi

git reset --hard $COMMIT_HASH
echo -e "${GREEN}✅ Code rolled back to $COMMIT_HASH${NC}"

# 3. Restore database
echo ""
echo "💾 Step 3/5: Restoring database from backup..."

# Start only MySQL
docker-compose up -d mysql
echo "Waiting for MySQL to start..."
sleep 20

# Restore database
if [[ $LATEST_BACKUP == *.gz ]]; then
    gunzip < "$LATEST_BACKUP" | docker-compose exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}"
else
    docker-compose exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" < "$LATEST_BACKUP"
fi

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Database restored from $LATEST_BACKUP${NC}"
else
    echo -e "${RED}❌ Database restore failed!${NC}"
    exit 1
fi

# 4. Rebuild services
echo ""
echo "🔨 Step 4/5: Rebuilding services..."
docker-compose build --no-cache
echo -e "${GREEN}✅ Services rebuilt${NC}"

# 5. Start all services
echo ""
echo "🚀 Step 5/5: Starting all services..."
docker-compose up -d
echo -e "${GREEN}✅ Services started${NC}"

# Wait and check health
echo ""
echo "⏳ Waiting for services to be healthy..."
sleep 30

# Health checks
echo "Performing health checks..."
BACKEND_HEALTHY=false
FRONTEND_HEALTHY=false

# Check backend
if curl -f http://localhost:8080/api/actuator/health > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Backend is healthy${NC}"
    BACKEND_HEALTHY=true
else
    echo -e "${RED}❌ Backend health check failed${NC}"
    echo "Backend logs:"
    docker-compose logs --tail=50 backend
fi

# Check frontend
if curl -f http://localhost:80/ > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Frontend is healthy${NC}"
    FRONTEND_HEALTHY=true
else
    echo -e "${RED}❌ Frontend health check failed${NC}"
    echo "Frontend logs:"
    docker-compose logs --tail=50 frontend
fi

echo ""
echo "=========================================="

if [ "$BACKEND_HEALTHY" = true ] && [ "$FRONTEND_HEALTHY" = true ]; then
    echo -e "${GREEN}✅ Rollback completed successfully!${NC}"
    echo "Application is running on previous version."
    echo ""
    echo "Current version:"
    git log --oneline -1
else
    echo -e "${YELLOW}⚠️  Rollback completed with issues${NC}"
    echo "Some services are not healthy. Please check logs."
fi

echo ""
echo "Access the application:"
echo "  Frontend: http://localhost/"
echo "  Backend: http://localhost:8080/api/actuator/health"
echo ""
echo "Monitor logs:"
echo "  docker-compose logs -f"

