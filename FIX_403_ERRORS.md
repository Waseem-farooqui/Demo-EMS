# 🔧 Fixing 403 Forbidden Errors - Complete Guide

## 🎯 Problem

After rebuilding the frontend with `redep-frontend.sh`, all API calls are returning **403 Forbidden** errors.

## 🔍 Root Causes

### 1. **CORS Configuration Missing**
The backend's CORS configuration in `.env` was not set, so it defaulted to `localhost:4200`, but the frontend is accessing from `62.169.20.104`.

### 2. **Frontend API URLs Not Baked In**
The frontend build wasn't using the correct API URLs, so it might still be trying to connect to `localhost` instead of the server IP.

### 3. **JWT Token Issues**
After login, the JWT token might not be stored correctly or sent with API requests.

## ✅ Solution

### Quick Fix (Automated)

```bash
# Make script executable
chmod +x fix-403-errors.sh

# Run the fix
./fix-403-errors.sh
```

This script will:
1. ✅ Verify CORS configuration in `.env`
2. ✅ Stop all containers
3. ✅ Remove old images
4. ✅ Rebuild backend with CORS config
5. ✅ Rebuild frontend with correct API URLs
6. ✅ Start all services
7. ✅ Test health and CORS headers
8. ✅ Verify configuration

### Manual Fix (Step-by-Step)

#### Step 1: Update `.env` File

Ensure your `.env` file has these lines:

```bash
# Application URLs
APP_URL=http://62.169.20.104
BACKEND_PORT=8080
FRONTEND_PORT=80

# CORS Configuration - CRITICAL!
CORS_ALLOWED_ORIGINS=http://62.169.20.104,http://62.169.20.104:80,http://localhost,http://localhost:80

# Frontend Build Arguments
API_URL=http://62.169.20.104:8080/api
API_BASE_URL=http://62.169.20.104:8080
FRONTEND_URL=http://62.169.20.104
```

#### Step 2: Rebuild Backend (Picks up CORS from .env)

```bash
docker compose down
docker compose build --no-cache backend
```

#### Step 3: Rebuild Frontend (Bakes in API URLs)

```bash
docker compose build --no-cache \
    --build-arg API_URL="http://62.169.20.104:8080/api" \
    --build-arg API_BASE_URL="http://62.169.20.104:8080" \
    --build-arg FRONTEND_URL="http://62.169.20.104" \
    frontend
```

#### Step 4: Start All Services

```bash
docker compose up -d
```

#### Step 5: Wait and Verify

```bash
# Wait for services to start (60 seconds)
sleep 60

# Check services are running
docker compose ps

# Test backend health
curl http://localhost:8080/api/actuator/health
# Expected: {"status":"UP"}

# Test CORS headers
curl -I -H "Origin: http://62.169.20.104" http://localhost:8080/api/actuator/health
# Expected: Access-Control-Allow-Origin: http://62.169.20.104
```

## 🔍 Verification Steps

### 1. Check Backend CORS Configuration

```bash
# Check environment variable in container
docker compose exec backend env | grep CORS
# Expected: CORS_ALLOWED_ORIGINS=http://62.169.20.104,http://62.169.20.104:80,...

# Check backend logs for CORS
docker compose logs backend | grep -i cors
```

### 2. Check Frontend API Configuration

```bash
# Verify API URLs are baked into frontend build
docker exec ems-frontend grep -r "62.169.20.104:8080" /usr/share/nginx/html/
# Should find the IP in compiled JavaScript files
```

### 3. Test API from Browser Console

Open browser console (F12) on http://62.169.20.104/ and run:

```javascript
// Test health endpoint (no auth required)
fetch('http://62.169.20.104:8080/api/actuator/health')
  .then(r => r.json())
  .then(d => console.log('Health:', d))
  .catch(e => console.error('Error:', e));

// If above works but other endpoints fail, it's likely JWT token issue
```

### 4. Check Network Tab in Browser

1. Open browser DevTools (F12)
2. Go to Network tab
3. Try to login or access any page
4. Look at failed requests (red ones)
5. Click on a failed request
6. Check:
   - **Request Headers**: Should have `Origin: http://62.169.20.104`
   - **Response Headers**: Should have `Access-Control-Allow-Origin: http://62.169.20.104`
   - **Status Code**: 403 means authentication/authorization failure

## 🐛 Troubleshooting Specific Scenarios

### Scenario 1: CORS Error in Browser Console

**Error Message:**
```
Access to fetch at 'http://62.169.20.104:8080/api/...' from origin 'http://62.169.20.104' 
has been blocked by CORS policy
```

**Solution:**
```bash
# Check backend CORS config
docker compose exec backend env | grep CORS

# If not set correctly, update .env and rebuild backend
docker compose down
docker compose build --no-cache backend
docker compose up -d
```

### Scenario 2: 403 on All Endpoints (Even Public Ones)

**Symptoms:**
- Even `/api/auth/login` returns 403
- `/api/actuator/health` works fine

**Possible Causes:**
1. Security configuration is blocking requests
2. JWT filter is interfering

**Solution:**
```bash
# Check backend logs
docker compose logs backend -f

# Look for authentication errors
# If you see "JWT token is missing" or similar, check SecurityConfig.java
```

### Scenario 3: 403 Only on Protected Endpoints

**Symptoms:**
- `/api/auth/login` works
- Protected endpoints return 403
- Token is generated on login

**Possible Causes:**
1. JWT token not being sent with requests
2. Token expired
3. Token format incorrect

