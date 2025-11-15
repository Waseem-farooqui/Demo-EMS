#!/bin/bash
# Fix Tesseract Path - Find and verify correct tessdata location

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}=== Fixing Tesseract Path ===${NC}"
echo ""

# Check if container is running
if ! docker ps | grep -q ems-backend; then
    echo -e "${RED}ERROR: ems-backend container is not running${NC}"
    exit 1
fi

echo -e "${YELLOW}Finding Tesseract installation in container...${NC}"

# Find Tesseract version and path
TESS_VERSION=$(docker exec ems-backend tesseract --version 2>/dev/null | head -1 | grep -oP '\d+\.\d+' | head -1 || echo "")
TESS_DATA_PATH=""

# Check common paths
PATHS=(
    "/usr/share/tesseract-ocr/5/tessdata"
    "/usr/share/tesseract-ocr/4.00/tessdata"
    "/usr/share/tesseract-ocr/tessdata"
    "/usr/local/share/tessdata"
    "/usr/share/tessdata"
)

for path in "${PATHS[@]}"; do
    if docker exec ems-backend test -d "$path" 2>/dev/null; then
        TESS_DATA_PATH="$path"
        echo -e "${GREEN}✓ Found tessdata at: $path${NC}"
        break
    fi
done

if [ -z "$TESS_DATA_PATH" ]; then
    echo -e "${RED}✗ Could not find tessdata directory${NC}"
    echo ""
    echo "Checking Tesseract installation..."
    docker exec ems-backend tesseract --version 2>/dev/null || echo "Tesseract not found"
    echo ""
    echo "Listing /usr/share/tesseract-ocr:"
    docker exec ems-backend ls -la /usr/share/tesseract-ocr/ 2>/dev/null || echo "Directory not found"
    exit 1
fi

echo ""
echo -e "${YELLOW}Updating .env file...${NC}"

# Update .env file
if [ -f .env ]; then
    if grep -q "^TESSERACT_DATA_PATH=" .env; then
        sed -i "s|^TESSERACT_DATA_PATH=.*|TESSERACT_DATA_PATH=$TESS_DATA_PATH|" .env
        echo -e "${GREEN}✓ Updated TESSERACT_DATA_PATH in .env${NC}"
    else
        echo "TESSERACT_DATA_PATH=$TESS_DATA_PATH" >> .env
        echo -e "${GREEN}✓ Added TESSERACT_DATA_PATH to .env${NC}"
    fi
else
    echo -e "${YELLOW}⚠ .env file not found, creating it...${NC}"
    echo "TESSERACT_DATA_PATH=$TESS_DATA_PATH" > .env
    echo -e "${GREEN}✓ Created .env with TESSERACT_DATA_PATH${NC}"
fi

echo ""
echo -e "${YELLOW}Restarting backend to apply changes...${NC}"
docker-compose restart backend

echo ""
echo -e "${GREEN}✅ Done!${NC}"
echo ""
echo "Tesseract path set to: $TESS_DATA_PATH"
echo "Check logs: docker-compose logs backend | grep -i tesseract"

