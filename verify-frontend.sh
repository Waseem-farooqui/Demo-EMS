#!/bin/bash
# verify-frontend.sh - Verify frontend is working

echo "🔍 Verifying Frontend Deployment"
echo "=================================="
echo ""

# Check if container is running
echo "1. Checking if frontend container is running..."
if docker ps | grep -q ems-frontend; then
    echo "✅ Frontend container is running"
else
    echo "❌ Frontend container is NOT running"
    docker ps -a | grep ems-frontend
    exit 1
fi

echo ""
echo "2. Checking files in nginx html directory..."
docker exec ems-frontend ls -la /usr/share/nginx/html/

echo ""
echo "3. Checking for index.html..."
if docker exec ems-frontend test -f /usr/share/nginx/html/index.html; then
    echo "✅ index.html exists"
    echo "File size:"
    docker exec ems-frontend ls -lh /usr/share/nginx/html/index.html
else
    echo "❌ index.html NOT FOUND!"
    echo "This is the problem - no index.html file"
fi

echo ""
echo "4. Checking nginx error logs..."
docker exec ems-frontend tail -20 /var/log/nginx/error.log 2>/dev/null || echo "No error logs yet"

echo ""
echo "5. Testing HTTP response..."
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost/)
echo "HTTP Status: $RESPONSE"

if [ "$RESPONSE" = "200" ]; then
    echo "✅ SUCCESS! Frontend is working"
elif [ "$RESPONSE" = "403" ]; then
    echo "❌ Still getting 403 Forbidden"
    echo "Checking what nginx sees..."
    docker exec ems-frontend cat /etc/nginx/nginx.conf | grep -A 5 "root"
elif [ "$RESPONSE" = "404" ]; then
    echo "❌ Getting 404 Not Found"
else
    echo "⚠️  Unexpected status code: $RESPONSE"
fi

echo ""
echo "6. Checking build logs from Docker build..."
echo "Last build should show: 'dist/frontend/browser'"
docker compose logs frontend --tail=50 | grep -i "dist\|build\|error" || echo "No relevant logs"

echo ""
echo "=================================="
echo "Verification complete!"

