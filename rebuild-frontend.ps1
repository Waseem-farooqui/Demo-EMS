# Frontend Rebuild Script with Nginx Configuration Check (PowerShell)
# This script rebuilds only the frontend container and verifies nginx config

$ErrorActionPreference = "Stop"

# Colors (PowerShell)
function Write-Step { Write-Host "`n▶ $args" -ForegroundColor Blue }
function Write-Success { Write-Host "✅ $args" -ForegroundColor Green }
function Write-Error { Write-Host "❌ $args" -ForegroundColor Red }
function Write-Warning { Write-Host "⚠️  $args" -ForegroundColor Yellow }
function Write-Info { Write-Host "ℹ️  $args" -ForegroundColor Cyan }

Write-Host "╔════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║   Frontend Rebuild Script with Nginx Verification         ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

# Step 1: Check Prerequisites
Write-Step "Step 1/10: Checking Prerequisites"

# Check Docker
try {
    $dockerVersion = docker --version 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Success "Docker is installed: $dockerVersion"
    } else {
        Write-Error "Docker is not installed or not in PATH"
        exit 1
    }
} catch {
    Write-Error "Docker is not installed"
    exit 1
}

# Check Docker Compose
try {
    $composeVersion = docker compose version 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Success "Docker Compose is available"
        $COMPOSE_CMD = "docker compose"
    } else {
        $composeVersion = docker-compose --version 2>&1
        if ($LASTEXITCODE -eq 0) {
            Write-Success "Docker Compose is available"
            $COMPOSE_CMD = "docker-compose"
        } else {
            Write-Error "Docker Compose is not installed"
            exit 1
        }
    }
} catch {
    Write-Error "Docker Compose is not installed"
    exit 1
}

# Check compose file
if (Test-Path "compose.yaml") {
    $COMPOSE_FILE = "compose.yaml"
} elseif (Test-Path "docker-compose.yml") {
    $COMPOSE_FILE = "docker-compose.yml"
} else {
    Write-Error "compose.yaml or docker-compose.yml not found"
    Write-Info "Please run this script from the project root directory"
    exit 1
}

Write-Success "Using compose file: $COMPOSE_FILE"

# Check frontend directory
if (-not (Test-Path "frontend")) {
    Write-Error "frontend directory not found"
    exit 1
}

Write-Success "Frontend directory found"

# Check nginx.conf
if (-not (Test-Path "frontend\nginx.conf")) {
    Write-Error "frontend\nginx.conf not found"
    exit 1
}

Write-Success "nginx.conf found"

# Step 2: Load Environment Variables
Write-Step "Step 2/10: Loading Environment Variables"

if (Test-Path ".env") {
    Write-Success ".env file found"
    Get-Content ".env" | ForEach-Object {
        if ($_ -match '^\s*([^#][^=]*)\s*=\s*(.*)\s*$') {
            $name = $matches[1].Trim()
            $value = $matches[2].Trim()
            [Environment]::SetEnvironmentVariable($name, $value, "Process")
        }
    }
} else {
    Write-Warning ".env file not found, using defaults"
}

# Step 3: Verify Nginx Configuration
Write-Step "Step 3/10: Verifying Nginx Configuration"

$NGINX_CONF = "frontend\nginx.conf"

if (-not (Test-Path $NGINX_CONF)) {
    Write-Error "nginx.conf not found at $NGINX_CONF"
    exit 1
}

Write-Success "nginx.conf file found"

# Check for common security issues
Write-Info "Checking for common security issues..."

$SECURITY_ISSUES = 0
$nginxContent = Get-Content $NGINX_CONF -Raw

# Check for server_tokens
if ($nginxContent -match "server_tokens\s+off") {
    Write-Success "server_tokens is disabled (good for security)"
} else {
    Write-Warning "server_tokens not explicitly disabled (may expose nginx version)"
    $SECURITY_ISSUES++
}

