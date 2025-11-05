@echo off
echo ============================================
echo  Frontend Build - Clear Cache and Rebuild
echo ============================================
echo.

cd frontend

echo Step 1: Clearing Angular cache...
if exist .angular\cache (
    rmdir /s /q .angular\cache
    echo   ✓ Angular cache cleared
) else (
    echo   ✓ No cache to clear
)
echo.

echo Step 2: Installing dependencies...
call npm install
if %errorlevel% neq 0 (
    echo   ✗ npm install failed
    pause
    exit /b 1
)
echo   ✓ Dependencies installed
echo.

echo Step 3: Building project...
call npm run build
if %errorlevel% neq 0 (
    echo   ✗ Build failed
    echo.
    echo Errors found. Please check the output above.
    pause
    exit /b 1
)
echo.

echo ============================================
echo  ✓ Build completed successfully!
echo ============================================
echo.
echo To start the development server, run:
echo   cd frontend
echo   npm start
echo.
pause

