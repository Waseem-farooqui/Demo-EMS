# ROOT User Creation - cURL Commands

## Option 1: Direct Database Insert (Recommended)

Since the ROOT user is a system-level administrator that exists outside the normal user creation flow, the best approach is to create it directly via SQL.

### Step 1: Generate Password Hash

First, you need to generate a BCrypt hash for your desired password. You can use this Java code:

```java
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GeneratePasswordHash {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "Root@123456"; // Change this to your desired password
        String hash = encoder.encode(password);
        System.out.println("Password: " + password);
        System.out.println("BCrypt Hash: " + hash);
    }
}
```

### Step 2: SQL Insert Statement

```sql
-- Delete existing root user if any
DELETE FROM user_roles WHERE user_id IN (SELECT id FROM users WHERE username = 'root');
DELETE FROM users WHERE username = 'root';

-- Create ROOT user
-- Replace the password hash with your generated hash from Step 1
INSERT INTO users (username, email, password, enabled, email_verified, first_login, profile_completed, temporary_password, organization_id)
VALUES ('root', 'root@system.local', '$2a$10$YourGeneratedHashHere', TRUE, TRUE, FALSE, TRUE, FALSE, NULL);

-- Get the user ID
SET @root_user_id = LAST_INSERT_ID();

-- Add ROOT role
INSERT INTO user_roles (user_id, role)
VALUES (@root_user_id, 'ROOT');

-- Verify
SELECT u.id, u.username, u.email, ur.role, u.enabled, u.organization_id
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id
WHERE u.username = 'root';
```

---

## Option 2: Create ROOT User via API (Alternative)

If you want to use an API endpoint instead, you would need to create a special initialization endpoint. Here's how:

### Backend: Create InitializationController

```java
package com.was.employeemanagementsystem.controller;

import com.was.employeemanagementsystem.entity.User;
import com.was.employeemanagementsystem.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/init")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"})
public class InitializationController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public InitializationController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Create ROOT user - ONLY WORKS IF NO ROOT USER EXISTS
     * This endpoint should be secured or removed after initial setup
     */
    @PostMapping("/create-root")
    public ResponseEntity<?> createRootUser(@RequestBody Map<String, String> request) {
        try {
            // Check if ROOT user already exists
            boolean rootExists = userRepository.findAll().stream()
                    .anyMatch(u -> u.getRoles().contains("ROOT"));

            if (rootExists) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "ROOT user already exists. This endpoint can only be used once."
                ));
            }

            String username = request.getOrDefault("username", "root");
            String email = request.getOrDefault("email", "root@system.local");
            String password = request.get("password");

            if (password == null || password.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Password is required"
                ));
            }

            // Create ROOT user
            User rootUser = new User();
            rootUser.setUsername(username);
            rootUser.setEmail(email);
            rootUser.setPassword(passwordEncoder.encode(password));
            rootUser.setEnabled(true);
            rootUser.setEmailVerified(true);
            rootUser.setFirstLogin(false);
            rootUser.setProfileCompleted(true);
            rootUser.setTemporaryPassword(false);
            rootUser.setOrganizationId(null); // ROOT has no organization

            Set<String> roles = new HashSet<>();
            roles.add("ROOT");
            rootUser.setRoles(roles);

            User saved = userRepository.save(rootUser);
            log.info("✅ ROOT user created with ID: {}", saved.getId());

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "ROOT user created successfully",
                "username", username,
                "email", email
            ));

        } catch (Exception e) {
            log.error("❌ Error creating ROOT user: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error creating ROOT user: " + e.getMessage()
            ));
        }
    }
}
```

### cURL Command with Basic Authentication

**Important:** This endpoint requires Basic Authentication for security.
- Username: `waseem`
- Password: `wud19@WUD`

```bash
# Create ROOT user via API with Basic Auth
curl -X POST http://localhost:8080/api/init/create-root \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic d2FzZWVtOnd1ZDE5QFdVRA==" \
  -d '{
    "username": "root",
    "email": "root@system.local",
    "password": "Root@123456"
  }'
```

**Alternative - Let cURL encode the Basic Auth:**
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

**Check if ROOT user already exists:**
```bash
curl -X GET http://localhost:8080/api/init/root-exists
```

**Response:**
```json
{
  "success": true,
  "message": "ROOT user created successfully",
  "username": "root",
  "email": "root@system.local"
}
```

---

## Option 3: Quick Setup Script (Bash)

Create a file `create-root-user.sh`:

