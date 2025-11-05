# 🚀 Quick Reference: One Admin Per Department

## For Developers

### API Endpoint (NEW)
```
GET /api/departments/with-admin-status
Authorization: Bearer {token}
Role Required: SUPER_ADMIN
```

**Response:**
```json
[
  {
    "id": 1,
    "name": "IT Department",
    "code": "IT",
    "hasAdmin": true,  // <-- New field
    "employeeCount": 15
  },
  {
    "id": 2,
    "name": "HR Department", 
    "code": "HR",
    "hasAdmin": false,  // <-- Available for admin assignment
    "employeeCount": 8
  }
]
```

### Validation Error (Backend)
```json
{
  "message": "Department 'IT' already has an ADMIN assigned. Each department can only have one ADMIN. Please select a different department or remove the existing ADMIN first."
}
```

---

## For SUPER_ADMIN Users

### Creating ADMIN User - Step by Step

1. **Navigate:** Dashboard → Create User
2. **Select Role:** Choose "ADMIN (Department Manager)"
3. **Select Department:** 
   - ✅ Available departments show normally
   - ❌ Occupied departments show: "Department Name (Already has Admin)" and are disabled
4. **Fill Other Fields:** Name, email, job title, etc.
5. **Submit:** Admin user created successfully

### Important Notes:
- 📌 Each department can only have **ONE** admin
- 📌 Departments with existing admins are **grayed out** in the dropdown
- 📌 You can create unlimited **USER** roles in any department
- 📌 Only **ADMIN** role is restricted to one per department

---

## For Testers

### Test Case 1: Normal Admin Creation ✅
```
Role: ADMIN
Department: HR (no existing admin)
Expected Result: Success
```

### Test Case 2: Duplicate Admin Prevention ❌
```
Role: ADMIN
Department: IT (already has admin)
Expected Result: Option disabled in dropdown
```

### Test Case 3: USER Creation (No Restriction) ✅
```
Role: USER
Department: IT (already has admin)
Expected Result: Success (no restriction on USER role)
```

### Test Case 4: API Bypass Attempt ❌
```
Direct API call to create ADMIN in occupied department
Expected Result: Backend validation error
```

---

## Backend Logic Flow

```
1. User submits form with role=ADMIN
   ↓
2. Backend receives request
   ↓
3. Check: Is role ADMIN? → YES
   ↓
4. Query: Does department have ADMIN? → YES
   ↓
5. Throw ValidationException
   ↓
6. Return error to frontend
```

---

## Frontend Logic Flow

```
1. SUPER_ADMIN opens Create User page
   ↓
2. Call: GET /api/departments/with-admin-status
   ↓
3. Receive departments with hasAdmin flag
   ↓
4. User selects role=ADMIN
   ↓
5. Department dropdown applies:
   - departments.hasAdmin=true → disabled
   - departments.hasAdmin=false → enabled
   ↓
6. User can only select available departments
```

---

## Quick Debug Commands

### Check which departments have admins:
```sql
SELECT 
  d.name AS department,
  u.username AS admin_username,
  u.roles
FROM department d
JOIN employee e ON e.department_id = d.id
JOIN user u ON u.id = e.user_id
WHERE u.roles LIKE '%ADMIN%';
```

### Count admins per department:
```sql
SELECT 
  d.name,
  COUNT(*) AS admin_count
FROM department d
JOIN employee e ON e.department_id = d.id
JOIN user u ON u.id = e.user_id
WHERE u.roles LIKE '%ADMIN%'
GROUP BY d.id, d.name;
```

---

## Files Changed Summary

**Backend:**
- `UserManagementService.java` - Added validation
- `DepartmentService.java` - Added admin status check
- `DepartmentController.java` - Added new endpoint
- `DepartmentDTO.java` - Added hasAdmin field

**Frontend:**
- `user-create.component.ts` - Smart dropdown logic
- `user-create.component.html` - Disabled options & hints

---

## Status: ✅ PRODUCTION READY

All validation in place, both frontend and backend protected.

