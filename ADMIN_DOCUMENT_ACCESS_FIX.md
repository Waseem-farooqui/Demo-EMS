# Admin Document Access Permission Fix

## Issue
Admin users (like Ali) could see documents of other admin users, which violates the access control policy.

## Required Access Control Policy
1. **SUPER_ADMIN**: Can view ALL documents (all employees)
2. **ADMIN**: Can ONLY view documents of USER role employees in their department
3. **USER**: Can ONLY view their own documents

## Changes Made

### 1. DocumentService.java
Updated the `canAccessEmployee()` method to implement proper access control:

**Before:**
- All admins and super admins could access all documents
- No distinction between ADMIN and SUPER_ADMIN permissions
- No check for target employee's role

**After:**
- **SUPER_ADMIN**: Full access to all documents (unchanged)
- **ADMIN**: 
  - ✅ Can view documents of USER role employees in their department
  - ❌ CANNOT view documents of other ADMIN users
  - ❌ CANNOT view documents of employees in other departments
- **USER**: Can only view their own documents (unchanged)

**Key Logic:**
```java
// For ADMIN users:
1. Check if target employee is in the same department
2. Check if target employee has USER role (not ADMIN)
3. Only grant access if both conditions are true
```

### 2. SecurityUtils.java
Added new helper method `getUserById(Long userId)`:
- Retrieves user by ID to check their roles
- Used to determine if target employee is an ADMIN or USER

## Testing Instructions

### Test Case 1: ADMIN viewing USER documents (Same Department)
**Expected**: ✅ Access GRANTED
- Admin Ali can view documents of USER role employees in his department

### Test Case 2: ADMIN viewing ADMIN documents
**Expected**: ❌ Access DENIED
- Admin Ali CANNOT view documents of other ADMIN users
- Error message: "Access denied. You can only view your own documents."

### Test Case 3: ADMIN viewing documents (Different Department)
**Expected**: ❌ Access DENIED
- Admin Ali CANNOT view documents of employees in other departments

### Test Case 4: SUPER_ADMIN viewing any documents
**Expected**: ✅ Access GRANTED
- SUPER_ADMIN can view ALL documents (no restrictions)

### Test Case 5: USER viewing own documents
**Expected**: ✅ Access GRANTED
- Regular users can view their own documents

### Test Case 6: USER viewing other documents
**Expected**: ❌ Access DENIED
- Regular users CANNOT view other employees' documents

## Deployment

### Backend
**Restart the backend application** for changes to take effect:
```bash
# Stop the current backend process
# Then restart:
cd C:\Users\waseem.uddin\EmployeeManagementSystem
mvnw.cmd spring-boot:run
```

### Frontend
No frontend changes required - the access control is enforced on the backend.

## Verification

After restarting the backend:

1. **Login as Admin Ali**
2. Try to view documents of another ADMIN user
3. **Expected Result**: Should see "Access denied" message or the documents should not appear in the list
4. Try to view documents of a USER in Ali's department
5. **Expected Result**: Should be able to view the documents successfully

## Log Messages

When access is checked, you'll see detailed log messages:

```
✅ SUPER_ADMIN access granted for employee ID: X
✅ ADMIN access granted for USER employee ID: X in same department
❌ ADMIN access denied: Target employee X is also an ADMIN
❌ ADMIN access denied: Employee X not in admin's department
✅ USER access granted for employee ID: X (own documents)
❌ USER access denied for employee ID: X
```

## Files Modified

1. `src/main/java/com/was/employeemanagementsystem/service/DocumentService.java`
   - Updated `canAccessEmployee()` method with proper role-based access control

2. `src/main/java/com/was/employeemanagementsystem/security/SecurityUtils.java`
   - Added `getUserById(Long userId)` helper method

## Security Notes

- Access control is enforced at the service layer
- All document operations (view, upload, delete, update) go through `canAccessEmployee()` check
- Attempting to bypass restrictions will result in "Access denied" exceptions
- All access attempts are logged for audit purposes

---

**Status**: ✅ FIXED - Admin users can no longer view other admin users' documents

**Date**: November 5, 2025

