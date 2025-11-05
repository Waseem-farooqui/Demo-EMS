# Share Code Document Type Implementation

## Overview
Added support for a new document type "SHARE_CODE" for UK Home Office share code documents. Like RESUME documents, Share Codes are stored and viewed as PDF files with no OCR extraction or data parsing required.

## What is a Share Code?
A Share Code is a document provided by the UK Home Office that allows employers to check an individual's right to work in the UK. It's a simple reference code that can be viewed online.

## Changes Made

### Backend Changes

#### DocumentService.java
**File:** `src/main/java/com/was/employeemanagementsystem/service/DocumentService.java`

**Updated Validation:**
```java
// Validate document type
if (!documentType.equals("PASSPORT") && !documentType.equals("VISA") &&
    !documentType.equals("CONTRACT") && !documentType.equals("RESUME") &&
    !documentType.equals("SHARE_CODE")) {
    throw new RuntimeException("Invalid document type. Must be PASSPORT, VISA, CONTRACT, RESUME, or SHARE_CODE");
}
```

**Skip OCR for Share Codes:**
```java
// For RESUME and SHARE_CODE documents, skip OCR extraction (just store the file)
String extractedText = null;
Map<String, Object> extractedInfo = new HashMap<>();

if (!documentType.equals("RESUME") && !documentType.equals("SHARE_CODE")) {
    // Extract text from document (skip for RESUME and SHARE_CODE)
    log.info("🔍 Starting OCR text extraction for file: {}", file.getOriginalFilename());
    // ...extraction logic
}
```

### Frontend Changes

#### 1. Document Upload Component
**File:** `document-upload.component.ts`

Added SHARE_CODE to document types:
```typescript
documentTypes = ['PASSPORT', 'VISA', 'CONTRACT', 'RESUME', 'SHARE_CODE'];
```

**File:** `document-upload.component.html`

Updated subtitle:
```html
<p class="subtitle">Upload passport, visa, contract, resume, or share code documents</p>
```

#### 2. Document List Component
**File:** `document-list.component.html`

Added Share Code filter button:
```html
<button
  class="filter-btn filter-share-code"
  [class.active]="filterType === 'SHARE_CODE'"
  (click)="filterByType('SHARE_CODE')">
  Share Codes
</button>
```

Added Share Code type styling to cards:
```html
<div class="doc-type" 
     [class.type-passport]="doc.documentType === 'PASSPORT'"
     [class.type-visa]="doc.documentType === 'VISA'"
     [class.type-contract]="doc.documentType === 'CONTRACT'"
     [class.type-resume]="doc.documentType === 'RESUME'"
     [class.type-share-code]="doc.documentType === 'SHARE_CODE'">
  {{ doc.documentType === 'SHARE_CODE' ? 'SHARE CODE' : doc.documentType }}
</div>
```

**File:** `document-list.component.css`

Added styling:
```css
.filter-share-code.active {
  background-color: #20c997; /* Teal color */
  border-color: #20c997;
}

.type-share-code {
  color: #fff;
}
```

## Features

### ✅ No OCR Processing
- Share Code documents are stored as-is (like RESUME)
- No text extraction
- No field parsing
- No validation
- Just upload and view

### ✅ PDF Viewing
- Share Codes are displayed as PDF documents
- Full document viewing in browser
- No image conversion needed

