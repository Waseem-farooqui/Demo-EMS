@echo off
REM Script to generate favicon.ico from logo.png
REM Requires ImageMagick (convert command)

set SOURCE_IMAGE=src\assets\logo.png
set OUTPUT_FAVICON=src\favicon.ico

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

echo 🖼️  Generating favicon.ico from %SOURCE_IMAGE%...

REM Generate favicon.ico with multiple sizes (16x16, 32x32, 48x48)
convert "%SOURCE_IMAGE%" ^
    ( -clone 0 -resize 16x16 ) ^
    ( -clone 0 -resize 32x32 ) ^
    ( -clone 0 -resize 48x48 ) ^
    -delete 0 ^
    -alpha on ^
    -colors 256 ^
    "%OUTPUT_FAVICON%"

if %ERRORLEVEL% EQU 0 (
    echo ✅ Generated favicon.ico: %OUTPUT_FAVICON%
    echo ✨ Favicon generation complete!
) else (
    echo ❌ Failed to generate favicon.ico
    exit /b 1
)


