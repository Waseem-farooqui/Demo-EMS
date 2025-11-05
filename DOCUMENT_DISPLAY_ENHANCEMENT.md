# Document Display Enhancement - Show Company Name & Full Name

## Summary
Enhanced the document display in the employee details modal to show:
- **Company Name** for VISA documents
- **Full Name** for Passport documents
- Additional extracted information (Reference Number, Date of Check)

## Changes Made

### 1. Frontend Model Update

**File**: `frontend/src/app/models/document.model.ts`

Added VISA-specific fields to the Document interface:
```typescript
// UK VISA specific fields
companyName?: string;
dateOfCheck?: string;
referenceNumber?: string;
```

These fields are now available for display in the frontend.

---

### 2. Employee List Component HTML

**File**: `frontend/src/app/components/employee-list/employee-list.component.html`

#### Updated Document Information Display:

**Before**:
```html
<div class="document-name">{{ doc.documentName }}</div>
<div class="document-meta">
  <span class="document-type">{{ doc.documentType }}</span>
  <span class="document-date">{{ doc.uploadDate | date:'short' }}</span>
</div>
<div class="document-details">
  <span *ngIf="doc.documentNumber">No: {{ doc.documentNumber }}</span>
  <span *ngIf="doc.expiryDate">Exp: {{ doc.expiryDate | date:'mediumDate' }}</span>
</div>
```

**After**:
```html
<div class="document-name">{{ doc.fileName }}</div>
<div class="document-meta">
  <span class="document-type">{{ doc.documentType }}</span>
  <span class="document-date">{{ doc.uploadedDate | date:'short' }}</span>
</div>

<!-- Show Full Name for Passport -->
<div class="document-extracted-info" *ngIf="doc.documentType === 'PASSPORT' && doc.fullName">
  <span class="doc-info-label">👤 Name:</span>
  <span class="doc-info-value">{{ doc.fullName }}</span>
</div>

<!-- Show Company Name for VISA -->
<div class="document-extracted-info" *ngIf="doc.documentType === 'VISA' && doc.companyName">
  <span class="doc-info-label">🏢 Company:</span>
  <span class="doc-info-value">{{ doc.companyName }}</span>
</div>

<div class="document-details">
  <span *ngIf="doc.documentNumber">No: {{ doc.documentNumber }}</span>
  <span *ngIf="doc.referenceNumber">Ref: {{ doc.referenceNumber }}</span>
  <span *ngIf="doc.expiryDate">Exp: {{ doc.expiryDate | date:'mediumDate' }}</span>
  <span *ngIf="doc.dateOfCheck">Check: {{ doc.dateOfCheck | date:'mediumDate' }}</span>
</div>
```

#### Key Changes:
1. **Fixed property names**: `doc.documentName` → `doc.fileName`, `doc.uploadDate` → `doc.uploadedDate`
2. **Added conditional display** for full name (Passport only)
3. **Added conditional display** for company name (VISA only)
4. **Added reference number** display (VISA)
5. **Added date of check** display (VISA)

---

### 3. CSS Styling

**File**: `frontend/src/app/components/employee-list/employee-list.component.css`

Added new styles for extracted information display:

```css
.document-extracted-info {
  display: flex;
  gap: 0.5rem;
  margin-top: 0.5rem;
  padding: 0.375rem 0.75rem;
  background: var(--primary-50);
  border-radius: var(--radius-md);
  font-size: 0.875rem;
}

.doc-info-label {
  font-weight: 600;
  color: var(--primary-600);
}

.doc-info-value {
  color: var(--text-primary);
  font-weight: 500;
}

.doc-check {
  color: var(--info);
  font-weight: 600;
}
```

**Features**:
- Highlighted background (light blue)
- Clear label/value distinction
- Icon + label format (👤 Name, 🏢 Company)
- Proper spacing and typography

---

## Visual Examples

### Passport Document Display

```
┌─────────────────────────────────────────────┐
│ 🛂  passport.pdf                            │
│     PASSPORT              2024-11-04 10:30  │
│                                             │
│  👤 Name: MUDASSIR MANSOOR                  │
│                                             │
│  No: AB123456    Exp: Dec 14, 2030         │
│                                  👁️  ⬇️      │
└─────────────────────────────────────────────┘
```

### VISA Document Display

