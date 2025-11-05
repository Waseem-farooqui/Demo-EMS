# UK VISA Field Mapping Guide

## Overview
UK Home Office VISA documents have specific fields that need to be mapped to standard document fields for consistency across the system.

## Field Mapping

### From UK VISA "Details of Check" Section:

| UK VISA Field | Maps To | Display As | Purpose |
|---------------|---------|------------|---------|
| **Company Name** | `companyName` | "Company Name" | Employer/organization name |
| **Date of Check** | `issueDate` + `dateOfCheck` | "Issue Date" / "Date of Check" | When VISA was checked/verified |
| **Reference Number** | `documentNumber` + `referenceNumber` | "Document Number" / "Reference" | VISA reference/check ID |

### Example UK VISA Data:

**Input:**
```
Details of check

Company name
Oceanway hospitality services Ltd

Date of check
4 May 2025

Reference number
WE-4W9FW53-EL
```

**Extracted & Mapped:**
```json
{
  "companyName": "Oceanway hospitality services Ltd",
  "issueDate": "2025-05-04",           // From Date of Check
  "dateOfCheck": "2025-05-04",         // Keep original
  "documentNumber": "WE-4W9FW53-EL",   // From Reference Number
  "referenceNumber": "WE-4W9FW53-EL"   // Keep original
}
```

## Why This Mapping?

### 1. Company Name
- **Keep as-is** in dedicated field
- Shows employer who sponsored the VISA
- Important for work permit verification

### 2. Date of Check → Issue Date
- UK VISAs show "Date of Check" instead of traditional issue date
- This is the date the right to work was verified
- Maps to `issueDate` for consistency with other documents
- **Also kept** in `dateOfCheck` field for UK-specific display

### 3. Reference Number → Document Number
- UK VISAs use "Reference number" as the primary identifier
- Maps to `documentNumber` for consistency with passports
- **Also kept** in `referenceNumber` field for UK-specific display

## Database Storage

```sql
documents table:
- document_type: "VISA"
- document_number: "WE-4W9FW53-EL"      (From Reference)
- issue_date: 2025-05-04                (From Date of Check)
- expiry_date: 2025-12-14               (From "until" clause)
- issuing_country: "United Kingdom"
- company_name: "Oceanway hospitality..." (UK VISA specific)
- date_of_check: 2025-05-04             (UK VISA specific - same as issue)
- reference_number: "WE-4W9FW53-EL"     (UK VISA specific - same as doc num)
```

## API Response Format

```json
{
  "id": 1,
  "employeeId": 1,
  "employeeName": "John Doe",
  "documentType": "VISA",
  "issuingCountry": "United Kingdom",
  
  "documentNumber": "WE-4W9FW53-EL",
  "issueDate": "2025-05-04",
  "expiryDate": "2025-12-14",
  
  "companyName": "Oceanway hospitality services Ltd",
  "dateOfCheck": "2025-05-04",
  "referenceNumber": "WE-4W9FW53-EL",
  
  "uploadedDate": "2025-11-04T00:00:00",
  "daysUntilExpiry": 40
}
```

## Frontend Display Recommendations

### Standard View (All VISAs)
```typescript
Document Number: {{ document.documentNumber }}      // WE-4W9FW53-EL
Issue Date: {{ document.issueDate }}                // 4 May 2025
Expiry Date: {{ document.expiryDate }}              // 14 Dec 2025
Issuing Country: {{ document.issuingCountry }}      // United Kingdom
```

### UK VISA Specific View (Optional Expanded Section)
```typescript
if (document.issuingCountry === 'United Kingdom') {
  Company Name: {{ document.companyName }}          // Oceanway hospitality...
  Date of Check: {{ document.dateOfCheck }}         // 4 May 2025
  Reference Number: {{ document.referenceNumber }}  // WE-4W9FW53-EL
}
```

## Benefits of This Approach

### ✅ Consistency
- All documents have `documentNumber` and `issueDate`
- Easy to search/filter across all document types
- Standard reports work for all documents

### ✅ UK-Specific Details Preserved
- Original UK VISA fields (`companyName`, `dateOfCheck`, `referenceNumber`) are kept
- Can display UK-specific information when needed
- No data loss

### ✅ Backward Compatibility
- Existing passport documents unaffected
- Non-UK VISAs work as before
- Standard fields always populated

## Implementation Status

✅ **Backend**: Field mapping implemented in `DocumentService.java`
- Date of Check → Issue Date (automatic)
- Reference Number → Document Number (automatic)
- UK-specific fields preserved

✅ **Database**: Schema supports all fields
- Standard fields: `document_number`, `issue_date`, `expiry_date`
- UK-specific: `company_name`, `date_of_check`, `reference_number`

✅ **OCR Extraction**: Enhanced in `OcrService.java`
- Extracts company name from "Company Name:" field
- Extracts date of check from "Date of check:" field
- Extracts reference from "Reference number:" field

## Testing

### Test Case: UK VISA Upload

**Given**: A UK VISA with
- Company: "Oceanway hospitality services Ltd"
- Date of Check: "4 May 2025"
- Reference: "WE-4W9FW53-EL"
- Work until: "14 December 2025"

**Expected Result**:
```json
{
  "documentType": "VISA",
  "issuingCountry": "United Kingdom",
  "documentNumber": "WE-4W9FW53-EL",     ✅ From Reference
  "issueDate": "2025-05-04",              ✅ From Date of Check
  "expiryDate": "2025-12-14",             ✅ From work permission
  "companyName": "Oceanway hospitality services Ltd", ✅ Extracted
  "dateOfCheck": "2025-05-04",            ✅ Extracted
  "referenceNumber": "WE-4W9FW53-EL"      ✅ Extracted
}
```

## Notes

1. **Fallback Logic**: If Date of Check is not found, issueDate remains null (normal behavior)
2. **Priority**: Reference Number is preferred over generic VISA number patterns
3. **Display**: Frontend can choose to show standard fields OR UK-specific fields based on `issuingCountry`

## Related Files

- `DocumentService.java` - Field mapping logic
- `OcrService.java` - Extraction patterns
- `Document.java` - Entity with all fields
- `DocumentDTO.java` - DTO for API responses

