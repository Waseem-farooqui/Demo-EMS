# Contract Document Extraction - Implementation Complete

## Summary
Implemented complete contract document extraction functionality to automatically extract key information from employment contract PDFs including:
- **Contract Date** (Date of Employment)
- **Place of Work** (Work Location)
- **Contract Between** (Parties involved)
- **Job Title** (from contract)

## Changes Made

### 1. Backend - Database Entity

**File**: `src/main/java/com/was/employeemanagementsystem/entity/Document.java`

Added contract-specific fields:
```java
// CONTRACT specific fields
@Column(name = "contract_date")
private LocalDate contractDate; // Date of employment/contract start date

@Column(name = "place_of_work")
private String placeOfWork; // Work location

@Column(name = "contract_between")
private String contractBetween; // Parties involved (e.g., "Company Ltd and Employee Name")

@Column(name = "job_title_contract")
private String jobTitleContract; // Job title from contract
```

---

### 2. Backend - DTO

**File**: `src/main/java/com/was/employeemanagementsystem/dto/DocumentDTO.java`

Added contract fields to DTO:
```java
// CONTRACT specific fields
private LocalDate contractDate; // Date of employment/contract start date
private String placeOfWork; // Work location
private String contractBetween; // Parties involved
private String jobTitleContract; // Job title from contract
```

---

### 3. Backend - Document Service

**File**: `src/main/java/com/was/employeemanagementsystem/service/DocumentService.java`

#### A. Updated Document Type Validation:
```java
// Now accepts CONTRACT in addition to PASSPORT and VISA
if (!documentType.equals("PASSPORT") && !documentType.equals("VISA") && !documentType.equals("CONTRACT")) {
    throw new RuntimeException("Invalid document type. Must be PASSPORT, VISA, or CONTRACT");
}
```

#### B. Added Contract Extraction Logic:
```java
} else if (documentType.equals("CONTRACT")) {
    log.info("📝 Processing as CONTRACT document");
    extractedInfo = ocrService.extractContractInformation(extractedText);
} else {
    extractedInfo = new HashMap<>();
}
```

#### C. Added Contract Field Mapping:
```java
// CONTRACT specific fields
if (extractedInfo.containsKey("contractDate")) {
    LocalDate contractDate = (LocalDate) extractedInfo.get("contractDate");
    document.setContractDate(contractDate);
    // Also set as issue date if not already set
    if (document.getIssueDate() == null) {
        document.setIssueDate(contractDate);
    }
}
if (extractedInfo.containsKey("placeOfWork")) {
    document.setPlaceOfWork((String) extractedInfo.get("placeOfWork"));
}
if (extractedInfo.containsKey("contractBetween")) {
    document.setContractBetween((String) extractedInfo.get("contractBetween"));
}
if (extractedInfo.containsKey("jobTitleContract")) {
    document.setJobTitleContract((String) extractedInfo.get("jobTitleContract"));
}
```

#### D. Updated DTO Mapping:
```java
// CONTRACT specific fields
dto.setContractDate(document.getContractDate());
dto.setPlaceOfWork(document.getPlaceOfWork());
dto.setContractBetween(document.getContractBetween());
dto.setJobTitleContract(document.getJobTitleContract());
```

---

### 4. Backend - OCR Service

**File**: `src/main/java/com/was/employeemanagementsystem/service/OcrService.java`

Added new method: `extractContractInformation(String text)`

#### Contract Date Extraction (3 patterns):

**Pattern 1**: "Date of Employment" / "Start Date" / "Commencement Date"
```java
Pattern.compile(
    "(?:Date\\s*of\\s*Employment|Start\\s*Date|Commencement\\s*Date|Employment\\s*Start\\s*Date)\\s*[:\\s]*(\\d{1,2}\\s+[A-Za-z]+\\s+\\d{4})",
    Pattern.CASE_INSENSITIVE
);
```
- Matches: `Date of Employment: 15 January 2024`
- Matches: `Start Date: 1 May 2025`

**Pattern 2**: Numeric date format
```java
Pattern.compile(
    "(?:Date\\s*of\\s*Employment|Start\\s*Date|Commencement\\s*Date)\\s*[:\\s]*(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})",
    Pattern.CASE_INSENSITIVE
);
```
- Matches: `Date of Employment: 15/01/2024`

**Pattern 3**: "dated" keyword (common in contracts)
```java
Pattern.compile(
    "\\bdated\\s+(\\d{1,2}\\s+[A-Za-z]+\\s+\\d{4})",
    Pattern.CASE_INSENSITIVE
);
```
- Matches: `...dated 15 January 2024`

#### Place of Work Extraction (2 patterns):

**Pattern 1**: "Place of Work" / "Work Location" / "Location"
```java
Pattern.compile(
    "(?:Place\\s*of\\s*Work|Work\\s*Location|Location|Principal\\s*Place\\s*of\\s*Work)[:\\s]+([A-Za-z0-9\\s,.-]+?)(?:\\n|\\.|;|$)",
    Pattern.CASE_INSENSITIVE
);
```
- Matches: `Place of Work: London Office, 123 High Street`