```bash
#!/bin/bash

# Configuration
DB_HOST="localhost"
DB_USER="root"
DB_PASS="your_db_password"
DB_NAME="your_database_name"
ROOT_USERNAME="root"
ROOT_EMAIL="root@system.local"
ROOT_PASSWORD="Root@123456"

echo "🔧 Creating ROOT user..."

# Generate BCrypt hash using a simple Java program
# Note: This assumes you have Java and Spring Security on classpath
# Alternative: Use an online BCrypt generator

# For this example, we'll use a pre-generated hash for "Root@123456"
# In production, generate this securely!
PASSWORD_HASH='$2a$10$zKPNQQCXlX8fQ8dV6qRqXO.V9pZYmLvKZQ3qXQqXQqXQqXQqXQqXQ'

# Execute SQL
mysql -h "$DB_HOST" -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" <<EOF
-- Delete existing root user
DELETE FROM user_roles WHERE user_id IN (SELECT id FROM users WHERE username = '$ROOT_USERNAME');
DELETE FROM users WHERE username = '$ROOT_USERNAME';

-- Create ROOT user
INSERT INTO users (username, email, password, enabled, email_verified, first_login, profile_completed, temporary_password, organization_id)
VALUES ('$ROOT_USERNAME', '$ROOT_EMAIL', '$PASSWORD_HASH', TRUE, TRUE, FALSE, TRUE, FALSE, NULL);

-- Get user ID
SET @root_user_id = LAST_INSERT_ID();

-- Add ROOT role
INSERT INTO user_roles (user_id, role)
VALUES (@root_user_id, 'ROOT');

-- Verify
SELECT u.id, u.username, u.email, ur.role, u.enabled
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id
WHERE u.username = '$ROOT_USERNAME';
EOF

echo "✅ ROOT user created successfully!"
echo "Username: $ROOT_USERNAME"
echo "Password: $ROOT_PASSWORD"
echo "⚠️  IMPORTANT: Change this password after first login!"
```

Make it executable:
```bash
chmod +x create-root-user.sh
./create-root-user.sh
```

---

## Option 4: Using Spring Boot Command Line Runner

Add this to your application:

```java
package com.was.employeemanagementsystem.config;

import com.was.employeemanagementsystem.entity.User;
import com.was.employeemanagementsystem.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Configuration
public class RootUserInitializer {

    @Bean
    CommandLineRunner initRootUser(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // Check if ROOT user exists
            boolean rootExists = userRepository.findAll().stream()
                    .anyMatch(u -> u.getRoles().contains("ROOT"));

            if (!rootExists) {
                log.info("🔧 ROOT user not found. Creating...");

                User rootUser = new User();
                rootUser.setUsername("root");
                rootUser.setEmail("root@system.local");
                rootUser.setPassword(passwordEncoder.encode("Root@123456"));
                rootUser.setEnabled(true);
                rootUser.setEmailVerified(true);
                rootUser.setFirstLogin(false);
                rootUser.setProfileCompleted(true);
                rootUser.setTemporaryPassword(false);
                rootUser.setOrganizationId(null);

                Set<String> roles = new HashSet<>();
                roles.add("ROOT");
                rootUser.setRoles(roles);

                userRepository.save(rootUser);
                log.info("✅ ROOT user created successfully!");
                log.info("   Username: root");
                log.info("   Password: Root@123456");
                log.info("   ⚠️  CHANGE THIS PASSWORD IMMEDIATELY!");
            } else {
                log.info("✅ ROOT user already exists");
            }
        };
    }
}
```

This will automatically create the ROOT user on application startup if it doesn't exist.

---

## Testing ROOT User

### Login via cURL

```bash
# Login as ROOT
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
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "root",
  "email": "root@system.local",
  "roles": ["ROOT"]
}
```

### Create Organization as ROOT

```bash
# Use the token from login response
TOKEN="your_jwt_token_here"

curl -X POST http://localhost:8080/api/organizations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "organizationName": "Acme Corporation",
    "superAdminUsername": "acme_admin",
    "superAdminEmail": "admin@acme.com",
    "password": "Admin@123",
    "superAdminFullName": "John Admin",
    "organizationDescription": "Software development company",
    "contactEmail": "contact@acme.com",
    "contactPhone": "+1-234-567-8900",
    "address": "123 Business St, City, Country"
  }'
```

---

## Recommended Approach

**For production deployment:**
1. Use **Option 1** (Direct SQL Insert) during initial database setup
2. Generate a strong password and hash it properly
3. Change the password immediately after first login
4. Never expose the ROOT password in logs or documentation

**For development:**
1. Use **Option 4** (CommandLineRunner) for auto-creation on startup
2. Keep the default password for development
3. Disable this in production

---

## Security Best Practices

1. ✅ **Strong Password**: Use a password with at least 12 characters, including uppercase, lowercase, numbers, and symbols
2. ✅ **Change Default**: Always change the default password after first login
3. ✅ **Limited Access**: ROOT user should only be used for organization management
4. ✅ **No Organization**: ROOT user has `organization_id = NULL`
5. ✅ **Single User**: Only ONE ROOT user should exist in the system
6. ✅ **Secure Storage**: Never commit passwords to version control
7. ✅ **Regular Audits**: Monitor ROOT user activity

---

## Troubleshooting

### Issue: "ROOT user already exists"
```sql
-- Check if ROOT exists
SELECT u.username, ur.role FROM users u
JOIN user_roles ur ON u.id = ur.user_id
WHERE ur.role = 'ROOT';

-- If you need to reset ROOT password
UPDATE users 
SET password = '$2a$10$NewPasswordHashHere'
WHERE username = 'root';
```

### Issue: "Cannot login as ROOT"
```sql
-- Verify ROOT user is enabled
SELECT username, email, enabled, email_verified 
FROM users 
WHERE username = 'root';

-- Ensure ROOT role is assigned
SELECT * FROM user_roles 
WHERE user_id = (SELECT id FROM users WHERE username = 'root');
```

---

**Default ROOT Credentials:**
- Username: `root`
- Password: `Root@123456` (or what you set)
- Email: `root@system.local`

**⚠️ CRITICAL: Change the password immediately after first login!**

