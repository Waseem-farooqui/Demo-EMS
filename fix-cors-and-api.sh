#!/bin/bash
# fix-cors-and-api.sh - Fix CORS and API configuration for IP 62.169.20.104

set -e

SERVER_IP="62.169.20.104"

echo "🔧 Fixing CORS and API Configuration"
echo "Server IP: $SERVER_IP"
echo "======================================"
echo ""

# 1. Update .env file CORS settings
echo "1/5: Updating CORS configuration in .env..."
if [ -f .env ]; then
    # Check if CORS_ALLOWED_ORIGINS exists
    if grep -q "CORS_ALLOWED_ORIGINS" .env; then
        # Update existing line
        sed -i "s|CORS_ALLOWED_ORIGINS=.*|CORS_ALLOWED_ORIGINS=http://${SERVER_IP},http://${SERVER_IP}:80|g" .env
        echo "✅ Updated CORS_ALLOWED_ORIGINS"
    else
        # Add new line
        echo "" >> .env
        echo "# CORS Configuration" >> .env
        echo "CORS_ALLOWED_ORIGINS=http://${SERVER_IP},http://${SERVER_IP}:80" >> .env
        echo "✅ Added CORS_ALLOWED_ORIGINS"
    fi

    # Update APP_URL
    if grep -q "APP_URL" .env; then
        sed -i "s|APP_URL=.*|APP_URL=http://${SERVER_IP}|g" .env
        echo "✅ Updated APP_URL"
    else
        echo "APP_URL=http://${SERVER_IP}" >> .env
        echo "✅ Added APP_URL"
    fi
else
    echo "❌ .env file not found!"
    exit 1
fi

echo ""
echo "2/5: Updated .env file. Current CORS config:"
grep "CORS_ALLOWED_ORIGINS\|APP_URL" .env

# 2. Verify frontend environment file is updated
echo ""
echo "3/5: Verifying frontend production environment..."
if grep -q "62.169.20.104" frontend/src/environments/environment.prod.ts; then
    echo "✅ Frontend environment already updated"
else
    echo "⚠️  Frontend environment needs update"
fi

# 3. Rebuild frontend with new configuration
echo ""
echo "4/5: Rebuilding frontend with new API configuration..."
docker compose stop frontend
docker rm -f ems-frontend 2>/dev/null || true
docker rmi $(docker images | grep ems-frontend | awk '{print $3}') 2>/dev/null || true
docker compose build --no-cache frontend

# 4. Restart backend to apply CORS changes
echo ""
echo "5/5: Restarting backend to apply CORS settings..."
docker compose restart backend

# Wait for backend to start
echo "Waiting for backend to start (15 seconds)..."
sleep 15

# Start frontend
echo "Starting frontend..."
docker compose up -d frontend

# Wait for frontend
sleep 10

echo ""
echo "======================================"
echo "✅ Configuration updated!"
echo ""
echo "Frontend will now connect to: http://${SERVER_IP}:8080/api"
echo "Backend CORS allows: http://${SERVER_IP}, http://${SERVER_IP}:80"
echo ""
echo "Testing connectivity..."
curl -s http://localhost:8080/api/actuator/health && echo "✅ Backend is up" || echo "❌ Backend not responding"
curl -s http://localhost/ > /dev/null && echo "✅ Frontend is up" || echo "❌ Frontend not responding"

echo ""
echo "Access your application at: http://${SERVER_IP}"

