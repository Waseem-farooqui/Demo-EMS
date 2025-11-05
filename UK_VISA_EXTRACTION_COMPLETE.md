# UK Home Office VISA Extraction - Implementation Complete

## Overview
Enhanced the OCR extraction to properly handle UK Home Office VISA documents, specifically extracting work permission dates, company details, and reference information.

## What Was Fixed

### 1. UK VISA Specific Extraction Pattern
The system now recognizes and extracts the work permission statement:
- **Pattern**: "They have permission to work in the UK until [date]"
- **Example**: "They have permission to work in the UK until 14 December 2025"
- The date after "until" is automatically extracted as the **expiry date**

### 2. New Fields Extracted from UK VISA

#### From Details Section:
- **Company Name**: Extracted from "Company Name" field
- **Date of Check**: Extracted from "Date of Check" field
- **Reference Number**: Extracted from "Reference" or "Reference Number" field

### 3. Database Schema Updates

Added three new columns to `documents` table:
- `company_name` VARCHAR(255) - Store employer/organization name
- `date_of_check` DATE - Date the VISA check was performed
- `reference_number` VARCHAR(100) - VISA reference/check ID

### 4. Enhanced Extraction Logic

#### UK Work Permission Pattern:
```java
Pattern: permission\s+to\s+work\s+in\s+the\s+UK\s+until\s+(\d{1,2}\s+[A-Za-z]+\s+\d{4})
Example Match: "14 December 2025"
Supported Formats:
  - "14 December 2025"
  - "1 January 2026"
  - "31 March 2025"
```

#### Company Name Pattern:
```java
Pattern: (?:Company\s*Name|Employer|Organisation)\s*:?\s*([A-Za-z0-9\s&.,'-]+?)
Example Matches:
  - "ABC Company Ltd"
  - "XYZ Corporation & Partners"
  - "Test Organization"
```

#### Date of Check Pattern:
```java
Pattern: Date\s*of\s*Check\s*:?\s*(\d{1,2}\s+[A-Za-z]+\s+\d{4}|\d{1,2}[/-]\d{1,2}[/-]\d{2,4})
Example Matches:
  - "14 November 2024"
  - "14/11/2024"
  - "14-11-2024"
```

#### Reference Number Pattern:
```java
Pattern: (?:Reference|Ref|Reference\s*Number|Check\s*ID)\s*:?\s*([A-Z0-9-]+)
Example Matches:
  - "ABC123456"
  - "REF-2024-001"
  - "CHK123"
```

## Updated Data Flow

### Upload Process:
1. User uploads UK VISA document (PDF/Image)
2. OCR extracts text from document
3. System detects UK VISA by keywords: "United Kingdom", "UK", "Home Office", "permission to work"
4. Extracts work permission date → **expiryDate**
5. Extracts company details from Details section
6. Stores all information in database

### Data Stored:
```java
Document {
  // Standard fields
  documentType: "VISA"
  documentNumber: "[extracted or reference number]"
  issuingCountry: "United Kingdom"
  fullName: "[person's name]"
  nationality: "[nationality]"
  
  // UK VISA specific fields
  expiryDate: [work permission until date]
  companyName: "[employer name]"
  dateOfCheck: "[check date]"
  referenceNumber: "[VISA reference]"
}
```

## API Response Format

When retrieving a UK VISA document, the response includes:

```json
{
  "id": 123,
  "employeeId": 1,
  "employeeName": "John Doe",
  "documentType": "VISA",
  "documentNumber": "ABC123456",
  "issuingCountry": "United Kingdom",
  "fullName": "JOHN DOE",
  "nationality": "Pakistani",
  "expiryDate": "2025-12-14",
  
  "companyName": "ABC Company Ltd",
  "dateOfCheck": "2024-11-04",
  "referenceNumber": "CHK-2024-001",
  
  "uploadedDate": "2024-11-04T00:00:00",
  "daysUntilExpiry": 40
}
```

## Frontend Integration

The frontend can now display these additional fields for UK VISA documents:

### Display Section:
```typescript
if (document.documentType === 'VISA' && document.issuingCountry === 'United Kingdom') {
  // Show UK specific fields
  display: document.companyName
  display: document.dateOfCheck
  display: document.referenceNumber
  display: document.expiryDate // Work permission until date
}
```

## Testing the Feature

### 1. Upload a UK VISA Document
- Login as admin or super admin
- Navigate to employee documents
- Upload a UK Home Office VISA (PDF or Image)

### 2. Verify Extraction
Check that the following are extracted:
- ✅ Work permission expiry date (from "until [date]")
- ✅ Company name (from Details section)
- ✅ Date of check (from Details section)
- ✅ Reference number (from Details section)

### 3. Example UK VISA Text Pattern
```
VISA CHECK RESULT

Name: JOHN DOE

They have permission to work in the UK until 14 December 2025,
subject to the conditions and restrictions below.

Details:
Company Name: ABC Corporation Ltd
Date of Check: 4 November 2024
Reference: CHK-2024-12345
```

**Expected Extraction:**
- expiryDate: 2025-12-14
- companyName: ABC Corporation Ltd
- dateOfCheck: 2024-11-04
- referenceNumber: CHK-2024-12345

## Database Migration

The database schema will be automatically updated when the application restarts (due to `spring.jpa.hibernate.ddl-auto=update`).

Manual SQL (if needed):
```sql
-- See: database/add_uk_visa_fields.sql
ALTER TABLE documents ADD COLUMN company_name VARCHAR(255);
ALTER TABLE documents ADD COLUMN date_of_check DATE;
ALTER TABLE documents ADD COLUMN reference_number VARCHAR(100);
```

## Files Modified

1. **Entity**: `Document.java` - Added 3 new fields
2. **DTO**: `DocumentDTO.java` - Added 3 new fields
3. **Service**: `DocumentService.java` - Updated to map new fields
4. **Service**: `OcrService.java` - Enhanced VISA extraction logic

## Important Notes

### ⚠️ Application Restart Required
After code changes, **you must restart the backend application** for changes to take effect:
```cmd
cd C:\Users\waseem.uddin\EmployeeManagementSystem
mvnw.cmd spring-boot:run
```

### 🔄 Automatic Schema Update
On restart, Hibernate will automatically add the new columns to the `documents` table.

### 🎯 Backward Compatible
- Existing documents without these fields will have NULL values
- Non-UK VISAs will not have these fields populated
- Passport documents are unaffected

## Validation

The extraction is logged with detailed information:
```
INFO - Found UK work permission date: '14 December 2025'
INFO - ✓ Expiry date extracted from work permission: 2025-12-14
INFO - ✓ Company name extracted: ABC Corporation Ltd
INFO - ✓ Date of check extracted: 2024-11-04
INFO - ✓ Reference number extracted: CHK-2024-12345
INFO - 📋 VISA extraction complete. Extracted fields: [expiryDate, companyName, dateOfCheck, referenceNumber, issuingCountry]
```

## Next Steps

1. **Restart backend application**
2. **Test with a UK VISA document**
3. **Verify all fields are extracted correctly**
4. **Update frontend to display new fields** (if desired)

## Status

✅ Backend implementation complete
✅ Database schema updated
✅ OCR extraction enhanced
✅ DTO and entities updated
⏳ **Pending**: Backend restart + testing
⏳ **Optional**: Frontend UI update to display new fields

