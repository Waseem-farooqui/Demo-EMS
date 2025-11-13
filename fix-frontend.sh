#!/bin/bash
# fix-frontend.sh - Fix "Welcome to Nginx" Issue

set -e

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}🔧 Fixing Frontend 'Welcome to Nginx' Issue${NC}"
echo "=========================================="
echo ""

# Check if in correct directory
if [ ! -f "docker-compose.yml" ]; then
    echo -e "${RED}❌ Error: docker-compose.yml not found${NC}"
    echo "Please run this script from the application directory"
    exit 1
fi

# Stop frontend
echo "1/5: Stopping frontend container..."
docker-compose stop frontend
echo -e "${GREEN}✅ Frontend stopped${NC}"

# Remove frontend container
echo ""
echo "2/5: Removing frontend container..."
docker rm -f ems-frontend 2>/dev/null || true
echo -e "${GREEN}✅ Container removed${NC}"

# Remove frontend image
echo ""
echo "3/5: Removing frontend image..."
docker rmi -f $(docker images | grep ems-frontend | awk '{print $3}') 2>/dev/null || true
echo -e "${GREEN}✅ Image removed${NC}"

# Rebuild frontend
echo ""
echo "4/5: Rebuilding frontend (this may take 2-3 minutes)..."
docker-compose build --no-cache frontend
echo -e "${GREEN}✅ Frontend rebuilt${NC}"

# Start frontend
echo ""
echo "5/5: Starting frontend..."
docker-compose up -d frontend

# Wait for frontend to be ready
echo ""
echo "Waiting for frontend to start..."
sleep 10

# Verify
echo ""
echo "Verifying fix..."
if curl -f http://localhost/ > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Frontend is responding${NC}"

    # Check if it's Angular app or nginx default
    if curl -s http://localhost/ | grep -q "Employee Management System\|app-root"; then
        echo -e "${GREEN}✅ SUCCESS! Angular application is now running${NC}"
        echo ""
        echo "You can now access the application at:"
        echo "  http://localhost/"
        echo "  or"
        echo "  http://$(hostname -I | awk '{print $1}')/"
    else
        echo -e "${YELLOW}⚠️  Frontend is running but might still show nginx default${NC}"
        echo "Checking container logs..."
        docker-compose logs frontend --tail=20
    fi
else
    echo -e "${RED}❌ Frontend is not responding${NC}"
    echo "Checking logs..."
    docker-compose logs frontend --tail=30
fi

echo ""
echo "=========================================="
echo "Fix script completed"
echo ""
echo "Useful commands:"
echo "  View logs: docker-compose logs frontend -f"
echo "  Restart: docker-compose restart frontend"
echo "  Check files: docker exec ems-frontend ls /usr/share/nginx/html/"