**Pattern 2**: "based at" / "work at" / "located at"
```java
Pattern.compile(
    "(?:based\\s+at|work\\s+at|located\\s+at)[:\\s]+([A-Za-z0-9\\s,.-]+?)(?:\\n|\\.|;|$)",
    Pattern.CASE_INSENSITIVE
);
```
- Matches: `based at Manchester Branch`

#### Contract Between Extraction (2 patterns):

**Pattern 1**: "Contract between X and Y"
```java
Pattern.compile(
    "(?:Contract|Agreement)\\s+between\\s+([A-Za-z0-9\\s&.,'-]+?)\\s+and\\s+([A-Za-z\\s]+?)(?:\\n|\\(|dated|$)",
    Pattern.CASE_INSENSITIVE
);
```
- Matches: `Contract between ABC Ltd and John Smith`

**Pattern 2**: "This Agreement is made between..."
```java
Pattern.compile(
    "(?:made|entered)\\s+between\\s+([A-Za-z0-9\\s&.,'-]+?)\\s+(?:and|&)\\s+([A-Za-z\\s]+?)(?:\\n|\\(|dated|$)",
    Pattern.CASE_INSENSITIVE
);
```
- Matches: `This Agreement is made between XYZ Company and Jane Doe`

#### Job Title Extraction (2 patterns):

**Pattern 1**: "Position" / "Job Title" / "Role" / "Post"
```java
Pattern.compile(
    "(?:Position|Job\\s*Title|Role|Post)[:\\s]+([A-Za-z\\s&/-]+?)(?:\\n|\\.|;|$)",
    Pattern.CASE_INSENSITIVE
);
```
- Matches: `Position: Senior Developer`
- Matches: `Job Title: Manager`

**Pattern 2**: "employed as" / "appointed as"
```java
Pattern.compile(
    "(?:employed\\s+as|appointed\\s+as)[:\\s]+([A-Za-z\\s&/-]+?)(?:\\n|\\.|;|at|$)",
    Pattern.CASE_INSENSITIVE
);
```
- Matches: `employed as Software Engineer`

---

### 5. Frontend - Model

**File**: `frontend/src/app/models/document.model.ts`

Added contract fields to Document interface:
```typescript
// CONTRACT specific fields
contractDate?: string; // Date of employment/contract start date
placeOfWork?: string; // Work location
contractBetween?: string; // Parties involved
jobTitleContract?: string; // Job title from contract
```

---

## How It Works

### Upload Flow:

```
User uploads CONTRACT PDF
    ↓
DocumentService validates document type (PASSPORT, VISA, or CONTRACT)
    ↓
OcrService extracts text from PDF using Tesseract/Cloud OCR
    ↓
OcrService.extractContractInformation(text) called
    ↓
Multiple regex patterns applied to extract:
  - Contract Date (3 patterns)
  - Place of Work (2 patterns)
  - Contract Between (2 patterns)
  - Job Title (2 patterns)
    ↓
Extracted info stored in Document entity
    ↓
Document saved to database
    ↓
Frontend receives DocumentDTO with contract fields
```

---

## Extraction Examples

### Example Contract Text:
```
EMPLOYMENT CONTRACT

This Agreement is made between ABC Hospitality Ltd and John Smith
dated 15 January 2024

Position: Restaurant Manager
Place of Work: London Branch, 123 High Street
Date of Employment: 15 January 2024
```

### Extracted Data:
```json
{
  "contractBetween": "ABC Hospitality Ltd and John Smith",
  "contractDate": "2024-01-15",
  "jobTitleContract": "Restaurant Manager",
  "placeOfWork": "London Branch, 123 High Street"
}
```

---

## Contract Document Variations Supported

### Contract Date Variations:
- ✅ `Date of Employment: 15 January 2024`
- ✅ `Start Date: 1 May 2025`
- ✅ `Commencement Date: 01/03/2024`
- ✅ `Employment Start Date: 15-01-2024`
- ✅ `...dated 15 January 2024`

### Place of Work Variations:
- ✅ `Place of Work: London Office`
- ✅ `Work Location: Manchester Branch`
- ✅ `Location: Birmingham Site`
- ✅ `based at Edinburgh Office`
- ✅ `work at Glasgow Branch`

### Contract Between Variations:
- ✅ `Contract between XYZ Ltd and Jane Doe`
- ✅ `Agreement between ABC Company and John Smith`
- ✅ `made between Company Ltd and Employee Name`
- ✅ `entered between Organization & Person Name`

### Job Title Variations:
- ✅ `Position: Senior Developer`
- ✅ `Job Title: Manager`
- ✅ `Role: Team Leader`
- ✅ `Post: Accountant`
- ✅ `employed as Software Engineer`
- ✅ `appointed as Director`

---

## Database Schema

New columns in `documents` table:
```sql
contract_date DATE NULL
place_of_work VARCHAR(255) NULL
contract_between VARCHAR(255) NULL
job_title_contract VARCHAR(100) NULL
```

These will be automatically created by Hibernate on next startup (JPA ddl-auto=update).

---

## API Response Example

