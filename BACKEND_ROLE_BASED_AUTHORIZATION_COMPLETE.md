# ✅ Backend Role-Based API Authorization - COMPLETE

## 🎯 Problem Solved

**Issue**: Backend was not enforcing role-based restrictions on API endpoints. ROOT could access employee endpoints, and SUPER_ADMIN/ADMIN/USER could access ROOT endpoints.

**Solution**: Added `@PreAuthorize` annotations to ALL controller endpoints with proper role restrictions.

---

## 🔒 Authorization Matrix

### **Employee Endpoints** (`/api/employees`)
| Endpoint | ROOT | SUPER_ADMIN | ADMIN | USER |
|----------|------|-------------|-------|------|
| `POST /employees` | ❌ 403 | ✅ Allow | ✅ Allow | ❌ 403 |
| `POST /employees/profile` | ❌ 403 | ✅ Allow | ✅ Allow | ✅ Allow |
| `GET /employees` | ❌ 403 | ✅ Allow | ✅ Allow | ✅ Allow |
| `GET /employees/{id}` | ❌ 403 | ✅ Allow | ✅ Allow | ✅ Allow |
| `PUT /employees/{id}` | ❌ 403 | ✅ Allow | ✅ Allow | ✅ Allow |
| `DELETE /employees/{id}` | ❌ 403 | ✅ Allow | ✅ Allow | ❌ 403 |

### **Dashboard Endpoint** (`/api/dashboard`)
| Endpoint | ROOT | SUPER_ADMIN | ADMIN | USER |
|----------|------|-------------|-------|------|
| `GET /dashboard/stats` | ❌ 403 | ✅ Allow | ❌ 403 | ❌ 403 |

**Note**: Dashboard is for SUPER_ADMIN ONLY (CEO view)

### **Document Endpoints** (`/api/documents`)
| Endpoint | ROOT | SUPER_ADMIN | ADMIN | USER |
|----------|------|-------------|-------|------|
| `POST /documents/upload` | ❌ 403 | ✅ Allow | ✅ Allow | ✅ Allow |
| `GET /documents` | ❌ 403 | ✅ Allow | ✅ Allow | ✅ Allow |
| `GET /documents/{id}` | ❌ 403 | ✅ Allow | ✅ Allow | ✅ Allow |
| `GET /documents/employee/{id}` | ❌ 403 | ✅ Allow | ✅ Allow | ✅ Allow |
| `GET /documents/expiring` | ❌ 403 | ✅ Allow | ✅ Allow | ❌ 403 |
| `DELETE /documents/{id}` | ❌ 403 | ✅ Allow | ✅ Allow | ✅ Allow |

### **Leave Endpoints** (`/api/leaves`)
| Endpoint | ROOT | SUPER_ADMIN | ADMIN | USER |
|----------|------|-------------|-------|------|
| `POST /leaves` | ❌ 403 | ✅ Allow | ✅ Allow | ✅ Allow |
| `GET /leaves` | ❌ 403 | ✅ Allow | ✅ Allow | ✅ Allow |
| `GET /leaves/{id}` | ❌ 403 | ✅ Allow | ✅ Allow | ✅ Allow |
| `GET /leaves/employee/{id}` | ❌ 403 | ✅ Allow | ✅ Allow | ✅ Allow |
| `GET /leaves/status/{status}` | ❌ 403 | ✅ Allow | ✅ Allow | ✅ Allow |
| `PUT /leaves/{id}` | ❌ 403 | ✅ Allow | ✅ Allow | ✅ Allow |
| `PUT /leaves/{id}/approve` | ❌ 403 | ✅ Allow | ✅ Allow | ❌ 403 |
| `PUT /leaves/{id}/reject` | ❌ 403 | ✅ Allow | ✅ Allow | ❌ 403 |
| `DELETE /leaves/{id}` | ❌ 403 | ✅ Allow | ✅ Allow | ✅ Allow |

### **Attendance Endpoints** (`/api/attendance`)
| Endpoint | ROOT | SUPER_ADMIN | ADMIN | USER |
|----------|------|-------------|-------|------|
| `POST /attendance/clock-in` | ❌ 403 | ✅ Allow | ✅ Allow | ✅ Allow |
| `POST /attendance/clock-out` | ❌ 403 | ✅ Allow | ✅ Allow | ✅ Allow |
| `GET /attendance/...` | ❌ 403 | ✅ Allow | ✅ Allow | ✅ Allow |

