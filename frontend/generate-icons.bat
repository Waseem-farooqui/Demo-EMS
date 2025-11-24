@echo off
REM Script to generate PWA icons from logo.png
REM Requires ImageMagick (convert command)

set SOURCE_IMAGE=src\assets\logo.png
set ICONS_DIR=src\assets\icons

REM Create icons directory if it doesn't exist
if not exist "%ICONS_DIR%" mkdir "%ICONS_DIR%"

REM Check if ImageMagick is installed
where convert >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ❌ ImageMagick not found. Please install it first:
    echo    Download from https://imagemagick.org/script/download.php
    exit /b 1
)

REM Check if source image exists
if not exist "%SOURCE_IMAGE%" (
    echo ❌ Source image not found: %SOURCE_IMAGE%
    exit /b 1
)

echo 🖼️  Generating PWA icons from %SOURCE_IMAGE%...

REM Generate icons in various sizes
convert "%SOURCE_IMAGE%" -resize 72x72 -background none -gravity center -extent 72x72 "%ICONS_DIR%\icon-72x72.png"
convert "%SOURCE_IMAGE%" -resize 96x96 -background none -gravity center -extent 96x96 "%ICONS_DIR%\icon-96x96.png"
convert "%SOURCE_IMAGE%" -resize 128x128 -background none -gravity center -extent 128x128 "%ICONS_DIR%\icon-128x128.png"
convert "%SOURCE_IMAGE%" -resize 144x144 -background none -gravity center -extent 144x144 "%ICONS_DIR%\icon-144x144.png"
convert "%SOURCE_IMAGE%" -resize 152x152 -background none -gravity center -extent 152x152 "%ICONS_DIR%\icon-152x152.png"
convert "%SOURCE_IMAGE%" -resize 192x192 -background none -gravity center -extent 192x192 "%ICONS_DIR%\icon-192x192.png"
convert "%SOURCE_IMAGE%" -resize 384x384 -background none -gravity center -extent 384x384 "%ICONS_DIR%\icon-384x384.png"
convert "%SOURCE_IMAGE%" -resize 512x512 -background none -gravity center -extent 512x512 "%ICONS_DIR%\icon-512x512.png"

echo ✨ Icon generation complete!
echo 📁 Icons saved to: %ICONS_DIR%

