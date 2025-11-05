@echo off
cls
color 0A
echo.
echo ========================================================================
echo    TESSERACT OCR - INSTALLATION REQUIRED
echo ========================================================================
echo.
echo Your Employee Management System is ready, but needs Tesseract OCR
echo to extract text from passport images.
echo.
echo ========================================================================
echo    WHAT YOU NEED TO DO:
echo ========================================================================
echo.
echo 1. Download Tesseract OCR Installer
echo    Opening download page in your browser...
echo.
echo 2. Run the installer (keep default settings)
echo    - Install to: C:\Program Files\Tesseract-OCR
echo    - Make sure "English language data" is checked
echo.
echo 3. After installation, run this command:
echo    mvnw clean install
echo.
echo 4. Then start your application:
echo    mvnw spring-boot:run
echo.
echo ========================================================================
echo.
echo Press any key to open the download page...
pause >nul
start https://github.com/UB-Mannheim/tesseract/wiki
echo.
echo Download link also opening in browser...
timeout /t 2 >nul
start https://digi.bib.uni-mannheim.de/tesseract/tesseract-ocr-w64-setup-5.3.3.20231005.exe
echo.
echo ========================================================================
echo After installing Tesseract, close this window and run:
echo    mvn clean install
echo ========================================================================
echo.
pause

