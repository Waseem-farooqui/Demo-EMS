#!/bin/bash

# Script to generate favicon.ico from icon-144x144.png
# Requires ImageMagick (convert command)

SOURCE_IMAGE="src/assets/logo.png"
OUTPUT_FAVICON="src/favicon.ico"

# Check if ImageMagick is installed
if ! command -v convert &> /dev/null; then
    echo "❌ ImageMagick not found. Please install it first:"
    echo "   Ubuntu/Debian: sudo apt-get install imagemagick"
    echo "   macOS: brew install imagemagick"
    echo "   Windows: Download from https://imagemagick.org/script/download.php"
    exit 1
fi

# Check if source image exists
if [ ! -f "$SOURCE_IMAGE" ]; then
    echo "❌ Source image not found: $SOURCE_IMAGE"
    exit 1
fi

echo "🖼️  Generating favicon.ico from $SOURCE_IMAGE..."

# Generate favicon.ico with multiple sizes (16x16, 32x32, 48x48)
convert "$SOURCE_IMAGE" \
    \( -clone 0 -resize 16x16 \) \
    \( -clone 0 -resize 32x32 \) \
    \( -clone 0 -resize 48x48 \) \
    -delete 0 \
    -alpha on \
    -colors 256 \
    "$OUTPUT_FAVICON"

if [ $? -eq 0 ]; then
    echo "✅ Generated favicon.ico: $OUTPUT_FAVICON"
    echo "✨ Favicon generation complete!"
else
    echo "❌ Failed to generate favicon.ico"
    exit 1
fi


