#!/bin/bash
# monitor.sh - Health Monitoring Script for Employee Management System

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo "🏥 Employee Management System - Health Monitor"
echo "=============================================="
echo "Timestamp: $(date)"
echo ""

# Function to check service
check_service() {
    local service=$1
    local check_cmd=$2

    if eval "$check_cmd" > /dev/null 2>&1; then
        echo -e "${GREEN}✅ $service: Healthy${NC}"
        return 0
    else
        echo -e "${RED}❌ $service: Unhealthy${NC}"
        return 1
    fi
}

# 1. Check Docker Services
echo "🐳 Docker Services:"
echo "-------------------"
docker-compose ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"
echo ""

# 2. Check MySQL
echo "💾 Database Health:"
echo "-------------------"
if check_service "MySQL Connection" "docker-compose exec -T mysql mysqladmin ping -h localhost -u root -p${DB_ROOT_PASSWORD} --silent"; then
    # Get database stats
    DB_SIZE=$(docker-compose exec -T mysql mysql -u root -p${DB_ROOT_PASSWORD} -e "SELECT table_schema AS 'Database', ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) AS 'Size (MB)' FROM information_schema.tables WHERE table_schema = 'employee_management_system' GROUP BY table_schema;" 2>/dev/null | tail -n 1)
    echo "  Database Size: $DB_SIZE"

    # Count records
    EMPLOYEES=$(docker-compose exec -T mysql mysql -u root -p${DB_ROOT_PASSWORD} -D employee_management_system -e "SELECT COUNT(*) FROM employees;" 2>/dev/null | tail -n 1)
    USERS=$(docker-compose exec -T mysql mysql -u root -p${DB_ROOT_PASSWORD} -D employee_management_system -e "SELECT COUNT(*) FROM users;" 2>/dev/null | tail -n 1)
    DOCUMENTS=$(docker-compose exec -T mysql mysql -u root -p${DB_ROOT_PASSWORD} -D employee_management_system -e "SELECT COUNT(*) FROM documents;" 2>/dev/null | tail -n 1)

    echo "  Employees: $EMPLOYEES"
    echo "  Users: $USERS"
    echo "  Documents: $DOCUMENTS"
fi
echo ""

