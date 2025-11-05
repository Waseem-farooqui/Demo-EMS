# Tesseract OCR Setup Guide

## Why Tesseract?

Apache Tika (the previous OCR solution) **does not perform OCR on images**. It only extracts text from PDFs and documents that already contain embedded text. For actual image-based OCR (like passport photos), we need **Tesseract OCR**.

## Installation Steps (Windows)

### 1. Download Tesseract OCR

Download the Windows installer from:
https://github.com/UB-Mannheim/tesseract/wiki

**Recommended:** Download `tesseract-ocr-w64-setup-5.3.3.20231005.exe` (64-bit) or the latest version.

### 2. Install Tesseract

1. Run the installer
2. **Important:** During installation, note the installation path (default: `C:\Program Files\Tesseract-OCR`)
3. Make sure to install the **English language data** (it's selected by default)
4. Complete the installation

### 3. Verify Installation

Open Command Prompt and run:
```cmd
"C:\Program Files\Tesseract-OCR\tesseract.exe" --version
```

You should see version information like:
```
tesseract 5.3.3
```

### 4. Add to System PATH (Optional but Recommended)

1. Open System Properties → Environment Variables
2. Under System Variables, find `Path`
3. Add: `C:\Program Files\Tesseract-OCR`
4. Click OK to save

After adding to PATH, you can simply run:
```cmd
tesseract --version
```

## What Was Changed

### 1. Added Tesseract Dependency to `pom.xml`

```xml
<!-- Tesseract OCR for image text extraction -->
<dependency>
    <groupId>net.sourceforge.tess4j</groupId>
    <artifactId>tess4j</artifactId>
    <version>5.9.0</version>
</dependency>
```

### 2. Updated `OcrService.java`

- Added Tesseract initialization in constructor
- Created `extractTextFromImage()` method for image files
- Modified `extractTextFromDocument()` to detect file type:
  - **Images** (jpg, png, etc.) → Use Tesseract OCR
  - **PDFs and other documents** → Use Tika

## Rebuild and Run

After installing Tesseract, rebuild the project:

```cmd
cd C:\Users\waseem.uddin\EmployeeManagementSystem
mvnw clean install
mvnw spring-boot:run
```

## Testing

Upload the Pakistani passport image again. You should now see:

```
INFO: Extracting text from document: passport.jpg (type: image/jpeg)
INFO: Performing OCR on image: passport.jpg
INFO: ✓ OCR complete - 450 characters extracted
INFO: ✓ Passport number extracted: M2748170
INFO: ✓ Nationality detected by keyword: Pakistani
```

## Troubleshooting

### Error: "Tesseract OCR failed"

**Cause:** Tesseract is not installed or not found in the expected location.

**Solution:**
1. Verify Tesseract is installed at `C:\Program Files\Tesseract-OCR`
2. If installed elsewhere, update the path in `OcrService.java` constructor:
```java
tesseract.setDatapath("YOUR_TESSERACT_PATH\\tessdata");
```

### Error: "tessdata not found"

**Cause:** Language data files are missing.

**Solution:**
1. Check that `C:\Program Files\Tesseract-OCR\tessdata` exists
2. It should contain `eng.traineddata` file
3. If missing, download from: https://github.com/tesseract-ocr/tessdata

### Low OCR Accuracy

**Solutions:**
- Ensure the image is clear and high-resolution
- Try preprocessing the image (increase contrast, remove noise)
- For multi-language documents, add more language packs

## Supported Languages

Current setup uses English only (`eng`). To add more languages:

1. Download language data from: https://github.com/tesseract-ocr/tessdata
2. Place `.traineddata` files in `C:\Program Files\Tesseract-OCR\tessdata`
3. Update OcrService to use multiple languages:
```java
tesseract.setLanguage("eng+ara+urd"); // English + Arabic + Urdu
```

## Performance Notes

- First OCR call may be slow (Tesseract initialization)
- Subsequent calls should be faster
- Large images (>5MB) may take 5-10 seconds
- Consider image preprocessing for better accuracy

## Next Steps

After setup is complete:
1. Restart the Spring Boot application
2. Upload a passport image
3. Check logs for successful OCR extraction
4. Document should be validated correctly

