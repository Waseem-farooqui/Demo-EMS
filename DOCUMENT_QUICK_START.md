# 🚀 Document Management - Quick Start Guide

## ✅ What's Been Implemented

A complete document management system with:
- 📤 **Upload Interface** - Upload passport/visa documents
- 🔍 **OCR Text Extraction** - Automatic data extraction
- 📊 **Data Display** - View all extracted information
- ⚠️ **Duplicate Detection** - Warns about duplicate documents
- 🔄 **Reupload** - Easy reupload after viewing results
- 📱 **Document List** - Grid view with filtering
- 🔐 **Role-Based Access** - Users see only their documents

---

## 🏃 Quick Start

### Step 1: Start Backend
```
Run EmployeeManagementSystemApplication in IntelliJ
```
Backend will run on: http://localhost:8080

### Step 2: Start Frontend
```cmd
cd C:\Users\waseem.uddin\EmployeeManagementSystem\frontend
npm start
```
Frontend will run on: http://localhost:4200

### Step 3: Access Document Management

1. **Login** to the application
2. **Navigate** to Employee List
3. **Click** "Documents" button in header
4. **Or** go directly to: http://localhost:4200/documents

---

## 📤 Upload Your First Document

### Step-by-Step:

1. **Click** "Upload Document" button

2. **Select Employee** 
   - Admin: Choose from dropdown
   - User: Auto-selected (you)

3. **Select Document Type**
   - PASSPORT
   - VISA

4. **Choose File**
   - Click "Choose File"
   - Select passport/visa image or PDF
   - Max size: 10MB
   - Formats: JPG, PNG, PDF

5. **Click** "Upload Document"

6. **Wait** for processing (OCR extraction)

7. **View** extracted data:
   - Document number
   - Issue & expiry dates
   - Personal information
   - Full extracted text
   - Duplicate warning (if applicable)

8. **Next Actions:**
   - "Upload Another" - Upload more documents
   - "View All Documents" - See document list

---

## 📋 View All Documents

### From Document List:

1. **Navigate** to /documents

2. **Filter** documents:
   - All Documents
   - Passports only
   - Visas only

3. **View** document cards showing:
   - Document type
   - Expiry status
   - Employee name
   - Document number
   - Key dates

4. **Click** "View Details" for full information

5. **Click** "Delete" to remove document

---

## ⚠️ Duplicate Detection

### How It Works:

When you upload a document:
1. System extracts document number
2. Checks if same document exists
3. Shows warning if duplicate found
4. Displays existing document details

**Example Warning:**
```
⚠ Warning: A similar PASSPORT document already exists 
with number N1234567. Uploaded on 20/10/2025.

Please verify if you intended to upload a duplicate document.
```

**What To Do:**
- **If intentional:** Keep both documents
- **If mistake:** Delete one and keep the correct one
- **If reupload needed:** Delete old, upload new

---

## 🎨 UI Features

### Upload Page
```
┌─────────────────────────────────┐
│ Upload Document                 │
│ ─────────────────────────────── │
│ Select Employee:  [John Doe ▼] │
│ Document Type:    [PASSPORT ▼] │
│ File:            [Choose File]  │
│                  passport.jpg   │
│                                 │
│ [Upload Document]  [Reset]     │
└─────────────────────────────────┘
```

### Extracted Data
```
┌─────────────────────────────────┐
│ ✓ Document Uploaded!            │
│ [Upload Another] [View All]     │
├─────────────────────────────────┤
│ DOCUMENT INFORMATION            │
│ • Employee: John Doe            │
│ • Type: PASSPORT                │
│ • Number: N1234567              │
│                                 │
│ DATES                           │
│ • Issue: 15/01/2020             │
│ • Expiry: 15/01/2030            │
│ • Status: VALID (1538 days)    │
│                                 │
│ PERSONAL INFO                   │
│ • Name: JOHN DOE                │
│ • DOB: 20/05/1990               │
│ • Nationality: British          │
└─────────────────────────────────┘
```

