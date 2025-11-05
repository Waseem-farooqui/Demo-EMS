# Quick Start - Role-Based Access Control

## 🚀 5-Minute Setup

### Step 1: Restart Backend
Stop and restart your Spring Boot application to apply the changes.

### Step 2: Create Admin Account
```bash
POST http://localhost:8080/api/auth/signup
{
  "username": "admin",
  "email": "admin@company.com",
  "password": "Admin@123",
  "roles": ["ADMIN"]
}
```

### Step 3: Create User Account
```bash
POST http://localhost:8080/api/auth/signup
{
  "username": "john.doe",
  "email": "john@company.com",
  "password": "User@123"
}
```

### Step 4: Login as Admin
```bash
POST http://localhost:8080/api/auth/login
{
  "username": "admin",
  "password": "Admin@123"
}
```
**Copy the token!**

### Step 5: Create Employee for User
```bash
POST http://localhost:8080/api/employees
Authorization: Bearer <admin-token>
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

**Note:** userId=2 is john.doe's user ID from Step 3

---

## 🔑 Role Comparison

### ADMIN Can:
✅ See ALL employees  
✅ Create employees  
✅ Update ANY employee  
✅ Delete ANY employee  
✅ See ALL leaves  
✅ Approve/Reject leaves  

### USER Can:
✅ See ONLY their own employee  
✅ Update ONLY their own employee  
✅ Apply ONLY their own leave  
✅ See ONLY their own leaves  
❌ Cannot create/delete employees  
❌ Cannot approve/reject leaves  
❌ Cannot see others' data  

---

## 🧪 Quick Tests

### Test 1: Admin Sees All
```bash
# Login as admin
POST /api/auth/login → Get admin token

# View all employees
GET /api/employees
Authorization: Bearer <admin-token>

Result: Returns ALL employees ✅
```

### Test 2: User Sees Only Own
```bash
# Login as user
POST /api/auth/login → Get user token

# View employees
GET /api/employees
Authorization: Bearer <user-token>

Result: Returns ONLY their employee ✅
```

### Test 3: User Cannot Create
```bash
# Try to create employee as user
POST /api/employees
Authorization: Bearer <user-token>

Result: "Access denied" ❌
```

---

## 📊 Quick Reference

| What | Who Can Do It |
|------|---------------|
| Create Employee | ADMIN only |
| View All Employees | ADMIN: all, USER: own only |
| Update Employee | ADMIN: any, USER: own only |
| Delete Employee | ADMIN only |
| Apply Leave | ADMIN: for any, USER: own only |
| Approve Leave | ADMIN only |
| View Leaves | ADMIN: all, USER: own only |

---

## ⚠️ Important

1. **Link Employee to User**
   - Always set `userId` when creating employee
   - This links employee record to user account

2. **First User = Admin**
   - Create first user with `"roles": ["ADMIN"]`

3. **User Needs Employee**
   - Users need employee record to use system
   - Admin creates employee with userId

---

## ✅ Files Modified

- ✅ SecurityUtils.java (new)
- ✅ Employee.java (added userId)
- ✅ EmployeeDTO.java (added userId)
- ✅ EmployeeRepository.java (added findByUserId)
- ✅ EmployeeService.java (added role checks)
- ✅ LeaveService.java (added role checks)

---

## 🎯 Ready to Use!

Your system now has complete role-based access control.

**Restart backend and start testing!** 🚀

