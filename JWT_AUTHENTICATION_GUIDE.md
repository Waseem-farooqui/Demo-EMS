# JWT Authentication Implementation Guide

## 🔐 Overview

JWT (JSON Web Token) authentication has been successfully added to the Employee Management System. This provides secure user registration, login, and protected API endpoints.

---

## 📦 What Was Added

### Backend Components (Spring Boot)

1. **Dependencies (pom.xml)**
   - Spring Security
   - JWT libraries (jjwt-api, jjwt-impl, jjwt-jackson)
   - Spring Validation

2. **Entities**
   - `User.java` - User entity with roles

3. **Repositories**
   - `UserRepository.java` - User data access

4. **DTOs**
   - `LoginRequest.java` - Login credentials
   - `SignupRequest.java` - Registration data
   - `JwtResponse.java` - JWT token response
   - `MessageResponse.java` - Generic message response

5. **Security Components**
   - `JwtUtils.java` - JWT token generation and validation
   - `JwtAuthenticationFilter.java` - JWT request filter
   - `UserDetailsServiceImpl.java` - User authentication service
   - `JwtAuthenticationEntryPoint.java` - Unauthorized handler
   - `SecurityConfig.java` - Spring Security configuration

6. **Controllers**
   - `AuthController.java` - Login and signup endpoints

7. **Configuration**
   - Updated `CorsConfig.java` for security integration
   - Added JWT properties to `application.properties`

### Frontend Components (Angular)

1. **Models**
   - `auth.model.ts` - Authentication interfaces

2. **Services**
   - `auth.service.ts` - Authentication service with token management

3. **Interceptors**
   - `jwt.interceptor.ts` - Adds JWT token to HTTP requests

4. **Guards**
   - `auth.guard.ts` - Route protection

5. **Components**
   - `login.component.*` - Login page
   - `signup.component.*` - Registration page
   - Updated `employee-list.component.*` - Added logout functionality

6. **Configuration**
   - Updated `app.routes.ts` - Added auth routes and guards
   - Updated `app.config.ts` - Added JWT interceptor

---

## 🚀 How to Use

### 1. Start the Application

**Backend:**
```cmd
# In IntelliJ IDEA
1. Reload Maven Project
2. Run EmployeeManagementSystemApplication
```

**Frontend:**
```cmd
cd C:\Users\waseem.uddin\EmployeeManagementSystem\frontend
npm start
```

### 2. Register a New User

1. Open: http://localhost:4200
2. You'll see the Login page
3. Click "Sign up" link
4. Fill in the registration form:
   - Username: `admin` (min 3 characters)
   - Email: `admin@example.com`
   - Password: `password123` (min 6 characters)
   - Confirm Password: `password123`
5. Click "Sign Up"
6. You'll be redirected to login page

### 3. Login

1. Enter your credentials:
   - Username: `admin`
   - Password: `password123`
2. Click "Login"
3. You'll be redirected to the Employee List page

### 4. Access Protected Routes

All employee management routes are now protected:
- `/employees` - View employees (requires authentication)
- `/employees/add` - Add employee (requires authentication)
- `/employees/edit/:id` - Edit employee (requires authentication)

If you try to access these without logging in, you'll be redirected to the login page.

### 5. Logout

Click the "Logout" button in the top right corner of the Employee List page.

---

## 🔑 API Endpoints

### Public Endpoints (No Authentication Required)

#### POST /api/auth/signup
Register a new user.

**Request:**
```json
{
  "username": "john",
  "email": "john@example.com",
  "password": "password123"
}
```

**Response (200 OK):**
```json
{
  "message": "User registered successfully!"
}
```

#### POST /api/auth/login
Authenticate and receive JWT token.

**Request:**
```json
{
  "username": "john",
  "password": "password123"
}
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "id": 1,
  "username": "john",
  "email": "john@example.com",
  "roles": ["USER"]
}
```

### Protected Endpoints (Require Authentication)

All `/api/employees/**` endpoints now require a valid JWT token in the Authorization header:

```
Authorization: Bearer <your-jwt-token>
```

The Angular frontend automatically adds this header to all requests.

---

## 🔒 Security Features

### 1. Password Encryption
- Passwords are encrypted using BCrypt
- Strength: BCrypt default rounds (10)
- Never stored in plain text

### 2. JWT Token
- **Algorithm:** HS256 (HMAC with SHA-256)
- **Expiration:** 24 hours (86400000 ms)
- **Secret Key:** Configured in application.properties
- **Claims:** Username, issued at, expiration

### 3. Session Management
- Stateless authentication (no server-side sessions)
- Token stored in browser's localStorage
- Token validated on every request

### 4. Role-Based Access
- Users have roles (USER, ADMIN)
- Default role: USER
- Can be extended for fine-grained permissions

### 5. CORS Protection
- Only allows requests from http://localhost:4200
- Credentials enabled
- Exposes Authorization header

### 6. Route Protection
- Frontend: Angular AuthGuard
- Backend: Spring Security configuration
- Unauthorized requests return 401 status

---

## 🛠️ Configuration

### Backend Configuration (application.properties)

```properties
# JWT Configuration
jwt.secret=mySecretKeyForJWTTokenGenerationThatIsAtLeast256BitsLongForHS256Algorithm
jwt.expirationMs=86400000
```

**⚠️ IMPORTANT FOR PRODUCTION:**
- Change `jwt.secret` to a strong, random secret
- Store secrets in environment variables, not in code
- Use a key management service

### Frontend Token Storage

Tokens are stored in browser's localStorage:
- Key: `auth-token`
- User info: `auth-user`

