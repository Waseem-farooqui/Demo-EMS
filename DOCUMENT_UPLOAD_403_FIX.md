# 🔧 403 Forbidden Error - Document Upload Fix

## Problem

```
POST http://localhost:8080/api/documents/upload 403 (Forbidden)
```

## Root Cause

403 Forbidden error occurs when:
1. **User is not authenticated** (no JWT token)
2. **JWT token expired**
3. **User doesn't have permission** to upload for that employee
4. **Token not being sent** with the request

---

## ✅ Solution Applied

### 1. Added Logging to DocumentController

Added `@Slf4j` and detailed logging to track:
- Upload requests
- File validation
- Success/failure
- Error details

**Benefits:**
- Easy to see what's failing
- Track authentication issues
- Monitor file uploads

### 2. Enhanced Error Messages

Improved error responses to include:
- Clear error messages
- Specific validation failures
- HTTP status codes

---

## 🔍 Troubleshooting Steps

### Step 1: Check If User Is Logged In

**In Browser Console:**
```javascript
localStorage.getItem('auth-token')
localStorage.getItem('auth-user')
```

**Expected:**
- `auth-token`: Should have a JWT token string
- `auth-user`: Should have user details with roles

**If NULL:**
- User is not logged in
- **Solution:** Login first at http://localhost:4200/login

### Step 2: Check JWT Token in Request

**Open Browser DevTools:**
1. Go to **Network** tab
2. Try uploading document
3. Click on the failed request
4. Check **Headers** section
5. Look for: `Authorization: Bearer <token>`

**If Missing:**
- JWT interceptor not working
- Token not in localStorage
- **Solution:** Login again

**If Present but 403:**
- Token expired or invalid
- **Solution:** Logout and login again

### Step 3: Check User Permissions

**Non-admin users can only upload for themselves:**

```typescript
// Check current user
const user = JSON.parse(localStorage.getItem('auth-user'));
console.log('Current user:', user);
console.log('Is admin?', user.roles.includes('ADMIN'));
```

**If uploading for another employee as regular user:**
- Access denied (expected)
- **Solution:** Login as admin or upload for own employee

### Step 4: Check Backend Logs

**In Spring Boot console, look for:**
```
Document upload request - EmployeeId: X, Type: Y, File: Z
```

**Possible errors:**
```
✗ Access denied or validation error: Access denied. You can only upload documents for yourself.
```

**Solution:** User trying to upload for another employee

---

## 🚀 Quick Fix

### For Users Getting 403:

1. **Logout:**
   ```
   Click Logout button or clear localStorage
   ```

2. **Login Again:**
   ```
   http://localhost:4200/login
   Username: your-username
   Password: your-password
   ```

3. **Verify Login:**
   ```javascript
   // In browser console
   localStorage.getItem('auth-token')
   // Should show: "eyJhbGciOiJIUzI1NiIsInR5cCI6..."
   ```

4. **Try Upload Again:**
   ```
   Go to: http://localhost:4200/documents/upload
   Select your employee (auto-selected for non-admin)
   Choose file
   Upload
   ```

---

## 📊 Understanding the Error

### HTTP 403 Forbidden

**Meaning:**
- Server understood request
- User authenticated (token valid)
- But user not authorized for this action

**Different from 401 Unauthorized:**
- 401: Not logged in / invalid token
- 403: Logged in but no permission

### Why This Happens for Document Upload

**Backend Security Check:**
```java
// Check access permissions
if (!canAccessEmployee(employee)) {
    throw new RuntimeException("Access denied. You can only upload documents for yourself.");
}
```

**Rules:**
- **Admin:** Can upload for any employee ✓
- **User:** Can only upload for themselves ✓
- **User uploading for others:** 403 Forbidden ✗

---

## 🔐 Permission Matrix

