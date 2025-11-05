.# Application Architecture

## System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                         Browser                              │
│                    http://localhost:4200                     │
│                                                               │
│  ┌────────────────────────────────────────────────────────┐ │
│  │              Angular Application                        │ │
│  │                                                          │ │
│  │  ┌──────────────────────────────────────────────────┐  │ │
│  │  │  App Component (Root)                            │  │ │
│  │  │  - Router Outlet                                  │  │ │
│  │  └──────────────────────────────────────────────────┘  │ │
│  │                          │                              │ │
│  │         ┌────────────────┴────────────────┐            │ │
│  │         ▼                                  ▼            │ │
│  │  ┌─────────────────┐            ┌──────────────────┐  │ │
│  │  │ Employee List   │            │ Employee Form     │  │ │
│  │  │ Component       │            │ Component         │  │ │
│  │  │                 │            │                   │  │ │
│  │  │ - Display Table │            │ - Create/Edit     │  │ │
│  │  │ - Edit Button   │            │ - Validation      │  │ │
│  │  │ - Delete Button │            │ - Submit          │  │ │
│  │  └────────┬────────┘            └─────────┬────────┘  │ │
│  │           │                               │            │ │
│  │           └───────────┬───────────────────┘            │ │
│  │                       ▼                                │ │
│  │            ┌────────────────────┐                      │ │
│  │            │ Employee Service   │                      │ │
│  │            │                    │                      │ │
│  │            │ - getAllEmployees()│                      │ │
│  │            │ - getEmployeeById()│                      │ │
│  │            │ - createEmployee() │                      │ │
│  │            │ - updateEmployee() │                      │ │
│  │            │ - deleteEmployee() │                      │ │
│  │            └─────────┬──────────┘                      │ │
│  │                      │                                  │ │
│  │                      │ HttpClient                       │ │
│  └──────────────────────┼──────────────────────────────────┘ │
│                         │                                    │
└─────────────────────────┼────────────────────────────────────┘
                          │ HTTP Requests
                          │ (JSON)
┌─────────────────────────▼────────────────────────────────────┐
│                   Spring Boot Backend                         │
│                  http://localhost:8080                        │
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐│
│  │  CORS Filter                                              ││
│  │  - Allow localhost:4200                                   ││
│  └────────────────────────────┬─────────────────────────────┘│
│                                │                               │
│  ┌────────────────────────────▼─────────────────────────────┐│
│  │  Employee Controller                                      ││
│  │  /api/employees                                           ││
│  │                                                            ││
│  │  GET    /api/employees          → getAllEmployees()       ││
│  │  GET    /api/employees/{id}     → getEmployeeById()       ││
│  │  POST   /api/employees          → createEmployee()        ││
│  │  PUT    /api/employees/{id}     → updateEmployee()        ││
│  │  DELETE /api/employees/{id}     → deleteEmployee()        ││
│  └────────────────────────────┬─────────────────────────────┘│
│                                │                               │
│  ┌────────────────────────────▼─────────────────────────────┐│
│  │  Employee Service                                         ││
│  │  - Business Logic                                         ││
│  │  - DTO ↔ Entity Conversion                                ││
│  │  - Validation                                             ││
│  └────────────────────────────┬─────────────────────────────┘│
│                                │                               │
│  ┌────────────────────────────▼─────────────────────────────┐│
│  │  Employee Repository (JPA)                                ││
│  │  - CRUD Operations                                        ││
│  │  - Custom Queries                                         ││
│  └────────────────────────────┬─────────────────────────────┘│
│                                │                               │
│  ┌────────────────────────────▼─────────────────────────────┐│
│  │  H2 Database (In-Memory)                                  ││
│  │  - employees table                                        ││
│  │  - Accessible at /h2-console                              ││
│  └──────────────────────────────────────────────────────────┘│
└───────────────────────────────────────────────────────────────┘
```

## Data Flow

### Creating an Employee

```
1. User fills form in Employee Form Component
   ↓
2. Click "Create" button
   ↓
3. Form validation (Angular Reactive Forms)
   ↓
4. Employee Service.createEmployee(employee)
   ↓
5. HTTP POST to /api/employees
   ↓
6. Spring Boot CORS Filter validates origin
   ↓
7. Employee Controller.createEmployee()
   ↓
8. Employee Service validates & converts DTO → Entity
   ↓
9. Employee Repository saves to H2 database
   ↓
10. Entity returned & converted to DTO
   ↓
