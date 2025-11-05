# 🚀 ROOT User Creation - Quick Start Guide

## ✅ Implementation Complete

The ROOT user creation endpoint is now protected with **Basic Authentication** to prevent unauthorized access.

### 🔐 Basic Authentication Credentials
- **Username**: `waseem`
- **Password**: `wud19@WUD`

---

## 📋 Quick Commands

### 1️⃣ Check if ROOT User Exists

```bash
curl -X GET http://localhost:8080/api/init/root-exists
```

**Expected Response:**
```json
{
  "exists": false,
  "message": "ROOT user not found"
}
```

### 2️⃣ Create ROOT User (Method 1 - Recommended)

**Using cURL with -u flag (auto-encodes Basic Auth):**

```bash
curl -X POST http://localhost:8080/api/init/create-root \
  -u waseem:wud19@WUD \
  -H "Content-Type: application/json" \
  -d '{
    "username": "root",
    "email": "root@system.local",
    "password": "Root@123456"
  }'
```

### 3️⃣ Create ROOT User (Method 2 - Manual Base64)

**Using pre-encoded Base64 (waseem:wud19@WUD = d2FzZWVtOnd1ZDE5QFdVRA==):**

```bash
curl -X POST http://localhost:8080/api/init/create-root \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic d2FzZWVtOnd1ZDE5QFdVRA==" \
  -d '{
    "username": "root",
    "email": "root@system.local",
    "password": "Root@123456"
  }'
```

**Success Response:**
```json
{
  "success": true,
  "message": "ROOT user created successfully",
  "userId": 1,
  "username": "root",
  "email": "root@system.local"
}
```

**Error Response (if ROOT already exists):**
```json
{
  "success": false,
  "message": "ROOT user already exists. This endpoint can only be used once."
}
```

**Error Response (if Basic Auth is invalid):**
```json
{
  "success": false,
  "message": "Unauthorized. Valid Basic Authentication required."
}
```

---

## 🧪 Testing Workflow

### Step 1: Restart Backend
```bash
cd C:\Users\waseem.uddin\EmployeeManagementSystem
mvn spring-boot:run
```

### Step 2: Check ROOT User Status
```bash
curl http://localhost:8080/api/init/root-exists
```

### Step 3: Create ROOT User
```bash
curl -X POST http://localhost:8080/api/init/create-root \
  -u waseem:wud19@WUD \
  -H "Content-Type: application/json" \
  -d '{
    "username": "root",
    "email": "root@system.local",
    "password": "Root@123456"
  }'
```

### Step 4: Login as ROOT
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "root",
    "password": "Root@123456"
  }'
```

**Expected Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJyb290Iiw...",
  "username": "root",
  "email": "root@system.local",
  "roles": ["ROOT"]
}
```

### Step 5: Create Organization (as ROOT)
```bash
# Use token from Step 4
TOKEN="eyJhbGciOiJIUzI1NiJ9..."

curl -X POST http://localhost:8080/api/organizations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "organizationName": "My Company",
    "superAdminUsername": "admin",
    "superAdminEmail": "admin@mycompany.com",
    "password": "Admin@123",
    "superAdminFullName": "Company Admin"
  }'
```

---

## 🔒 Security Features

### ✅ What's Protected
1. **Basic Authentication Required**
   - Must provide valid username and password
   - Hardcoded credentials: `waseem:wud19@WUD`
   - Invalid credentials return 401 Unauthorized

2. **One-Time Use**
   - Endpoint only works if ROOT user doesn't exist
   - Prevents creating multiple ROOT users
   - Returns error if ROOT already exists

3. **Password Validation**
   - Password must be at least 8 characters
   - Strong password recommended
   - Password is hashed with BCrypt before storage

4. **No JWT Required**
   - `/api/init/**` endpoints don't require JWT
   - Only Basic Auth needed
   - Accessible before any users exist

### ✅ What Happens Behind the Scenes

1. **Request arrives** → `/api/init/create-root`
2. **Extract Authorization header** → `Basic d2FzZWVtOnd1ZDE5QFdVRA==`
3. **Decode Base64** → `waseem:wud19@WUD`
4. **Validate credentials** → Match hardcoded values
5. **Check ROOT exists** → Query database
6. **Create ROOT user** → Hash password, save to DB
7. **Add ROOT role** → Insert into user_roles table
8. **Return success** → Send JSON response

