# UK VISA Extraction Troubleshooting Guide

## Issue: Not Reading Full Document / Not Extracting Data

### What Was Fixed

Enhanced the OCR extraction patterns to be more robust and flexible:

1. ✅ **Name Extraction Enhanced**
   - Now looks for name pattern: "Home Office [NAME] They have permission"
   - Fallback pattern for names in ALL CAPS near document start
   - Example: "dl Home Office MUDASSIR MANSOOR They have..."

2. ✅ **Better Logging**
   - Shows full extracted text length
   - Logs first 500 characters of text for debugging
   - Warns if patterns don't match

3. ✅ **More Flexible Patterns**
   - Company name pattern now handles multi-line text
   - Date of Check pattern case-insensitive
   - Reference Number pattern with multiple variations

### How to Test

#### Step 1: Restart Backend
```cmd
cd C:\Users\waseem.uddin\EmployeeManagementSystem
mvnw.cmd spring-boot:run
```

#### Step 2: Upload Document & Check Logs

Look for these log messages in the console:

```
🔍 Starting OCR text extraction for file: [filename]
✅ Text extraction successful. Length: XXX characters
📄 Extracted text preview (first 500 chars): [text preview]
✈️ Processing as VISA document
Found UK work permission date: '14 December 2025'
✓ Expiry date extracted from work permission: 2025-12-14
✓ Full name extracted: MUDASSIR MANSOOR
✓ Company name extracted: [company name]
✓ Date of check extracted: [date]
✓ Reference number extracted: [reference]
📋 VISA extraction complete. Extracted fields: [list of fields]
```

### Common Issues & Solutions

#### Issue 1: "No text extracted from document"
**Cause**: OCR service failed to extract text from the image/PDF

**Solutions**:
- Check if Tesseract OCR is properly installed
- Verify the document is not encrypted
- Try with better quality image (300 DPI recommended)
- Check if the image format is supported (JPG, PNG, PDF)

#### Issue 2: "Could not find work permission date pattern"
**Cause**: The text doesn't match the expected pattern

**Expected Pattern**: 
```
"They have permission to work in the UK until 14 December 2025"
```

**Check**:
- Look at the log "Text preview" to see what was extracted
- Verify the text contains the phrase "permission to work in the UK until"
- Check if the date is in format: "14 December 2025" (not "14/12/2025")

#### Issue 3: "Could not find company name/date/reference"
**Cause**: The Details section might have different formatting

**Check Log Output**:
```
⚠ Could not find company name in text
⚠ Could not find date of check in text
⚠ Could not find reference number in text
```

**Expected Format**:
```
Details of check

Company name
Oceanway hospitality services Ltd

Date of check
4 May 2025

Reference number
WE-4W9FW53-EL
```

**If format is different**, check the "Text preview" in logs to see actual format.

### Debugging Steps

#### 1. Enable Debug Logging
Add to `application.properties`:
```properties
logging.level.com.was.employeemanagementsystem.service.OcrService=DEBUG
logging.level.com.was.employeemanagementsystem.service.DocumentService=DEBUG
```

#### 2. Check Extracted Text
After upload, check logs for:
```
📄 Extracted text preview (first 500 chars): [shows what OCR read]
```

Compare this with the actual document. If the text looks garbled or incomplete:
- ✅ Image quality might be poor
- ✅ OCR engine might need better configuration
- ✅ Try uploading a clearer scan

#### 3. Check Pattern Matches
Look for these logs:
```
✓ Full name extracted: MUDASSIR MANSOOR
✓ Expiry date extracted from work permission: 2025-12-14
✓ Company name extracted: [name]
✓ Date of check extracted: [date]
✓ Reference number extracted: [ref]
```

If you see `⚠ Could not find...` instead, the pattern didn't match.

### Expected Extraction for Sample Document

