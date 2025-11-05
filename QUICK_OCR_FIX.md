# Quick Fix - 3 Steps to Enable OCR

## The Problem
Your uploaded passport image contains text, but **Apache Tika cannot read images**.  
You need **Tesseract OCR** for image-based text extraction.

## The Solution - 3 Steps

### Step 1: Install Tesseract (5 minutes)
```
1. Download: https://digi.bib.uni-mannheim.de/tesseract/tesseract-ocr-w64-setup-5.3.3.20231005.exe
2. Run the installer
3. Use default path: C:\Program Files\Tesseract-OCR
4. Ensure "English language data" is selected
5. Complete installation
```

### Step 2: Rebuild Project (2 minutes)
```cmd
mvnw clean install
```

### Step 3: Run Application (1 minute)
```cmd
mvnw spring-boot:run
```

## Test It
Upload your passport image again - it will now work! ✓

---

## Alternative: Use the Helper Script
Double-click: **`install-tesseract.bat`**

---

## Verify Tesseract Installation
```cmd
"C:\Program Files\Tesseract-OCR\tesseract.exe" --version
```

Expected output:
```
tesseract 5.3.3
```

---

## Full Documentation
- **OCR_FIX_README.md** - Complete guide with troubleshooting
- **TESSERACT_OCR_SETUP.md** - Detailed technical documentation

---

## What Changed?
✅ Added Tesseract OCR for image processing  
✅ Images (jpg, png) → Tesseract OCR  
✅ PDFs, documents → Apache Tika  
✅ Automatic file type detection  
✅ Better error messages  

## Why This Failed Before?
❌ Apache Tika **does not do OCR** on images  
✓ Now using **Tesseract** - industry standard OCR engine

