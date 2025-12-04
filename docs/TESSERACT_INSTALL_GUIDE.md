## Tesseract OCR Installation Guide (Windows 10/11)

### 1. Prerequisites
- Windows 10 or 11 with administrator rights
- PowerShell 7+ (optional but recommended)
- Latest `winget` (Windows Package Manager). Run `winget --version` to verify; update via Microsoft Store if missing.

### 2. Fastest Method: `winget`
1. Open PowerShell **as Administrator**.
2. Install the UB Mannheim build (includes latest language packs):
   ```powershell
   winget install -e --id UB-Mannheim.TesseractOCR
   ```
3. Default install path: `C:\Program Files\Tesseract-OCR\`

### 3. Alternative Methods
#### Chocolatey
```powershell
choco install tesseract --version=5.3.4
choco install tesseract-lang -y  # optional extra languages
```

#### Manual Installer
1. Download `tesseract-ocr-w64-setup-5.x.x.exe` from https://github.com/UB-Mannheim/tesseract/wiki
2. Run installer, choose **Add to PATH** and desired language packs.

### 4. Post-Install Verification
```powershell
tesseract --version
tesseract --list-langs
```
Expect to see `eng` plus any other languages you selected.

### 5. Configure Environment Variables
Tesseract needs `TESSDATA_PREFIX` so Java/Tess4J can find language files.

```powershell
$tessPath = "C:\Program Files\Tesseract-OCR\tessdata"
[System.Environment]::SetEnvironmentVariable("TESSDATA_PREFIX", $tessPath, "User")
[System.Environment]::SetEnvironmentVariable("TESSERACT_DATA_PATH", $tessPath, "User")
```

Log out/in (or restart your IDE) to refresh environment variables.

### 6. Application Configuration
Update `application.properties` (already template-ready):
```
ocr.tesseract.datapath=${TESSERACT_DATA_PATH:${user.home}/AppData/Local/Programs/Tesseract-OCR/tessdata}
```
If the app still cannot locate the language files, explicitly set:
```powershell
$env:TESSERACT_DATA_PATH = "C:\Program Files\Tesseract-OCR\tessdata"
$env:TESSDATA_PREFIX = $env:TESSERACT_DATA_PATH
```
Then restart the backend (`mvn spring-boot:run` or your IDE).

### 7. Troubleshooting
| Symptom | Fix |
|---------|-----|
| `eng.traineddata not found` | Ensure `TESSDATA_PREFIX` points to the `tessdata` folder and that `eng.traineddata` exists there. |
| `Invalid memory access` | Usually happens when language files are missing or corrupted. Reinstall Tesseract or re-copy `eng.traineddata`. |
| Tomcat temp files not deleted | Make sure upload streams are closed (already handled in code). If it persists, restart the backend to clear handles. |
| OCR returns empty text | Increase scan DPI, ensure the file is legible, or enable cloud OCR fallback via `ocr.cloud.enabled=true`. |

### 8. Optional Language Packs
Download additional `.traineddata` files from https://github.com/tesseract-ocr/tessdata, place them into `tessdata`, and re-run `tesseract --list-langs` to confirm.

---
**Summary**: Install via `winget`, set `TESSDATA_PREFIX`, verify with `tesseract --version`, then restart the backend so Spring picks up the environment variables. ***

