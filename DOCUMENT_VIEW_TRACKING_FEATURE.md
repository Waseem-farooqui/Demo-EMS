# Document View Tracking Feature

## Overview
Implemented a feature to track when documents were last viewed and by whom. This tracking is automatically recorded whenever a SUPER_ADMIN or ADMIN clicks "View Details" on any document.

## Changes Made

### Backend Changes

#### 1. Database Schema Updates
**File:** `V13__Add_Document_View_Tracking.sql`

Added two new columns to the `documents` table:
- `last_viewed_at` (DATETIME) - Timestamp of when the document was last viewed
- `last_viewed_by` (VARCHAR) - Username of the person who last viewed the document
- Added indexes for better query performance

**Migration will run automatically on next backend restart.**

#### 2. Document Entity Updates
**File:** `Document.java`

Added fields:
```java
@Column(name = "last_viewed_at")
private LocalDateTime lastViewedAt;

@Column(name = "last_viewed_by")
private String lastViewedBy;
```

#### 3. DocumentDTO Updates
**File:** `DocumentDTO.java`

Added fields to transfer view tracking data:
```java
private LocalDateTime lastViewedAt;
private String lastViewedBy;
```

#### 4. DocumentService Updates
**File:** `DocumentService.java`

Updated `getDocumentById()` method to track views:
- ✅ Checks if user is ADMIN or SUPER_ADMIN
- ✅ Records current timestamp
- ✅ Records username
- ✅ Saves to database
- ✅ Logs the view action

```java
// Track document view for ADMIN and SUPER_ADMIN
if (securityUtils.isAdminOrSuperAdmin()) {
    User currentUser = securityUtils.getCurrentUser();
    if (currentUser != null) {
        document.setLastViewedAt(LocalDateTime.now());
        document.setLastViewedBy(currentUser.getUsername());
        documentRepository.save(document);
        log.info("📊 Document {} viewed by {} at {}", id, username, timestamp);
    }
}
```

Updated `convertToDTO()` method to include view tracking fields in response.

### Frontend Changes

#### 1. Document Model Updates
**File:** `document.model.ts`

Added fields to interface:
```typescript
lastViewedAt?: string;
lastViewedBy?: string;
```

#### 2. Document List Component Updates
**File:** `document-list.component.html`

Added display of view tracking information on document cards:
```html
<div class="info-row" *ngIf="doc.lastViewedAt">
  <span class="label">Last Viewed:</span>
  <span class="value">{{ formatDateTime(doc.lastViewedAt) }}</span>
</div>

<div class="info-row" *ngIf="doc.lastViewedBy">
  <span class="label">Viewed By:</span>
  <span class="value">{{ doc.lastViewedBy }}</span>
</div>
```

**File:** `document-list.component.ts`

Added `formatDateTime()` method to display dates with time:
```typescript
formatDateTime(dateStr: string): string {
  const date = new Date(dateStr);
  return date.toLocaleString('en-GB', { 
    day: '2-digit', 
    month: 'short', 
    year: 'numeric',
    hour: '2-digit', 
    minute: '2-digit'
  });
}
```

## How It Works

### Tracking Flow

1. **ADMIN or SUPER_ADMIN** navigates to documents page
2. Clicks **"View Details"** button on any document
3. Backend `getDocumentById()` method is called
4. System checks if user is ADMIN/SUPER_ADMIN
5. If yes:
   - Records current timestamp → `last_viewed_at`
   - Records username → `last_viewed_by`
   - Saves to database
   - Logs the view action
6. Returns document data with view tracking info
7. Frontend displays the information on document card

### Who Can See This Information?

**View Tracking is Recorded For:**
- ✅ SUPER_ADMIN users
- ✅ ADMIN users

**Not Recorded For:**
- ❌ Regular USER role (they can only view their own documents anyway)

**Display:**
- All users who can access the documents page can see the "Last Viewed" information
- This provides transparency about document access

## Display Examples

### Document Card Display

```
┌─────────────────────────────────────┐
│ PASSPORT           EXPIRED          │
├─────────────────────────────────────┤
│ John Doe                            │
│                                     │
│ Document No:  AB123456              │
│ Expiry Date:  15 Oct 2025           │
│ Nationality:  British               │
│ Uploaded:     05 Nov 2025           │
│ Last Viewed:  05 Nov 2025, 14:30    │ ← NEW
│ Viewed By:    waseem                │ ← NEW
├─────────────────────────────────────┤
│ [View Details]  [Delete]            │
└─────────────────────────────────────┘
```

