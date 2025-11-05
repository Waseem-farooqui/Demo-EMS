# Application Output Demonstration

## Expected Console Outputs

### 1. Spring Boot Backend Output

When you run the backend in IntelliJ IDEA, you should see:

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.4.0)

2025-10-29T10:30:15.123  INFO 12345 --- [           main] c.w.e.EmployeeManagementSystemApp        : Starting EmployeeManagementSystemApplication using Java 17.0.8
2025-10-29T10:30:15.125  INFO 12345 --- [           main] c.w.e.EmployeeManagementSystemApp        : No active profile set, falling back to 1 default profile: "default"
2025-10-29T10:30:16.234  INFO 12345 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Bootstrapping Spring Data JPA repositories in DEFAULT mode.
2025-10-29T10:30:16.345  INFO 12345 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Finished Spring Data repository scanning in 95 ms. Found 1 JPA repository interfaces.
2025-10-29T10:30:17.123  INFO 12345 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat initialized with port 8080 (http)
2025-10-29T10:30:17.134  INFO 12345 --- [           main] o.apache.catalina.core.StandardService   : Starting service [Tomcat]
2025-10-29T10:30:17.135  INFO 12345 --- [           main] o.apache.catalina.core.StandardEngine    : Starting Servlet engine: [Apache Tomcat/10.1.16]
2025-10-29T10:30:17.234  INFO 12345 --- [           main] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring embedded WebApplicationContext
2025-10-29T10:30:17.567  INFO 12345 --- [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Starting...
2025-10-29T10:30:17.789  INFO 12345 --- [           main] com.zaxxer.hikari.pool.HikariPool        : HikariPool-1 - Added connection conn0: url=jdbc:h2:mem:employeedb user=SA
2025-10-29T10:30:17.791  INFO 12345 --- [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Start completed.
2025-10-29T10:30:17.845  INFO 12345 --- [           main] o.hibernate.jpa.internal.util.LogHelper  : HHH000204: Processing PersistenceUnitInfo [name: default]
2025-10-29T10:30:17.912  INFO 12345 --- [           main] org.hibernate.Version                    : HHH000412: Hibernate ORM core version 6.3.1.Final
2025-10-29T10:30:18.234  INFO 12345 --- [           main] o.h.e.t.j.p.i.JtaPlatformInitiator       : HHH000490: Using JtaPlatform implementation: [org.hibernate.engine.transaction.jta.platform.internal.NoJtaPlatform]
2025-10-29T10:30:18.456  INFO 12345 --- [           main] o.s.o.j.LocalContainerEntityManagerFB    : Initialized JPA EntityManagerFactory for persistence unit 'default'
2025-10-29T10:30:18.789  WARN 12345 --- [           main] JpaBaseConfiguration$JpaWebConfiguration : spring.jpa.open-in-view is enabled by default.
2025-10-29T10:30:19.234  INFO 12345 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port 8080 (http) with context path ''
2025-10-29T10:30:19.245  INFO 12345 --- [           main] c.w.e.EmployeeManagementSystemApp        : Started EmployeeManagementSystemApplication in 4.234 seconds (process running for 4.789)
```

**Key Messages:**
- ✅ `Tomcat started on port 8080` - Backend is running
- ✅ `HikariPool-1 - Start completed` - Database connection established
- ✅ `Started EmployeeManagementSystemApplication` - Application ready

---

### 2. Angular Frontend Output

When you run `npm start` in the frontend directory:

```
> frontend@0.0.0 start
> ng serve

Initial chunk files | Names         |  Raw Size
polyfills.js        | polyfills     |  83.60 kB | 
main.js             | main          |  23.45 kB | 
styles.css          | styles        |   2.34 kB | 

                    | Initial Total | 109.39 kB

Application bundle generation complete. [2.456 seconds]

Watch mode enabled. Watching for file changes...
NOTE: Raw file sizes do not reflect development server per-request transformations.
  ➜  Local:   http://localhost:4200/
  ➜  press h + enter to show help
```

**Key Messages:**
- ✅ `Application bundle generation complete` - Compilation successful
- ✅ `Local: http://localhost:4200/` - Frontend is accessible
- ✅ `Watch mode enabled` - Will auto-reload on code changes

---

## Visual Output in Browser

### Initial Load (http://localhost:4200)

```
┌─────────────────────────────────────────────────────────────┐
│  Employee Management System              [Add New Employee] │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│          No employees found.                                 │
│          Click "Add New Employee" to get started.           │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### After Adding First Employee

Click "Add New Employee" and fill the form:
```
┌─────────────────────────────────────────────────────────────┐
│  Add New Employee                                            │
├─────────────────────────────────────────────────────────────┤
│  Full Name *                                                 │
│  [John Doe                                      ]            │
│                                                              │
│  Person Type *                                               │
│  [Full-time ▼]                                              │
│                                                              │
│  Work Email *                                                │
│  [john.doe@company.com                          ]            │
│                                                              │
│  Job Title *                                                 │
│  [Software Engineer                             ]            │
│                                                              │
│  Reference                                                   │
│  [EMP001                                        ]            │
│                                                              │
│  Date of Joining *                                           │
│  [2025-10-29                                    ]            │
│                                                              │
│  Working Timing                                              │
│  [9 AM - 5 PM                                   ]            │
│                                                              │
│  Holiday Allowance (days)                                    │
│  [20                                            ]            │
│                                                              │
│  [Create]  [Cancel]                                          │
└─────────────────────────────────────────────────────────────┘
```

After clicking "Create", you'll see:

```
┌───────────────────────────────────────────────────────────────────────────────────────────┐
│  Employee Management System                                        [Add New Employee]     │
├───────────────────────────────────────────────────────────────────────────────────────────┤
│ ID │ Full Name  │ Person   │ Work Email           │ Job Title │ Ref    │ Date Joined      │
│    │            │ Type     │                      │           │        │                  │
├────┼────────────┼──────────┼──────────────────────┼───────────┼────────┼──────────────────┤
│ 1  │ John Doe   │ Full-time│ john.doe@company.com │ Software  │ EMP001 │ Oct 29, 2025     │
│    │            │          │                      │ Engineer  │        │                  │
│    │            │          │                      │           │        │ [Edit] [Delete]  │
└────┴────────────┴──────────┴──────────────────────┴───────────┴────────┴──────────────────┘
```

---

## Backend API Responses

### GET /api/employees (Get All)

**Request:**
```http
GET http://localhost:8080/api/employees
```

**Response (Empty):**
```json
[]
```

**Response (With Data):**
```json
[
  {
    "id": 1,
    "fullName": "John Doe",
    "personType": "Full-time",
    "workEmail": "john.doe@company.com",
    "jobTitle": "Software Engineer",
    "reference": "EMP001",
    "dateOfJoining": "2025-10-29",
    "workingTiming": "9 AM - 5 PM",
    "holidayAllowance": 20
  }
]
```

### POST /api/employees (Create)

**Request:**
```http
POST http://localhost:8080/api/employees
Content-Type: application/json

{
  "fullName": "Jane Smith",
  "personType": "Part-time",
  "workEmail": "jane.smith@company.com",
  "jobTitle": "Project Manager",
  "reference": "EMP002",
  "dateOfJoining": "2025-10-29",
  "workingTiming": "10 AM - 3 PM",
  "holidayAllowance": 15
}
```

**Response (201 Created):**
```json
{
  "id": 2,
  "fullName": "Jane Smith",
  "personType": "Part-time",
  "workEmail": "jane.smith@company.com",
  "jobTitle": "Project Manager",
  "reference": "EMP002",
  "dateOfJoining": "2025-10-29",
  "workingTiming": "10 AM - 3 PM",
  "holidayAllowance": 15
}
```

### GET /api/employees/1 (Get One)

**Request:**
```http
GET http://localhost:8080/api/employees/1
```

**Response (200 OK):**
```json
{
  "id": 1,
  "fullName": "John Doe",
  "personType": "Full-time",
  "workEmail": "john.doe@company.com",
  "jobTitle": "Software Engineer",
  "reference": "EMP001",
  "dateOfJoining": "2025-10-29",
  "workingTiming": "9 AM - 5 PM",
  "holidayAllowance": 20
}
```

### PUT /api/employees/1 (Update)

**Request:**
```http
PUT http://localhost:8080/api/employees/1
Content-Type: application/json

{
  "fullName": "John Doe",
  "personType": "Full-time",
  "workEmail": "john.doe@company.com",
  "jobTitle": "Senior Software Engineer",
  "reference": "EMP001",
  "dateOfJoining": "2025-10-29",
  "workingTiming": "9 AM - 5 PM",
  "holidayAllowance": 25
}
```

**Response (200 OK):**
```json
{
  "id": 1,
  "fullName": "John Doe",
  "personType": "Full-time",
  "workEmail": "john.doe@company.com",
  "jobTitle": "Senior Software Engineer",
  "reference": "EMP001",
  "dateOfJoining": "2025-10-29",
  "workingTiming": "9 AM - 5 PM",
  "holidayAllowance": 25
}
```

### DELETE /api/employees/1 (Delete)

**Request:**
```http
DELETE http://localhost:8080/api/employees/1
```

**Response (204 No Content):**
```
(Empty response body)
```

---

## Browser Network Tab Output

When the Angular app loads and fetches employees:

```
Request URL: http://localhost:8080/api/employees
Request Method: GET
Status Code: 200 OK
Response Headers:
  Content-Type: application/json
  Access-Control-Allow-Origin: http://localhost:4200
  
Response Body:
[{"id":1,"fullName":"John Doe",...}]
```

When creating an employee:

```
Request URL: http://localhost:8080/api/employees
Request Method: POST
Status Code: 201 Created
Request Headers:
  Content-Type: application/json
  Origin: http://localhost:4200
Request Body:
  {"fullName":"Jane Smith",...}
Response Body:
  {"id":2,"fullName":"Jane Smith",...}
```

---

## H2 Database Console Output

Access: http://localhost:8080/h2-console

**Login Screen:**
```
┌──────────────────────────────┐
│  H2 Console                  │
├──────────────────────────────┤
│ JDBC URL:                    │
│ [jdbc:h2:mem:employeedb]    │
│                              │
│ User Name: [sa]              │
│ Password:  [    ]            │
│                              │
│        [Connect]             │
└──────────────────────────────┘
```

**After Connecting - Query:**
```sql
SELECT * FROM EMPLOYEES;
```

**Result:**
```
┌────┬───────────┬───────────┬──────────────────────┬──────────────────┬──────────┬────────────────┬────────────────┬──────────────────┐
│ ID │ FULL_NAME │PERSON_TYPE│    WORK_EMAIL        │    JOB_TITLE     │REFERENCE │DATE_OF_JOINING │WORKING_TIMING  │HOLIDAY_ALLOWANCE │
├────┼───────────┼───────────┼──────────────────────┼──────────────────┼──────────┼────────────────┼────────────────┼──────────────────┤
│ 1  │ John Doe  │ Full-time │john.doe@company.com  │Software Engineer │ EMP001   │ 2025-10-29     │ 9 AM - 5 PM    │ 20               │
│ 2  │Jane Smith │ Part-time │jane.smith@company.com│Project Manager   │ EMP002   │ 2025-10-29     │ 10 AM - 3 PM   │ 15               │
└────┴───────────┴───────────┴──────────────────────┴──────────────────┴──────────┴────────────────┴────────────────┴──────────────────┘
```

---

## Error Scenarios

### Backend Not Running

**Browser Console:**
```
GET http://localhost:8080/api/employees net::ERR_CONNECTION_REFUSED
```

**UI Display:**
```
┌─────────────────────────────────────────────────────────────┐
│  Employee Management System              [Add New Employee] │
├─────────────────────────────────────────────────────────────┤
│  ⚠ Failed to load employees. Please try again.              │
└─────────────────────────────────────────────────────────────┘
```

### Validation Error

**When submitting invalid form:**
```
┌─────────────────────────────────────────────────────────────┐
│  Add New Employee                                            │
├─────────────────────────────────────────────────────────────┤
│  Full Name *                                                 │
│  [                                          ]                │
│  ❌ Full name is required.                                   │
│                                                              │
│  Work Email *                                                │
│  [invalid-email                             ]                │
│  ❌ Valid work email is required.                            │
└─────────────────────────────────────────────────────────────┘
```

### Duplicate Email Error

**Backend Response (409 Conflict):**
```json
{
  "error": "Employee with this email already exists"
}
```

**UI Display:**
```
⚠ Failed to create employee. Please try again.
```

---

## Summary of Outputs

✅ **Backend Console:** Spring Boot startup logs, Tomcat on port 8080
✅ **Frontend Console:** Angular compilation success, running on port 4200
✅ **Browser UI:** Clean table interface with CRUD operations
✅ **API Responses:** JSON data for all operations
✅ **H2 Console:** SQL queries to view data directly
✅ **Network Tab:** HTTP requests/responses between frontend and backend

---

## To See These Outputs Yourself:

1. **Start Backend:** Open in IntelliJ IDEA and run the main application class
2. **Start Frontend:** Run `npm start` in the frontend directory
3. **Open Browser:** Navigate to http://localhost:4200
4. **Use the App:** Create, view, edit, and delete employees
5. **Check Console:** View backend logs in IntelliJ, frontend logs in terminal
6. **Inspect Network:** Use browser DevTools → Network tab to see API calls