---

## 🧪 Testing Authentication

### Using Browser

1. **Register:**
   - Go to http://localhost:4200/signup
   - Create account

2. **Login:**
   - Go to http://localhost:4200/login
   - Enter credentials

3. **Access Protected Page:**
   - Navigate to http://localhost:4200/employees
   - Should see employee list

4. **Test Token Expiration:**
   - Wait 24 hours or manually delete token from localStorage
   - Try accessing employees page
   - Should redirect to login

### Using Postman/curl

**1. Register:**
```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"test\",\"email\":\"test@example.com\",\"password\":\"password123\"}"
```

**2. Login:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"test\",\"password\":\"password123\"}"
```

**3. Access Protected Endpoint:**
```bash
curl -X GET http://localhost:8080/api/employees \
  -H "Authorization: Bearer <your-token-here>"
```

---

## 📊 Database Schema

### Users Table
```sql
CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(255) NOT NULL UNIQUE,
  email VARCHAR(255) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  enabled BOOLEAN DEFAULT TRUE
);
```

### User Roles Table
```sql
CREATE TABLE user_roles (
  user_id BIGINT NOT NULL,
  role VARCHAR(255),
  FOREIGN KEY (user_id) REFERENCES users(id)
);
```

### View in H2 Console

1. Access: http://localhost:8080/h2-console
2. JDBC URL: `jdbc:h2:mem:employeedb`
3. Username: `sa`, Password: (empty)
4. Run queries:
```sql
SELECT * FROM users;
SELECT * FROM user_roles;
```

---

## 🔄 Authentication Flow

### Registration Flow
```
1. User fills signup form
   ↓
2. Angular sends POST to /api/auth/signup
   ↓
3. Backend validates data
   ↓
4. Password encrypted with BCrypt
   ↓
5. User saved to database
   ↓
6. Success message returned
   ↓
7. User redirected to login page
```

### Login Flow
```
1. User enters credentials
   ↓
2. Angular sends POST to /api/auth/login
   ↓
3. Backend validates username/password
   ↓
4. JWT token generated
   ↓
5. Token + user info returned
   ↓
6. Angular stores token in localStorage
   ↓
7. User redirected to employees page
```

### Protected Request Flow
```
1. User navigates to protected route
   ↓
2. AuthGuard checks for valid token
   ↓
3. JwtInterceptor adds token to request header
   ↓
4. Request sent to backend
   ↓
5. JwtAuthenticationFilter validates token
   ↓
6. If valid: Request processed
   If invalid: 401 Unauthorized returned
   ↓
7. On 401: User redirected to login
```

---

## 🎯 User Roles

### Available Roles

1. **USER** (Default)
   - Access to all employee operations
   - Can create, read, update, delete employees

2. **ADMIN**
   - Same as USER (can be extended)
   - Future: Add admin-only features

### Assigning Roles

**During Signup:**
```json
{
  "username": "admin",
  "email": "admin@example.com",
  "password": "password123",
  "roles": ["ADMIN"]
}
```

**Default:** If no roles specified, USER role is assigned.

---

## 🐛 Troubleshooting

### Issue: "401 Unauthorized" on Protected Endpoints

**Cause:** Token expired or invalid

**Solution:**
1. Logout and login again
2. Check browser console for token
3. Verify token in localStorage

### Issue: "Username is already taken"

**Cause:** User already registered

**Solution:**
1. Use different username
2. Or login with existing credentials

### Issue: "Invalid username or password"

**Cause:** Wrong credentials or user doesn't exist

**Solution:**
1. Verify credentials
2. Register if new user

### Issue: CORS errors

**Cause:** Backend CORS not configured properly

**Solution:**
1. Verify backend is running
2. Check SecurityConfig allows origin http://localhost:4200

### Issue: Token not being sent with requests

**Cause:** Interceptor not registered

**Solution:**
1. Verify app.config.ts includes HTTP_INTERCEPTORS
2. Check JwtInterceptor is properly configured

---

## 📝 Next Steps (Optional Enhancements)

1. **Refresh Tokens**
   - Implement refresh token mechanism
   - Extend session without re-login

2. **Password Reset**
   - Forgot password functionality
   - Email verification

3. **Email Verification**
   - Verify email on registration
   - Send verification link

4. **Role-Based UI**
   - Show/hide features based on roles
   - Admin-only sections

5. **Audit Logging**
   - Log authentication events
   - Track user actions

6. **Two-Factor Authentication**
   - Add 2FA support
   - SMS or authenticator app

7. **Social Login**
   - OAuth2 integration
   - Google, GitHub, etc.

8. **Password Strength Meter**
   - Visual feedback on password strength
   - Enforce strong passwords

---

## ✅ Testing Checklist

- [ ] User can register with valid credentials
- [ ] Registration fails with duplicate username
- [ ] Registration fails with duplicate email
- [ ] User can login with correct credentials
- [ ] Login fails with wrong password
- [ ] Protected routes require authentication
- [ ] Unauthenticated users redirected to login
- [ ] Logout clears token and redirects to login
- [ ] Token is sent with API requests
- [ ] Expired token redirects to login
- [ ] Employee CRUD operations work when authenticated

---

## 🎉 Summary

**Authentication Features Implemented:**
✅ User registration with validation
✅ Secure login with JWT tokens
✅ Password encryption (BCrypt)
✅ Protected routes (frontend & backend)
✅ Automatic token injection in requests
✅ Logout functionality
✅ Role-based access control
✅ Token expiration handling
✅ CORS configuration
✅ User-friendly login/signup UI

**Your application is now secure with JWT authentication!**

