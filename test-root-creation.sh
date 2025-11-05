#!/bin/bash

# Test Script for ROOT User Creation
# This script tests the initialization endpoint with Basic Auth

echo "======================================"
echo "ROOT User Creation Test Script"
echo "======================================"
echo ""

BASE_URL="http://localhost:8080"
BASIC_AUTH_USER="waseem"
BASIC_AUTH_PASS="wud19@WUD"

echo "Step 1: Check if backend is running..."
if curl -s -f "$BASE_URL/api/init/root-exists" > /dev/null 2>&1; then
    echo "✅ Backend is running!"
else
    echo "❌ Backend is not running or not accessible"
    echo "Please start the backend: mvn spring-boot:run"
    exit 1
fi

echo ""
echo "Step 2: Check if ROOT user exists..."
RESPONSE=$(curl -s "$BASE_URL/api/init/root-exists")
echo "Response: $RESPONSE"

if echo "$RESPONSE" | grep -q '"exists":true'; then
    echo "⚠️  ROOT user already exists!"
    echo "To reset, run these SQL commands:"
    echo "DELETE FROM user_roles WHERE user_id IN (SELECT id FROM users WHERE username = 'root');"
    echo "DELETE FROM users WHERE username = 'root';"
    exit 0
fi

echo "✅ ROOT user does not exist. Proceeding with creation..."
echo ""

echo "Step 3: Create ROOT user with Basic Auth..."
CREATE_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
  -X POST "$BASE_URL/api/init/create-root" \
  -u "$BASIC_AUTH_USER:$BASIC_AUTH_PASS" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "root",
    "email": "root@system.local",
    "password": "Root@123456"
  }')

HTTP_CODE=$(echo "$CREATE_RESPONSE" | grep "HTTP_CODE:" | cut -d':' -f2)
RESPONSE_BODY=$(echo "$CREATE_RESPONSE" | sed '/HTTP_CODE:/d')

echo "HTTP Status Code: $HTTP_CODE"
echo "Response Body: $RESPONSE_BODY"

if [ "$HTTP_CODE" == "200" ]; then
    echo "✅ ROOT user created successfully!"
    echo ""
    echo "Step 4: Testing ROOT user login..."

    LOGIN_RESPONSE=$(curl -s \
      -X POST "$BASE_URL/api/auth/login" \
      -H "Content-Type: application/json" \
      -d '{
        "username": "root",
        "password": "Root@123456"
      }')

    echo "Login Response: $LOGIN_RESPONSE"

    if echo "$LOGIN_RESPONSE" | grep -q '"token"'; then
        echo "✅ ROOT user login successful!"
        echo ""
        echo "======================================"
        echo "✅ ALL TESTS PASSED!"
        echo "======================================"
        echo ""
        echo "ROOT User Credentials:"
        echo "  Username: root"
        echo "  Password: Root@123456"
        echo ""
        echo "Next Steps:"
        echo "1. Use the token from login to make authenticated requests"
        echo "2. Create your first organization"
        echo "3. Start managing tenants!"
    else
        echo "❌ ROOT user login failed!"
        exit 1
    fi
else
    echo "❌ Failed to create ROOT user!"
    echo ""
    if echo "$RESPONSE_BODY" | grep -q "Unauthorized"; then
        echo "Error: Unauthorized - Check if Basic Auth credentials are correct"
        echo "Expected: waseem:wud19@WUD"
    elif echo "$RESPONSE_BODY" | grep -q "already exists"; then
        echo "Error: ROOT user already exists"
    else
        echo "Unknown error. Check the response above."
    fi
    exit 1
fi

