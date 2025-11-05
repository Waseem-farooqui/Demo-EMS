@echo off
echo ============================================
echo  Local OCR Setup - Tesseract Installation
echo ============================================
echo.

echo Step 1: Downloading Tesseract OCR...
echo.
echo Opening download page in your browser...
start https://github.com/UB-Mannheim/tesseract/wiki
echo.
echo Please download: tesseract-ocr-w64-setup-5.3.3.20231005.exe
echo Direct link: https://digi.bib.uni-mannheim.de/tesseract/tesseract-ocr-w64-setup-5.3.3.20231005.exe
echo.
pause

echo.
echo Step 2: Install Tesseract
echo.
echo IMPORTANT: During installation:
echo   - Use default path: C:\Program Files\Tesseract-OCR
echo   - Select "Additional language data" if needed
echo.
echo Press any key when installation is complete...
pause

echo.
echo Step 3: Verifying installation...
echo.
"C:\Program Files\Tesseract-OCR\tesseract.exe" --version
if %errorlevel% == 0 (
    echo.
    echo [SUCCESS] Tesseract installed successfully!
    echo.
) else (
    echo.
    echo [ERROR] Tesseract not found at C:\Program Files\Tesseract-OCR
    echo Please install Tesseract manually.
    echo.
    pause
    exit /b 1
)

echo Step 4: Rebuilding application...
echo.
call mvnw.cmd clean package -DskipTests
if %errorlevel% == 0 (
    echo.
    echo [SUCCESS] Application rebuilt successfully!
    echo.
) else (
    echo.
    echo [ERROR] Build failed. Please check the output above.
    echo.
    pause
    exit /b 1
)

echo.
echo ============================================
echo  Installation Complete!
echo ============================================
echo.
echo Your application is now ready to use LOCAL OCR.
echo.
echo To run:
echo   java -jar target\employee-management-system-0.0.1-SNAPSHOT.jar
echo.
echo Check logs for:
echo   "Local OCR (Tesseract) initialized successfully"
echo.
pause

