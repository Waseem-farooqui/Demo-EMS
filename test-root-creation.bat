@echo off
REM Test Script for ROOT User Creation (Windows)
REM This script tests the initialization endpoint with Basic Auth

echo ======================================
echo ROOT User Creation Test Script
echo ======================================
echo.

set BASE_URL=http://localhost:8080
set BASIC_AUTH_USER=waseem
set BASIC_AUTH_PASS=wud19@WUD

echo Step 1: Check if backend is running...
curl -s -f "%BASE_URL%/api/init/root-exists" >nul 2>&1
if %errorlevel% neq 0 (
    echo X Backend is not running or not accessible
    echo Please start the backend: mvn spring-boot:run
    exit /b 1
)
echo + Backend is running!
echo.

echo Step 2: Check if ROOT user exists...
curl -s "%BASE_URL%/api/init/root-exists"
echo.
echo.

echo Step 3: Create ROOT user with Basic Auth...
echo.
curl -X POST "%BASE_URL%/api/init/create-root" ^
  -u %BASIC_AUTH_USER%:%BASIC_AUTH_PASS% ^
  -H "Content-Type: application/json" ^
  -d "{\"username\": \"root\", \"email\": \"root@system.local\", \"password\": \"Root@123456\"}"
echo.
echo.

echo Step 4: Testing ROOT user login...
echo.
curl -X POST "%BASE_URL%/api/auth/login" ^
  -H "Content-Type: application/json" ^
  -d "{\"username\": \"root\", \"password\": \"Root@123456\"}"
echo.
echo.

echo ======================================
echo Test completed!
echo ======================================
echo.
echo If successful, you should see:
echo - success: true in the creation response
echo - A JWT token in the login response
echo.
echo Next Steps:
echo 1. Use the token from login to make authenticated requests
echo 2. Create your first organization
echo 3. Start managing tenants!
echo.
pause

