# How to Run the Application (IntelliJ IDEA)

## Running the Backend (Spring Boot)

Since Maven is not available from command line, use IntelliJ IDEA:

### Method 1: Using IntelliJ IDEA (Recommended)
1. Open IntelliJ IDEA
2. Open the project: `C:\Users\waseem.uddin\EmployeeManagementSystem`
3. Wait for the project to load and dependencies to download
4. Navigate to: `src/main/java/com/was/employeemanagementsystem/EmployeeManagementSystemApplication.java`
5. Right-click on the file
6. Select **"Run 'EmployeeManagementSystemApplication'"**
7. The application will start on **http://localhost:8080**

### Method 2: Using IntelliJ Maven Tool Window
1. Open IntelliJ IDEA
2. Click on **"Maven"** tab on the right side
3. Expand your project
4. Expand **"Plugins"** → **"spring-boot"**
5. Double-click **"spring-boot:run"**

### Verify Backend is Running
- Open browser: http://localhost:8080/api/employees
- You should see: `[]` (empty array)
- H2 Console: http://localhost:8080/h2-console

---

## Running the Frontend (Angular)

### Prerequisites Check
First, verify Node.js is installed:
```cmd
node -v
npm -v
```

### Start Angular Application

1. Open a **new** Command Prompt or PowerShell
2. Navigate to frontend directory:
```cmd
cd C:\Users\waseem.uddin\EmployeeManagementSystem\frontend
```

3. Install dependencies (first time only):
```cmd
npm install
```

4. Start the development server:
```cmd
npm start
```

5. Wait for: **"Compiled successfully!"**

6. Open browser: **http://localhost:4200**

---

## Testing Without Backend

You can test the Angular UI even if the backend isn't running:
- The UI will load
- You'll see error messages when trying to fetch data
- This lets you verify the UI is working

---

## Expected Output

### Backend Console Output (IntelliJ)
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.x.x)

2025-10-29T... INFO ... Starting EmployeeManagementSystemApplication
2025-10-29T... INFO ... Tomcat initialized with port 8080 (http)
2025-10-29T... INFO ... Started EmployeeManagementSystemApplication in X.XXX seconds
```

### Frontend Console Output
```
** Angular Live Development Server is listening on localhost:4200 **

✔ Compiled successfully.
✔ Browser application bundle generation complete.

Initial Chunk Files | Names         |  Raw Size
main.js             | main          | XXX.XX kB
...

Application bundle generation complete. [X.XXX seconds]

Watch mode enabled. Watching for file changes...
```

### Browser Output
- Navigate to: http://localhost:4200
- You should see: **"Employee Management System"** header
- A button: **"Add New Employee"**
- Message: **"No employees found. Click 'Add New Employee' to get started."**

---

## Quick Test Steps

Once both are running:

1. **Create an Employee:**
   - Click "Add New Employee"
   - Fill in:
     - Full Name: `John Doe`
     - Person Type: `Full-time`
     - Work Email: `john.doe@company.com`
     - Job Title: `Software Engineer`
     - Date of Joining: Select today's date
   - Click "Create"

2. **Verify in List:**
   - You'll be redirected to the employee list
   - John Doe should appear in the table

3. **Edit Employee:**
   - Click green "Edit" button
   - Change Job Title to `Senior Software Engineer`
   - Click "Update"

4. **Delete Employee:**
   - Click red "Delete" button
   - Confirm deletion
   - Employee disappears from list

---

## Troubleshooting

### Backend Won't Start in IntelliJ
- **Issue:** Dependencies not downloaded
- **Solution:** Right-click `pom.xml` → Maven → Reload Project

### Frontend Won't Start
- **Issue:** Dependencies not installed
- **Solution:** Delete `node_modules` folder and run `npm install` again

### Port Already in Use
- **Backend (8080):** 
  - Check if another application is using port 8080
  - Change port in `application.properties`: `server.port=8081`
- **Frontend (4200):**
  - Run: `npm start -- --port 4201`

### CORS Errors
- Make sure backend started successfully before starting frontend
- Check browser console for specific error messages

---

## Alternative: Test Backend with Postman/curl

If you just want to verify the backend works:

### Using Browser
Visit: `http://localhost:8080/api/employees`

### Using PowerShell
```powershell
# Get all employees
Invoke-RestMethod -Uri "http://localhost:8080/api/employees" -Method GET

# Create an employee
$body = @{
    fullName = "Jane Smith"
    personType = "Full-time"
    workEmail = "jane.smith@company.com"
    jobTitle = "Project Manager"
    dateOfJoining = "2025-10-29"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/employees" -Method POST -Body $body -ContentType "application/json"
```

---

## Summary

**To run both applications:**
1. ✅ Start Backend: Use IntelliJ IDEA to run the main application class
2. ✅ Start Frontend: Open terminal, run `npm install` then `npm start`
3. ✅ Access: Open http://localhost:4200 in browser
4. ✅ Test: Create, view, edit, and delete employees

**Ports:**
- Backend: http://localhost:8080
- Frontend: http://localhost:4200
- H2 Console: http://localhost:8080/h2-console

