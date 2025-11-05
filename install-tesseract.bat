@echo off
echo ========================================
echo Tesseract OCR Installation Script
echo ========================================
echo.
echo This script will help you install Tesseract OCR for the Employee Management System.
echo.
echo STEP 1: Download Tesseract
echo -------------------------
echo Please download Tesseract OCR manually from:
echo https://github.com/UB-Mannheim/tesseract/wiki
echo.
echo Direct download link (Windows 64-bit):
echo https://digi.bib.uni-mannheim.de/tesseract/tesseract-ocr-w64-setup-5.3.3.20231005.exe
echo.
echo STEP 2: Install Tesseract
echo -------------------------
echo 1. Run the downloaded installer
echo 2. Use default installation path: C:\Program Files\Tesseract-OCR
echo 3. Make sure "English language data" is selected
echo 4. Complete the installation
echo.
echo STEP 3: Verify Installation
echo -------------------------
echo After installation, run this command to verify:
echo    "C:\Program Files\Tesseract-OCR\tesseract.exe" --version
echo.
echo STEP 4: Rebuild and Run
echo -------------------------
echo After Tesseract is installed, rebuild the project:
echo    mvnw clean install
echo    mvnw spring-boot:run
echo.
echo ========================================
echo Press any key to open the download page in your browser...
pause >nul
start https://github.com/UB-Mannheim/tesseract/wiki

