# 🚨 Immediate Action Items - Production Readiness

**Priority:** CRITICAL - Must fix before production deployment

---

## 🔴 Critical Security Fixes (Do First)

### 1. Remove Hardcoded API Key
**File:** `src/main/resources/application.properties:93`  
**Action:**
```properties
# Change from:
ocr.api.key=K81768751288957

# To:
ocr.api.key=${OCR_API_KEY:}
```
**Then:** Add `OCR_API_KEY` to your `.env` file

---

### 2. Fix Hardcoded IP Addresses
**File:** `frontend/src/environments/environment.prod.ts`  
**Action:** Replace hardcoded IPs with environment-based configuration or build-time variables.

**Option A - Use build-time replacement:**
```typescript
export const environment = {
  production: true,
  apiUrl: '${API_URL}',
  apiBaseUrl: '${API_BASE_URL}',
  frontendUrl: '${FRONTEND_URL}',
  // ... rest of config
};
```

**Option B - Use runtime configuration:**
Create a config service that loads from `/assets/config.json` at runtime.

---

### 3. Add .env to .gitignore
**File:** `.gitignore`  
**Action:** Add these lines:
```
# Environment variables
.env
.env.*
!.env.example
```

---

### 4. Create .env.example File
**Action:** Create `.env.example` with all required variables (no actual values):
```bash
# Database Configuration
DB_NAME=employee_management_system
DB_USERNAME=emsuser
DB_PASSWORD=your_strong_password_here
DB_ROOT_PASSWORD=your_root_password_here
DB_PORT=3307

# JWT Configuration
JWT_SECRET=generate_with_openssl_rand_base64_64
JWT_EXPIRATION=86400000

# Email Configuration
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_specific_password
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=true
MAIL_SMTP_STARTTLS_REQUIRED=true
MAIL_SMTP_SSL_TRUST=smtp.gmail.com

# Application URLs
APP_URL=https://yourdomain.com
CORS_ALLOWED_ORIGINS=https://yourdomain.com

# OCR Configuration (Optional)
OCR_API_KEY=your_ocr_api_key_here

# Port Configuration
BACKEND_PORT=8080
FRONTEND_PORT=80
```

---

### 5. Restrict Actuator Endpoints
**File:** `src/main/resources/application-prod.properties:101-102`  
**Action:**
```properties
# Change from:
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=when-authorized

# To:
management.endpoints.web.exposure.include=health
management.endpoint.health.show-details=never
management.endpoints.web.base-path=/actuator
```

---

### 6. Disable H2 Console in Production
**File:** `src/main/java/com/was/employeemanagementsystem/config/SecurityConfig.java:81`  
**Action:** Remove or conditionally enable:
```java
// Remove this line:
.antMatchers("/h2-console/**").permitAll()

// Or make it conditional:
.antMatchers("/h2-console/**").access("hasRole('ROOT') and @environment.acceptsProfiles('dev')")
```

---

### 7. Scope H2 to Test Only
**File:** `pom.xml:119-123`  
**Action:**
```xml
<!-- Change from: -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- To: -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

---

## 🟠 High Priority Fixes (Do Next)

### 8. Set DDL Auto to Validate
**File:** `src/main/resources/application-prod.properties:14`  
**Action:**
```properties
# Change from:
spring.jpa.hibernate.ddl-auto=${JPA_DDL_AUTO:update}

# To:
spring.jpa.hibernate.ddl-auto=validate
```

---

### 9. Enable SSL/TLS
**File:** `src/main/resources/application-prod.properties:98`  
**Action:**
```properties
# Change from:
server.ssl.enabled=false

# To:
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=${SSL_KEYSTORE_PASSWORD}
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=tomcat
```

**Note:** You'll need to generate a keystore or use Let's Encrypt with a reverse proxy.

---

### 10. Add Resource Limits to Docker
**File:** `compose.yaml`  
**Action:** Add to each service:
```yaml
backend:
  # ... existing config ...
  deploy:
    resources:
      limits:
        cpus: '2'
        memory: 2G
      reservations:
        cpus: '1'
        memory: 1G
```

---

## ✅ Quick Verification Checklist

After making changes, verify:

- [ ] No hardcoded credentials in source code
- [ ] `.env` is in `.gitignore`
- [ ] `.env.example` exists with all variables
- [ ] Actuator endpoints restricted
- [ ] H2 console disabled in production
- [ ] H2 dependency scoped to test
- [ ] DDL auto set to validate
- [ ] SSL configured (or reverse proxy with SSL)
- [ ] Resource limits set in Docker

---

## 🧪 Test Your Changes

1. **Build the application:**
   ```bash
   mvn clean package -DskipTests
   ```

2. **Check for hardcoded secrets:**
   ```bash
   grep -r "K81768751288957" src/
   grep -r "62.169.20.104" frontend/
   ```

3. **Verify .env is ignored:**
   ```bash
   git status
   # .env should not appear
   ```

4. **Test Docker build:**
   ```bash
   docker-compose build
   docker-compose up -d
   ```

---

## 📝 Next Steps After Critical Fixes

1. Implement rate limiting on auth endpoints
2. Add password strength validation
3. Implement JWT token blacklist
4. Add comprehensive test coverage
5. Set up monitoring and alerting

See `PRODUCTION_READINESS_ASSESSMENT.md` for complete details.

---

**Estimated Time:** 2-4 hours for critical fixes

