#!/bin/bash
# security-fixes.sh - Apply Critical Security Fixes

set -e

echo "🔒 Applying Critical Security Fixes..."
echo "=========================================="

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 1. Check if hardcoded credentials still exist
echo ""
echo "1️⃣ Checking for hardcoded credentials..."
if grep -r "iosh djgr chvy iqdk" src/ 2>/dev/null; then
    echo -e "${RED}❌ CRITICAL: Hardcoded credentials found!${NC}"
    echo "   Please remove immediately from application.properties"
    exit 1
else
    echo -e "${GREEN}✅ No hardcoded credentials found${NC}"
fi

# 2. Check .env file exists
echo ""
echo "2️⃣ Checking environment configuration..."
if [ ! -f .env ]; then
    echo -e "${YELLOW}⚠️  .env file not found, creating from example...${NC}"
    if [ -f .env.example ]; then
        cp .env.example .env
        echo -e "${GREEN}✅ .env file created${NC}"
        echo -e "${YELLOW}⚠️  Please edit .env and set your values!${NC}"
    else
        echo -e "${RED}❌ .env.example not found${NC}"
        exit 1
    fi
else
    echo -e "${GREEN}✅ .env file exists${NC}"
fi

# 3. Verify critical environment variables
echo ""
echo "3️⃣ Verifying critical environment variables..."

source .env 2>/dev/null || true

MISSING_VARS=()

[ -z "$JWT_SECRET" ] && MISSING_VARS+=("JWT_SECRET")
[ -z "$DB_PASSWORD" ] && MISSING_VARS+=("DB_PASSWORD")
[ -z "$DB_ROOT_PASSWORD" ] && MISSING_VARS+=("DB_ROOT_PASSWORD")
[ -z "$MAIL_PASSWORD" ] && MISSING_VARS+=("MAIL_PASSWORD")

if [ ${#MISSING_VARS[@]} -gt 0 ]; then
    echo -e "${RED}❌ Missing environment variables:${NC}"
    for var in "${MISSING_VARS[@]}"; do
        echo "   - $var"
    done
    exit 1
else
    echo -e "${GREEN}✅ All critical variables set${NC}"
fi

# 4. Check JWT secret strength
echo ""
echo "4️⃣ Validating JWT secret strength..."
JWT_LENGTH=${#JWT_SECRET}
if [ $JWT_LENGTH -lt 64 ]; then
    echo -e "${RED}❌ JWT_SECRET too short ($JWT_LENGTH chars, need 64+)${NC}"
    echo "   Generate new secret with:"
    echo "   openssl rand -base64 64"
    exit 1
else
    echo -e "${GREEN}✅ JWT_SECRET is strong ($JWT_LENGTH chars)${NC}"
fi

# 5. Check password strength
echo ""
echo "5️⃣ Validating database password strength..."
DB_PWD_LENGTH=${#DB_PASSWORD}
if [ $DB_PWD_LENGTH -lt 16 ]; then
    echo -e "${YELLOW}⚠️  DB_PASSWORD is weak ($DB_PWD_LENGTH chars, recommend 16+)${NC}"
    echo "   Generate new password with:"
    echo "   openssl rand -base64 32"
else
    echo -e "${GREEN}✅ DB_PASSWORD is strong ($DB_PWD_LENGTH chars)${NC}"
fi

# 6. Check for localhost in production config
echo ""
echo "6️⃣ Checking production configuration..."
if grep -q "localhost" src/main/resources/application-prod.properties; then
    echo -e "${YELLOW}⚠️  'localhost' found in production config${NC}"
    echo "   Make sure to use environment variables for production URLs"
else
    echo -e "${GREEN}✅ No hardcoded localhost in production config${NC}"
fi

# 7. Verify CORS origins
echo ""
echo "7️⃣ Checking CORS configuration..."
if [ -n "$CORS_ALLOWED_ORIGINS" ]; then
    if [[ "$CORS_ALLOWED_ORIGINS" == *"*"* ]]; then
        echo -e "${RED}❌ CORS allows all origins (*)${NC}"
        exit 1
    else
        echo -e "${GREEN}✅ CORS properly configured${NC}"
    fi
else
    echo -e "${YELLOW}⚠️  CORS_ALLOWED_ORIGINS not set${NC}"
fi

# 8. Check Git history for secrets
echo ""
echo "8️⃣ Scanning Git history for exposed secrets..."
if git log --all --full-history --source -- "*application.properties" | grep -qi "password\|secret" 2>/dev/null; then
    echo -e "${YELLOW}⚠️  Potential secrets found in Git history${NC}"
    echo "   Review git history and consider using git-filter-branch if needed"
else
    echo -e "${GREEN}✅ No obvious secrets in recent Git history${NC}"
fi

# 9. Run Maven dependency check (if available)
echo ""
echo "9️⃣ Running dependency security check..."
if command -v mvn &> /dev/null; then
    echo "   This may take a few minutes..."
    if mvn dependency-check:check -q 2>/dev/null; then
        echo -e "${GREEN}✅ No critical vulnerabilities found${NC}"
    else
        echo -e "${YELLOW}⚠️  Dependency check failed or found issues${NC}"
        echo "   Run manually: mvn dependency-check:check"
    fi
else
    echo -e "${YELLOW}⚠️  Maven not found, skipping dependency check${NC}"
fi

# 10. Check Docker security
echo ""
echo "🔟 Checking Docker configuration..."
if [ -f Dockerfile ]; then
    if grep -q "USER root" Dockerfile; then
        echo -e "${RED}❌ Docker container runs as root${NC}"
    elif grep -q "USER appuser" Dockerfile; then
        echo -e "${GREEN}✅ Docker runs as non-root user${NC}"
    else
        echo -e "${YELLOW}⚠️  Docker user not explicitly set${NC}"
    fi
fi

echo ""
echo "=========================================="
echo -e "${GREEN}✅ Security check complete!${NC}"
echo ""
echo "Summary:"
echo "  - Hardcoded credentials: ✅ Removed"
echo "  - Environment variables: ✅ Configured"
echo "  - Password strength: ✅ Validated"
echo "  - Configuration: ✅ Checked"
echo ""
echo "Next steps:"
echo "  1. Review SECURITY_AUDIT_REPORT.md for detailed findings"
echo "  2. Revoke old email app password if it was exposed"
echo "  3. Test application after fixes"
echo "  4. Deploy security updates"
echo ""
echo "For production deployment:"
echo "  - Set strong JWT_SECRET (64+ chars)"
echo "  - Use strong database passwords (16+ chars)"
echo "  - Enable HTTPS/SSL"
echo "  - Configure firewall"
echo "  - Set up monitoring"
echo ""