---

## 📝 PowerShell Commands (Windows)

If you're using PowerShell on Windows:

```powershell
# Check if ROOT exists
Invoke-RestMethod -Uri "http://localhost:8080/api/init/root-exists" -Method Get

# Create ROOT user
$basicAuth = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("waseem:wud19@WUD"))
$headers = @{
    "Authorization" = "Basic $basicAuth"
    "Content-Type" = "application/json"
}
$body = @{
    username = "root"
    email = "root@system.local"
    password = "Root@123456"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/init/create-root" -Method Post -Headers $headers -Body $body

# Login as ROOT
$loginBody = @{
    username = "root"
    password = "Root@123456"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -ContentType "application/json" -Body $loginBody
```

---

## 🐛 Troubleshooting

### Issue: "401 Unauthorized" or "Full authentication is required"
**Cause**: JWT filter was protecting the endpoint

**Solution Applied**: ✅ **FIXED!**
The `JwtAuthenticationFilter` has been updated to skip `/api/init/**` endpoints. The filter now includes a `shouldNotFilter()` method that excludes:
- `/api/init/*` - Initialization endpoints
- `/api/auth/*` - Authentication endpoints

**After restarting the backend**, the endpoint should work without JWT authentication, only requiring Basic Auth.

### Issue: "401 Unauthorized" (Basic Auth)
**Cause**: Invalid or missing Basic Auth credentials

**Solution**:
```bash
# Make sure you're using the correct credentials
curl -X POST http://localhost:8080/api/init/create-root \
  -u waseem:wud19@WUD \
  -H "Content-Type: application/json" \
  -d '{"username":"root","email":"root@system.local","password":"Root@123456"}'
```

### Issue: "ROOT user already exists"
**Solution**: ROOT user can only be created once. If you need to reset:

```sql
-- Delete ROOT user from database
DELETE FROM user_roles WHERE user_id IN (SELECT id FROM users WHERE username = 'root');
DELETE FROM users WHERE username = 'root';
```

Then try creating again.

### Issue: "Password is required"
**Cause**: Request body missing password field

**Solution**: Ensure JSON includes password:
```json
{
  "username": "root",
  "email": "root@system.local",
  "password": "Root@123456"
}
```

### Issue: Cannot connect to backend
**Check**:
1. Backend is running: `mvn spring-boot:run`
2. Port 8080 is not blocked
3. No firewall issues
4. Database is running

---

## 📊 Files Created/Modified

### New Files Created:
1. ✅ `InitializationController.java` - ROOT user creation endpoint
2. ✅ `ROOT_USER_CREATION_QUICK_START.md` - This guide

### Files Modified:
1. ✅ `SecurityConfig.java` - Added `/api/init/**` to permitAll
2. ✅ `CREATE_ROOT_USER_CURL.md` - Updated with Basic Auth commands

---

## 🎯 Summary

### What Was Implemented:
✅ **InitializationController** with Basic Auth protection
✅ **POST /api/init/create-root** - Create ROOT user
✅ **GET /api/init/root-exists** - Check if ROOT exists
✅ **Basic Auth validation** - Hardcoded credentials
✅ **One-time use** - Prevents duplicate ROOT users
✅ **Security configuration** - Allows init endpoints

### Basic Auth Credentials:
- Username: `waseem`
- Password: `wud19@WUD`
- Base64: `d2FzZWVtOnd1ZDE5QFdVRA==`

### ROOT User Default Credentials:
- Username: `root`
- Password: `Root@123456` (customizable)
- Email: `root@system.local`
- Role: `ROOT`
- Organization: `NULL`

---

## 🚀 Ready to Use!

1. **Restart backend** → `mvn spring-boot:run`
2. **Create ROOT user** → Use cURL command above
3. **Login as ROOT** → Get JWT token
4. **Create organizations** → Start managing tenants

**Next Step**: Create your first organization using the ROOT user token!

---

**Security Note**: ⚠️
- The Basic Auth credentials (`waseem:wud19@WUD`) are hardcoded
- In production, consider using environment variables
- The `/api/init/create-root` endpoint should be disabled or removed after initial setup
- Always use HTTPS in production

**Status**: ✅ Ready for Testing
**Implementation Date**: November 5, 2025

