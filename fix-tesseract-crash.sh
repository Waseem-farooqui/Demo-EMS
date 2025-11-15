#!/bin/bash
# Fix Tesseract Crash - Find correct path and set environment variables

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}=== Fixing Tesseract Crash ===${NC}"
echo ""

# Check if container is running
if ! docker ps | grep -q ems-backend; then
    echo -e "${RED}ERROR: ems-backend container is not running${NC}"
    exit 1
fi

echo -e "${YELLOW}Finding Tesseract installation and language data...${NC}"

# Find Tesseract version
TESS_VERSION=$(docker exec ems-backend tesseract --version 2>/dev/null | head -1 || echo "")
echo "Tesseract version: $TESS_VERSION"

# Find tessdata directory with eng.traineddata
TESS_DATA_PATH=""
PATHS=(
    "/usr/share/tesseract-ocr/5/tessdata"
    "/usr/share/tesseract-ocr/4.00/tessdata"
    "/usr/share/tesseract-ocr/tessdata"
    "/usr/local/share/tessdata"
    "/usr/share/tessdata"
)

for path in "${PATHS[@]}"; do
    if docker exec ems-backend test -f "$path/eng.traineddata" 2>/dev/null; then
        TESS_DATA_PATH="$path"
        echo -e "${GREEN}✓ Found eng.traineddata at: $path${NC}"
        break
    fi
done

if [ -z "$TESS_DATA_PATH" ]; then
    echo -e "${RED}✗ Could not find eng.traineddata${NC}"
    echo ""
    echo "Checking Tesseract installation..."
    docker exec ems-backend find /usr/share -name "eng.traineddata" 2>/dev/null || echo "Not found"
    echo ""
    echo "Listing /usr/share/tesseract-ocr:"
    docker exec ems-backend ls -la /usr/share/tesseract-ocr/ 2>/dev/null || echo "Directory not found"
    echo ""
    echo -e "${YELLOW}Trying to reinstall tesseract-ocr-eng...${NC}"
    docker exec -u root ems-backend apt-get update && \
    docker exec -u root ems-backend apt-get install -y --reinstall tesseract-ocr-eng || true
    exit 1
fi

echo ""
echo -e "${YELLOW}Updating .env file...${NC}"

# Update .env file
if [ -f .env ]; then
    # Update TESSERACT_DATA_PATH
    if grep -q "^TESSERACT_DATA_PATH=" .env; then
        sed -i "s|^TESSERACT_DATA_PATH=.*|TESSERACT_DATA_PATH=$TESS_DATA_PATH|" .env
        echo -e "${GREEN}✓ Updated TESSERACT_DATA_PATH in .env${NC}"
    else
        echo "TESSERACT_DATA_PATH=$TESS_DATA_PATH" >> .env
        echo -e "${GREEN}✓ Added TESSERACT_DATA_PATH to .env${NC}"
    fi
    
    # Update TESSDATA_PREFIX
    if grep -q "^TESSDATA_PREFIX=" .env; then
        sed -i "s|^TESSDATA_PREFIX=.*|TESSDATA_PREFIX=$TESS_DATA_PATH|" .env
        echo -e "${GREEN}✓ Updated TESSDATA_PREFIX in .env${NC}"
    else
        echo "TESSDATA_PREFIX=$TESS_DATA_PATH" >> .env
        echo -e "${GREEN}✓ Added TESSDATA_PREFIX to .env${NC}"
    fi
else
    echo -e "${YELLOW}⚠ .env file not found, creating it...${NC}"
    cat > .env <<EOF
TESSERACT_DATA_PATH=$TESS_DATA_PATH
TESSDATA_PREFIX=$TESS_DATA_PATH
EOF
    echo -e "${GREEN}✓ Created .env with Tesseract paths${NC}"
fi

echo ""
echo -e "${YELLOW}Rebuilding and restarting backend...${NC}"

# Stop backend
docker-compose stop backend
docker-compose rm -f backend

# Rebuild backend to pick up new environment variables
docker-compose build backend

# Start backend
docker-compose up -d backend

echo ""
echo -e "${GREEN}✅ Done!${NC}"
echo ""
echo "Tesseract paths set to: $TESS_DATA_PATH"
echo "Waiting for backend to start..."
sleep 10

echo ""
echo "Check logs:"
echo "  docker-compose logs backend | grep -i tesseract"
echo ""
echo "If you still see errors, check:"
echo "  docker exec ems-backend ls -la $TESS_DATA_PATH/"