When fetching a CONTRACT document:
```json
{
  "id": 123,
  "employeeId": 45,
  "employeeName": "John Smith",
  "documentType": "CONTRACT",
  "fileName": "employment_contract.pdf",
  "contractDate": "2024-01-15",
  "placeOfWork": "London Branch, 123 High Street",
  "contractBetween": "ABC Hospitality Ltd and John Smith",
  "jobTitleContract": "Restaurant Manager",
  "uploadedDate": "2024-11-04T10:30:00",
  ...
}
```

---

## Frontend Integration (Ready)

The frontend Document model is updated with contract fields. You can now display contract information in:

### Employee Details Modal:
```html
<div *ngIf="doc.documentType === 'CONTRACT'">
  <div>Contract Date: {{ doc.contractDate | date }}</div>
  <div>Place of Work: {{ doc.placeOfWork }}</div>
  <div>Contract Between: {{ doc.contractBetween }}</div>
  <div>Job Title: {{ doc.jobTitleContract }}</div>
</div>
```

### Document List Component:
```html
<div *ngIf="doc.documentType === 'CONTRACT' && doc.jobTitleContract">
  <span class="doc-info-label">💼 Position:</span>
  <span class="doc-info-value">{{ doc.jobTitleContract }}</span>
</div>
```

---

## Testing Guide

### Test Case 1: Upload Contract with All Fields
```
1. Navigate to Documents → Upload
2. Select document type: CONTRACT
3. Upload a PDF employment contract containing:
   - Date of Employment
   - Place of Work
   - Contract parties
   - Job Title
4. Verify all fields are extracted and displayed
```

### Test Case 2: Contract Date Formats
Test different date formats:
- `15 January 2024`
- `15/01/2024`
- `dated 15 January 2024`

All should be correctly extracted.

### Test Case 3: Job Title Variations
Test different job title formats:
- `Position: Manager`
- `employed as Developer`
- `Role: Team Leader`

All should be correctly extracted.

---

## Logging Output Example

When uploading a contract, you'll see:
```
INFO  : 📝 Processing as CONTRACT document
INFO  : Found contract date (pattern 1): '15 January 2024'
INFO  : ✓ Contract date extracted: 2024-01-15
INFO  : ✓ Place of work extracted (pattern 1): London Branch, 123 High Street
INFO  : ✓ Contract between extracted (pattern 1): ABC Hospitality Ltd and John Smith
INFO  : ✓ Job title extracted (pattern 1): Restaurant Manager
INFO  : 📝 CONTRACT extraction complete. Extracted fields: [contractDate, placeOfWork, contractBetween, jobTitleContract]
INFO  : ✓ Set contract date: 2024-01-15
INFO  : ✓ Set place of work: London Branch, 123 High Street
INFO  : ✓ Set contract between: ABC Hospitality Ltd and John Smith
INFO  : ✓ Set job title (from contract): Restaurant Manager
```

---

## Benefits

✅ **Automated Extraction** - No manual data entry needed  
✅ **Multiple Pattern Support** - Handles various contract formats  
✅ **Robust Parsing** - Cascading patterns ensure high success rate  
✅ **Database Storage** - All contract data persisted  
✅ **API Ready** - Contract fields available via REST API  
✅ **Frontend Ready** - TypeScript model includes contract fields  
✅ **Logging** - Detailed extraction logs for debugging  

---

## Next Steps

### To Use Contract Extraction:

1. **Restart Backend** to apply database schema changes:
   ```cmd
   cd C:\Users\waseem.uddin\EmployeeManagementSystem
   mvnw.cmd spring-boot:run
   ```

2. **Upload Contract Document**:
   - Navigate to Documents → Upload
   - Select document type: CONTRACT
   - Upload employment contract PDF

3. **Verify Extraction**:
   - Check backend logs for extraction results
   - View document details in frontend
   - Verify fields in database

### To Display Contract Info in Frontend:

Update `document-list.component.html` to show contract fields:
```html
<div *ngIf="doc.documentType === 'CONTRACT'">
  <div class="contract-info">
    <div *ngIf="doc.jobTitleContract">
      💼 Position: {{ doc.jobTitleContract }}
    </div>
    <div *ngIf="doc.placeOfWork">
      📍 Location: {{ doc.placeOfWork }}
    </div>
    <div *ngIf="doc.contractDate">
      📅 Start Date: {{ doc.contractDate | date:'mediumDate' }}
    </div>
  </div>
</div>
```

---

## Status

✅ **Backend Entity** - Contract fields added to Document entity  
✅ **Backend DTO** - Contract fields in DocumentDTO  
✅ **Backend Service** - Document upload supports CONTRACT type  
✅ **OCR Extraction** - extractContractInformation() method implemented  
✅ **Pattern Matching** - 9 regex patterns for contract field extraction  
✅ **Frontend Model** - Contract fields in TypeScript Document interface  
✅ **Database Schema** - Ready to store contract data  
✅ **No Compilation Errors** - Code compiles successfully  

**Contract document extraction is fully implemented and ready to use! Users can now upload employment contracts and the system will automatically extract contract date, place of work, contract parties, and job title!** 🎉

