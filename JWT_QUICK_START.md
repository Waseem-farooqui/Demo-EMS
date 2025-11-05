# Quick Start - JWT Authentication

## 🚀 Running the Application with JWT Auth

### Step 1: Reload Maven Project
1. Open IntelliJ IDEA
2. Right-click on `pom.xml`
3. Select **Maven** → **Reload Project**
4. Wait for dependencies to download (Spring Security, JWT libraries)

### Step 2: Start Backend
1. Run `EmployeeManagementSystemApplication.java`
2. Wait for: `Started EmployeeManagementSystemApplication`
3. Backend running on: http://localhost:8080

### Step 3: Start Frontend
```cmd
cd C:\Users\waseem.uddin\EmployeeManagementSystem\frontend
npm start
```
Wait for: "Compiled successfully!"
Frontend running on: http://localhost:4200

### Step 4: Access the Application
Open browser: http://localhost:4200

You'll see the **Login Page** (authentication is now required!)

---

## 👤 First Time Setup

### Create Your First User

1. **Click "Sign up" link** on the login page

2. **Fill the registration form:**
   - Username: `admin`
   - Email: `admin@example.com`
   - Password: `admin123`
   - Confirm Password: `admin123`

3. **Click "Sign Up"**

4. **You'll see:** "User registered successfully! Redirecting to login..."

5. **Login with your credentials:**
   - Username: `admin`
   - Password: `admin123`

6. **Click "Login"**

7. **You're in!** You'll see the Employee Management page

---

## 🔐 What Changed?

### Before JWT (Old Version)
- ❌ No login required
- ❌ Anyone could access employee data
- ❌ No user accounts
- ❌ Direct access to /employees

### After JWT (Current Version)
- ✅ Login required to access employee data
- ✅ Secure user registration
- ✅ Password encryption
- ✅ Token-based authentication
- ✅ Protected API endpoints
- ✅ Logout functionality

---

## 📋 Quick Test

1. **Register:** Create account with signup form
2. **Login:** Use your credentials to login
3. **Create Employee:** Add a new employee
4. **View Employees:** See your employee list
5. **Edit Employee:** Modify employee details
6. **Delete Employee:** Remove an employee
7. **Logout:** Click logout button (top right)
8. **Try accessing /employees:** You'll be redirected to login ✅

---

## 🎯 Key Features

### Authentication Pages
- **Login Page:** http://localhost:4200/login
- **Signup Page:** http://localhost:4200/signup

### Protected Pages
- **Employee List:** http://localhost:4200/employees (requires login)
- **Add Employee:** http://localhost:4200/employees/add (requires login)
- **Edit Employee:** http://localhost:4200/employees/edit/:id (requires login)

### User Info
- See "Welcome, [username]" in the header
- Logout button always visible when authenticated

---

## 🔑 Default Credentials (After First Signup)

After you register, use your own credentials:
- Username: (what you chose)
- Password: (what you chose)

**Example:**
- Username: `admin`
- Password: `admin123`

---

## 🛠️ Testing with Postman

### 1. Register
```
POST http://localhost:8080/api/auth/signup
Content-Type: application/json

{
  "username": "testuser",
  "email": "test@example.com",
  "password": "password123"
}
```

### 2. Login
```
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "testuser",
  "password": "password123"
}
```

**Response:** You'll get a JWT token:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "id": 1,
  "username": "testuser",
  "email": "test@example.com",
  "roles": ["USER"]
}
```

### 3. Access Protected Endpoint
```
GET http://localhost:8080/api/employees
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## ⚠️ Common Issues

### Issue: Can't access /employees directly
**This is correct!** You must login first.

### Issue: "401 Unauthorized" error
**Solution:** Your token expired or is invalid. Logout and login again.

### Issue: Redirected to login after some time
**This is normal!** Token expires after 24 hours for security.

### Issue: Backend shows security errors
**Solution:** Make sure you reloaded Maven project to download Spring Security and JWT dependencies.

---

## 📊 View Users in Database

1. Access H2 Console: http://localhost:8080/h2-console
2. Login:
   - JDBC URL: `jdbc:h2:mem:employeedb`
   - Username: `sa`
   - Password: (leave empty)
3. Run query:
```sql
SELECT * FROM USERS;
SELECT * FROM USER_ROLES;
```

---

## 🎯 Quick Commands

**Start Backend (IntelliJ):**
- Run → Run 'EmployeeManagementSystemApplication'

**Start Frontend:**
```cmd
cd C:\Users\waseem.uddin\EmployeeManagementSystem\frontend
npm start
```

**Access Application:**
- Frontend: http://localhost:4200
- Backend API: http://localhost:8080/api
- H2 Console: http://localhost:8080/h2-console

---

## ✅ Success Checklist

- [ ] Backend started successfully
- [ ] Frontend started successfully
- [ ] Can access login page
- [ ] Can register new user
- [ ] Can login with credentials
- [ ] Redirected to employee list after login
- [ ] Can see "Welcome, [username]" in header
- [ ] Can perform employee CRUD operations
- [ ] Can logout successfully
- [ ] Redirected to login when accessing /employees without auth

---

## 🎉 You're Ready!

Your Employee Management System now has full JWT authentication!

**Next:** Create your account and start managing employees securely!

For detailed information, see: **JWT_AUTHENTICATION_GUIDE.md**

