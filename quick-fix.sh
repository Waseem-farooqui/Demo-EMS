#!/bin/bash
# quick-fix.sh - Quick fix for container conflicts and 403 forbidden

set -e

echo "🔧 Fixing container conflicts and 403 forbidden error..."
echo ""

# Stop all containers
echo "1/6: Stopping all containers..."
docker compose down 2>/dev/null || true
docker stop $(docker ps -aq) 2>/dev/null || true

# Remove all EMS containers
echo "2/6: Removing all EMS containers..."
docker rm -f ems-backend ems-frontend ems-mysql 2>/dev/null || true
docker rm -f ems-backend-blue ems-frontend-blue 2>/dev/null || true
docker ps -a | grep -E "ems-" | awk '{print $1}' | xargs -r docker rm -f 2>/dev/null || true

# Remove frontend image to force rebuild
echo "3/6: Removing frontend image..."
docker rmi -f $(docker images | grep ems-frontend | awk '{print $3}') 2>/dev/null || true
docker rmi -f $(docker images | grep demo-ems-frontend | awk '{print $3}') 2>/dev/null || true

echo ""
echo "✅ Cleanup complete!"
echo ""

# Rebuild frontend with no cache
echo "4/6: Rebuilding frontend (this may take 2-3 minutes)..."
docker compose build --no-cache frontend

echo ""
echo "✅ Frontend rebuilt!"
echo ""

# Start all services
echo "5/6: Starting all services..."
docker compose up -d

echo ""
echo "✅ Services started!"
echo ""

# Wait for services to initialize
echo "6/6: Waiting for services to initialize (30 seconds)..."
sleep 30

echo ""
echo "Checking service status..."
docker compose ps

echo ""
echo "Testing frontend..."
if curl -s http://localhost/ | grep -q "403 Forbidden"; then
    echo "⚠️  Still getting 403 Forbidden. Checking logs..."
    docker compose logs frontend --tail=30
elif curl -s http://localhost/ | grep -q "Welcome to nginx"; then
    echo "⚠️  Still showing default nginx page. Checking container contents..."
    docker exec ems-frontend ls -la /usr/share/nginx/html/
else
    echo "✅ Frontend is responding correctly!"
    echo ""
    echo "Access your application at:"
    echo "  http://localhost/"
fi

echo ""
echo "=========================================="
echo "Fix completed!"
echo ""
echo "If still having issues, check:"
echo "  docker compose logs frontend"
echo "  docker exec ems-frontend ls -la /usr/share/nginx/html/"