# Check for X-Frame-Options
if ($nginxContent -match "X-Frame-Options" -CaseSensitive:$false) {
    Write-Success "X-Frame-Options header found"
} else {
    Write-Warning "X-Frame-Options header not found (recommended for security)"
    $SECURITY_ISSUES++
}

# Check for X-Content-Type-Options
if ($nginxContent -match "X-Content-Type-Options" -CaseSensitive:$false) {
    Write-Success "X-Content-Type-Options header found"
} else {
    Write-Warning "X-Content-Type-Options header not found (recommended for security)"
    $SECURITY_ISSUES++
}

# Check for Content-Security-Policy
if ($nginxContent -match "Content-Security-Policy" -CaseSensitive:$false) {
    Write-Success "Content-Security-Policy header found"
} else {
    Write-Warning "Content-Security-Policy header not found (recommended for security)"
    $SECURITY_ISSUES++
}

# Check for try_files directive
if ($nginxContent -match "try_files") {
    Write-Success "try_files directive found (required for Angular routing)"
} else {
    Write-Error "try_files directive not found - Angular routing will not work!"
    $SECURITY_ISSUES++
}

# Check for root directive
if ($nginxContent -match "root\s+/usr/share/nginx/html") {
    Write-Success "root directive points to correct location"
} else {
    Write-Warning "root directive may not point to /usr/share/nginx/html"
    $SECURITY_ISSUES++
}

if ($SECURITY_ISSUES -eq 0) {
    Write-Success "No security issues found in nginx configuration"
} else {
    Write-Warning "Found $SECURITY_ISSUES potential security/configuration issues"
    Write-Info "Review nginx.conf and consider adding security headers"
}

# Step 4: Check Frontend Files
Write-Step "Step 4/10: Checking Frontend Source Files"

if (-not (Test-Path "frontend\package.json")) {
    Write-Error "frontend\package.json not found"
    exit 1
}

if (-not (Test-Path "frontend\Dockerfile")) {
    Write-Error "frontend\Dockerfile not found"
    exit 1
}

Write-Success "Frontend source files found"

# Step 5: Check Current Container Status
Write-Step "Step 5/10: Checking Current Frontend Container Status"

$containerStatus = & docker ps -a --filter "name=ems-frontend" --format "{{.Status}}" 2>&1
if ($containerStatus -match "Up") {
    Write-Info "Frontend container is currently running"
    $CONTAINER_RUNNING = $true
} else {
    Write-Info "Frontend container is not running"
    $CONTAINER_RUNNING = $false
}

# Step 6: Stop Frontend Container
Write-Step "Step 6/10: Stopping Frontend Container"

if ($CONTAINER_RUNNING) {
    Write-Info "Stopping frontend container..."
    Invoke-Expression "$COMPOSE_CMD -f $COMPOSE_FILE stop frontend"
    if ($LASTEXITCODE -eq 0) {
        Write-Success "Frontend container stopped"
    } else {
        Write-Warning "Failed to stop container (may not be running)"
    }
} else {
    Write-Info "Frontend container was not running"
}

# Step 7: Remove Old Container and Image
Write-Step "Step 7/10: Removing Old Frontend Container and Image"

Write-Info "Removing frontend container..."
Invoke-Expression "$COMPOSE_CMD -f $COMPOSE_FILE rm -f frontend" 2>&1 | Out-Null

Write-Info "Removing old frontend image (if exists)..."
docker rmi ems-frontend 2>&1 | Out-Null
docker images --filter "reference=*frontend*" -q | ForEach-Object { docker rmi $_ 2>&1 | Out-Null }

Write-Success "Old frontend container and image removed"

# Step 8: Rebuild Frontend
Write-Step "Step 8/10: Rebuilding Frontend Container"

Write-Info "Building frontend image..."
Write-Info "This may take several minutes..."

# Build with no cache
Invoke-Expression "$COMPOSE_CMD -f $COMPOSE_FILE build --no-cache frontend"
if ($LASTEXITCODE -eq 0) {
    Write-Success "Frontend image built successfully"
} else {
    Write-Error "Failed to build frontend image"
    exit 1
}

