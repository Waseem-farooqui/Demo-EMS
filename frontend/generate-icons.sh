#!/bin/bash

# Script to generate PWA icons from icon-144x144.png
# Requires ImageMagick (convert command)

SOURCE_IMAGE="src/assets/logo.png"
ICONS_DIR="src/assets/icons"

# Create icons directory if it doesn't exist
mkdir -p "$ICONS_DIR"

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

echo "🖼️  Generating PWA icons from $SOURCE_IMAGE..."

# Generate icons in various sizes
sizes=(72 96 128 144 152 192 384 512)

for size in "${sizes[@]}"; do
    output_file="$ICONS_DIR/icon-${size}x${size}.png"
    convert "$SOURCE_IMAGE" -resize "${size}x${size}" -background none -gravity center -extent "${size}x${size}" "$output_file"
    if [ $? -eq 0 ]; then
        echo "✅ Generated: $output_file"
    else
        echo "❌ Failed to generate: $output_file"
    fi
done

echo "✨ Icon generation complete!"
echo "📁 Icons saved to: $ICONS_DIR"

