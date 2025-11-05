# How to Create Admin Users - Complete Guide

## 🎯 Quick Answer

To create an ADMIN user, you need to specify the role during signup. Here are the methods:

---

## Method 1: Register Admin via Frontend (Recommended for First Admin)

### Option A: Modify Signup Component (Temporary)

**Step 1:** Temporarily add a role selector to the signup form

Open `frontend/src/app/components/signup/signup.component.html` and add:

```html
<!-- Add after password confirm field -->
<div class="form-group">
  <label for="role">Role (Optional - leave empty for USER)</label>
  <select id="role" name="role" [(ngModel)]="signupRequest.role" class="form-control">
    <option value="">USER (Default)</option>
    <option value="ADMIN">ADMIN</option>
  </select>
</div>
```

**Step 2:** Update the TypeScript model

In `frontend/src/app/models/auth.model.ts`, ensure SignupRequest has:
```typescript
export interface SignupRequest {
  username: string;
  email: string;
  password: string;
  role?: string;  // Optional
}
```

**Step 3:** Register your admin user
1. Go to http://localhost:4200/signup
2. Fill the form
3. Select "ADMIN" from role dropdown
4. Click Sign Up

**Step 4:** Remove the role selector (security)
After creating your admin, remove the role dropdown to prevent unauthorized admin creation.

---

## Method 2: Register Admin via Postman/API (Easiest)

### Using Postman:

**Request:**
```
POST http://localhost:8080/api/auth/signup
Content-Type: application/json
```

**Body:**
```json
{
  "username": "admin",
  "email": "admin@company.com",
  "password": "Admin@123",
  "roles": ["ADMIN"]
}
```

**Response:**
```json
{
  "message": "User registered successfully!"
}
```

### Using cURL:

```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "email": "admin@company.com",
    "password": "Admin@123",
    "roles": ["ADMIN"]
  }'
```

---

## Method 3: Direct Database Insert (H2 Console)

### Step 1: Access H2 Console
- URL: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:employeedb`
- Username: `sa`
- Password: (empty)

### Step 2: Create User
```sql
-- Insert user with encrypted password
INSERT INTO users (username, email, password, enabled) 
VALUES ('admin', 'admin@company.com', '$2a$10$dummyHashedPasswordHere', true);
```

**Note:** For real password, use BCrypt. Password "Admin@123" hashed:
```sql
-- Use this for password: Admin@123
INSERT INTO users (username, email, password, enabled) 
VALUES ('admin', 'admin@company.com', 
'$2a$10$N9qo8uLOickgx2ZMRZoMye1EE9GZ/7B5lYfT3B3F3QZVF4w5YhB5q', true);
```

### Step 3: Assign ADMIN Role
```sql
-- Get the user ID first
SELECT id FROM users WHERE username = 'admin';

-- Assign ADMIN role (replace 1 with actual user ID)
INSERT INTO user_roles (user_id, role) VALUES (1, 'ADMIN');
```

---

## Method 4: Modify AuthController (Backend)

### Create Admin-Only Signup Endpoint

**Add to AuthController.java:**

```java
@PostMapping("/signup/admin")
public ResponseEntity<?> registerAdmin(@Valid @RequestBody SignupRequest signUpRequest) {
    // Check if requester is already an admin (optional security check)
    // For first admin, you can temporarily disable this check
    
    if (userRepository.existsByUsername(signUpRequest.getUsername())) {
        return ResponseEntity.badRequest()
            .body(new MessageResponse("Error: Username is already taken!"));
    }

    if (userRepository.existsByEmail(signUpRequest.getEmail())) {
        return ResponseEntity.badRequest()
            .body(new MessageResponse("Error: Email is already in use!"));
    }

    // Create new user's account
    User user = new User();
    user.setUsername(signUpRequest.getUsername());
    user.setEmail(signUpRequest.getEmail());
    user.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
    user.setEnabled(true);

    Set<String> strRoles = new HashSet<>();
    strRoles.add("ADMIN");  // Force ADMIN role
    strRoles.add("USER");   // Can have both roles
    
    user.setRoles(strRoles);
    userRepository.save(user);

    return ResponseEntity.ok(new MessageResponse("Admin user registered successfully!"));
}
```

**Use it:**
```bash
POST http://localhost:8080/api/auth/signup/admin
```

---

## Current Backend Implementation

Your `AuthController.java` already supports role assignment! Check the signup method:

```java
@PostMapping("/signup")
public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
    // ... validation ...
    
    Set<String> strRoles = signUpRequest.getRoles();  // Gets roles from request
    Set<String> roles = new HashSet<>();

    if (strRoles == null || strRoles.isEmpty()) {
        roles.add("USER");  // Default role
    } else {
        strRoles.forEach(role -> {
            switch (role.toUpperCase()) {
                case "ADMIN":
                    roles.add("ADMIN");
                    break;
                default:
                    roles.add("USER");
            }
        });
    }

    user.setRoles(roles);
    // ... save user ...
}
```

This means you can **already** create admin users by including `"roles": ["ADMIN"]` in the signup request!

---

## Recommended Approach: Create First Admin

### Step-by-Step Process:

1. **Start your backend** (Spring Boot)

2. **Use Postman** to create first admin:
   ```json
   POST http://localhost:8080/api/auth/signup
   {
     "username": "admin",
     "email": "admin@company.com",
     "password": "Admin@123",
     "roles": ["ADMIN"]
   }
   ```

3. **Verify in H2 Console:**
   ```sql
   SELECT u.id, u.username, u.email, ur.role
   FROM users u
   LEFT JOIN user_roles ur ON u.id = ur.user_id
   WHERE u.username = 'admin';
   ```
   
   Should show:
   ```
   ID | USERNAME | EMAIL                | ROLE
   1  | admin    | admin@company.com    | ADMIN
   ```

4. **Test login:**
   ```json
   POST http://localhost:8080/api/auth/login
   {
     "username": "admin",
     "password": "Admin@123"
   }
   ```
   
   Response should include `"roles": ["ADMIN"]`

5. **Login via frontend:**
   - Go to http://localhost:4200/login
   - Username: `admin`
   - Password: `Admin@123`
   - Click Login

---

## Verify Admin Role Works

### Test Admin Permissions:

1. **Login as Admin**
   - Should see all employees (not just own)

2. **Check Console**
   ```javascript
   // In browser console
   localStorage.getItem('auth-user')
   ```
   Should show: `"roles":["ADMIN"]`

3. **Test Admin Actions:**
   - Navigate to Leave Management
   - Should see "Approve" and "Reject" buttons
   - Can approve/reject any leave

4. **Test User Actions (for comparison):**
   - Create a regular user (without roles)
   - Login as that user
   - Should only see their own data
   - No approve/reject buttons

---

## Create Multiple Admin Users

You can have multiple admins:

```bash
# Admin 1
POST /api/auth/signup
{
  "username": "admin1",
  "email": "admin1@company.com",
  "password": "Admin1@123",
  "roles": ["ADMIN"]
}

