@echo off
REM cleanup-vm.bat - Complete VM Cleanup Script for Windows
REM This script removes all Employee Management System installations and data

setlocal enabledelayedexpansion

echo ========================================================================
echo          COMPLETE VM CLEANUP - REMOVE ALL DATA
echo               Employee Management System
echo ========================================================================
echo.
echo WARNING: This will PERMANENTLY DELETE:
echo   * All Docker containers, images, and volumes
echo   * All application data and databases
echo   * All uploaded documents
echo   * All configuration files
echo   * All logs and backups
echo   * Docker Desktop (optional)
echo   * Application source code (optional)
echo.
echo THIS CANNOT BE UNDONE!
echo.

set /p confirmation="Type 'DELETE EVERYTHING' to proceed: "

if not "%confirmation%"=="DELETE EVERYTHING" (
    echo Cleanup cancelled. No changes made.
    exit /b 0
)

echo.
echo Starting complete cleanup...
echo.

REM Step 1: Stop all Docker containers
echo [Step 1/10] Stopping all Docker containers...
docker stop ems-backend ems-frontend ems-mysql 2>nul
docker stop ems-backend-blue ems-frontend-blue 2>nul
for /f "tokens=*" %%i in ('docker ps -aq 2^>nul') do docker stop %%i 2>nul
echo Done: All containers stopped

REM Step 2: Remove Employee Management System containers
echo.
echo [Step 2/10] Removing EMS containers...
docker rm -f ems-backend ems-frontend ems-mysql 2>nul
docker rm -f ems-backend-blue ems-frontend-blue 2>nul
echo Done: EMS containers removed

REM Step 3: Remove all Docker containers
echo.
echo [Step 3/10] Removing all Docker containers...
for /f "tokens=*" %%i in ('docker ps -aq 2^>nul') do docker rm -f %%i 2>nul
echo Done: All containers removed

REM Step 4: Remove Docker images
echo.
echo [Step 4/10] Removing Docker images...
set /p remove_all_images="Remove ALL Docker images? (Y/N): "
if /i "%remove_all_images%"=="Y" (
    for /f "tokens=*" %%i in ('docker images -aq 2^>nul') do docker rmi -f %%i 2>nul
    echo Done: All Docker images removed
) else (
    for /f "tokens=*" %%i in ('docker images ^| findstr /i "ems employeemanagementsystem" 2^>nul') do (
        for /f "tokens=3" %%j in ("%%i") do docker rmi -f %%j 2>nul
    )
    echo Done: EMS Docker images removed
)

REM Step 5: Remove Docker volumes
echo.
echo [Step 5/10] Removing Docker volumes...
docker volume rm employeemanagementsystem_mysql_data 2>nul
docker volume rm employeemanagementsystem_uploads_data 2>nul
set /p remove_all_volumes="Remove ALL Docker volumes? (Y/N): "
if /i "%remove_all_volumes%"=="Y" (
    docker volume prune -f 2>nul
    echo Done: All Docker volumes removed
) else (
    echo Done: EMS Docker volumes removed
)

REM Step 6: Remove Docker networks
echo.
echo [Step 6/10] Removing Docker networks...
docker network rm employeemanagementsystem_ems-network 2>nul
docker network prune -f 2>nul
echo Done: Docker networks removed

REM Step 7: Clean Docker system
echo.
echo [Step 7/10] Cleaning Docker system...
set /p docker_prune="Run full Docker system prune? (Y/N): "
if /i "%docker_prune%"=="Y" (
    docker system prune -a --volumes -f
    echo Done: Docker system cleaned
) else (
    echo Skipped: Docker system prune
)

REM Step 8: Remove application directory
echo.
echo [Step 8/10] Removing application files...
set /p remove_source="Remove application source code? (Y/N): "
if /i "%remove_source%"=="Y" (
    set "APP_DIR=%CD%"
    echo Current directory: !APP_DIR!
    set /p confirm_delete="Delete this directory? (Y/N): "
    if /i "!confirm_delete!"=="Y" (
        cd ..
        rmdir /s /q "!APP_DIR!" 2>nul
        echo Done: Application directory removed
    )
) else (
    echo Skipped: Application directory kept
)

REM Step 9: Remove backups
echo.
echo [Step 9/10] Removing backups...
set /p remove_backups="Remove all backup files? (Y/N): "
if /i "%remove_backups%"=="Y" (
    del /f /q backup_*.sql* 2>nul
    rmdir /s /q uploads_backup_* 2>nul
    del /f /q .env.backup_* 2>nul
    echo Done: Backup files removed
)

REM Step 10: Additional cleanup
echo.
echo [Step 10/10] Additional cleanup...
del /f /q .env 2>nul
del /f /q .env.* 2>nul
rmdir /s /q frontend\node_modules 2>nul
rmdir /s /q target 2>nul
rmdir /s /q .idea 2>nul
rmdir /s /q .vscode 2>nul
del /f /q *.iml 2>nul
del /f /q *.log 2>nul
echo Done: Additional cleanup complete

echo.
echo ========================================================================
echo               CLEANUP COMPLETED SUCCESSFULLY
echo ========================================================================
echo.
echo Summary of actions taken:
echo   * Docker containers stopped and removed
echo   * Docker images removed
echo   * Docker volumes removed (all data deleted)
echo   * Docker networks removed
echo   * Backups removed
echo   * Configuration files removed
echo.
echo Your VM has been restored to a clean state.
echo.
echo To verify cleanup:
echo   docker ps -a          # Should show no containers
echo   docker images         # Should show no/few images
echo   docker volume ls      # Should show no/few volumes
echo.
echo To uninstall Docker Desktop:
echo   1. Open Windows Settings
echo   2. Go to Apps ^& Features
echo   3. Find "Docker Desktop"
echo   4. Click Uninstall
echo.
echo To reinstall the application:
echo   1. Install Docker Desktop
echo   2. Clone the repository
echo   3. Follow the deployment guide
echo.

pause

