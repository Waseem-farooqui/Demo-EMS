# Fix Tesseract Path Warning

## Problem
Getting warning: `WARNING: Tesseract tessdata directory not found at: /usr/share/tesseract-ocr/4.00/tessdata`

## Solution

### Option 1: Use the Automated Fix Script

```bash
chmod +x fix-tesseract-path.sh
sudo ./fix-tesseract-path.sh
```

This script will:
1. Find the correct Tesseract tessdata path in the container
2. Update your `.env` file
3. Restart the backend

### Option 2: Find Path Manually

```bash
# Check Tesseract version
docker exec ems-backend tesseract --version

# Find tessdata directory
docker exec ems-backend find /usr/share -name "tessdata" -type d 2>/dev/null

# Or check common locations
docker exec ems-backend ls -la /usr/share/tesseract-ocr/
```

### Option 3: Update .env File

Once you find the correct path, add it to your `.env`:

```bash
TESSERACT_DATA_PATH=/usr/share/tesseract-ocr/5/tessdata
```

Common paths:
- `/usr/share/tesseract-ocr/5/tessdata` (Tesseract 5.x - most common on Ubuntu/Debian)
- `/usr/share/tesseract-ocr/4.00/tessdata` (Tesseract 4.x)
- `/usr/share/tesseract-ocr/tessdata` (Generic)

Then restart:
```bash
docker-compose restart backend
```

### Option 4: Rebuild Container

The application will auto-detect the correct path. The updated code tries these paths in order:
1. `/usr/share/tesseract-ocr/5/tessdata` (Tesseract 5.x)
2. `/usr/share/tesseract-ocr/4.00/tessdata` (Tesseract 4.x)
3. `/usr/share/tesseract-ocr/tessdata` (Generic)
4. `/usr/local/share/tessdata`
5. `/usr/share/tessdata`

If you rebuild the container, it will automatically find the correct path.

## Verify Fix

After applying the fix, check logs:

```bash
docker-compose logs backend | grep -i tesseract
```

You should see:
```
✓ Tesseract tessdata found at: /usr/share/tesseract-ocr/5/tessdata
✓ Tesseract OCR configured successfully
```

Instead of:
```
⚠️ WARNING: Tesseract tessdata directory not found
```

## Note

The warning doesn't break functionality - the application will auto-detect the path. However, setting it explicitly in `.env` prevents the warning and ensures faster startup.

