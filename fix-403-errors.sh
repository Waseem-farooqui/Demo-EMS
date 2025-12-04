#!/bin/bash

# ================================================================
# Fix 403 Errors - Rebuild with Correct CORS and API Configuration
# ================================================================

set -e

echo "🔧 Fixing 403 API errors..."
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Load environment variables
if [ -f .env ]; then
    echo -e "${BLUE}📋 Loading environment variables from .env...${NC}"
    export $(grep -v '^#' .env | xargs)
else
    echo -e "${RED}❌ .env file not found!${NC}"
    exit 1
fi

# Verify CORS configuration
echo -e "${BLUE}🔍 Checking CORS configuration...${NC}"
if [ -z "$CORS_ALLOWED_ORIGINS" ]; then
    echo -e "${RED}❌ CORS_ALLOWED_ORIGINS not set in .env!${NC}"
    echo "Please add: CORS_ALLOWED_ORIGINS=http://62.169.20.104,http://62.169.20.104:80"
    exit 1
fi

echo -e "${GREEN}✅ CORS Origins: $CORS_ALLOWED_ORIGINS${NC}"

# Verify API URLs
echo -e "${BLUE}🔍 Checking API configuration...${NC}"
echo "API_URL: ${API_URL:-Not Set}"
echo "API_BASE_URL: ${API_BASE_URL:-Not Set}"
echo "FRONTEND_URL: ${FRONTEND_URL:-Not Set}"
echo ""

# Step 1: Stop all containers
echo -e "${YELLOW}⏹️  Step 1: Stopping all containers...${NC}"
docker compose down
echo ""

# Step 2: Remove old images (optional, but ensures clean build)
echo -e "${YELLOW}🗑️  Step 2: Removing old images...${NC}"
docker rmi ems-backend ems-frontend 2>/dev/null || echo "No old images to remove"
echo ""

# Step 3: Rebuild backend (picks up CORS config from .env)
echo -e "${YELLOW}🔨 Step 3: Rebuilding backend with CORS configuration...${NC}"
docker compose build --no-cache backend
echo ""

# Step 4: Rebuild frontend with API URLs
echo -e "${YELLOW}🔨 Step 4: Rebuilding frontend with API URLs...${NC}"
docker compose build --no-cache \
    --build-arg API_URL="${API_URL:-http://62.169.20.104:8080/api}" \
    --build-arg API_BASE_URL="${API_BASE_URL:-http://62.169.20.104:8080}" \
    --build-arg FRONTEND_URL="${FRONTEND_URL:-http://62.169.20.104}" \
    frontend
echo ""

# Step 5: Start all services
echo -e "${YELLOW}🚀 Step 5: Starting all services...${NC}"
docker compose up -d
echo ""

# Step 6: Wait for services to be healthy
echo -e "${YELLOW}⏳ Step 6: Waiting for services to be healthy (60 seconds)...${NC}"
sleep 60
echo ""

# Step 7: Check service status
echo -e "${BLUE}📊 Service Status:${NC}"
docker compose ps
echo ""

# Step 8: Test backend health
echo -e "${BLUE}🏥 Testing Backend Health:${NC}"
BACKEND_HEALTH=$(curl -s http://localhost:8080/api/actuator/health || echo "Failed")
if [[ "$BACKEND_HEALTH" == *"UP"* ]]; then
    echo -e "${GREEN}✅ Backend is healthy${NC}"
else
    echo -e "${RED}❌ Backend health check failed${NC}"
    echo "$BACKEND_HEALTH"
fi
echo ""

# Step 9: Test CORS headers
echo -e "${BLUE}🌐 Testing CORS Headers:${NC}"
CORS_TEST=$(curl -s -I -H "Origin: http://62.169.20.104" http://localhost:8080/api/actuator/health | grep -i "access-control-allow-origin" || echo "No CORS headers found")
if [[ "$CORS_TEST" == *"62.169.20.104"* ]]; then
    echo -e "${GREEN}✅ CORS headers are correctly configured${NC}"
    echo "$CORS_TEST"
else
    echo -e "${RED}⚠️  CORS headers might not be configured correctly${NC}"
    echo "$CORS_TEST"
fi
echo ""

# Step 10: Check backend logs for CORS configuration
echo -e "${BLUE}📋 Backend CORS Configuration in Logs:${NC}"
docker compose logs backend 2>&1 | grep -i "cors" | tail -5 || echo "No CORS logs found yet"
echo ""

# Step 11: Test frontend
echo -e "${BLUE}🌐 Testing Frontend:${NC}"
FRONTEND_TEST=$(curl -s -o /dev/null -w "%{http_code}" http://localhost/)
if [ "$FRONTEND_TEST" == "200" ]; then
    echo -e "${GREEN}✅ Frontend is accessible (HTTP $FRONTEND_TEST)${NC}"
else
    echo -e "${RED}❌ Frontend returned HTTP $FRONTEND_TEST${NC}"
fi
echo ""

# Step 12: Verify frontend environment variables were baked in
echo -e "${BLUE}🔍 Checking if frontend has correct API URLs...${NC}"
docker exec ems-frontend grep -r "62.169.20.104:8080" /usr/share/nginx/html/ | head -1 && \
    echo -e "${GREEN}✅ Frontend has correct API URL embedded${NC}" || \
    echo -e "${RED}⚠️  Frontend might not have correct API URL${NC}"
echo ""

# Final summary
echo -e "${GREEN}===========================================${NC}"
echo -e "${GREEN}✅ Rebuild Complete!${NC}"
echo -e "${GREEN}===========================================${NC}"
echo ""
echo -e "${BLUE}📝 Configuration Summary:${NC}"
echo "  • CORS Origins: $CORS_ALLOWED_ORIGINS"
echo "  • Backend URL: $API_BASE_URL"
echo "  • API URL: $API_URL"
echo "  • Frontend URL: $FRONTEND_URL"
echo ""
echo -e "${BLUE}🌐 Access URLs:${NC}"
echo "  • Frontend: http://62.169.20.104/"
echo "  • Backend: http://62.169.20.104:8080/api"
echo "  • Health Check: http://62.169.20.104:8080/api/actuator/health"
echo ""
echo -e "${YELLOW}📝 Next Steps:${NC}"
echo "  1. Open browser and go to: http://62.169.20.104/"
echo "  2. Open browser console (F12) and check for errors"
echo "  3. Try to login and verify API calls work"
echo ""
echo -e "${YELLOW}🔍 If still getting 403 errors:${NC}"
echo "  • Check browser console for exact error message"
echo "  • Verify JWT token is being sent: Check 'Authorization' header"
echo "  • Check backend logs: docker compose logs backend -f"
echo "  • Verify login works: POST to http://62.169.20.104:8080/api/auth/login"
echo ""
echo -e "${GREEN}Done!${NC}"

