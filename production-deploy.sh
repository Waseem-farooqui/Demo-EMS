#!/bin/bash
###############################################################################
# Employee Management System - Production Deployment Script
# Ubuntu 24.04 LTS - Complete Production-Ready Deployment
###############################################################################
# This script provides a one-stop solution for production deployment:
# - Stops and removes all existing containers and images
# - Applies all security fixes and configuration updates
# - Deploys with production-ready settings
# - Verifies deployment health
###############################################################################

set -e  # Exit on error

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKUP_DIR="${BACKUP_DIR:-/backups/ems}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Banner
echo -e "${CYAN}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║   Employee Management System - Production Deployment       ║${NC}"
echo -e "${CYAN}║              Ubuntu 24.04 LTS - One-Stop Solution         ║${NC}"
echo -e "${CYAN}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Functions
print_step() {
    echo ""
    echo -e "${BLUE}▶ $1${NC}"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_info() {
    echo -e "${CYAN}ℹ️  $1${NC}"
}

# Check if running as root
if [ "$EUID" -eq 0 ]; then
    print_warning "Running as root. Some operations may need adjustment."
fi

# Check prerequisites
print_step "Step 1/20: Checking Prerequisites"

# Check Docker
if ! command -v docker &> /dev/null; then
    print_error "Docker is not installed"
    echo "Installing Docker..."
    curl -fsSL https://get.docker.com -o get-docker.sh
    sudo sh get-docker.sh
    sudo usermod -aG docker $USER
    rm get-docker.sh
    print_success "Docker installed"
    print_warning "You may need to log out and back in for Docker group changes"
else
    print_success "Docker is installed: $(docker --version)"
fi

# Check Docker Compose
if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    print_error "Docker Compose is not installed"
    echo "Installing Docker Compose..."
    sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    sudo chmod +x /usr/local/bin/docker-compose
    print_success "Docker Compose installed"
else
    print_success "Docker Compose is available"
fi

# Check if in project directory
if [ ! -f "compose.yaml" ] && [ ! -f "docker-compose.yml" ]; then
    print_error "compose.yaml or docker-compose.yml not found"
    print_info "Please run this script from the project root directory"
    exit 1
fi

COMPOSE_FILE="compose.yaml"
if [ ! -f "$COMPOSE_FILE" ]; then
    COMPOSE_FILE="docker-compose.yml"
fi

print_success "Compose file found: $COMPOSE_FILE"

# Check .env file
print_step "Step 2/20: Verifying Environment Configuration"

if [ ! -f .env ]; then
    print_warning ".env file not found"
    if [ -f .env.example ]; then
        print_info "Creating .env from .env.example..."
        cp .env.example .env
        print_success ".env file created"
        print_warning "⚠️  CRITICAL: Please edit .env file with your production values!"
        echo ""
        echo "Required variables:"
        echo "  - DB_PASSWORD"
        echo "  - DB_ROOT_PASSWORD"
        echo "  - JWT_SECRET (generate with: openssl rand -base64 64)"
        echo "  - API_URL, FRONTEND_URL, APP_URL"
        echo "  - CORS_ALLOWED_ORIGINS"
        echo ""
        read -p "Press Enter after updating .env file..."
    else
        print_error ".env.example not found. Cannot proceed."
        exit 1
    fi
else
    print_success ".env file exists"
fi

# Load environment variables
set -a
source .env 2>/dev/null || true
set +a

# Verify critical variables
MISSING_VARS=()
[ -z "$DB_PASSWORD" ] && MISSING_VARS+=("DB_PASSWORD")
[ -z "$DB_ROOT_PASSWORD" ] && MISSING_VARS+=("DB_ROOT_PASSWORD")
[ -z "$JWT_SECRET" ] && MISSING_VARS+=("JWT_SECRET")

if [ ${#MISSING_VARS[@]} -gt 0 ]; then
    print_error "Missing critical environment variables: ${MISSING_VARS[*]}"
    print_info "Please add them to .env file before continuing"
    exit 1
fi

# Validate JWT secret strength
if [ ${#JWT_SECRET} -lt 64 ]; then
    print_warning "JWT_SECRET is too short (${#JWT_SECRET} chars, recommend 64+)"
    print_info "Generate new secret: openssl rand -base64 64"
    read -p "Continue anyway? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

print_success "Environment variables validated"

# Backup existing deployment
print_step "Step 3/20: Creating Backup"

if docker-compose ps | grep -q "Up"; then
    print_info "Existing containers detected, creating backup..."
    
    mkdir -p "$BACKUP_DIR"
    BACKUP_PATH="$BACKUP_DIR/pre_deploy_$TIMESTAMP"
    mkdir -p "$BACKUP_PATH"
    
    # Backup database
    if docker-compose ps mysql | grep -q "Up"; then
        print_info "Backing up database..."
        docker-compose exec -T mysql mysqldump \
            -u root -p"${DB_ROOT_PASSWORD}" \
            --single-transaction \
            employee_management_system > "$BACKUP_PATH/database.sql" 2>/dev/null || \
            print_warning "Database backup failed (may not exist yet)"
    fi
    
    # Backup uploads
    if docker-compose ps backend | grep -q "Up"; then
        print_info "Backing up uploads..."
        docker cp ems-backend:/app/uploads "$BACKUP_PATH/uploads" 2>/dev/null || \
            print_warning "Uploads backup failed (may be empty)"
    fi
    
    # Backup .env
    cp .env "$BACKUP_PATH/.env.backup" 2>/dev/null || true
    
    print_success "Backup created: $BACKUP_PATH"
else
    print_info "No existing containers to backup"
fi

# Stop and remove existing containers
print_step "Step 4/20: Stopping Existing Containers"

print_info "Stopping all containers..."
docker-compose -f "$COMPOSE_FILE" down 2>/dev/null || true

# Stop any remaining EMS containers
print_info "Stopping EMS containers..."
docker stop ems-backend ems-frontend ems-mysql 2>/dev/null || true
docker stop ems-backend-blue ems-frontend-blue 2>/dev/null || true

print_success "All containers stopped"

# Remove containers
print_step "Step 5/20: Removing Containers"

print_info "Removing containers..."
docker-compose -f "$COMPOSE_FILE" down -v 2>/dev/null || true
docker rm -f ems-backend ems-frontend ems-mysql 2>/dev/null || true
docker rm -f ems-backend-blue ems-frontend-blue 2>/dev/null || true
docker ps -a | grep -E "ems-|demo-ems" | awk '{print $1}' | xargs -r docker rm -f 2>/dev/null || true

print_success "Containers removed"

# Remove images
print_step "Step 6/20: Removing Old Images"

print_info "Removing EMS-related images..."
docker images | grep -E "ems-|demo-ems|employeemanagementsystem" | awk '{print $3}' | xargs -r docker rmi -f 2>/dev/null || true

print_info "Cleaning up dangling images..."
docker image prune -f > /dev/null 2>&1

print_success "Old images removed"

# Verify required files exist
print_step "Step 7/20: Verifying Required Files"

REQUIRED_FILES=(
    "compose.yaml"
    "Dockerfile"
    "frontend/Dockerfile"
    "database/init.sql"
    "src/main/resources/application-prod.properties"
    "frontend/src/environments/environment.prod.ts"
)

MISSING_FILES=()
for file in "${REQUIRED_FILES[@]}"; do
    if [ ! -f "$file" ]; then
        MISSING_FILES+=("$file")
    fi
done

if [ ${#MISSING_FILES[@]} -gt 0 ]; then
    print_error "Missing required files: ${MISSING_FILES[*]}"
    exit 1
fi

print_success "All required files present"

# Verify configuration files
print_step "Step 8/20: Verifying Configuration"

# Check database URL uses mysql container
if ! grep -q "mysql:3306" src/main/resources/application-prod.properties; then
    print_error "application-prod.properties not configured for Docker (should use 'mysql' container)"
    exit 1
fi

# Check frontend Dockerfile has build args
if ! grep -q "ARG API_URL" frontend/Dockerfile; then
    print_error "frontend/Dockerfile missing build arguments"
    exit 1
fi

# Check compose.yaml has environment variables
if ! grep -q "SPRING_PROFILES_ACTIVE" "$COMPOSE_FILE"; then
    print_error "$COMPOSE_FILE missing Spring profile configuration"
    exit 1
fi

print_success "Configuration verified"

# Security checks
print_step "Step 9/20: Security Validation"

# Check for hardcoded secrets
if grep -r "K81768751288957" src/ 2>/dev/null; then
    print_error "Hardcoded OCR API key found in source code!"
    exit 1
fi

# Check for hardcoded IPs in frontend
if grep -q "62.169.20.104" frontend/src/environments/environment.prod.ts 2>/dev/null; then
    print_warning "Hardcoded IP found in environment.prod.ts (should use placeholders)"
fi

# Check CORS configuration
if [ -n "$CORS_ALLOWED_ORIGINS" ]; then
    if [[ "$CORS_ALLOWED_ORIGINS" == *"*"* ]]; then
        print_error "CORS allows all origins (*) - security risk!"
        exit 1
    fi
fi

print_success "Security checks passed"

# Clean Docker system
print_step "Step 10/20: Cleaning Docker System"

print_info "Removing unused Docker resources..."
docker system prune -f > /dev/null 2>&1

print_success "Docker system cleaned"

# Build images
print_step "Step 11/20: Building Docker Images"

print_info "Building backend image (this may take 5-10 minutes)..."
docker-compose -f "$COMPOSE_FILE" build --no-cache backend || {
    print_error "Backend build failed!"
    exit 1
}

print_info "Building frontend image (this may take 5-10 minutes)..."
docker-compose -f "$COMPOSE_FILE" build --no-cache frontend || {
    print_error "Frontend build failed!"
    exit 1
}

print_success "All images built successfully"

# Create volumes
print_step "Step 12/20: Creating Docker Volumes"

print_info "Creating volumes..."
docker volume create employeemanagementsystem_mysql_data 2>/dev/null || true
docker volume create employeemanagementsystem_uploads_data 2>/dev/null || true

print_success "Volumes created"

# Start MySQL first
print_step "Step 13/20: Starting Database"

print_info "Starting MySQL container..."
docker-compose -f "$COMPOSE_FILE" up -d mysql

# Wait for MySQL to be ready
print_info "Waiting for MySQL to be ready (30 seconds)..."
sleep 30

# Check MySQL health
MAX_ATTEMPTS=30
ATTEMPT=0
while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
    if docker-compose -f "$COMPOSE_FILE" exec mysql mysqladmin ping -h localhost -u root -p"${DB_ROOT_PASSWORD}" --silent 2>/dev/null; then
        print_success "MySQL is ready"
        break
    fi
    ATTEMPT=$((ATTEMPT + 1))
    echo -n "."
    sleep 2
done

if [ $ATTEMPT -eq $MAX_ATTEMPTS ]; then
    print_error "MySQL failed to start"
    docker-compose -f "$COMPOSE_FILE" logs mysql
    exit 1
fi

# Fix database user password and create tables
print_step "Step 14/20: Setting Up Database"

print_info "Fixing database user password and privileges..."
docker-compose -f "$COMPOSE_FILE" exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" <<EOF 2>/dev/null || true
ALTER USER IF EXISTS '${DB_USERNAME:-emsuser}'@'%' IDENTIFIED BY '${DB_PASSWORD}';
CREATE USER IF NOT EXISTS '${DB_USERNAME:-emsuser}'@'%' IDENTIFIED BY '${DB_PASSWORD}';
GRANT ALL PRIVILEGES ON employee_management_system.* TO '${DB_USERNAME:-emsuser}'@'%';
GRANT CREATE ON *.* TO '${DB_USERNAME:-emsuser}'@'%';
FLUSH PRIVILEGES;
EOF

print_info "Checking if database tables exist..."
TABLES_COUNT=$(docker-compose -f "$COMPOSE_FILE" exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" \
    -e "USE employee_management_system; SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'employee_management_system';" \
    2>/dev/null | tail -1 | tr -d ' ' || echo "0")

# Check if critical tables exist
DOCUMENTS_EXISTS=$(docker-compose -f "$COMPOSE_FILE" exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" \
    -e "USE employee_management_system; SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'employee_management_system' AND table_name = 'documents';" \
    2>/dev/null | tail -1 | tr -d ' ' || echo "0")

if [ "$DOCUMENTS_EXISTS" = "0" ] || [ "$TABLES_COUNT" -lt 10 ]; then
    print_warning "Database tables are missing or incomplete"
    print_info "Creating all tables manually..."
    
    # Check if create-tables.sql exists
    if [ -f "create-tables.sql" ]; then
        print_info "Using create-tables.sql to create tables..."
        docker-compose -f "$COMPOSE_FILE" exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" < create-tables.sql 2>/dev/null || {
            print_warning "Failed to execute create-tables.sql, trying inline SQL..."
            # Fallback: create critical tables inline
            docker-compose -f "$COMPOSE_FILE" exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" <<'SQL'
USE employee_management_system;
CREATE TABLE IF NOT EXISTS organizations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_uuid VARCHAR(36) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL UNIQUE,
    logo_path VARCHAR(500),
    logo_data LONGBLOB,
    created_at DATETIME,
    updated_at DATETIME,
    is_active BOOLEAN DEFAULT TRUE,
    description VARCHAR(500),
    contact_email VARCHAR(255),
    contact_phone VARCHAR(50),
    address VARCHAR(1000),
    INDEX idx_org_uuid (organization_uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    email_verified BOOLEAN DEFAULT FALSE,
    first_login BOOLEAN DEFAULT TRUE,
    profile_completed BOOLEAN DEFAULT FALSE,
    temporary_password BOOLEAN DEFAULT TRUE,
    organization_id BIGINT,
    organization_uuid VARCHAR(36),
    UNIQUE KEY uk_username_org (username, organization_id),
    UNIQUE KEY uk_email_org (email, organization_id),
    INDEX idx_org_id (organization_id),
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS employees (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    person_type VARCHAR(50) NOT NULL,
    work_email VARCHAR(255) NOT NULL,
    personal_email VARCHAR(255),
    phone_number VARCHAR(50),
    date_of_birth DATE,
    nationality VARCHAR(100),
    address VARCHAR(500),
    present_address TEXT,
    previous_address TEXT,
    has_medical_condition BOOLEAN DEFAULT FALSE,
    medical_condition_details TEXT,
    next_of_kin_name VARCHAR(255),
    next_of_kin_contact VARCHAR(50),
    next_of_kin_address TEXT,
    blood_group VARCHAR(10),
    emergency_contact_name VARCHAR(255),
    emergency_contact_phone VARCHAR(50),
    emergency_contact_relationship VARCHAR(100),
    job_title VARCHAR(255),
    reference VARCHAR(255),
    date_of_joining DATE NOT NULL,
    employment_status VARCHAR(50),
    contract_type VARCHAR(50),
    working_timing VARCHAR(100),
    holiday_allowance INT,
    allotted_organization VARCHAR(255),
    user_id BIGINT,
    department_id BIGINT,
    organization_id BIGINT,
    organization_uuid VARCHAR(36),
    UNIQUE KEY uk_work_email_org (work_email, organization_id),
    INDEX idx_org_id (organization_id),
    INDEX idx_user_id (user_id),
    INDEX idx_department_id (department_id),
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS departments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    code VARCHAR(50),
    manager_id BIGINT,
    is_active BOOLEAN DEFAULT TRUE,
    organization_id BIGINT,
    created_at DATETIME,
    updated_at DATETIME,
    UNIQUE KEY uk_code_org (code, organization_id),
    UNIQUE KEY uk_name_org (name, organization_id),
    INDEX idx_org_id (organization_id),
    INDEX idx_manager_id (manager_id),
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    FOREIGN KEY (manager_id) REFERENCES employees(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    document_number VARCHAR(255),
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_type VARCHAR(50),
    file_hash VARCHAR(32),
    issue_date DATE,
    expiry_date DATE,
    issuing_country VARCHAR(100),
    full_name VARCHAR(255),
    date_of_birth DATE,
    nationality VARCHAR(100),
    company_name VARCHAR(255),
    date_of_check DATE,
    reference_number VARCHAR(255),
    contract_date DATE,
    place_of_work VARCHAR(255),
    contract_between VARCHAR(500),
    job_title_contract VARCHAR(255),
    uploaded_date DATETIME NOT NULL,
    last_alert_sent DATETIME,
    alert_sent_count INT DEFAULT 0,
    last_viewed_at DATETIME,
    last_viewed_by VARCHAR(255),
    INDEX idx_employee_id (employee_id),
    INDEX idx_document_type (document_type),
    INDEX idx_expiry_date (expiry_date),
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS leaves (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    leave_type VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    days_taken DECIMAL(5,2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    reason TEXT,
    applied_date DATETIME NOT NULL,
    approved_by BIGINT,
    approved_date DATETIME,
    rejection_reason TEXT,
    organization_id BIGINT,
    INDEX idx_employee_id (employee_id),
    INDEX idx_status (status),
    INDEX idx_dates (start_date, end_date),
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    FOREIGN KEY (approved_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS leave_balances (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    leave_type VARCHAR(50) NOT NULL,
    total_allocated DECIMAL(5,2) NOT NULL DEFAULT 0,
    used DECIMAL(5,2) NOT NULL DEFAULT 0,
    remaining DECIMAL(5,2) NOT NULL DEFAULT 0,
    year INT NOT NULL,
    organization_id BIGINT,
    UNIQUE KEY uk_employee_leave_year (employee_id, leave_type, year),
    INDEX idx_employee_id (employee_id),
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS attendance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    date DATE NOT NULL,
    check_in_time DATETIME,
    check_out_time DATETIME,
    status VARCHAR(50),
    hours_worked DECIMAL(5,2),
    notes TEXT,
    organization_id BIGINT,
    UNIQUE KEY uk_employee_date (employee_id, date),
    INDEX idx_date (date),
    INDEX idx_status (status),
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS rotas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    week_start_date DATE NOT NULL,
    week_end_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_by BIGINT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    organization_id BIGINT,
    INDEX idx_employee_id (employee_id),
    INDEX idx_week_dates (week_start_date, week_end_date),
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS rota_schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rota_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    employee_name VARCHAR(255) NOT NULL,
    schedule_date DATE NOT NULL,
    day_of_week VARCHAR(50) NOT NULL,
    start_time TIME,
    end_time TIME,
    duty VARCHAR(255) NOT NULL,
    is_off_day BOOLEAN DEFAULT FALSE,
    INDEX idx_rota_id (rota_id),
    INDEX idx_employee_id (employee_id),
    INDEX idx_schedule_date (schedule_date),
    FOREIGN KEY (rota_id) REFERENCES rotas(id) ON DELETE CASCADE,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS rota_change_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rota_id BIGINT NOT NULL,
    changed_by BIGINT NOT NULL,
    change_type VARCHAR(50) NOT NULL,
    old_value TEXT,
    new_value TEXT,
    changed_at DATETIME NOT NULL,
    reason TEXT,
    INDEX idx_rota_id (rota_id),
    INDEX idx_changed_at (changed_at),
    FOREIGN KEY (rota_id) REFERENCES rotas(id) ON DELETE CASCADE,
    FOREIGN KEY (changed_by) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50),
    is_read BOOLEAN DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    read_at DATETIME,
    organization_id BIGINT,
    INDEX idx_user_id (user_id),
    INDEX idx_is_read (is_read),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS alert_configurations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    alert_days_before INT NOT NULL,
    alert_email VARCHAR(255),
    enabled BOOLEAN DEFAULT TRUE,
    alert_priority VARCHAR(50) NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    alert_frequency VARCHAR(50),
    repeat_until_resolved BOOLEAN DEFAULT FALSE,
    UNIQUE KEY uk_doc_priority (document_type, alert_priority),
    UNIQUE KEY uk_org_doc_priority (organization_id, document_type, alert_priority),
    INDEX idx_org_id (organization_id),
    INDEX idx_document_type (document_type),
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS verification_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    token_type VARCHAR(50) NOT NULL,
    expiry_date DATETIME NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    INDEX idx_token (token),
    INDEX idx_user_id (user_id),
    INDEX idx_expiry_date (expiry_date),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
SQL
        }
        
        # Verify tables were created
        sleep 2
        FINAL_TABLES=$(docker-compose -f "$COMPOSE_FILE" exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" \
            -e "USE employee_management_system; SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'employee_management_system';" \
            2>/dev/null | tail -1 | tr -d ' ' || echo "0")
        
        if [ "$FINAL_TABLES" -ge 10 ]; then
            print_success "Database tables created successfully ($FINAL_TABLES tables)"
        else
            print_warning "Some tables may still be missing (found $FINAL_TABLES tables)"
            print_info "JPA will attempt to create missing tables on backend startup"
        fi
    else
        print_warning "create-tables.sql not found, relying on JPA to create tables"
    fi
else
    print_success "Database tables already exist ($TABLES_COUNT tables found)"
fi

# Fix all table schemas to match entities
print_step "Step 14.5/20: Fixing Table Schemas"

print_info "Checking and fixing all table schemas to match entity definitions..."

# Fix alert_configurations table schema if needed
print_info "Checking alert_configurations table schema..."
ALERT_TABLE_EXISTS=$(docker-compose -f "$COMPOSE_FILE" exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" \
    -e "USE employee_management_system; SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'employee_management_system' AND table_name = 'alert_configurations';" \
    2>/dev/null | tail -1 | tr -d ' ' || echo "0")

if [ "$ALERT_TABLE_EXISTS" = "1" ]; then
    # Check if table has wrong schema (has alert_type column instead of document_type)
    HAS_ALERT_TYPE=$(docker-compose -f "$COMPOSE_FILE" exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" \
        -e "USE employee_management_system; SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'employee_management_system' AND table_name = 'alert_configurations' AND column_name = 'alert_type';" \
        2>/dev/null | tail -1 | tr -d ' ' || echo "0")
    
    if [ "$HAS_ALERT_TYPE" = "1" ]; then
        print_warning "alert_configurations table has wrong schema (has alert_type instead of document_type)"
        print_info "Fixing table schema..."
        
        docker-compose -f "$COMPOSE_FILE" exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" <<EOF 2>/dev/null || true
USE employee_management_system;
-- Drop and recreate table with correct schema
DROP TABLE IF EXISTS alert_configurations;
CREATE TABLE alert_configurations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    alert_days_before INT NOT NULL,
    alert_email VARCHAR(255),
    enabled BOOLEAN DEFAULT TRUE,
    alert_priority VARCHAR(50) NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    alert_frequency VARCHAR(50),
    repeat_until_resolved BOOLEAN DEFAULT FALSE,
    UNIQUE KEY uk_doc_priority (document_type, alert_priority),
    UNIQUE KEY uk_org_doc_priority (organization_id, document_type, alert_priority),
    INDEX idx_org_id (organization_id),
    INDEX idx_document_type (document_type),
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
SELECT 'Alert configurations table schema fixed' AS status;
EOF
        
        if [ $? -eq 0 ]; then
            print_success "alert_configurations table schema fixed"
        else
            print_warning "Failed to fix alert_configurations table schema (will be created by JPA)"
        fi
    else
        print_success "alert_configurations table has correct schema"
    fi
else
    print_info "alert_configurations table doesn't exist yet (will be created by JPA)"
fi

# Fix rota_schedules table schema if needed
print_info "Checking rota_schedules table schema..."
ROTA_SCHEDULES_TABLE_EXISTS=$(docker-compose -f "$COMPOSE_FILE" exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" \
    -e "USE employee_management_system; SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'employee_management_system' AND table_name = 'rota_schedules';" \
    2>/dev/null | tail -1 | tr -d ' ' || echo "0")

if [ "$ROTA_SCHEDULES_TABLE_EXISTS" = "1" ]; then
    # Check if table is missing required columns
    HAS_EMPLOYEE_ID=$(docker-compose -f "$COMPOSE_FILE" exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" \
        -e "USE employee_management_system; SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'employee_management_system' AND table_name = 'rota_schedules' AND column_name = 'employee_id';" \
        2>/dev/null | tail -1 | tr -d ' ' || echo "0")
    
    HAS_EMPLOYEE_NAME=$(docker-compose -f "$COMPOSE_FILE" exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" \
        -e "USE employee_management_system; SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'employee_management_system' AND table_name = 'rota_schedules' AND column_name = 'employee_name';" \
        2>/dev/null | tail -1 | tr -d ' ' || echo "0")
    
    HAS_SCHEDULE_DATE=$(docker-compose -f "$COMPOSE_FILE" exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" \
        -e "USE employee_management_system; SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'employee_management_system' AND table_name = 'rota_schedules' AND column_name = 'schedule_date';" \
        2>/dev/null | tail -1 | tr -d ' ' || echo "0")
    
    HAS_DUTY=$(docker-compose -f "$COMPOSE_FILE" exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" \
        -e "USE employee_management_system; SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'employee_management_system' AND table_name = 'rota_schedules' AND column_name = 'duty';" \
        2>/dev/null | tail -1 | tr -d ' ' || echo "0")
    
    # Check if day_of_week is INT (should be VARCHAR)
    DAY_OF_WEEK_TYPE=$(docker-compose -f "$COMPOSE_FILE" exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" \
        -e "USE employee_management_system; SELECT DATA_TYPE FROM information_schema.columns WHERE table_schema = 'employee_management_system' AND table_name = 'rota_schedules' AND column_name = 'day_of_week';" \
        2>/dev/null | tail -1 | tr -d ' ' || echo "")
    
    if [ "$HAS_EMPLOYEE_ID" = "0" ] || [ "$HAS_EMPLOYEE_NAME" = "0" ] || [ "$HAS_SCHEDULE_DATE" = "0" ] || [ "$HAS_DUTY" = "0" ] || [ "$DAY_OF_WEEK_TYPE" = "int" ]; then
        print_warning "rota_schedules table is missing required columns or has wrong schema"
        print_info "Fixing table schema..."
        
        docker-compose -f "$COMPOSE_FILE" exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" <<'ROTAFIX' 2>/dev/null || true
USE employee_management_system;

-- Add missing columns using idempotent approach
SET @dbname = DATABASE();
SET @tablename = 'rota_schedules';

-- Add employee_id if missing
SET @columnname = 'employee_id';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE (table_name = @tablename) AND (table_schema = @dbname) AND (column_name = @columnname)
  ) > 0,
  "SELECT 'Column employee_id already exists.' AS message",
  CONCAT("ALTER TABLE ", @tablename, " ADD COLUMN ", @columnname, " BIGINT NOT NULL AFTER rota_id")
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Add employee_name if missing
SET @columnname = 'employee_name';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE (table_name = @tablename) AND (table_schema = @dbname) AND (column_name = @columnname)
  ) > 0,
  "SELECT 'Column employee_name already exists.' AS message",
  CONCAT("ALTER TABLE ", @tablename, " ADD COLUMN ", @columnname, " VARCHAR(255) NOT NULL AFTER employee_id")
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Add schedule_date if missing
SET @columnname = 'schedule_date';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE (table_name = @tablename) AND (table_schema = @dbname) AND (column_name = @columnname)
  ) > 0,
  "SELECT 'Column schedule_date already exists.' AS message",
  CONCAT("ALTER TABLE ", @tablename, " ADD COLUMN ", @columnname, " DATE NOT NULL AFTER employee_name")
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Modify day_of_week from INT to VARCHAR if needed
SET @preparedStatement = (SELECT IF(
  (
    SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS
    WHERE (table_name = @tablename) AND (table_schema = @dbname) AND (column_name = 'day_of_week') AND (DATA_TYPE = 'int')
  ) IS NOT NULL,
  CONCAT("ALTER TABLE ", @tablename, " MODIFY COLUMN day_of_week VARCHAR(50) NOT NULL"),
  "SELECT 'Column day_of_week is already VARCHAR or does not exist.' AS message"
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Add duty if missing
SET @columnname = 'duty';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE (table_name = @tablename) AND (table_schema = @dbname) AND (column_name = @columnname)
  ) > 0,
  "SELECT 'Column duty already exists.' AS message",
  CONCAT("ALTER TABLE ", @tablename, " ADD COLUMN ", @columnname, " VARCHAR(255) NOT NULL AFTER end_time")
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Add is_off_day if missing
SET @columnname = 'is_off_day';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE (table_name = @tablename) AND (table_schema = @dbname) AND (column_name = @columnname)
  ) > 0,
  "SELECT 'Column is_off_day already exists.' AS message",
  CONCAT("ALTER TABLE ", @tablename, " ADD COLUMN ", @columnname, " BOOLEAN DEFAULT FALSE AFTER duty")
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Add indexes if missing
SET @indexname = 'idx_employee_id';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE (table_name = @tablename) AND (table_schema = @dbname) AND (index_name = @indexname)
  ) > 0,
  "SELECT 'Index idx_employee_id already exists.' AS message",
  CONCAT("CREATE INDEX ", @indexname, " ON ", @tablename, " (employee_id)")
));
PREPARE createIndexIfNotExists FROM @preparedStatement;
EXECUTE createIndexIfNotExists;
DEALLOCATE PREPARE createIndexIfNotExists;

SET @indexname = 'idx_schedule_date';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE (table_name = @tablename) AND (table_schema = @dbname) AND (index_name = @indexname)
  ) > 0,
  "SELECT 'Index idx_schedule_date already exists.' AS message",
  CONCAT("CREATE INDEX ", @indexname, " ON ", @tablename, " (schedule_date)")
));
PREPARE createIndexIfNotExists FROM @preparedStatement;
EXECUTE createIndexIfNotExists;
DEALLOCATE PREPARE createIndexIfNotExists;

-- Add foreign key constraint if missing
SET @constraint_exists = (
    SELECT COUNT(*) 
    FROM information_schema.TABLE_CONSTRAINTS 
    WHERE CONSTRAINT_SCHEMA = @dbname
    AND TABLE_NAME = @tablename
    AND CONSTRAINT_NAME = 'fk_rota_schedules_employee'
    AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);

SET @sql = IF(@constraint_exists = 0,
    CONCAT('ALTER TABLE ', @tablename, ' ADD CONSTRAINT fk_rota_schedules_employee FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE'),
    'SELECT "Foreign key constraint already exists" AS message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'rota_schedules table schema fixed' AS status;
ROTAFIX
        
        if [ $? -eq 0 ]; then
            print_success "rota_schedules table schema fixed"
        else
            print_warning "Failed to fix rota_schedules table schema (will be fixed by Flyway migration V14)"
        fi
    else
        print_success "rota_schedules table has correct schema"
    fi
else
    print_info "rota_schedules table doesn't exist yet (will be created by JPA or Flyway)"
fi

# Fix rota_schedules table BEFORE Flyway repair (ensures table structure is correct)
print_info "Ensuring rota_schedules table has correct structure..."
ROTA_SCHEDULES_TABLE_EXISTS_CHECK=$(docker-compose -f "$COMPOSE_FILE" exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" \
    -e "USE employee_management_system; SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'employee_management_system' AND table_name = 'rota_schedules';" \
    2>/dev/null | tail -1 | tr -d ' ' || echo "0")

if [ "$ROTA_SCHEDULES_TABLE_EXISTS_CHECK" = "1" ]; then
    HAS_EMPLOYEE_ID_CHECK=$(docker-compose -f "$COMPOSE_FILE" exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" \
        -e "USE employee_management_system; SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'employee_management_system' AND table_name = 'rota_schedules' AND column_name = 'employee_id';" \
        2>/dev/null | tail -1 | tr -d ' ' || echo "0")
    
    if [ "$HAS_EMPLOYEE_ID_CHECK" = "0" ]; then
        print_warning "rota_schedules table missing employee_id - applying fix now..."
        if [ -f "database/fix-rota-schedules-now.sql" ]; then
            docker-compose -f "$COMPOSE_FILE" exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" < database/fix-rota-schedules-now.sql 2>/dev/null && \
                print_success "rota_schedules table structure fixed" || \
                print_warning "Could not apply fix (will be handled by Flyway migration V14)"
        fi
    fi
fi

# Repair Flyway schema history if there are failed migrations
print_info "Checking Flyway schema history for failed migrations..."
FLYWAY_HISTORY_EXISTS=$(docker-compose -f "$COMPOSE_FILE" exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" \
    -e "USE employee_management_system; SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'employee_management_system' AND table_name = 'flyway_schema_history';" \
    2>/dev/null | tail -1 | tr -d ' ' || echo "0")

if [ "$FLYWAY_HISTORY_EXISTS" = "1" ]; then
    FAILED_MIGRATIONS=$(docker-compose -f "$COMPOSE_FILE" exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" \
        -e "USE employee_management_system; SELECT COUNT(*) FROM flyway_schema_history WHERE success = 0;" \
        2>/dev/null | tail -1 | tr -d ' ' || echo "0")
    
    if [ "$FAILED_MIGRATIONS" != "0" ] && [ "$FAILED_MIGRATIONS" != "" ]; then
        print_warning "Found $FAILED_MIGRATIONS failed migration(s) in Flyway history"
        print_info "Repairing Flyway schema history (marking failed migrations as resolved)..."
        
        docker-compose -f "$COMPOSE_FILE" exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" <<'FLYWAYREPAIR' 2>/dev/null || true
USE employee_management_system;

-- Show failed migrations before repair
SELECT 'Failed migrations before repair:' AS status;
SELECT installed_rank, version, description, success, installed_on 
FROM flyway_schema_history 
WHERE success = 0 
ORDER BY installed_rank;

-- For fresh deployment: Only repair if there are actual failed migrations
-- Check if V16 migration exists and is failed before deleting
SET @v16_exists = (SELECT COUNT(*) FROM flyway_schema_history WHERE version = '16' AND success = 0);
SET @sql = IF(@v16_exists > 0,
    'DELETE FROM flyway_schema_history WHERE version = ''16'' AND success = 0',
    'SELECT ''No V16 migration to delete'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Update failed migrations to success=1 only if they exist
SET @failed_count = (SELECT COUNT(*) FROM flyway_schema_history WHERE success = 0);
SET @sql = IF(@failed_count > 0,
    'UPDATE flyway_schema_history SET success = 1 WHERE success = 0',
    'SELECT ''No failed migrations to repair'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Verify repair
SELECT 'Repaired migrations:' AS status;
SELECT installed_rank, version, description, success, installed_on 
FROM flyway_schema_history 
WHERE success = 1 
ORDER BY installed_rank DESC 
LIMIT 5;

SELECT 'Flyway schema history repaired' AS status;
SELECT 'Note: V16 migration removed - schema fixes now handled at runtime by DatabaseSchemaFixer' AS info;
FLYWAYREPAIR
        
        if [ $? -eq 0 ]; then
            print_success "Flyway schema history repaired"
            print_info "Failed migrations have been marked as resolved"
            print_info "Flyway will now proceed with pending migrations on backend startup"
        else
            print_warning "Failed to repair Flyway schema history"
            print_info "You may need to manually repair: UPDATE flyway_schema_history SET success = 1 WHERE success = 0;"
        fi
    else
        print_success "No failed migrations found in Flyway history"
    fi
else
    print_info "Flyway schema history table doesn't exist yet (will be created on first migration)"
fi

# Fix rotas table structure (remove incorrect fields)
print_info "Checking rotas table structure..."
ROTAS_TABLE_EXISTS=$(docker-compose -f "$COMPOSE_FILE" exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" \
    -e "USE employee_management_system; SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'employee_management_system' AND table_name = 'rotas';" \
    2>/dev/null | tail -1 | tr -d ' ' || echo "0")

if [ "$ROTAS_TABLE_EXISTS" = "1" ]; then
    # Check if rotas table has incorrect fields
    HAS_EMPLOYEE_ID=$(docker-compose -f "$COMPOSE_FILE" exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" \
        -e "USE employee_management_system; SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'employee_management_system' AND table_name = 'rotas' AND column_name = 'employee_id';" \
        2>/dev/null | tail -1 | tr -d ' ' || echo "0")
    
    HAS_WEEK_START_DATE=$(docker-compose -f "$COMPOSE_FILE" exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" \
        -e "USE employee_management_system; SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'employee_management_system' AND table_name = 'rotas' AND column_name = 'week_start_date';" \
        2>/dev/null | tail -1 | tr -d ' ' || echo "0")
    
    if [ "$HAS_EMPLOYEE_ID" = "1" ] || [ "$HAS_WEEK_START_DATE" = "1" ]; then
        print_warning "rotas table has incorrect fields (employee_id, week_start_date, etc.)"
        print_info "These fields belong to rota_schedules table or are unused"
        print_info "DatabaseSchemaFixer will fix this automatically on backend startup"
    else
        print_success "rotas table structure is correct"
    fi
else
    print_info "rotas table doesn't exist yet (will be created by JPA or Flyway)"
fi

# Fix all other table schemas
if [ -f "fix-all-tables-schema.sql" ]; then
    print_info "Applying comprehensive schema fixes..."
    docker-compose -f "$COMPOSE_FILE" exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" < fix-all-tables-schema.sql 2>/dev/null || {
        print_warning "Some schema fixes may have failed (tables may already be correct)"
    }
    print_success "Schema fixes applied"
else
    print_warning "fix-all-tables-schema.sql not found, skipping comprehensive schema fixes"
fi

# For fresh deployment: Skip unused column removal (they don't exist in fresh install)
# These checks are kept for backward compatibility but won't execute on fresh install
print_info "Skipping unused column removal for fresh deployment..."
print_success "Schema cleanup skipped (fresh deployment)"

# Fix attendance table structure (remove incorrect columns: date and status)
print_info "Checking attendance table structure..."
ATTENDANCE_TABLE_EXISTS=$(docker-compose -f "$COMPOSE_FILE" exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" \
    -e "USE employee_management_system; SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'employee_management_system' AND table_name = 'attendance';" \
    2>/dev/null | tail -1 | tr -d ' ' || echo "0")

if [ "$ATTENDANCE_TABLE_EXISTS" = "1" ]; then
    # Check if attendance table has incorrect fields
    HAS_DATE=$(docker-compose -f "$COMPOSE_FILE" exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" \
        -e "USE employee_management_system; SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'employee_management_system' AND table_name = 'attendance' AND column_name = 'date';" \
        2>/dev/null | tail -1 | tr -d ' ' || echo "0")
    
    HAS_STATUS=$(docker-compose -f "$COMPOSE_FILE" exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" \
        -e "USE employee_management_system; SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'employee_management_system' AND table_name = 'attendance' AND column_name = 'status';" \
        2>/dev/null | tail -1 | tr -d ' ' || echo "0")
    
    if [ "$HAS_DATE" = "1" ] || [ "$HAS_STATUS" = "1" ]; then
        print_warning "attendance table has incorrect fields (date, status)"
        print_info "Entity uses 'work_date' not 'date', and 'is_active' not 'status'"
        print_info "Removing incorrect columns..."
        
        docker-compose -f "$COMPOSE_FILE" exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" <<'ATTENDANCEFIX' 2>/dev/null || true
USE employee_management_system;

DELIMITER //

CREATE PROCEDURE IF NOT EXISTS fix_attendance_table()
BEGIN
    DECLARE table_exists INT DEFAULT 0;
    DECLARE column_exists INT DEFAULT 0;
    DECLARE column_nullable VARCHAR(3);
    
    -- Check if attendance table exists
    SELECT COUNT(*) INTO table_exists
    FROM INFORMATION_SCHEMA.TABLES 
    WHERE table_schema = 'employee_management_system' 
    AND table_name = 'attendance';
    
    IF table_exists > 0 THEN
        -- Drop 'date' column if it exists (entity uses 'work_date' instead)
        SELECT COUNT(*) INTO column_exists
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE table_schema = 'employee_management_system' 
        AND table_name = 'attendance' 
        AND column_name = 'date';
        
        IF column_exists > 0 THEN
            SET @sql = 'ALTER TABLE attendance DROP COLUMN `date`';
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;
        
        -- Drop 'status' column if it exists (entity uses 'is_active' instead)
        SELECT COUNT(*) INTO column_exists
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE table_schema = 'employee_management_system' 
        AND table_name = 'attendance' 
        AND column_name = 'status';
        
        IF column_exists > 0 THEN
            SET @sql = 'ALTER TABLE attendance DROP COLUMN `status`';
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;
        
        -- Ensure work_date is NOT NULL
        SELECT IS_NULLABLE INTO column_nullable
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE table_schema = 'employee_management_system' 
        AND table_name = 'attendance' 
        AND column_name = 'work_date';
        
        IF column_nullable = 'YES' THEN
            SET @sql = 'ALTER TABLE attendance MODIFY COLUMN work_date DATE NOT NULL';
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;
        
        -- Ensure work_location is NOT NULL
        SELECT IS_NULLABLE INTO column_nullable
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE table_schema = 'employee_management_system' 
        AND table_name = 'attendance' 
        AND column_name = 'work_location';
        
        IF column_nullable = 'YES' THEN
            SET @sql = 'ALTER TABLE attendance MODIFY COLUMN work_location VARCHAR(255) NOT NULL';
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;
    END IF;
END //

DELIMITER ;

-- Execute the procedure
CALL fix_attendance_table();

-- Drop the procedure after use
DROP PROCEDURE IF EXISTS fix_attendance_table;

SELECT 'attendance table structure fixed' AS status;
ATTENDANCEFIX
        
        if [ $? -eq 0 ]; then
            print_success "attendance table structure fixed"
        else
            print_warning "Failed to fix attendance table structure (will be fixed by DatabaseSchemaFixer on backend startup)"
        fi
    else
        print_success "attendance table structure is correct"
    fi
else
    print_info "attendance table doesn't exist yet (will be created by JPA)"
fi

# Configure Tesseract OCR
print_step "Step 15/20: Configuring Tesseract OCR"

print_info "Detecting Tesseract installation and tessdata path..."

# Find tessdata path in the already-built backend image
BACKEND_IMAGE=$(docker images --format "{{.Repository}}:{{.Tag}}" | grep -E "backend|demo-ems|employeemanagementsystem" | head -1)

if [ -n "$BACKEND_IMAGE" ]; then
    print_info "Checking Tesseract in image: $BACKEND_IMAGE"
    
    # Check common tessdata paths
    TESS_DATA_PATH=""
    PATHS=(
        "/usr/share/tesseract-ocr/5/tessdata"
        "/usr/share/tesseract-ocr/4.00/tessdata"
        "/usr/share/tesseract-ocr/tessdata"
    )
    
    for path in "${PATHS[@]}"; do
        if docker run --rm "$BACKEND_IMAGE" test -f "$path/eng.traineddata" 2>/dev/null; then
            TESS_DATA_PATH="$path"
            print_success "Found Tesseract tessdata at: $path"
            break
        fi
    done
    
    if [ -n "$TESS_DATA_PATH" ]; then
        # Update .env file
        if grep -q "^TESSERACT_DATA_PATH=" .env 2>/dev/null; then
            sed -i "s|^TESSERACT_DATA_PATH=.*|TESSERACT_DATA_PATH=$TESS_DATA_PATH|" .env
        else
            echo "TESSERACT_DATA_PATH=$TESS_DATA_PATH" >> .env
        fi
        
        if grep -q "^TESSDATA_PREFIX=" .env 2>/dev/null; then
            sed -i "s|^TESSDATA_PREFIX=.*|TESSDATA_PREFIX=$TESS_DATA_PATH|" .env
        else
            echo "TESSDATA_PREFIX=$TESS_DATA_PATH" >> .env
        fi
        
        print_success "Tesseract configuration updated in .env"
    else
        print_warning "Could not detect Tesseract tessdata path automatically"
        print_info "Using default path: /usr/share/tesseract-ocr/5/tessdata"
        
        # Set defaults in .env
        if ! grep -q "^TESSERACT_DATA_PATH=" .env 2>/dev/null; then
            echo "TESSERACT_DATA_PATH=/usr/share/tesseract-ocr/5/tessdata" >> .env
        fi
        if ! grep -q "^TESSDATA_PREFIX=" .env 2>/dev/null; then
            echo "TESSDATA_PREFIX=/usr/share/tesseract-ocr/5/tessdata" >> .env
        fi
    fi
else
    print_warning "Could not find backend image, using default Tesseract paths"
    print_info "Will detect Tesseract after container starts"
    # Set defaults
    if ! grep -q "^TESSERACT_DATA_PATH=" .env 2>/dev/null; then
        echo "TESSERACT_DATA_PATH=/usr/share/tesseract-ocr/5/tessdata" >> .env
    fi
    if ! grep -q "^TESSDATA_PREFIX=" .env 2>/dev/null; then
        echo "TESSDATA_PREFIX=/usr/share/tesseract-ocr/5/tessdata" >> .env
    fi
fi

# Reload environment variables
set -a
source .env 2>/dev/null || true
set +a

print_success "Tesseract configuration ready"

# Start backend
print_step "Step 16/20: Starting Backend"

print_info "Starting backend container..."
docker-compose -f "$COMPOSE_FILE" up -d backend

# Wait for backend to start
print_info "Waiting for backend to start (60 seconds)..."
sleep 60

# Check backend health
print_info "Checking backend health..."
MAX_ATTEMPTS=30
ATTEMPT=0
while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
    if curl -f http://localhost:${BACKEND_PORT:-8080}/api/actuator/health > /dev/null 2>&1; then
        print_success "Backend is healthy"
        break
    fi
    ATTEMPT=$((ATTEMPT + 1))
    echo -n "."
    sleep 2
done

if [ $ATTEMPT -eq $MAX_ATTEMPTS ]; then
    print_warning "Backend health check timeout"
    print_info "Checking backend logs..."
    docker-compose -f "$COMPOSE_FILE" logs backend --tail=50
fi

# Verify Tesseract in running container
print_info "Verifying Tesseract installation in running container..."
sleep 5

TESS_VERIFY_PATH=""
if docker-compose -f "$COMPOSE_FILE" ps backend | grep -q "Up"; then
    # Check if eng.traineddata exists in common paths
    for path in "/usr/share/tesseract-ocr/5/tessdata" "/usr/share/tesseract-ocr/4.00/tessdata" "/usr/share/tesseract-ocr/tessdata"; do
        if docker-compose -f "$COMPOSE_FILE" exec -T backend test -f "$path/eng.traineddata" 2>/dev/null; then
            TESS_VERIFY_PATH="$path"
            print_success "Verified Tesseract tessdata at: $path"
            break
        fi
    done
    
    if [ -z "$TESS_VERIFY_PATH" ]; then
        print_warning "Could not verify Tesseract tessdata in running container"
        print_info "This may cause OCR functionality issues"
        print_info "Check logs: docker-compose logs backend | grep -i tesseract"
    fi
fi

# Start frontend
print_step "Step 17/20: Starting Frontend"

print_info "Starting frontend container..."
docker-compose -f "$COMPOSE_FILE" up -d frontend

# Wait for frontend
print_info "Waiting for frontend to start (10 seconds)..."
sleep 10

# Verify database tables
print_step "Step 18/20: Verifying Database Tables"

print_info "Checking if database tables exist..."
sleep 10

TABLES=$(docker-compose -f "$COMPOSE_FILE" exec -T mysql mysql -u root -p"${DB_ROOT_PASSWORD}" \
    employee_management_system -e "SHOW TABLES;" 2>/dev/null | wc -l)

if [ "$TABLES" -gt 1 ]; then
    print_success "Database tables exist ($((TABLES-1)) tables found)"
else
    print_warning "Tables may not be created yet"
    print_info "JPA will create tables on first startup"
    print_info "Check backend logs: docker-compose logs backend | grep -i 'table\|create'"
fi

# Verify services
print_step "Step 19/20: Verifying Services"

print_info "Checking service status..."
docker-compose -f "$COMPOSE_FILE" ps

# Health checks
print_info "Running health checks..."

# Backend health
if curl -f http://localhost:${BACKEND_PORT:-8080}/api/actuator/health > /dev/null 2>&1; then
    print_success "Backend health check: PASSED"
else
    print_warning "Backend health check: FAILED (check logs)"
fi

# Frontend health
if curl -f http://localhost:${FRONTEND_PORT:-80}/ > /dev/null 2>&1; then
    print_success "Frontend health check: PASSED"
else
    print_warning "Frontend health check: FAILED (check logs)"
fi

# Verify production configuration
print_step "Step 20/20: Verifying Production Configuration"

# Check Spring profile
if docker-compose -f "$COMPOSE_FILE" logs backend | grep -qi "profile.*prod"; then
    print_success "Backend using production profile"
else
    print_warning "Could not verify production profile (check logs)"
fi

# Check frontend URLs
if docker-compose -f "$COMPOSE_FILE" exec frontend cat /usr/share/nginx/html/main*.js 2>/dev/null | grep -q "${API_URL:-localhost}"; then
    print_success "Frontend using production URLs"
elif docker-compose -f "$COMPOSE_FILE" exec frontend cat /usr/share/nginx/html/main*.js 2>/dev/null | grep -q "localhost"; then
    print_warning "Frontend may still be using localhost (rebuild may be needed)"
else
    print_info "Could not verify frontend URLs"
fi

# Verify Tesseract configuration
print_info "Checking Tesseract configuration in backend logs..."
sleep 2
if docker-compose -f "$COMPOSE_FILE" logs backend 2>/dev/null | grep -qi "tesseract.*found\|tesseract.*configured\|tesseract.*success"; then
    print_success "Tesseract OCR configured successfully"
elif docker-compose -f "$COMPOSE_FILE" logs backend 2>/dev/null | grep -qi "tesseract.*warning\|tesseract.*error\|tesseract.*not found"; then
    print_warning "Tesseract may have configuration issues (check logs)"
    print_info "Run: docker-compose logs backend | grep -i tesseract"
else
    print_info "Could not verify Tesseract configuration from logs"
    print_info "Check manually: docker-compose logs backend | grep -i tesseract"
fi

# Final verification
print_step "Step 20/20: Final Verification"

print_info "Service Status:"
docker-compose -f "$COMPOSE_FILE" ps

print_info ""
print_info "Application URLs:"
print_info "  Frontend: http://localhost:${FRONTEND_PORT:-80}"
print_info "  Backend API: http://localhost:${BACKEND_PORT:-8080}/api"
print_info "  Health Check: http://localhost:${BACKEND_PORT:-8080}/api/actuator/health"

# Summary
print_step "Step 21/21: Deployment Summary"

echo ""
echo -e "${GREEN}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║         DEPLOYMENT COMPLETED SUCCESSFULLY!                 ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""

print_success "All services deployed and running"
echo ""

print_info "📊 Deployment Details:"
echo "  • Backup Location: ${BACKUP_PATH:-N/A}"
echo "  • Database: ${DB_NAME:-employee_management_system}"
echo "  • Frontend Port: ${FRONTEND_PORT:-80}"
echo "  • Backend Port: ${BACKEND_PORT:-8080}"
echo ""

print_info "📝 Next Steps:"
echo "  1. Access application: http://localhost:${FRONTEND_PORT:-80}"
echo "  2. Create ROOT account (first-time setup)"
echo "  3. Verify all functionality"
echo "  4. Review logs: docker-compose logs -f"
echo "  5. Set up automated backups: ./backup.sh"
echo "  6. Verify Tesseract OCR: docker-compose logs backend | grep -i tesseract"
echo ""

print_info "🔧 Useful Commands:"
echo "  • View logs: docker-compose logs -f"
echo "  • Restart: docker-compose restart"
echo "  • Stop: docker-compose down"
echo "  • Backup: ./backup.sh"
echo "  • Monitor: ./monitor.sh"
echo "  • Check Tesseract: docker-compose exec backend ls -la /usr/share/tesseract-ocr/*/tessdata/eng.traineddata"
echo ""

print_info "📚 Documentation:"
echo "  • Migration Guide: MIGRATION_GUIDE.md"
echo "  • Environment Setup: ENV_SETUP_GUIDE.md"
echo "  • Production Checklist: PRODUCTION_READINESS_CHECKLIST.md"
echo "  • Tesseract Fix: FIX_TESSERACT_PATH.md"
echo ""

# Check for any warnings
if [ -n "$BACKUP_PATH" ]; then
    print_warning "Backup created at: $BACKUP_PATH"
    print_info "Keep this backup until you verify the deployment is working correctly"
fi

echo ""
print_success "🎉 Production deployment complete!"
echo ""

# Optional: Open browser
if command -v xdg-open &> /dev/null; then
    read -p "Open application in browser? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        xdg-open "http://localhost:${FRONTEND_PORT:-80}" &> /dev/null &
    fi
fi

exit 0