11. HTTP Response (201 Created) with employee data
   ↓
12. Angular navigates to Employee List
   ↓
13. List automatically loads with new employee
```

### Editing an Employee

```
1. User clicks "Edit" on Employee List
   ↓
2. Navigate to /employees/edit/:id
   ↓
3. Employee Form Component loads
   ↓
4. Employee Service.getEmployeeById(id)
   ↓
5. HTTP GET to /api/employees/{id}
   ↓
6. Backend retrieves employee from database
   ↓
7. Returns employee data as JSON
   ↓
8. Form auto-populates with existing data
   ↓
9. User modifies fields
   ↓
10. Click "Update" button
   ↓
11. Employee Service.updateEmployee(id, employee)
   ↓
12. HTTP PUT to /api/employees/{id}
   ↓
13. Backend updates database
   ↓
14. Returns updated employee
   ↓
15. Navigate to Employee List
```

### Deleting an Employee

```
1. User clicks "Delete" button
   ↓
2. Browser shows confirmation dialog
   ↓
3. User confirms
   ↓
4. Employee Service.deleteEmployee(id)
   ↓
5. HTTP DELETE to /api/employees/{id}
   ↓
6. Backend deletes from database
   ↓
7. Returns 204 No Content
   ↓
8. List reloads automatically
```

## Component Relationships

```
app.component
    │
    ├─── router-outlet
    │
    ├─── employee-list.component
    │    │
    │    ├─── Uses: employee.service
    │    ├─── Displays: employee.model[]
    │    └─── Links to: employee-form.component
    │
    └─── employee-form.component
         │
         ├─── Uses: employee.service
         ├─── Manages: employee.model
         └─── Returns to: employee-list.component
```

## File Dependencies

```
employee-list.component.ts
    ├─── imports CommonModule
    ├─── imports RouterModule
    ├─── imports EmployeeService
    └─── imports Employee (model)

employee-form.component.ts
    ├─── imports CommonModule
    ├─── imports ReactiveFormsModule
    ├─── imports EmployeeService
    ├─── imports Employee (model)
    ├─── imports Router
    └─── imports ActivatedRoute

employee.service.ts
    ├─── imports HttpClient
    ├─── imports Observable (RxJS)
    └─── imports Employee (model)

app.config.ts
    ├─── imports provideRouter
    ├─── imports provideHttpClient
    └─── imports routes

app.routes.ts
    ├─── imports EmployeeListComponent
    └─── imports EmployeeFormComponent
```

## State Management Flow

```
┌─────────────────────────────────────────┐
│  Component State (Local)                │
│                                          │
│  Employee List:                          │
│  - employees: Employee[]                 │
│  - loading: boolean                      │
│  - error: string | null                  │
│                                          │
│  Employee Form:                          │
│  - employeeForm: FormGroup               │
│  - isEditMode: boolean                   │
│  - employeeId: number | null             │
│  - loading: boolean                      │
│  - error: string | null                  │
└─────────────────────────────────────────┘
            │
            │ Service Layer
            ▼
┌─────────────────────────────────────────┐
│  Employee Service                        │
│  - Stateless                             │
│  - Returns Observables                   │
│  - No data caching                       │
└─────────────────────────────────────────┘
            │
            │ HTTP Layer
            ▼
┌─────────────────────────────────────────┐
│  Backend API                             │
│  - Single source of truth                │
│  - Database persistence                  │
└─────────────────────────────────────────┘
```

## Technology Integration

```
Angular 17
    │
    ├─── Standalone Components (no NgModule)
    ├─── Reactive Forms (FormBuilder, Validators)
    ├─── HttpClient (with Fetch API)
    ├─── Router (for navigation)
    └─── RxJS (Observables, operators)

Spring Boot
    │
    ├─── Spring Web (REST Controllers)
    ├─── Spring Data JPA (Repository)
    ├─── H2 Database (In-memory)
    └─── Lombok (Reduce boilerplate)
```

## Security Considerations

```
CORS Configuration
    ├─── Origin: http://localhost:4200 (Development only)
    ├─── Methods: GET, POST, PUT, DELETE, OPTIONS
    ├─── Headers: All allowed
    └─── Credentials: Enabled

Note: For production:
    ├─── Restrict origins to actual domain
    ├─── Add authentication (JWT, OAuth2)
    ├─── Implement authorization
    ├─── Use HTTPS
    └─── Validate all inputs server-side
```

