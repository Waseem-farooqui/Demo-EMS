# Role-Based Access Control (RBAC) Implementation

## Overview

Complete role-based access control has been implemented with two primary roles:
- **ADMIN** - Full access to all system resources
- **USER** - Limited access to only their own data

---

## Roles and Permissions

### ADMIN Role

**Permissions:**
- ✅ Create employees
- ✅ View ALL employees
- ✅ Update ANY employee
- ✅ Delete ANY employee
- ✅ View ALL leaves (all employees)
- ✅ Approve/Reject ANY leave
- ✅ View leaves by ANY status
- ✅ Full system access

**Access Level:** FULL ACCESS

### USER Role

**Permissions:**
- ❌ Cannot create employees
- ✅ View ONLY their own employee record
- ✅ Update ONLY their own employee data
- ❌ Cannot delete employees
- ✅ Apply leave for themselves
- ✅ View ONLY their own leaves
- ✅ Update ONLY their own pending leaves
- ✅ Delete ONLY their own pending/rejected leaves
- ❌ Cannot approve/reject leaves

**Access Level:** SELF-SERVICE ONLY

---

## Implementation Details

### SecurityUtils Component

**Location:** `src/main/java/.../security/SecurityUtils.java`

**Methods:**
- `getCurrentUsername()` - Get logged-in user's username
- `getCurrentUser()` - Get logged-in User entity
- `isAdmin()` - Check if user has ADMIN role
- `isUser()` - Check if user has USER role
- `hasRole(String role)` - Check for specific role

### Employee-User Linking

**Employee Entity:**
```java
@Column(name = "user_id")
private Long userId;
```

Links an employee record to a user account. This enables:
- Users to see their own employee data
- Admin to assign employees to user accounts
- Leave management per user

---

## Access Control Rules

### Employee Management

#### Create Employee
```java
// Only ADMIN can create
if (!securityUtils.isAdmin()) {
    throw new RuntimeException("Access denied. Only admins can create employees.");
}
```

#### View All Employees
```java
// ADMIN: sees all employees
if (securityUtils.isAdmin()) {
    return all employees;
}

// USER: sees only their own employee record
User currentUser = securityUtils.getCurrentUser();
return employee where userId == currentUser.getId();
```

#### View Single Employee
```java
// ADMIN: can view any employee
// USER: can only view if employee.userId == currentUser.getId()
if (!canAccessEmployee(employee)) {
    throw new RuntimeException("Access denied");
}
```

#### Update Employee
```java
// ADMIN: can update any employee
// USER: can only update their own employee
if (!canAccessEmployee(employee)) {
    throw new RuntimeException("Access denied");
}

// Only ADMIN can change userId
if (securityUtils.isAdmin() && employeeDTO.getUserId() != null) {
    employee.setUserId(employeeDTO.getUserId());
}
```

#### Delete Employee
```java
// Only ADMIN can delete
if (!securityUtils.isAdmin()) {
    throw new RuntimeException("Access denied. Only admins can delete employees.");
}
```

### Leave Management

#### Apply Leave
```java
// USER: can apply leave for their own employee record
// ADMIN: can apply leave for any employee
if (!canAccessEmployee(employee)) {
    throw new RuntimeException("Access denied. You can only apply leave for yourself.");
}
```

#### View All Leaves
```java
// ADMIN: sees all leaves from all employees
if (securityUtils.isAdmin()) {
    return all leaves;
}

// USER: sees only leaves for their employee record
return leaves where employee.userId == currentUser.getId();
```

#### Approve/Reject Leave
```java
// Only ADMIN can approve or reject
if (!securityUtils.isAdmin()) {
    throw new RuntimeException("Access denied. Only admins can approve/reject leaves.");
}
```

#### Update/Delete Leave
```java
// USER: can update/delete only their own leaves
// ADMIN: can update/delete any leave
if (!canAccessEmployee(leave.getEmployee())) {
    throw new RuntimeException("Access denied");
}
```

---

## User Registration with Roles

### Default Registration (USER Role)
```json
POST /api/auth/signup
{
  "username": "john.doe",
  "email": "john@example.com",
  "password": "password123"
}
```
**Result:** User created with **USER** role (default)