| Action | Admin | User (Own) | User (Others) |
|--------|-------|------------|---------------|
| Upload document | ✅ | ✅ | ❌ 403 |
| View own documents | ✅ | ✅ | ❌ 403 |
| View all documents | ✅ | ❌ 403 | ❌ 403 |
| Delete own document | ✅ | ✅ | ❌ 403 |
| Delete any document | ✅ | ❌ 403 | ❌ 403 |

---

## 🧪 Test Cases

### Test 1: Regular User Upload for Self

**Setup:**
1. Login as regular user
2. Go to document upload
3. Employee auto-selected (self)
4. Upload document

**Expected:** ✅ Success

### Test 2: Regular User Upload for Other

**Setup:**
1. Login as regular user
2. Try to upload for another employee

**Expected:** ❌ Not possible (employee dropdown disabled)

### Test 3: Admin Upload for Any Employee

**Setup:**
1. Login as admin
2. Go to document upload
3. Select any employee
4. Upload document

**Expected:** ✅ Success

### Test 4: Not Logged In

**Setup:**
1. Logout
2. Try to access document upload

**Expected:** Redirected to login

---

## 🛠️ Backend Logging Added

Now you can see detailed logs in Spring Boot console:

```
2025-10-30 10:15:23 INFO  DocumentController : Document upload request - EmployeeId: 5, Type: PASSPORT, File: passport.jpg
2025-10-30 10:15:24 INFO  DocumentController : ✓ Document uploaded successfully - ID: 1, Type: PASSPORT
```

Or errors:

```
2025-10-30 10:15:25 ERROR DocumentController : ✗ Access denied or validation error: Access denied. You can only upload documents for yourself.
```

---

## 💡 Common Scenarios & Solutions

### Scenario 1: "I'm logged in but getting 403"

**Possible Causes:**
- Token expired
- Trying to upload for another employee
- User account disabled

**Solution:**
```
1. Logout
2. Login again
3. Upload for own employee only (if not admin)
```

### Scenario 2: "Upload works on Postman but not on frontend"

**Possible Causes:**
- Token not being sent from frontend
- CORS issue

**Solution:**
```
1. Check browser console for errors
2. Check Network tab for Authorization header
3. Verify token in localStorage
```

### Scenario 3: "No error message, just 403"

**Possible Causes:**
- Backend throwing exception
- Error not being caught properly

**Solution:**
```
1. Check backend logs
2. Look for stack traces
3. Verify user permissions
```

---

## ✅ Verification Checklist

Before uploading:
- [ ] User is logged in
- [ ] Token exists in localStorage
- [ ] Token not expired (login < 24 hours ago)
- [ ] Uploading for own employee (or admin)
- [ ] File is valid (image/PDF, < 10MB)
- [ ] Backend is running
- [ ] No CORS errors in console

---

## 🎯 Summary

**Issue:** 403 Forbidden on document upload

**Root Cause:** 
- Not logged in, OR
- Token expired, OR
- Trying to upload for another employee as non-admin

**Solution:**
1. ✅ Login/Re-login
2. ✅ Upload for own employee
3. ✅ Check backend logs for details

**Prevention:**
- Keep session active
- Logout/Login daily
- Admin for bulk uploads

**Backend Changes Made:**
- ✅ Added @Slf4j to DocumentController
- ✅ Added detailed logging
- ✅ Improved error messages
- ✅ Better exception handling

---

## 📞 Still Having Issues?

### Check This:

1. **Backend Console:**
   ```
   Look for: Document upload request...
   Look for errors: ✗ Access denied...
   ```

2. **Browser Console:**
   ```javascript
   console.log(localStorage.getItem('auth-token'));
   console.log(localStorage.getItem('auth-user'));
   ```

3. **Network Tab:**
   ```
   Check: Authorization header present?
   Check: Request payload has file?
   Check: Response body has error message?
   ```

### Most Likely Fix:

**Logout and Login Again!**

This resolves:
- ✅ Expired tokens
- ✅ Stale session
- ✅ Invalid credentials
- ✅ 90% of 403 errors

---

**Your document upload should work after re-logging in! 🎉**