```
┌─────────────────────────────────────────────┐
│ 📋  visa-document.pdf                       │
│     VISA                  2024-11-04 10:35  │
│                                             │
│  🏢 Company: Oceanway hospitality services  │
│             Ltd                             │
│                                             │
│  Ref: WE-4W9FW53-EL  Check: May 4, 2025    │
│  Exp: Dec 14, 2025                          │
│                                  👁️  ⬇️      │
└─────────────────────────────────────────────┘
```

---

## Backend Status

✅ **Already Complete** - No backend changes needed!

The backend was already properly configured:
- ✅ `Document` entity has all fields (companyName, dateOfCheck, referenceNumber, fullName)
- ✅ `DocumentDTO` includes all fields
- ✅ `DocumentService.convertToDTO()` maps all fields correctly
- ✅ OCR extraction populates these fields from uploaded documents

---

## Information Displayed by Document Type

### Passport Documents
- ✅ **Document Name** (filename)
- ✅ **Document Type** (PASSPORT)
- ✅ **Upload Date**
- ✅ **👤 Full Name** (extracted from passport)
- ✅ **Document Number** (passport number)
- ✅ **Expiry Date**

### VISA Documents
- ✅ **Document Name** (filename)
- ✅ **Document Type** (VISA)
- ✅ **Upload Date**
- ✅ **🏢 Company Name** (extracted from UK VISA)
- ✅ **Reference Number** (UK Home Office reference)
- ✅ **Date of Check** (UK VISA check date)
- ✅ **Expiry Date** (work permission expiry)

### Other Documents (ID Card, Driving License, etc.)
- ✅ **Document Name** (filename)
- ✅ **Document Type**
- ✅ **Upload Date**
- ✅ **Document Number** (if available)
- ✅ **Expiry Date** (if available)

---

## User Experience Flow

### Viewing Documents with Extracted Information:

1. Navigate to Employee List
2. Click "Details" on any employee
3. Scroll to "📄 Documents" section
4. **For Passports**: See extracted full name highlighted
5. **For VISAs**: See extracted company name highlighted
6. View additional details (reference number, dates)
7. Click document to view inline or download

---

## Technical Details

### Conditional Rendering

**Passport Full Name**:
```html
*ngIf="doc.documentType === 'PASSPORT' && doc.fullName"
```
- Only shows if document type is PASSPORT
- Only shows if fullName was successfully extracted

**VISA Company Name**:
```html
*ngIf="doc.documentType === 'VISA' && doc.companyName"
```
- Only shows if document type is VISA
- Only shows if companyName was successfully extracted

### Data Flow

```
Document Upload
    ↓
OCR Extraction (OcrService)
    ↓
Extract Full Name (Passport) OR Company Name (VISA)
    ↓
Save to Document Entity
    ↓
Convert to DocumentDTO
    ↓
Send to Frontend
    ↓
Display in Employee Details Modal
```

---

## Benefits

✅ **Better Context** - Users immediately see who the passport belongs to  
✅ **VISA Clarity** - Company name clearly displayed for work authorization  
✅ **Verification** - Easy to verify extracted information is correct  
✅ **Professional Look** - Highlighted info with icons stands out  
✅ **Consistent Display** - All document types show relevant extracted data  
✅ **No Manual Entry** - Information automatically extracted and displayed  

---

## Testing Checklist

- [x] Frontend model updated with VISA fields
- [x] HTML template updated to display company name (VISA)
- [x] HTML template updated to display full name (Passport)
- [x] CSS styling added for extracted info
- [x] Property names corrected (fileName, uploadedDate)
- [x] Reference number display added
- [x] Date of check display added
- [x] No compilation errors

---

## Next Steps to Test

1. **Restart frontend** (if running):
   ```cmd
   ng serve
   ```

2. **Navigate to Employee Details**:
   - Go to Employee List
   - Click "Details" on employee with documents

3. **Verify Display**:
   - **Passport documents** should show: 👤 Name: [Extracted Name]
   - **VISA documents** should show: 🏢 Company: [Extracted Company]
   - Check that reference numbers and dates are visible

4. **Upload New Documents**:
   - Upload a passport → Verify full name appears
   - Upload a UK VISA → Verify company name appears

---

## Status

✅ **COMPLETE** - Document display now shows:
- **Company name for VISA documents** with 🏢 icon
- **Full name for Passport documents** with 👤 icon
- **Reference number and date of check** for VISAs
- **Highlighted display** with proper styling
- **Fixed property names** (fileName, uploadedDate)

**Users can now see extracted company name for VISAs and full name for passports directly in the document list!** 🎉