### ✅ Filter Option
- "Share Codes" filter button on documents page
- Teal color (#20c997) for visual distinction
- Works with existing filter logic

### ✅ No Expiry Badge
- Share Codes don't have expiry dates
- No expiry badge shown on cards
- Cleaner card display

## Document Type Comparison

| Feature | PASSPORT | VISA | CONTRACT | RESUME | SHARE_CODE |
|---------|----------|------|----------|--------|------------|
| OCR Extraction | ✅ Yes | ✅ Yes | ✅ Yes (1st page) | ❌ No | ❌ No |
| Field Parsing | ✅ Yes | ✅ Yes | ✅ Yes | ❌ No | ❌ No |
| Expiry Date | ✅ Yes | ✅ Yes | ❌ No | ❌ No | ❌ No |
| PDF Viewing | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes |
| Filter Color | Blue | Green | Orange | Purple | Teal |

## Usage

### Upload Share Code
1. Go to Documents → Upload Document
2. Select employee
3. Choose **"SHARE_CODE"** from document type dropdown
4. Select PDF file
5. Upload
6. Document stored without OCR processing

### View Share Code
1. Go to Documents page
2. Click **"Share Codes"** filter (teal button)
3. See all share code documents
4. Click **"View Details"** on any share code
5. PDF displays in viewer

### Share Code Card Display
```
┌─────────────────────────────────┐
│ SHARE CODE         (No Expiry)  │
├─────────────────────────────────┤
│ John Doe                        │
│                                 │
│ Uploaded: 05 Nov 2025           │
│ Last Viewed: 05 Nov 2025, 14:30 │
│ Viewed By: admin                │
├─────────────────────────────────┤
│ [View Details]  [Delete]        │
└─────────────────────────────────┘
```

## API Endpoints

Uses existing document endpoints:

**Upload:**
```http
POST /api/documents/upload
Content-Type: multipart/form-data

{
  "employeeId": 1,
  "documentType": "SHARE_CODE",
  "file": <binary>
}
```

**Response:**
```json
{
  "id": 123,
  "employeeId": 1,
  "documentType": "SHARE_CODE",
  "fileName": "share_code.pdf",
  "uploadedDate": "2025-11-05T15:30:00",
  "extractedText": null,
  "documentNumber": null,
  "issueDate": null,
  "expiryDate": null
}
```

## Validation

**Backend:**
- ✅ Document type must be valid (including SHARE_CODE)
- ✅ File must be uploaded
- ✅ Employee must exist
- ✅ User must have permission to upload for that employee

**Frontend:**
- ✅ All form fields required
- ✅ File type validation (PDF, JPG, PNG)
- ✅ File size limit (10MB)

## Database

No database changes required. Uses existing `documents` table:

```sql
INSERT INTO documents (
  employee_id,
  document_type,
  file_name,
  file_path,
  file_type,
  file_data,
  uploaded_date
) VALUES (
  1,
  'SHARE_CODE',
  'share_code.pdf',
  '/uploads/documents/abc123_share_code.pdf',
  'application/pdf',
  <binary_data>,
  '2025-11-05 15:30:00'
);
```

All extraction fields remain NULL:
- `extracted_text` = NULL
- `document_number` = NULL
- `issue_date` = NULL
- `expiry_date` = NULL
- `issuing_country` = NULL
- etc.

## Testing Instructions

### Test Case 1: Upload Share Code
1. Login as ADMIN or user with employee profile
2. Go to Documents → Upload Document
3. Select employee
4. Choose "SHARE_CODE" from dropdown
5. Upload a PDF file
6. **Expected:** Upload successful, no OCR processing, document stored

### Test Case 2: View Share Code List
1. Go to Documents page
2. Click "Share Codes" filter button (teal)
3. **Expected:** See only share code documents

### Test Case 3: View Share Code Details
1. Click "View Details" on a share code
2. **Expected:** 
   - PDF viewer displays document
   - No "Document Information" section (no extracted data)
   - Only shows: Employee Info, Upload Info

### Test Case 4: Filter Combination
1. Upload share codes for multiple employees
2. Click "Share Codes" filter
3. **Expected:** See all share codes regardless of other filters

### Test Case 5: No Expiry Badge
1. View documents page with share codes
2. **Expected:** Share code cards don't show expiry badges (like resume)

## Visual Design

### Colors
- **Filter Button (Active):** Teal (#20c997)
- **Card Header:** Purple gradient (default)
- **Text:** White on colored backgrounds

### Icons
- No special icon (uses default document icon)
- Could add 🔑 or 🏠 emoji if desired

## Logging

Backend logs when share code is uploaded:
```
📄 Uploading SHARE_CODE document for employee ID: 1
✅ SHARE_CODE document uploaded successfully (skipped OCR)
📊 Document 123 stored without extraction
```

## Security

- ✅ Same access control as other documents
- ✅ Only SUPER_ADMIN can view all share codes
- ✅ ADMIN can view share codes for their department
- ✅ Regular users can only view their own share codes

## Performance

- ✅ **Faster uploads** - No OCR processing time
- ✅ **Less server load** - No image processing
- ✅ **Simpler logic** - Just store and retrieve
- ✅ **Same as RESUME** - Already optimized flow

## Future Enhancements (Optional)

Possible improvements:
- 📋 Add share code number field (manual entry)
- 📋 Add expiry date field (manual entry if needed)
- 📋 Link to UK Home Office checker
- 📋 Reminder system for share code renewals
- 📋 Bulk upload share codes

## Comparison with Other Document Types

### Like RESUME:
- ✅ No OCR extraction
- ✅ No field parsing
- ✅ Just view PDF
- ✅ Simple upload/storage

### Unlike PASSPORT/VISA:
- ❌ No automatic data extraction
- ❌ No expiry tracking
- ❌ No validation
- ❌ No document number extraction

## Notes

- Share codes are typically short documents (1-2 pages)
- They contain a reference code that can be checked online
- No sensitive data extraction needed
- Simple document storage is sufficient

---

**Status**: ✅ COMPLETE - Share Code Document Type Implemented
**Date**: November 5, 2025
**Files Modified**: 5 files (1 backend Java, 4 frontend files)
**Build Status**: ✅ Successful
**Testing**: Ready for use