# Admin 2
POST /api/auth/signup
{
  "username": "admin2",
  "email": "admin2@company.com",
  "password": "Admin2@123",
  "roles": ["ADMIN"]
}
```

---

## User with Both Roles

A user can have both USER and ADMIN roles:

```json
{
  "username": "superuser",
  "email": "super@company.com",
  "password": "Super@123",
  "roles": ["USER", "ADMIN"]
}
```

This user will have ADMIN privileges (since SecurityUtils checks for ADMIN role).

---

## Security Recommendations

### Production Setup:

1. **Create First Admin via Postman/API**
   - Not through public signup form

2. **Disable Public Admin Registration**
   - Remove role selection from signup form
   - Or add authentication check

3. **Admin-Only Admin Creation**
   ```java
   @PostMapping("/signup/admin")
   @PreAuthorize("hasRole('ADMIN')")  // Only existing admins can create new admins
   public ResponseEntity<?> registerAdmin(...) {
       // ... create admin ...
   }
   ```

4. **Use Strong Passwords**
   - Minimum 8 characters
   - Mix of letters, numbers, symbols

5. **Environment Variables**
   - Store admin credentials in environment
   - Not hardcoded

---

## Quick Reference

### Create Admin (Postman):
```json
POST http://localhost:8080/api/auth/signup
{
  "username": "admin",
  "email": "admin@company.com",
  "password": "Admin@123",
  "roles": ["ADMIN"]
}
```

### Create Regular User (Postman):
```json
POST http://localhost:8080/api/auth/signup
{
  "username": "john",
  "email": "john@company.com",
  "password": "User@123"
}
```
*Note: No roles = USER role by default*

### Check User Roles (H2 Console):
```sql
SELECT u.username, ur.role 
FROM users u 
LEFT JOIN user_roles ur ON u.id = ur.user_id;
```

### Login (Frontend):
```
http://localhost:4200/login
Username: admin
Password: Admin@123
```

---

## Troubleshooting

### Issue: "Username already taken"
**Solution:** User already exists. Try different username or login.

### Issue: Admin can't approve leaves
**Solution:** 
1. Check user roles in H2 Console
2. Verify roles array includes "ADMIN"
3. Clear browser cache and re-login

### Issue: Role not assigned
**Solution:**
1. Check AuthController accepts roles
2. Verify JSON includes "roles": ["ADMIN"]
3. Check database after signup

---

## Summary

**Easiest Method:**
1. Use Postman/cURL to POST to `/api/auth/signup`
2. Include `"roles": ["ADMIN"]` in JSON body
3. Login and verify admin permissions

**For First Time:**
```bash
# Create your admin account
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "email": "admin@company.com",
    "password": "Admin@123",
    "roles": ["ADMIN"]
  }'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "Admin@123"
  }'
```

You're all set! Your backend already supports admin role creation. 🎉