### Document List
```
┌─────┐ ┌─────┐ ┌─────┐
│PASS │ │VISA │ │PASS │
│PORT │ │     │ │PORT │
├─────┤ ├─────┤ ├─────┤
│John │ │Jane │ │Mike │
│Doe  │ │Smith│ │Lee  │
└─────┘ └─────┘ └─────┘
```

---

## 🔐 Role-Based Access

### Admin Users
✅ Upload for any employee
✅ View all documents
✅ Delete any document
✅ Full access

### Regular Users
✅ Upload for themselves only
✅ View only own documents
✅ Delete own documents
❌ Cannot see others' documents

---

## 📊 Expiry Status Indicators

| Status | Color | Days Until Expiry |
|--------|-------|-------------------|
| **VALID** | 🟢 Green | More than 90 days |
| **EXPIRES IN X DAYS** | 🟡 Yellow | 31-90 days |
| **EXPIRING SOON** | 🟠 Orange | 1-30 days |
| **EXPIRED** | 🔴 Red | Already expired |

---

## ✅ File Requirements

### Accepted Formats:
- ✅ JPG / JPEG images
- ✅ PNG images
- ✅ PDF documents

### File Size:
- ✅ Maximum: 10MB
- ❌ Larger files rejected

### Document Types:
- ✅ PASSPORT
- ✅ VISA

---

## 🧪 Quick Test

### Test Upload:

1. **Login** as user
2. **Go to** /documents/upload
3. **Select** PASSPORT
4. **Choose** a passport image/PDF
5. **Click** Upload Document
6. **Verify** OCR extracted:
   - Document number
   - Dates
   - Personal info
7. **Check** expiry status
8. **Try** uploading same document again
9. **See** duplicate warning

---

## 🎯 Navigation

### Main Menu:
```
Employee List
    ↓
┌─────────────┬──────────────┬──────────┐
│ Documents   │ Leave Mgmt   │ Add Emp  │
└─────────────┴──────────────┴──────────┘
```

### Document Flow:
```
Documents → Upload → View Data → All Documents
                 ↓
            Upload Another
```

---

## 📝 Common Actions

### Upload New Document:
```
Documents → Upload Document → Select & Upload → View Results
```

### View Document Details:
```
Documents → Find Card → View Details → See Full Info
```

### Delete Document:
```
Documents → Find Card → Delete → Confirm
```

### Check Expiring Documents:
```
Documents → Filter → Look for orange/red badges
```

---

## 🐛 Troubleshooting

### File Upload Fails
**Problem:** Cannot upload file
**Check:**
- File is JPG, PNG, or PDF
- File size < 10MB
- Employee selected
- Document type selected

### OCR Not Extracting Data
**Problem:** No data extracted
**Check:**
- Image is clear and readable
- Text is in English
- Document is properly oriented
- File not corrupted

### Duplicate Warning Doesn't Show
**Problem:** Expected warning not displayed
**Check:**
- Document number was extracted
- Same employee and document type
- Check console for logs

### Cannot See Documents
**Problem:** Document list empty
**Check:**
- Documents uploaded for this employee
- Logged in correctly
- User/admin role correct

---

## 🎊 Summary

**Created:**
- Upload page with OCR extraction
- Document list with filtering
- Duplicate detection system
- Details modal view
- Role-based access control

**Features:**
- Upload → Extract → Display → Warn if duplicate
- View all documents in grid
- Filter by type
- Delete documents
- Responsive design

**Access:**
- Upload: http://localhost:4200/documents/upload
- List: http://localhost:4200/documents

**Ready to use! Start uploading documents now! 📄✨**

---

## 📚 Documentation

- **Complete Guide:** DOCUMENT_FRONTEND_COMPLETE.md
- **Backend Guide:** DOCUMENT_MANAGEMENT_COMPLETE.md
- **API Endpoints:** See backend documentation

---

**Need Help?**
- Check browser console for errors
- Verify backend is running
- Check network tab for API responses
- Review validation messages

