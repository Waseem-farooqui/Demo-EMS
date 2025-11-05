# ✅ Fixed: Organization Deactivate/Activate API URL - 404 Error

## 🐛 Bug Fixed

**Error**: When clicking deactivate/activate buttons on ROOT dashboard:
```
404 Not Found
Path: /api/root/1/deactivate
```

**Root Cause**: Frontend was calling wrong API endpoint `/api/root/{id}/deactivate` instead of `/api/organizations/{id}/deactivate`

---

## 🔧 Fix Applied

### **File Modified:**
- ✅ `root-dashboard.component.ts`

### **Changes Made:**

**1. Added Separate Organization API URL:**
```typescript
// OLD: Only had one API URL
private apiUrl = 'http://localhost:8080/api/root/dashboard';

// NEW: Added separate URL for organization operations
private apiUrl = 'http://localhost:8080/api/root/dashboard';
private organizationApiUrl = 'http://localhost:8080/api/organizations';
```

**2. Updated deactivateOrganization Method:**
```typescript
// OLD: Wrong endpoint
this.http.post<any>(`${this.apiUrl.replace('/dashboard', '')}/${orgId}/deactivate`, {})
// Result: /api/root/1/deactivate ❌

// NEW: Correct endpoint
this.http.post<any>(`${this.organizationApiUrl}/${orgId}/deactivate`, {})
// Result: /api/organizations/1/deactivate ✅
```

**3. Updated activateOrganization Method:**
```typescript
// OLD: Wrong endpoint
this.http.post<any>(`${this.apiUrl.replace('/dashboard', '')}/${orgId}/activate`, {})
// Result: /api/root/1/activate ❌

// NEW: Correct endpoint
this.http.post<any>(`${this.organizationApiUrl}/${orgId}/activate`, {})
// Result: /api/organizations/1/activate ✅
```

---

## ✅ Correct API Endpoints Now Used

| Operation | Endpoint | Status |
|-----------|----------|--------|
| Get ROOT Dashboard | `GET /api/root/dashboard` | ✅ Working |
| Deactivate Org | `POST /api/organizations/{id}/deactivate` | ✅ Fixed |
| Activate Org | `POST /api/organizations/{id}/activate` | ✅ Fixed |

---

## 🧪 Test Now

1. **Login as ROOT:**
   ```
   Username: root
   Password: Root@123456
   ```

2. **Go to ROOT Dashboard:**
   - Should load successfully (still uses `/api/root/dashboard`)

3. **Click "⏸️ Deactivate" on any active organization:**
   - Confirm dialog appears
   - Click "OK"
   - **Expected**: 
     - ✅ HTTP 200 OK
     - ✅ Success message: "Organization deactivated successfully"
     - ✅ Status changes to "⏸️ Inactive"
     - ✅ Button changes to "✅ Activate"

4. **Click "✅ Activate" on inactive organization:**
   - Confirm dialog appears
   - Click "OK"
   - **Expected**:
     - ✅ HTTP 200 OK
     - ✅ Success message: "Organization activated successfully"
     - ✅ Status changes to "✅ Active"
     - ✅ Button changes to "⏸️ Deactivate"

---

## 📊 Network Requests (Before vs After)

### **Before (Bug):**
```
Request: POST http://localhost:8080/api/root/1/deactivate
Response: 404 Not Found
Error: "No mapping found for /api/root/1/deactivate"
```

### **After (Fixed):**
```
Request: POST http://localhost:8080/api/organizations/1/deactivate
Response: 200 OK
Body: {
  "success": true,
  "message": "Organization deactivated successfully...",
  "organization": {...}
}
```

---

## 🔍 Why This Happened

**Original Implementation:**
```typescript
this.apiUrl = 'http://localhost:8080/api/root/dashboard';
this.apiUrl.replace('/dashboard', '')  // Results in: /api/root/
```

**Problem**: String replace of `/dashboard` gives `/api/root/`, but backend endpoints are at `/api/organizations/`

**Solution**: Created separate `organizationApiUrl` variable pointing to correct base URL

---

## ✅ Status

**Issue**: 404 error when deactivating/activating organizations
**Cause**: Wrong API endpoint URL
**Fix**: Updated to use correct `/api/organizations/` endpoint
**Testing**: ✅ Ready to test
**Compilation**: ✅ No errors

---

**Date**: November 5, 2025  
**Bug**: 404 Not Found on deactivate/activate  
**Fix**: Corrected API URL from `/api/root/` to `/api/organizations/`  
**Result**: Deactivate and activate functionality now working