### Admin Registration
```json
POST /api/auth/signup
{
  "username": "admin",
  "email": "admin@example.com",
  "password": "adminpass",
  "roles": ["ADMIN"]
}
```
**Result:** User created with **ADMIN** role

### Mixed Roles (If Needed)
```json
{
  "username": "manager",
  "email": "manager@example.com",
  "password": "managerpass",
  "roles": ["USER", "ADMIN"]
}
```
**Result:** User with both roles (has ADMIN privileges)

---

## Setting Up Users

### Step 1: Create Admin User

**Using Postman:**
```http
POST http://localhost:8080/api/auth/signup
Content-Type: application/json

{
  "username": "admin",
  "email": "admin@company.com",
  "password": "Admin@123",
  "roles": ["ADMIN"]
}
```

### Step 2: Create Regular User

```http
POST http://localhost:8080/api/auth/signup
Content-Type: application/json

{
  "username": "john.doe",
  "email": "john@company.com",
  "password": "User@123"
}
```
**Note:** No roles specified = USER role by default

### Step 3: Login as Admin

```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "Admin@123"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "id": 1,
  "username": "admin",
  "email": "admin@company.com",
  "roles": ["ADMIN"]
}
```

### Step 4: Create Employee and Link to User

**As Admin:**
```http
POST http://localhost:8080/api/employees
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "fullName": "John Doe",
  "personType": "Full-time",
  "workEmail": "john@company.com",
  "jobTitle": "Software Engineer",
  "dateOfJoining": "2024-01-15",
  "holidayAllowance": 20,
  "userId": 2
}
```

**Note:** `userId: 2` links this employee to the user account with ID 2

---

## Testing Access Control

### Test 1: Admin Can See All Employees

**Login as Admin:**
```http
POST /api/auth/login
Body: {"username": "admin", "password": "Admin@123"}
```

**Get All Employees:**
```http
GET /api/employees
Authorization: Bearer <admin-token>
```

**Expected:** Returns ALL employees in the system ✅

### Test 2: User Can Only See Their Own Employee

**Login as User:**
```http
POST /api/auth/login
Body: {"username": "john.doe", "password": "User@123"}
```

**Get All Employees:**
```http
GET /api/employees
Authorization: Bearer <user-token>
```

**Expected:** Returns ONLY the employee record where `userId = john.doe's id` ✅

### Test 3: User Cannot Create Employee

**As User:**
```http
POST /api/employees
Authorization: Bearer <user-token>
Body: { employee data }
```

**Expected:** 
```json
{
  "error": "Access denied. Only admins can create employees."
}
```
❌ Access Denied

### Test 4: User Cannot View Other Employee

**As User (userId=2):**
```http
GET /api/employees/3
Authorization: Bearer <user-token>
```

**Expected:**
```json
{
  "error": "Access denied. You can only access your own employee data."
}
```
❌ Access Denied

### Test 5: User Can View Own Employee

**As User (userId=2, linked to employee id=5):**
```http
GET /api/employees/5
Authorization: Bearer <user-token>
```

**Expected:** Returns employee data ✅

### Test 6: Admin Can Approve Leaves

**As Admin:**
```http
PUT /api/leaves/1/approve
Authorization: Bearer <admin-token>
Body: {"approvedBy": "Admin", "remarks": "Approved"}
```

**Expected:** Leave approved ✅

### Test 7: User Cannot Approve Leaves

**As User:**
```http
PUT /api/leaves/1/approve
Authorization: Bearer <user-token>
Body: {"approvedBy": "User", "remarks": "Approved"}
```

**Expected:**
```json
{
  "error": "Access denied. Only admins can approve leaves."
}
```
❌ Access Denied

### Test 8: User Can Apply Their Own Leave

**As User (userId=2, linked to employee=5):**
```http
POST /api/leaves
Authorization: Bearer <user-token>
Body: {
  "employeeId": 5,
  "leaveType": "Annual Leave",
  "startDate": "2025-11-01",
  "endDate": "2025-11-05",
  "reason": "Vacation"
}
```

**Expected:** Leave created ✅

### Test 9: User Cannot Apply Leave for Others

**As User (userId=2):**
```http
POST /api/leaves
Authorization: Bearer <user-token>
Body: {
  "employeeId": 3,
  "leaveType": "Annual Leave",
  ...
}
```