**Solution:**

**Check if token is stored:**
```javascript
// In browser console
console.log('Token:', localStorage.getItem('token'));
```

**Check if token is sent:**
1. Open Network tab
2. Click on a failed request
3. Check Request Headers
4. Look for: `Authorization: Bearer <token>`

**If token is NOT in headers:**
- Frontend interceptor might not be working
- Check `http-interceptor.service.ts` or similar

**If token IS in headers but still 403:**
- Token might be invalid/expired
- Backend might not be validating correctly
- Check backend logs: `docker compose logs backend -f`

### Scenario 4: Frontend Shows Blank Page or 404

**Symptoms:**
- Nginx serves files but frontend doesn't load
- Browser console shows 404 for JavaScript files

**Solution:**
```bash
# Check if files exist in container
docker exec ems-frontend ls -la /usr/share/nginx/html/

# Should show:
# index.html
# main.*.js
# polyfills.*.js
# etc.

# If files are missing, rebuild frontend
docker compose build --no-cache frontend
docker compose up -d
```

## 📋 Pre-Deployment Checklist

Before deploying or rebuilding:

- [ ] `.env` file has `CORS_ALLOWED_ORIGINS` set to server IP
- [ ] `.env` file has `API_URL`, `API_BASE_URL`, `FRONTEND_URL` set correctly
- [ ] Backend is configured to read CORS from environment variable
- [ ] Frontend Dockerfile uses build args for API URLs
- [ ] `compose.yaml` passes build args to frontend
- [ ] Security configuration allows public endpoints (login, health check)

## 🔐 Security Configuration Reference

### Backend Public Endpoints (No Auth Required)

From `SecurityConfig.java`:
```java
.antMatchers("/api/auth/login").permitAll()
.antMatchers("/api/auth/signup").permitAll()
.antMatchers("/api/auth/verify-email").permitAll()
.antMatchers("/api/auth/forgot-password").permitAll()
.antMatchers("/api/organizations/*/logo").permitAll()
.antMatchers("/api/actuator/health").permitAll()
```

### Backend Protected Endpoints (Auth Required)

All other `/api/*` endpoints require:
1. Valid JWT token in `Authorization: Bearer <token>` header
2. User must have appropriate role (ROLE_USER, ROLE_ADMIN, ROLE_SUPER_ADMIN)

## 🧪 Testing After Fix

### Test 1: Health Check (No Auth)
```bash
curl http://62.169.20.104:8080/api/actuator/health
# Expected: {"status":"UP"}
```

### Test 2: CORS Headers
```bash
curl -I -H "Origin: http://62.169.20.104" http://62.169.20.104:8080/api/actuator/health
# Expected: Access-Control-Allow-Origin: http://62.169.20.104
```

### Test 3: Login (Should Work)
```bash
curl -X POST http://62.169.20.104:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -H "Origin: http://62.169.20.104" \
  -d '{
    "username": "admin@example.com",
    "password": "password"
  }'
# Expected: {"token": "...", "user": {...}}
```

### Test 4: Protected Endpoint (With Token)
```bash
TOKEN="<token-from-login>"

curl http://62.169.20.104:8080/api/users/profile \
  -H "Authorization: Bearer $TOKEN" \
  -H "Origin: http://62.169.20.104"
# Expected: User profile data
```

### Test 5: Protected Endpoint (Without Token)
```bash
curl http://62.169.20.104:8080/api/users/profile \
  -H "Origin: http://62.169.20.104"
# Expected: 401 Unauthorized (not 403!)
```

## 🎯 Expected Behavior vs Issues

| Scenario | Expected | If 403 | If Other Error |
|----------|----------|--------|----------------|
| Health Check (no auth) | 200 OK | CORS issue | Server down |
| Login (no auth) | 200 + token | CORS issue | Credentials wrong |
| Protected API (no token) | 401 Unauthorized | Security config wrong | - |
| Protected API (with valid token) | 200 + data | Token validation issue | Authorization/permissions |
| Protected API (with expired token) | 401 Unauthorized | - | - |

## 📞 Getting More Information

### View Real-Time Backend Logs
```bash
docker compose logs backend -f
```

### View Real-Time Frontend Logs
```bash
docker compose logs frontend -f
```

### View All Logs
```bash
docker compose logs -f
```

### Check Container Status
```bash
docker compose ps
```

### Restart Everything
```bash
docker compose restart
```

## ✅ Success Criteria

After applying the fix, you should see:

1. ✅ Backend health check returns `{"status":"UP"}`
2. ✅ CORS headers include `Access-Control-Allow-Origin: http://62.169.20.104`
3. ✅ Frontend loads without errors
4. ✅ Login works and returns JWT token
5. ✅ Protected API calls work with token
6. ✅ No CORS errors in browser console
7. ✅ No 403 errors on public endpoints
8. ✅ 401 (not 403) on protected endpoints without token

## 🚀 Quick Commands Reference

```bash
# Stop everything
docker compose down

# Rebuild with correct config
docker compose build --no-cache

# Start
docker compose up -d

# Check status
docker compose ps

# View logs
docker compose logs -f backend

# Test health
curl http://localhost:8080/api/actuator/health

# Test CORS
curl -I -H "Origin: http://62.169.20.104" http://localhost:8080/api/actuator/health
```

---

**Status**: Ready to fix 403 errors
**Script**: `./fix-403-errors.sh`
**Time**: ~5 minutes for complete rebuild

