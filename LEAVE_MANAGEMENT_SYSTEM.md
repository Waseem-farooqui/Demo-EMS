# Leave Management System - Complete Implementation

## Overview

A comprehensive Leave Management system has been implemented and integrated with the Employee Management System. Employees can apply for leaves, and administrators can approve or reject them.

---

## Features Implemented

### Core Features
- ✅ Apply for leave
- ✅ View all leaves
- ✅ View leaves by employee
- ✅ View leaves by status (PENDING, APPROVED, REJECTED)
- ✅ Update pending leaves
- ✅ Approve leaves
- ✅ Reject leaves
- ✅ Delete leaves (pending only)
- ✅ Automatic calculation of leave days
- ✅ Leave linked to employees

### Leave Types Supported
- Annual Leave
- Sick Leave
- Unpaid Leave
- Casual Leave
- Maternity/Paternity Leave
- Bereavement Leave
- Other (custom)

### Leave Status
- **PENDING** - Newly applied, awaiting approval
- **APPROVED** - Approved by manager/admin
- **REJECTED** - Rejected with remarks

---

## Database Schema

### Leave Table
```sql
CREATE TABLE leaves (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    leave_type VARCHAR(255) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    number_of_days INT NOT NULL,
    reason VARCHAR(500),
    status VARCHAR(255) NOT NULL,
    applied_date DATE NOT NULL,
    approved_by VARCHAR(255),
    approval_date DATE,
    remarks VARCHAR(255),
    FOREIGN KEY (employee_id) REFERENCES employees(id)
);
```

### Relationship
- **One-to-Many**: One Employee can have many Leaves
- **Cascade**: When employee is deleted, all their leaves are deleted (orphanRemoval = true)

---

## API Endpoints

### Base URL: `/api/leaves`

### 1. Apply for Leave
**POST** `/api/leaves`

**Request Body:**
```json
{
  "employeeId": 1,
  "leaveType": "Annual Leave",
  "startDate": "2025-11-01",
  "endDate": "2025-11-05",
  "reason": "Family vacation"
}
```

**Response (201 Created):**
```json
{
  "id": 1,
  "employeeId": 1,
  "employeeName": "John Doe",
  "leaveType": "Annual Leave",
  "startDate": "2025-11-01",
  "endDate": "2025-11-05",
  "numberOfDays": 5,
  "reason": "Family vacation",
  "status": "PENDING",
  "appliedDate": "2025-10-29",
  "approvedBy": null,
  "approvalDate": null,
  "remarks": null
}
```

### 2. Get All Leaves
**GET** `/api/leaves`

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "employeeId": 1,
    "employeeName": "John Doe",
    "leaveType": "Annual Leave",
    "startDate": "2025-11-01",
    "endDate": "2025-11-05",
    "numberOfDays": 5,
    "reason": "Family vacation",
    "status": "PENDING",
    "appliedDate": "2025-10-29"
  }
]
```

### 3. Get Leave by ID
**GET** `/api/leaves/{id}`

**Example:** `/api/leaves/1`

### 4. Get Leaves by Employee
**GET** `/api/leaves/employee/{employeeId}`

**Example:** `/api/leaves/employee/1`

Returns all leaves for a specific employee.

### 5. Get Leaves by Status
**GET** `/api/leaves/status/{status}`

**Examples:**
- `/api/leaves/status/PENDING`
- `/api/leaves/status/APPROVED`
- `/api/leaves/status/REJECTED`

### 6. Update Leave
**PUT** `/api/leaves/{id}`

**Request Body:**
```json
{
  "employeeId": 1,
  "leaveType": "Sick Leave",
  "startDate": "2025-11-01",
  "endDate": "2025-11-03",
  "reason": "Medical emergency"
}
```

**Note:** Only PENDING leaves can be updated.

### 7. Approve Leave
**PUT** `/api/leaves/{id}/approve`

**Request Body:**
```json
{
  "approvedBy": "Manager Name",
  "remarks": "Approved for vacation"
}
```

### 8. Reject Leave
**PUT** `/api/leaves/{id}/reject`

**Request Body:**
```json
{
  "rejectedBy": "Manager Name",
  "remarks": "Insufficient leave balance"
}
```

### 9. Delete Leave
**DELETE** `/api/leaves/{id}`

**Response:** 204 No Content

**Note:** Cannot delete approved leaves.

---

## Business Rules

### 1. Leave Application
- Employee must exist
- Start date must be before or equal to end date
- Number of days calculated automatically
- Status automatically set to "PENDING"
- Applied date set to current date

### 2. Leave Approval
- Only PENDING leaves can be approved
- Approval date set to current date
- Approver name must be provided

### 3. Leave Rejection
- Only PENDING leaves can be rejected
- Rejection date recorded
- Rejection reason required

### 4. Leave Update
- Only PENDING leaves can be updated
- Cannot update approved/rejected leaves
- Days recalculated automatically

### 5. Leave Deletion
- Cannot delete APPROVED leaves
- Can delete PENDING or REJECTED leaves
- Soft delete option can be implemented

---

## Testing with Postman

### Step 1: Create an Employee
```
POST http://localhost:8080/api/employees
Content-Type: application/json
Authorization: Bearer <your-jwt-token>