**Document Text:**
```
dl Home Office

MUDASSIR MANSOOR

They have permission to work in the UK until 14 December 2025,
subject to the conditions and restrictions below.

Conditions
They must work for the employer who sponsored them. They can:
...

Details of check

Company name
Oceanway hospitality services Ltd

Date of check
4 May 2025

Reference number
WE-4W9FW53-EL
```

**Expected Extraction:**
```json
{
  "fullName": "MUDASSIR MANSOOR",
  "expiryDate": "2025-12-14",
  "issuingCountry": "United Kingdom",
  "companyName": "Oceanway hospitality services Ltd",
  "dateOfCheck": "2025-05-04",
  "referenceNumber": "WE-4W9FW53-EL",
  "documentNumber": "WE-4W9FW53-EL",
  "issueDate": "2025-05-04"
}
```

### Enhanced Patterns Added

#### 1. Name Pattern
```java
Pattern: (?:Home\\s*Office|dl\\s*Home\\s*Office)\\s+([A-Z][A-Z\\s]{2,50}?)\\s+(?:They\\s+have\\s+permission|Conditions)

Matches:
- "Home Office MUDASSIR MANSOOR They have..."
- "dl Home Office JOHN DOE They have..."
```

#### 2. Company Name Pattern
```java
Pattern: (?:Company\\s*[Nn]ame|Employer|Organisation)\\s*[:\\s]+([A-Za-z0-9\\s&.,'-]+?)(?:\\s*(?:Date\\s*of\\s*[Cc]heck|Reference|\\n\\s*\\n|$))

More flexible - handles:
- Multi-line company names
- Company names with special characters (&, ., ,, -, ')
- Case variations (Company name, Company Name, COMPANY NAME)
```

#### 3. Date of Check Pattern
```java
Pattern: Date\\s*of\\s*[Cc]heck\\s*[:\\s]*(\\d{1,2}\\s+[A-Za-z]+\\s+\\d{4}|\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})

Matches:
- "Date of check: 4 May 2025"
- "Date of Check 4 May 2025"
- "Date of check: 04/05/2025"
```

#### 4. Reference Number Pattern
```java
Pattern: Reference\\s*[Nn]umber\\s*[:\\s]*([A-Z0-9-]+)|Reference\\s*[:\\s]*([A-Z0-9-]+)

Matches:
- "Reference number: WE-4W9FW53-EL"
- "Reference: ABC-123-XYZ"
- "Reference WE4W9FW53EL"
```

### If Extraction Still Fails

#### Option 1: Manual Review
1. Check the logs for "Text preview"
2. Copy the extracted text
3. Verify it contains the expected information
4. Check if format matches patterns above

#### Option 2: Adjust Patterns
If your documents have different formatting, we can adjust the regex patterns to match.

**Example**:
- If company name has ":" after label: `Company name: XYZ Corp`
- If date format is different: `04-05-2025`
- If reference has different label: `Check ID: ABC123`

#### Option 3: Improve OCR Quality
- Use higher resolution images (300 DPI minimum)
- Ensure good contrast (black text on white background)
- Avoid skewed or rotated documents
- Remove shadows or artifacts

### Quick Check Command

After restart, upload a document and check logs for:
```bash
# Look for this sequence:
1. ✅ Text extraction successful
2. ✈️ Processing as VISA document
3. Found UK work permission date
4. ✓ Full name extracted
5. ✓ Company name extracted
6. ✓ Date of check extracted
7. ✓ Reference number extracted
8. 📋 VISA extraction complete
```

If you see all ✓ marks, extraction worked!
If you see ⚠ warnings, check what's missing and look at the text preview.

### Contact Points

**Log Location**: Console output or application log file
**Key Classes**: 
- `OcrService.java` - Handles text extraction and pattern matching
- `DocumentService.java` - Handles document upload and field mapping

**Most Important Log**: 
```
📄 Extracted text preview (first 500 chars): [THIS SHOWS WHAT OCR READ]
```
This tells you exactly what the system is "seeing" from your document.