**Note**: SUPER_ADMIN typically doesn't clock in/out (CEO doesn't track time)

### **Rota Endpoints** (`/api/rota`)
| Endpoint | ROOT | SUPER_ADMIN | ADMIN | USER |
|----------|------|-------------|-------|------|
| `POST /rota/upload` | ❌ 403 | ✅ Allow | ✅ Allow | ❌ 403 |
| `GET /rota/...` | ❌ 403 | ✅ Allow | ✅ Allow | ✅ Allow |
| `PUT /rota/...` | ❌ 403 | ✅ Allow | ✅ Allow | ❌ 403 |
| `DELETE /rota/...` | ❌ 403 | ✅ Allow | ✅ Allow | ❌ 403 |

### **Organization Endpoints** (`/api/organizations`)
| Endpoint | ROOT | SUPER_ADMIN | ADMIN | USER |
|----------|------|-------------|-------|------|
| `POST /organizations` | ✅ Allow | ❌ 403 | ❌ 403 | ❌ 403 |
| `GET /organizations` | ✅ Allow | ❌ 403 | ❌ 403 | ❌ 403 |
| `GET /organizations/{id}` | ✅ Allow | ❌ 403 | ❌ 403 | ❌ 403 |
| `PUT /organizations/{id}` | ✅ Allow | ❌ 403 | ❌ 403 | ❌ 403 |
| `POST /organizations/{id}/logo` | ✅ Allow | ❌ 403 | ❌ 403 | ❌ 403 |

### **ROOT Dashboard** (`/api/root/dashboard`)
| Endpoint | ROOT | SUPER_ADMIN | ADMIN | USER |
|----------|------|-------------|-------|------|
| `GET /root/dashboard/stats` | ✅ Allow | ❌ 403 | ❌ 403 | ❌ 403 |
| `GET /root/dashboard/organization/{id}` | ✅ Allow | ❌ 403 | ❌ 403 | ❌ 403 |

---

## 🔧 Implementation Details

### **Controllers Modified (5 files)**

#### 1. **EmployeeController.java** ✅
```java
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')") // Create employee
@PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SUPER_ADMIN')") // Get/Update
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')") // Delete
```

#### 2. **DocumentController.java** ✅
```java
@PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SUPER_ADMIN')") // Most operations
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')") // View expiring
```

#### 3. **LeaveController.java** ✅
```java
@PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SUPER_ADMIN')") // Apply/View
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')") // Approve/Reject
```

#### 4. **OrganizationController.java** ✅
```java
@PreAuthorize("hasRole('ROOT')") // ALL endpoints
```

#### 5. **DashboardController.java** ✅
```java
@PreAuthorize("hasRole('SUPER_ADMIN')") // Dashboard stats (already had this)
```

**Already Protected:**
- ✅ **AttendanceController.java** - All endpoints have `@PreAuthorize`
- ✅ **RotaController.java** - All endpoints have `@PreAuthorize`
- ✅ **RootDashboardController.java** - Has `@PreAuthorize("hasRole('ROOT')")`

---

## 🧪 Testing

### **Test 1: ROOT Cannot Access Employee Endpoints**
```bash
# Login as ROOT
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"root","password":"Root@123456"}' \
  | jq -r '.token' > root_token.txt

ROOT_TOKEN=$(cat root_token.txt)

# Try to access employees (should fail with 403)
curl -i http://localhost:8080/api/employees \
  -H "Authorization: Bearer $ROOT_TOKEN"

# Expected: HTTP/1.1 403 Forbidden
# Response: Access is denied
```

### **Test 2: ROOT Cannot Access Documents**
```bash
curl -i http://localhost:8080/api/documents \
  -H "Authorization: Bearer $ROOT_TOKEN"

# Expected: HTTP/1.1 403 Forbidden
```

### **Test 3: ROOT Cannot Access Leaves**
```bash
curl -i http://localhost:8080/api/leaves \
  -H "Authorization: Bearer $ROOT_TOKEN"

# Expected: HTTP/1.1 403 Forbidden
```

### **Test 4: ROOT Can Access Organizations**
```bash
curl -i http://localhost:8080/api/organizations \
  -H "Authorization: Bearer $ROOT_TOKEN"

# Expected: HTTP/1.1 200 OK
# Response: List of organizations
```

