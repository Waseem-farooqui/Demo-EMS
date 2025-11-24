# ✅ CORS and API Configuration Fixed

## 🎯 Changes Made

### 1. Frontend Configuration Updated ✅
**File:** `frontend/src/environments/environment.prod.ts`

```typescript
// Changed from localhost to server IP
apiUrl: 'http://62.169.20.104:8080/api',
apiBaseUrl: 'http://62.169.20.104:8080',
frontendUrl: 'http://62.169.20.104',
```

### 2. Backend CORS Configuration ✅
**File:** `.env` (created with correct settings)

```bash
APP_URL=http://62.169.20.104
CORS_ALLOWED_ORIGINS=http://62.169.20.104,http://62.169.20.104:80
```

### 3. Frontend Dockerfile Fixed ✅
**File:** `frontend/Dockerfile`

```dockerfile
# Fixed Angular 17+ output path
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
```

## 🚀 Quick Fix Commands

### Option 1: Automated Fix (Recommended)
```bash
chmod +x fix-cors-and-api.sh
./fix-cors-and-api.sh
```

### Option 2: Manual Steps
```bash
# Stop all services
docker compose down

# Remove old containers and images
docker rm -f ems-backend ems-frontend ems-mysql
docker rmi $(docker images | grep ems- | awk '{print $3}')

# Rebuild everything with new configuration
docker compose build --no-cache

# Start services
docker compose up -d

# Wait for services (30 seconds)
sleep 30

# Verify
docker compose ps
curl http://localhost:8080/api/actuator/health
curl http://localhost/
```

## 📋 What Was Fixed

| Issue | Before | After |
|-------|--------|-------|
| Frontend API URL | `localhost:8080` | `62.169.20.104:8080` |
| CORS Origins | Not configured | `http://62.169.20.104` |
| Angular Build Path | `dist/frontend` | `dist/frontend/browser` |
| Nginx 403 Error | Files not found | Files in correct location |

## ✅ Verification

After rebuild, verify everything works:

```bash
# 1. Check all services running
docker compose ps

# Expected:
# ems-backend    running    8080/tcp
# ems-frontend   running    80/tcp  
# ems-mysql      running    3307/tcp

# 2. Test backend
curl http://localhost:8080/api/actuator/health
# Should return: {"status":"UP"}

# 3. Test frontend
curl http://localhost/
# Should return: HTML with Angular app

# 4. Check CORS headers
curl -H "Origin: http://62.169.20.104" -I http://localhost:8080/api/actuator/health
# Should include: Access-Control-Allow-Origin: http://62.169.20.104
```

## 🌐 Access Application

**From Server:**
- Frontend: http://localhost/
- Backend: http://localhost:8080/api

**From External (Browser):**
- Frontend: http://62.169.20.104/
- Backend: http://62.169.20.104:8080/api

## 🔍 Troubleshooting

### Still Getting 403 Forbidden?
```bash
# Check files in container
docker exec ems-frontend ls -la /usr/share/nginx/html/

# Should show:
# index.html
# main.*.js
# styles.*.css
# etc.
```

### CORS Errors in Browser Console?
```bash
# Check backend logs
docker compose logs backend | grep -i cors

# Verify CORS setting
docker compose exec backend env | grep CORS
```

### Frontend Not Connecting to Backend?
```bash
# Check frontend build included correct environment
docker exec ems-frontend grep -r "62.169.20.104" /usr/share/nginx/html/

# Should find the IP in compiled JS files
```

## 📝 Environment Variables (.env)

Your `.env` file now contains:

```bash
# Application URLs
APP_URL=http://62.169.20.104

# CORS - Only allow requests from server IP
CORS_ALLOWED_ORIGINS=http://62.169.20.104,http://62.169.20.104:80

# Database
DB_NAME=employee_management_system
DB_USERNAME=emsuser
DB_PASSWORD=SecurePassword123!
DB_ROOT_PASSWORD=RootPassword456!

# JWT
JWT_SECRET=YourSecureJWTSecretKeyHere...

# Email (optional - leave empty if not configured)
MAIL_USERNAME=
MAIL_PASSWORD=
```

## ⚙️ Files Modified

1. ✅ `frontend/src/environments/environment.prod.ts` - API URLs updated
2. ✅ `frontend/Dockerfile` - Build output path fixed
3. ✅ `.env` - Created with correct CORS and URLs
4. ✅ `fix-cors-and-api.sh` - Automated fix script

## 🎯 Summary

**Before:**
- ❌ Frontend trying to connect to localhost
- ❌ CORS not configured
- ❌ 403 Forbidden error
- ❌ Wrong Angular build path

**After:**
- ✅ Frontend connects to 62.169.20.104:8080
- ✅ CORS allows only 62.169.20.104
- ✅ Files in correct nginx location
- ✅ Angular 17+ build path fixed

## 🚀 Next Steps

1. Run the fix script or manual rebuild
2. Access http://62.169.20.104 in browser
3. You should see the Employee Management System login page
4. Backend API calls will work correctly with CORS

---

**Status:** ✅ Ready to deploy
**Run:** `./fix-cors-and-api.sh` or rebuild manually