### Date Format
- **Uploaded:** `05 Nov 2025` (date only)
- **Last Viewed:** `05 Nov 2025, 14:30` (date and time)

## Use Cases

### 1. Audit Trail
Track which admins have accessed employee documents for compliance and security.

### 2. Document Review Status
Quickly see if a document has been reviewed by management.

### 3. Workload Distribution
See which admin is handling which employee's documents.

### 4. Accountability
Clear record of who accessed sensitive employee information.

## Database Migration

The migration will run automatically when you restart the backend:

**SQL executed:**
```sql
ALTER TABLE documents 
ADD COLUMN last_viewed_at DATETIME NULL,
ADD COLUMN last_viewed_by VARCHAR(255) NULL;

CREATE INDEX idx_documents_last_viewed ON documents(last_viewed_at);
CREATE INDEX idx_documents_last_viewed_by ON documents(last_viewed_by);
```

## Testing Instructions

### Test Case 1: View Tracking for ADMIN
1. Login as ADMIN user
2. Go to Documents page
3. Click "View Details" on any document
4. Go back to Documents list
5. **Expected:** Document card shows "Last Viewed" with current time and your username

### Test Case 2: View Tracking for SUPER_ADMIN
1. Login as SUPER_ADMIN (CEO)
2. Go to Documents page
3. Click "View Details" on any document
4. Go back to Documents list
5. **Expected:** Document card shows "Last Viewed" with current time and your username

### Test Case 3: No Tracking for Regular Users
1. Login as regular USER
2. Go to Documents page (if they have their own documents)
3. View a document
4. **Expected:** View tracking may or may not be recorded (as per business rules)

### Test Case 4: Multiple Views
1. Admin A views a document
2. Go back to list - shows Admin A viewed it
3. Admin B views the same document
4. Go back to list - now shows Admin B viewed it (most recent)
5. **Expected:** Only the MOST RECENT view is recorded

### Test Case 5: Initial State
1. Upload a new document
2. View documents list
3. **Expected:** "Last Viewed" and "Viewed By" fields are NOT shown (document never viewed yet)

## API Response Example

**GET `/api/documents/{id}`**

Response now includes:
```json
{
  "id": 1,
  "employeeId": 5,
  "employeeName": "John Doe",
  "documentType": "PASSPORT",
  "documentNumber": "AB123456",
  ...
  "uploadedDate": "2025-11-05T10:30:00",
  "lastViewedAt": "2025-11-05T14:30:45",
  "lastViewedBy": "waseem",
  "daysUntilExpiry": -21
}
```

## Performance Considerations

- ✅ Indexes added on `last_viewed_at` and `last_viewed_by` for fast queries
- ✅ View tracking happens AFTER access control checks (no unnecessary writes)
- ✅ Only writes to database when view is legitimate
- ✅ Minimal performance impact (single UPDATE query per view)

## Security & Privacy

- ✅ Only ADMIN and SUPER_ADMIN views are tracked
- ✅ Access control enforced BEFORE tracking
- ✅ Cannot track views of documents you don't have access to
- ✅ Username (not sensitive data) is stored
- ✅ Audit trail for compliance requirements

## Future Enhancements (Optional)

Possible future improvements:
- 📊 Track ALL views (not just most recent) in a separate audit table
- 📊 View history with timestamps
- 📊 Analytics dashboard showing document access patterns
- 📊 Export view audit logs
- 📊 Alerts when documents are accessed unusually frequently

## Deployment

### Backend
1. **Restart the backend** - migration will run automatically
2. Verify migration in logs:
   ```
   Flyway migration V13__Add_Document_View_Tracking.sql successful
   ```

### Frontend
**Status**: ✅ Already built and ready
- Run hard refresh (Ctrl+Shift+R) to clear cache

### Verification
1. Check database:
   ```sql
   DESC documents;
   -- Should show last_viewed_at and last_viewed_by columns
   ```

2. View a document as ADMIN
3. Check database:
   ```sql
   SELECT id, document_type, last_viewed_at, last_viewed_by 
   FROM documents 
   WHERE last_viewed_at IS NOT NULL;
   ```

## Log Messages

When document is viewed, you'll see:
```
📊 Document 1 viewed by waseem at 2025-11-05T14:30:45.123
```

---

**Status**: ✅ COMPLETE - Document View Tracking Implemented
**Date**: November 5, 2025
**Files Modified**: 8 files (4 backend, 3 frontend, 1 migration)