{
  "fullName": "John Doe",
  "personType": "Full-time",
  "workEmail": "john.doe@company.com",
  "jobTitle": "Software Engineer",
  "dateOfJoining": "2024-01-15",
  "holidayAllowance": 20
}
```

### Step 2: Apply for Leave
```
POST http://localhost:8080/api/leaves
Content-Type: application/json
Authorization: Bearer <your-jwt-token>

{
  "employeeId": 1,
  "leaveType": "Annual Leave",
  "startDate": "2025-11-01",
  "endDate": "2025-11-05",
  "reason": "Family vacation"
}
```

### Step 3: View All Leaves
```
GET http://localhost:8080/api/leaves
Authorization: Bearer <your-jwt-token>
```

### Step 4: Approve Leave
```
PUT http://localhost:8080/api/leaves/1/approve
Content-Type: application/json
Authorization: Bearer <your-jwt-token>

{
  "approvedBy": "Manager Name",
  "remarks": "Approved"
}
```

---

## Example Usage Scenarios

### Scenario 1: Employee Applies for Leave

1. Employee logs in
2. Navigates to leave application
3. Selects leave type: "Annual Leave"
4. Selects dates: Nov 1-5, 2025
5. Enters reason: "Family vacation"
6. System calculates: 5 days
7. Leave submitted with status: PENDING

### Scenario 2: Manager Reviews Leave

1. Manager logs in
2. Views pending leaves
3. Reviews John's leave request
4. Checks employee's leave balance
5. Approves with remarks
6. Status changed to: APPROVED
7. Employee notified

### Scenario 3: Employee Updates Leave

1. Employee realizes dates are wrong
2. Views their pending leave
3. Updates dates: Nov 1-3 (3 days)
4. System recalculates days
5. Leave updated, still PENDING
6. Manager can review updated request

---

## Leave Balance Tracking

### Current Implementation
- Each employee has `holidayAllowance` field
- Stores total annual leave days

### Future Enhancement (Recommended)
Add leave balance calculation:

```java
public int getAvailableLeaves(Long employeeId) {
    Employee employee = employeeRepository.findById(employeeId)
        .orElseThrow();
    
    int totalAllowed = employee.getHolidayAllowance();
    int usedLeaves = leaveRepository
        .findByEmployeeIdAndStatus(employeeId, "APPROVED")
        .stream()
        .mapToInt(Leave::getNumberOfDays)
        .sum();
    
    return totalAllowed - usedLeaves;
}
```

---

## Files Created

### Backend Files

1. **Entity**
   - `Leave.java` - Leave entity with all fields and relationships

2. **DTO**
   - `LeaveDTO.java` - Data transfer object for API

3. **Repository**
   - `LeaveRepository.java` - Database operations

4. **Service**
   - `LeaveService.java` - Business logic and leave management

5. **Controller**
   - `LeaveController.java` - REST API endpoints

6. **Updated**
   - `Employee.java` - Added one-to-many relationship with leaves

---

## Database Queries

### View All Leaves in H2 Console
```sql
SELECT * FROM leaves;
```

### View Leaves with Employee Info
```sql
SELECT 
    l.id,
    e.full_name,
    l.leave_type,
    l.start_date,
    l.end_date,
    l.number_of_days,
    l.status,
    l.applied_date