# Step 9: Start Frontend Container
Write-Step "Step 9/10: Starting Frontend Container"

Invoke-Expression "$COMPOSE_CMD -f $COMPOSE_FILE up -d frontend"
if ($LASTEXITCODE -eq 0) {
    Write-Success "Frontend container started"
} else {
    Write-Error "Failed to start frontend container"
    exit 1
}

# Wait for container
Write-Info "Waiting for frontend container to be healthy..."
Start-Sleep -Seconds 5

# Step 10: Verify Deployment
Write-Step "Step 10/10: Verifying Frontend Deployment"

# Check container status
$containerStatus = & docker ps --filter "name=ems-frontend" --format "{{.Status}}" 2>&1
if ($containerStatus -match "Up") {
    Write-Success "Frontend container is running"
} else {
    Write-Error "Frontend container is not running"
    Write-Info "Check logs with: $COMPOSE_CMD -f $COMPOSE_FILE logs frontend"
    exit 1
}

# Check if index.html exists
Write-Info "Checking if index.html exists in container..."
$indexCheck = docker exec ems-frontend test -f /usr/share/nginx/html/index.html 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Success "index.html found in container"
} else {
    Write-Error "index.html NOT found in container - build may have failed!"
    Write-Info "Checking container logs..."
    Invoke-Expression "$COMPOSE_CMD -f $COMPOSE_FILE logs --tail=50 frontend"
    exit 1
}

# Check nginx configuration
Write-Info "Validating nginx configuration in container..."
$nginxTest = docker exec ems-frontend nginx -t 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Success "Nginx configuration is valid in container"
} else {
    Write-Error "Nginx configuration is invalid in container!"
    Write-Host $nginxTest
    exit 1
}

# Test HTTP response
Write-Info "Testing HTTP response..."
try {
    $response = Invoke-WebRequest -Uri "http://localhost" -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
    if ($response.StatusCode -eq 200) {
        Write-Success "Frontend is responding with HTTP 200"
        
        # Check for default nginx page
        if ($response.Content -match "Welcome to nginx") {
            Write-Error "Default nginx page is being served - application files not deployed!"
            Write-Info "This indicates the build failed or files were not copied correctly"
            Write-Info "Checking container contents..."
            docker exec ems-frontend ls -la /usr/share/nginx/html/
            exit 1
        } elseif ($response.Content -match "app-root") {
            Write-Success "Angular application is being served (found app-root)"
        } elseif ($response.Content -match "<!DOCTYPE html") {
            Write-Success "HTML content is being served"
        } else {
            Write-Warning "Could not verify application content (may need manual check)"
        }
    } else {
        Write-Error "Frontend returned HTTP $($response.StatusCode) (expected 200)"
    }
} catch {
    Write-Warning "Could not connect to frontend: $_"
    Write-Info "Wait a few seconds and check: curl http://localhost"
}

# Display logs
Write-Step "Recent Frontend Container Logs"
Invoke-Expression "$COMPOSE_CMD -f $COMPOSE_FILE logs --tail=20 frontend"

# Summary
Write-Host ""
Write-Host "════════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "Summary:" -ForegroundColor Cyan
Write-Host ""
Write-Success "Frontend rebuild completed"
Write-Host ""
Write-Host "Container Status:"
Invoke-Expression "$COMPOSE_CMD -f $COMPOSE_FILE ps frontend"
Write-Host ""
Write-Host "Useful commands:"
Write-Host "  $COMPOSE_CMD -f $COMPOSE_FILE logs frontend - View frontend logs" -ForegroundColor Green
Write-Host "  docker exec ems-frontend nginx -t - Test nginx config" -ForegroundColor Green
Write-Host "  docker exec ems-frontend ls -la /usr/share/nginx/html/ - List files" -ForegroundColor Green
Write-Host "  Invoke-WebRequest http://localhost - Test frontend" -ForegroundColor Green
Write-Host ""

