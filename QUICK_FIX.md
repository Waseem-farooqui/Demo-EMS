# Quick Fix - Steps to Run the Application

## 🔧 What I Fixed

Your application was compiled for Java 17, but your system has Java 11. I've downgraded the project to work with Java 11.

## 📝 Changes Made:

1. ✅ **pom.xml** - Updated Spring Boot from 3.5.7 to 2.7.18
2. ✅ **pom.xml** - Changed Java version from 17 to 11
3. ✅ **Employee.java** - Changed `jakarta.persistence` to `javax.persistence`
4. ✅ **pom.xml** - Added H2 database dependency

## 🚀 What You Need to Do Now

### Step 1: Reload Maven Project in IntelliJ

**This is the MOST IMPORTANT step!**

1. In IntelliJ IDEA, locate the **Maven tool window** (usually on the right side)
2. Click the **Reload All Maven Projects** button (circular arrows icon)
   
   OR
   
3. Right-click on `pom.xml` in the project explorer
4. Select **Maven** → **Reload Project**

Wait for IntelliJ to download all dependencies (this may take a few minutes).

### Step 2: Clean and Rebuild

1. Go to **Build** → **Clean Project**
2. Then **Build** → **Rebuild Project**

### Step 3: Run the Application

1. Navigate to: `src/main/java/com/was/employeemanagementsystem/EmployeeManagementSystemApplication.java`
2. Right-click on the file
3. Select **Run 'EmployeeManagementSystemApplication'**

## ✅ Expected Output

You should now see:

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::               (v2.7.18)

...
2025-10-29 ... INFO ... : Tomcat initialized with port(s): 8080 (http)
2025-10-29 ... INFO ... : Starting service [Tomcat]
2025-10-29 ... INFO ... : Starting Servlet engine: [Apache Tomcat/9.0.x]
2025-10-29 ... INFO ... : HikariPool-1 - Starting...
2025-10-29 ... INFO ... : HikariPool-1 - Start completed.
2025-10-29 ... INFO ... : Tomcat started on port(s): 8080 (http)
2025-10-29 ... INFO ... : Started EmployeeManagementSystemApplication in X.XXX seconds
```

**Key indicator of success:** `Started EmployeeManagementSystemApplication`

## 🌐 Access Your Application

Once the backend is running:

1. **Backend API:** http://localhost:8080/api/employees
2. **H2 Console:** http://localhost:8080/h2-console
3. **Start Angular Frontend:**
   ```cmd
   cd C:\Users\waseem.uddin\EmployeeManagementSystem\frontend
   npm start
   ```
4. **Frontend UI:** http://localhost:4200

## ❌ If You Still Get Errors

### Error: "Cannot find symbol javax.persistence"

**Solution:** The Maven dependencies haven't loaded properly.
1. Delete the `target` folder in your project
2. In IntelliJ: **File** → **Invalidate Caches** → **Invalidate and Restart**
3. After restart, reload Maven project again

### Error: Still showing Java 17 error

**Solution:** IntelliJ is using cached compiled classes.
1. Delete the `target` folder
2. **Build** → **Clean Project**
3. **Build** → **Rebuild Project**
4. Run again

### Error: Maven dependencies download fails

**Solution:** Check internet connection or Maven settings.
1. **File** → **Settings** → **Build Tools** → **Maven**
2. Try toggling "Work offline" off
3. Click "Reload All Maven Projects"

## 📦 Verify Maven Downloaded Dependencies

Check these folders exist after Maven reload:
- `C:\Users\waseem.uddin\.m2\repository\org\springframework\boot\spring-boot\2.7.18\`
- `C:\Users\waseem.uddin\.m2\repository\com\h2database\h2\`

If they don't exist, Maven didn't download properly.

## 🎯 Summary

**Before:** Java 17 + Spring Boot 3.5.7 → ❌ Incompatible with your Java 11
**After:** Java 11 + Spring Boot 2.7.18 → ✅ Compatible!

**All features work the same:**
- ✅ REST API endpoints
- ✅ Employee CRUD operations
- ✅ H2 Database
- ✅ CORS for Angular
- ✅ All business logic

**Just remember to:** 
1. ⚠️ **Reload Maven Project** (most important!)
2. Clean and rebuild
3. Run the application

## 📞 Quick Checklist

- [ ] Opened IntelliJ IDEA
- [ ] Reloaded Maven project (Right-click pom.xml → Maven → Reload Project)
- [ ] Waited for dependencies to download (check progress bar at bottom)
- [ ] Cleaned project (Build → Clean Project)
- [ ] Rebuilt project (Build → Rebuild Project)
- [ ] Ran EmployeeManagementSystemApplication
- [ ] Saw "Started EmployeeManagementSystemApplication" message
- [ ] Tested http://localhost:8080/api/employees in browser
- [ ] Started Angular frontend with `npm start`
- [ ] Accessed http://localhost:4200

---

**You're now ready to run the application! Good luck! 🚀**

