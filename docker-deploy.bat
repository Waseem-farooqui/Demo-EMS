@echo off
REM Employee Management System - Docker Production Deployment Script for Windows

echo ========================================
echo Employee Management System - Docker Deployment
echo ========================================
echo.

REM Check if Docker is installed
docker --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Docker is not installed. Please install Docker Desktop first.
    pause
    exit /b 1
)

REM Check if Docker Compose is available
docker compose version >nul 2>&1
if %errorlevel% neq 0 (
    docker-compose --version >nul 2>&1
    if %errorlevel% neq 0 (
        echo [ERROR] Docker Compose is not available. Please install Docker Compose.
        pause
        exit /b 1
    )
    set COMPOSE_CMD=docker-compose
) else (
    set COMPOSE_CMD=docker compose
)

echo [OK] Docker and Docker Compose are installed
echo.

REM Check if .env file exists
if not exist .env (
    echo [WARNING] .env file not found. Creating from template...

    if exist .env.example (
        copy .env.example .env
        echo [INFO] .env file created from template
        echo [WARNING] Please edit .env file with your production values before continuing!
        echo.
        echo Required changes:
        echo   1. Update database passwords (DB_PASSWORD, DB_ROOT_PASSWORD^)
        echo   2. Generate secure JWT_SECRET
        echo   3. Configure email settings (MAIL_USERNAME, MAIL_PASSWORD^)
        echo   4. Set your domain/URL (APP_URL, CORS_ALLOWED_ORIGINS^)
        echo.
        pause
    ) else (
        echo [ERROR] .env.example file not found
        pause
        exit /b 1
    )
)

echo [OK] .env file exists
echo.

:menu
echo Select an option:
echo   1) Deploy (build and start)
echo   2) Show logs
echo   3) Stop services
echo   4) Restart services
echo   5) Backup database
echo   6) Clean (remove all)
echo   7) Exit
echo.
set /p option="Enter option (1-7): "

if "%option%"=="1" goto deploy
if "%option%"=="2" goto logs
if "%option%"=="3" goto stop
if "%option%"=="4" goto restart
if "%option%"=="5" goto backup
if "%option%"=="6" goto clean
if "%option%"=="7" goto exit
echo [ERROR] Invalid option
goto menu

:deploy
echo.
echo [INFO] Building and starting containers...
echo.
%COMPOSE_CMD% build --no-cache
%COMPOSE_CMD% up -d
echo.
echo [OK] Deployment completed successfully!
echo.
echo Services status:
%COMPOSE_CMD% ps
echo.
echo Application URLs:
echo   Frontend: http://localhost
echo   Backend API: http://localhost:8080/api
echo   Backend Health: http://localhost:8080/api/actuator/health
echo.
echo Useful commands:
echo   View logs: %COMPOSE_CMD% logs -f
echo   View backend logs: %COMPOSE_CMD% logs -f backend
echo   View frontend logs: %COMPOSE_CMD% logs -f frontend
echo   Stop services: %COMPOSE_CMD% down
echo   Restart services: %COMPOSE_CMD% restart
echo.
pause
goto menu

:logs
echo.
echo [INFO] Showing logs (Ctrl+C to exit)...
%COMPOSE_CMD% logs -f
goto menu

:stop
echo.
echo [INFO] Stopping services...
%COMPOSE_CMD% down
echo [OK] Services stopped
pause
goto menu

:restart
echo.
echo [INFO] Restarting services...
%COMPOSE_CMD% restart
echo [OK] Services restarted
pause
goto menu

:backup
echo.
echo [INFO] Creating database backup...
set BACKUP_FILE=backup_%date:~-4,4%%date:~-10,2%%date:~-7,2%_%time:~0,2%%time:~3,2%%time:~6,2%.sql
%COMPOSE_CMD% exec -T mysql mysqldump -uemsuser -pemspassword employee_management_system > "%BACKUP_FILE%"
echo [OK] Database backup created: %BACKUP_FILE%
pause
goto menu

:clean
echo.
echo [WARNING] This will remove all containers, volumes, and images!
set /p confirm="Are you sure? (yes/no): "
if "%confirm%"=="yes" (
    echo [INFO] Cleaning up...
    %COMPOSE_CMD% down -v --rmi all
    echo [OK] Cleanup completed
) else (
    echo [INFO] Cleanup cancelled
)
pause
goto menu

:exit
echo Goodbye!
exit /b 0