### **Test 5: ROOT Can Access ROOT Dashboard**
```bash
curl -i http://localhost:8080/api/root/dashboard/stats \
  -H "Authorization: Bearer $ROOT_TOKEN"

# Expected: HTTP/1.1 200 OK
# Response: Organization statistics
```

### **Test 6: SUPER_ADMIN Cannot Access Organizations**
```bash
# Login as SUPER_ADMIN
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin@acme.com","password":"Admin@123"}' \
  | jq -r '.token' > admin_token.txt

ADMIN_TOKEN=$(cat admin_token.txt)

# Try to access organizations (should fail with 403)
curl -i http://localhost:8080/api/organizations \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Expected: HTTP/1.1 403 Forbidden
```

### **Test 7: SUPER_ADMIN Cannot Access ROOT Dashboard**
```bash
curl -i http://localhost:8080/api/root/dashboard/stats \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Expected: HTTP/1.1 403 Forbidden
```

### **Test 8: SUPER_ADMIN Can Access Employee Dashboard**
```bash
curl -i http://localhost:8080/api/dashboard/stats \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Expected: HTTP/1.1 200 OK
# Response: Employee statistics
```

### **Test 9: ADMIN Cannot Access Employee Dashboard**
```bash
# Login as ADMIN (not SUPER_ADMIN)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin_dept","password":"Password123"}' \
  | jq -r '.token' > dept_admin_token.txt

DEPT_ADMIN_TOKEN=$(cat dept_admin_token.txt)

# Try to access dashboard (should fail with 403)
curl -i http://localhost:8080/api/dashboard/stats \
  -H "Authorization: Bearer $DEPT_ADMIN_TOKEN"

# Expected: HTTP/1.1 403 Forbidden
```

### **Test 10: USER Cannot Approve Leaves**
```bash
# Login as USER
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"john.doe","password":"Password123"}' \
  | jq -r '.token' > user_token.txt

USER_TOKEN=$(cat user_token.txt)

# Try to approve leave (should fail with 403)
curl -i -X PUT http://localhost:8080/api/leaves/1/approve \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"approvedBy":"John Doe","remarks":"Approved"}'

# Expected: HTTP/1.1 403 Forbidden
```

---

## 🔐 Security Layers

### **Layer 1: Spring Security `@PreAuthorize`**
- Annotation on controller methods
- Checked BEFORE method execution
- Returns 403 Forbidden if unauthorized

### **Layer 2: Service Layer Validation**
- Additional role checks in service methods
- Organization UUID validation
- Department-based access control

### **Layer 3: HTTP Interceptor**
- Organization UUID validation
- Request attribute injection
- Cross-organization access prevention

---

## 🎯 Summary

### **What Was Implemented:**
✅ Added `@PreAuthorize` to **EmployeeController** (6 endpoints)
✅ Added `@PreAuthorize` to **DocumentController** (6 endpoints)  
✅ Added `@PreAuthorize` to **LeaveController** (9 endpoints)
✅ Added `@PreAuthorize` to **OrganizationController** (5 endpoints - ROOT only)
✅ Verified **DashboardController** has proper authorization
✅ Verified **AttendanceController** has proper authorization
✅ Verified **RotaController** has proper authorization
✅ Verified **RootDashboardController** has proper authorization

### **Security Enforcement:**
✅ **ROOT** can ONLY access:
   - `/api/organizations/**`
   - `/api/root/dashboard/**`

✅ **SUPER_ADMIN, ADMIN, USER** can ONLY access:
   - `/api/employees/**`
   - `/api/documents/**`
   - `/api/leaves/**`
   - `/api/attendance/**`
   - `/api/rota/**`

✅ **SUPER_ADMIN ONLY** can access:
   - `/api/dashboard/stats` (Employee dashboard)

✅ **Complete role-based separation enforced at API level**

---

## ✅ Status

**Implementation**: 🟢 **COMPLETE**

**Security Level**: 🔒 **HIGH**

**Files Modified**: 5 controllers

**Compilation Status**: ✅ No errors (only warnings)

**Backend will now REJECT unauthorized API access with 403 Forbidden, regardless of frontend routing!**

---

**Date**: November 5, 2025  
**Issue**: Backend not enforcing role-based API restrictions  
**Solution**: Added `@PreAuthorize` annotations to all endpoints  
**Result**: Complete backend API authorization enforcement

