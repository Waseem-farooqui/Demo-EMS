# ⚡ QUICK REFERENCE - UK VISA Field Mapping

## 📋 The Mapping (Simple Version)

```
UK VISA "Details of check" → Database Fields

Company name                    → companyName (display as-is)
Date of check                   → issueDate + dateOfCheck
Reference number                → documentNumber + referenceNumber
```

## 🎯 Why Dual Storage?

Each UK field is stored in **TWO places**:

1. **Standard field** (documentNumber, issueDate) - for consistency
2. **UK-specific field** (referenceNumber, dateOfCheck) - for UK display

This way:
- ✅ Standard reports/searches work across all documents
- ✅ UK-specific information is preserved
- ✅ Frontend can show either view

## 📊 Example

**Input**: UK VISA with
- Company: "Oceanway hospitality services Ltd"  
- Date of Check: "4 May 2025"
- Reference: "WE-4W9FW53-EL"

**Output**: Database stores
```json
{
  "documentNumber": "WE-4W9FW53-EL",      // ← From reference
  "issueDate": "2025-05-04",              // ← From date of check
  "companyName": "Oceanway hospitality services Ltd",
  "dateOfCheck": "2025-05-04",            // ← Original preserved
  "referenceNumber": "WE-4W9FW53-EL"      // ← Original preserved
}
```

## 🖥️ Frontend Display

### Simple Display (Use Standard Fields):
```html
Document Number: WE-4W9FW53-EL
Issue Date: 4 May 2025
Company Name: Oceanway hospitality services Ltd
```

### UK-Specific Display (Use UK Fields):
```html
Reference Number: WE-4W9FW53-EL
Date of Check: 4 May 2025
Company Name: Oceanway hospitality services Ltd
```

Both work! Your choice based on user preference.

## ✅ Status

- ✅ Backend: Complete (automatic mapping)
- ✅ Database: Complete (all fields available)
- ✅ API: Complete (returns all fields)
- ⚠️ **Action Required**: RESTART BACKEND

## 🚀 Test It

1. Restart backend
2. Upload UK VISA image
3. Check API response includes all fields
4. Done! 🎉

---

**Key Point**: Date of Check = Issue Date, Reference Number = Document Number (automatically mapped)