**Expected:**
```json
{
  "error": "Access denied. You can only apply leave for yourself."
}
```
❌ Access Denied

---

## Database Verification

### Check User Roles

```sql
SELECT 
    u.id,
    u.username,
    u.email,
    ur.role
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id
ORDER BY u.id;
```

**Example Output:**
```
ID | USERNAME  | EMAIL               | ROLE
1  | admin     | admin@company.com   | ADMIN
2  | john.doe  | john@company.com    | USER
3  | jane.smith| jane@company.com    | USER
```

### Check Employee-User Mapping

```sql
SELECT 
    e.id AS emp_id,
    e.full_name,
    e.user_id,
    u.username
FROM employees e
LEFT JOIN users u ON e.user_id = u.id
ORDER BY e.id;
```

**Example Output:**
```
EMP_ID | FULL_NAME   | USER_ID | USERNAME
5      | John Doe    | 2       | john.doe
6      | Jane Smith  | 3       | jane.smith
7      | Bob Wilson  | NULL    | NULL
```

**Note:** Bob Wilson has no user_id, so no user can access his data (admin-only)

---

## Access Control Matrix

| Operation | ADMIN | USER |
|-----------|-------|------|
| **Employees** | | |
| Create Employee | ✅ | ❌ |
| View All Employees | ✅ (all) | ✅ (own only) |
| View Single Employee | ✅ (any) | ✅ (own only) |
| Update Employee | ✅ (any) | ✅ (own only) |
| Delete Employee | ✅ (any) | ❌ |
| **Leaves** | | |
| Apply Leave | ✅ (for any) | ✅ (own only) |
| View All Leaves | ✅ (all) | ✅ (own only) |
| View Leave by ID | ✅ (any) | ✅ (own only) |
| View by Employee | ✅ (any) | ✅ (own only) |
| View by Status | ✅ (all) | ✅ (own only) |
| Update Leave | ✅ (any) | ✅ (own only) |
| Approve Leave | ✅ | ❌ |
| Reject Leave | ✅ | ❌ |
| Delete Leave | ✅ (any) | ✅ (own only) |

---

## Error Messages

### Access Denied Errors

1. **Create Employee (USER):**
   ```
   "Access denied. Only admins can create employees."
   ```

2. **View Other Employee (USER):**
   ```
   "Access denied. You can only access your own employee data."
   ```

3. **Update Other Employee (USER):**
   ```
   "Access denied. You can only update your own employee data."
   ```

4. **Delete Employee (USER):**
   ```
   "Access denied. Only admins can delete employees."
   ```

5. **Approve/Reject Leave (USER):**
   ```
   "Access denied. Only admins can approve/reject leaves."
   ```

6. **Apply Leave for Others (USER):**
   ```
   "Access denied. You can only apply leave for yourself."
   ```

7. **View Other's Leaves (USER):**
   ```
   "Access denied. You can only view your own leaves."
   ```

---

## Best Practices

### 1. Always Link Employees to Users

When creating an employee for a user:
```java
employeeDTO.setUserId(userId);
```

### 2. First User Should Be Admin

Create the first user as ADMIN:
```json
{"username": "admin", "roles": ["ADMIN"]}
```

### 3. Protect Sensitive Operations

Only admins should:
- Create/delete employees
- Approve/reject leaves
- View all system data

### 4. User Self-Service

Regular users can:
- View their own data
- Update their own profile
- Apply and manage their own leaves

---

## Configuration Files

### SecurityUtils.java
- Provides role checking utilities
- Gets current user information
- Used by all services for access control

### EmployeeService.java
- Enforces employee access rules
- Uses `canAccessEmployee()` method
- Checks admin vs user permissions

### LeaveService.java
- Enforces leave access rules
- Uses `canAccessEmployee()` for employee check
- Restricts approve/reject to admins

---

## Summary

✅ **Role-Based Access Control Implemented**

**Features:**
- Two roles: ADMIN and USER
- Admin has full system access
- Users can only see their own data
- Employee-User linking via userId
- Leave management with role restrictions
- Comprehensive access control checks

**Security:**
- All operations checked for permissions
- Clear error messages for denied access
- Consistent enforcement across all services

**Testing:**
- Create admin and user accounts
- Link employees to users
- Test access restrictions
- Verify in H2 database

**Your system now has complete role-based access control! 🔒**