FROM leaves l
JOIN employees e ON l.employee_id = e.id
ORDER BY l.applied_date DESC;
```

### View Pending Leaves
```sql
SELECT * FROM leaves WHERE status = 'PENDING';
```

### View Approved Leaves for an Employee
```sql
SELECT * FROM leaves 
WHERE employee_id = 1 AND status = 'APPROVED'
ORDER BY start_date DESC;
```

### Calculate Total Leave Days by Employee
```sql
SELECT 
    e.full_name,
    COUNT(l.id) as total_requests,
    SUM(CASE WHEN l.status = 'APPROVED' THEN l.number_of_days ELSE 0 END) as approved_days,
    SUM(CASE WHEN l.status = 'PENDING' THEN l.number_of_days ELSE 0 END) as pending_days
FROM employees e
LEFT JOIN leaves l ON e.id = l.employee_id
GROUP BY e.id, e.full_name;
```

---

## Error Handling

### Common Errors

1. **Employee Not Found**
```json
{
  "error": "Employee not found with id: 999"
}
```

2. **Leave Not Found**
```json
{
  "error": "Leave not found with id: 999"
}
```

3. **Cannot Update Approved Leave**
```json
{
  "error": "Cannot update leave that is not pending"
}
```

4. **Cannot Delete Approved Leave**
```json
{
  "error": "Cannot delete approved leave"
}
```

5. **Already Processed**
```json
{
  "error": "Leave is already approved"
}
```

---

## Testing Checklist

- [ ] Apply leave for employee
- [ ] View all leaves
- [ ] View leaves by employee
- [ ] View pending leaves
- [ ] View approved leaves
- [ ] Update pending leave
- [ ] Approve pending leave
- [ ] Reject pending leave
- [ ] Try to update approved leave (should fail)
- [ ] Try to delete approved leave (should fail)
- [ ] Delete pending leave (should succeed)
- [ ] Verify leave days calculation
- [ ] Verify employee-leave relationship in database

---

## Next Steps (Optional Enhancements)

### 1. Email Notifications
- Send email when leave is applied
- Notify manager for approval
- Notify employee on approval/rejection

### 2. Leave Balance API
```java
@GetMapping("/employee/{employeeId}/balance")
public ResponseEntity<LeaveBalanceDTO> getLeaveBalance(@PathVariable Long employeeId);
```

### 3. Leave Types Configuration
- Store leave types in database
- Configure leave types per company policy

### 4. Leave Calendar
- View leaves in calendar format
- Check team availability

### 5. Conflict Detection
- Prevent overlapping leaves
- Check team coverage

### 6. Reports
- Monthly leave reports
- Department-wise leave statistics
- Leave trends analysis

### 7. Approval Workflow
- Multi-level approval
- Department head → HR → Final approval

### 8. Leave Carry Forward
- Unused leaves to next year
- Expiry dates for leaves

---

## Summary

✅ **Complete Leave Management System Implemented**

**Features:**
- Apply, update, approve, reject, delete leaves
- Employee-leave relationship established
- Status tracking (PENDING, APPROVED, REJECTED)
- Automatic day calculation
- Multiple query endpoints

**Ready to Use:**
- Restart Spring Boot application
- Tables created automatically
- API endpoints available
- Fully integrated with Employee Management

**Access:**
- H2 Console: http://localhost:8080/h2-console
- API Base: http://localhost:8080/api/leaves
- Requires JWT authentication

---

## Quick Start

1. **Restart Backend** - Apply new entity and tables
2. **Create Employee** - Use existing employee API
3. **Apply Leave** - POST to `/api/leaves`
4. **View Leaves** - GET from `/api/leaves`
5. **Manage Leaves** - Approve/Reject/Update

**Your Leave Management System is Ready! 🎉**