# 3. Check Backend
echo "🚀 Backend Health:"
echo "-------------------"
if check_service "Backend API" "curl -f http://localhost:8080/api/actuator/health"; then
    # Get actuator health details
    HEALTH_JSON=$(curl -s http://localhost:8080/api/actuator/health 2>/dev/null)
    echo "  Status: $(echo $HEALTH_JSON | grep -o '"status":"[^"]*"' | cut -d'"' -f4)"

    # Get JVM memory info
    if curl -s http://localhost:8080/api/actuator/health > /dev/null 2>&1; then
        CONTAINER_STATS=$(docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}" ems-backend 2>/dev/null)
        echo "  $CONTAINER_STATS"
    fi
fi
echo ""

# 4. Check Frontend
echo "🌐 Frontend Health:"
echo "-------------------"
if check_service "Frontend" "curl -f http://localhost:80/"; then
    RESPONSE_TIME=$(curl -o /dev/null -s -w '%{time_total}' http://localhost:80/ 2>/dev/null)
    echo "  Response Time: ${RESPONSE_TIME}s"

    CONTAINER_STATS=$(docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}" ems-frontend 2>/dev/null)
    echo "  $CONTAINER_STATS"
fi
echo ""

# 5. Check Disk Usage
echo "💾 Disk Usage:"
echo "-------------------"
df -h | grep -E '^/dev/' | awk '{printf "  %s: %s used of %s (%s)\n", $1, $3, $2, $5}'
echo ""

# 6. Check Docker Volumes
echo "📦 Volume Usage:"
echo "-------------------"
docker system df --format "table {{.Type}}\t{{.TotalCount}}\t{{.Size}}\t{{.Reclaimable}}" | grep -i "volume"
echo ""

# Check specific volumes
echo "  Named Volumes:"
docker volume ls --format "table {{.Name}}\t{{.Driver}}\t{{.Mountpoint}}" | grep ems
echo ""

# 7. Check Network
echo "🌍 Network Status:"
echo "-------------------"
docker network inspect employeemanagementsystem_ems-network --format '  Network: {{.Name}}  Driver: {{.Driver}}  Containers: {{len .Containers}}' 2>/dev/null
echo ""

# 8. Check Logs for Errors
echo "📋 Recent Errors:"
echo "-------------------"
BACKEND_ERRORS=$(docker-compose logs --tail=100 backend 2>/dev/null | grep -i "error\|exception\|failed" | tail -5)
if [ -n "$BACKEND_ERRORS" ]; then
    echo -e "${YELLOW}  Backend (last 5):${NC}"
    echo "$BACKEND_ERRORS" | sed 's/^/    /'
else
    echo -e "${GREEN}  No recent backend errors${NC}"
fi

FRONTEND_ERRORS=$(docker-compose logs --tail=100 frontend 2>/dev/null | grep -i "error" | tail -5)
if [ -n "$FRONTEND_ERRORS" ]; then
    echo -e "${YELLOW}  Frontend (last 5):${NC}"
    echo "$FRONTEND_ERRORS" | sed 's/^/    /'
else
    echo -e "${GREEN}  No recent frontend errors${NC}"
fi
echo ""

# 9. Resource Usage Summary
echo "📊 Resource Usage Summary:"
echo "-------------------"
docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}\t{{.BlockIO}}"
echo ""

# 10. Security Checks
echo "🔒 Security Status:"
echo "-------------------"

# Check if running as root
ROOT_CONTAINERS=$(docker-compose ps -q | xargs -I {} docker inspect {} --format '{{.Config.User}}' | grep -c "^$")
if [ "$ROOT_CONTAINERS" -gt 0 ]; then
    echo -e "${YELLOW}  ⚠️  Warning: $ROOT_CONTAINERS container(s) running as root${NC}"
else
    echo -e "${GREEN}  ✅ No containers running as root${NC}"
fi

# Check exposed ports
echo "  Exposed Ports:"
docker-compose ps --format "table {{.Name}}\t{{.Ports}}" | tail -n +2 | sed 's/^/    /'
echo ""

# 11. Overall Health Score
echo "📈 Overall Health Score:"
echo "-------------------"
TOTAL_CHECKS=3
PASSED_CHECKS=0

# MySQL check
docker-compose exec -T mysql mysqladmin ping -h localhost -u root -p${DB_ROOT_PASSWORD} --silent > /dev/null 2>&1 && ((PASSED_CHECKS++))

# Backend check
curl -f http://localhost:8080/api/actuator/health > /dev/null 2>&1 && ((PASSED_CHECKS++))

# Frontend check
curl -f http://localhost:80/ > /dev/null 2>&1 && ((PASSED_CHECKS++))

HEALTH_PERCENTAGE=$((PASSED_CHECKS * 100 / TOTAL_CHECKS))

if [ $HEALTH_PERCENTAGE -eq 100 ]; then
    echo -e "  ${GREEN}✅ $HEALTH_PERCENTAGE% - All systems operational${NC}"
elif [ $HEALTH_PERCENTAGE -ge 66 ]; then
    echo -e "  ${YELLOW}⚠️  $HEALTH_PERCENTAGE% - Some issues detected${NC}"
else
    echo -e "  ${RED}❌ $HEALTH_PERCENTAGE% - Critical issues detected${NC}"
fi

echo ""
echo "=============================================="
echo "Monitoring completed at $(date)"
echo ""
echo "For detailed logs, run:"
echo "  docker-compose logs -f [service_name]"
echo ""
echo "For real-time stats, run:"
echo "  docker stats"

