# ✅ Leave Approval Rules Implementation

## 🎯 Overview

The leave approval system has been updated to enforce a hierarchical approval workflow where **ADMIN users' leave requests must be approved by SUPER_ADMIN**, and ADMINs cannot approve or reject their own leaves.

## 📋 Leave Approval Rules

### Rule 1: Self-Approval Prevention
**ADMINs cannot approve or reject their own leave requests**

- ❌ ADMIN cannot approve their own leave
- ❌ ADMIN cannot reject their own leave
- ✅ Must contact SUPER_ADMIN for approval

**Error Message:**
```
"You cannot approve your own leave request. Please contact your supervisor."
"You cannot reject your own leave request. Please contact your supervisor."
```

### Rule 2: ADMIN Leave Approval Authority
**ADMIN leave requests can ONLY be approved/rejected by SUPER_ADMIN**

- ✅ SUPER_ADMIN can approve/reject ADMIN leaves
- ❌ ADMIN cannot approve other ADMINs' leaves
- ❌ Regular ADMIN has no authority over ADMIN-level leaves

**Error Message:**
```
"ADMIN leave requests can only be approved by SUPER_ADMIN."
"ADMIN leave requests can only be rejected by SUPER_ADMIN."
```

### Rule 3: ADMIN Approval Scope
**ADMIN can only approve/reject USER role employees in their department**

- ✅ ADMIN can approve USER leaves in their department
- ❌ ADMIN cannot approve leaves outside their department
- ❌ ADMIN cannot approve ADMIN or SUPER_ADMIN leaves
- ❌ ADMIN cannot approve leaves from other departments

**Error Messages:**
```
"You can only approve leaves for regular employees (USER role)."
"You can only approve leaves for employees in your department."
```

## 🔄 Leave Approval Workflow

### Scenario 1: USER Employee Leave Request

```
USER (John) → Requests Leave
    ↓
ADMIN (Department Manager) → Can Approve/Reject
    ↓
✅ Approved/Rejected
```

**Requirements:**
- John must be a USER role
- John must be in the ADMIN's department
- ADMIN is not approving their own leave

### Scenario 2: ADMIN Employee Leave Request

```
ADMIN (Department Manager) → Requests Leave
    ↓
SUPER_ADMIN (Organization Admin) → MUST Approve/Reject
    ↓
✅ Approved/Rejected
```

**Requirements:**
- Only SUPER_ADMIN has authority
- Regular ADMIN cannot approve
- Self-approval is prevented

### Scenario 3: SUPER_ADMIN Leave Request

```
SUPER_ADMIN → Requests Leave
    ↓
ROOT or another SUPER_ADMIN → Approves/Rejects
    ↓
✅ Approved/Rejected
```

**Note:** Implementation may vary based on organizational structure.

## 🎨 Frontend Updates Needed

### 1. Hide Approve/Reject Buttons for Own Leaves

```typescript
// leave-list.component.ts
canApproveLeave(leave: LeaveDTO): boolean {
  const currentUser = this.authService.getCurrentUser();
  
  // Cannot approve own leave
  if (leave.employeeId === currentUser.employeeId) {
    return false;
  }
  
  // Get employee role for the leave request
  const leaveUserRole = this.getEmployeeRole(leave.employeeId);
  
  // ADMIN can only approve USER leaves
  if (currentUser.roles.includes('ADMIN') && !currentUser.roles.includes('SUPER_ADMIN')) {
    return leaveUserRole === 'USER';
  }
  
  // SUPER_ADMIN can approve all leaves
  if (currentUser.roles.includes('SUPER_ADMIN')) {
    return true;
  }
  
  return false;
}
```

### 2. Show Appropriate Messages

```html
<!-- leave-list.component.html -->
<div *ngIf="leave.employeeId === currentUser.employeeId && leave.status === 'PENDING'">
  <p class="info-message">
    ⏳ Your leave request is pending approval from 
    <span *ngIf="currentUser.roles.includes('ADMIN')">SUPER_ADMIN</span>
    <span *ngIf="currentUser.roles.includes('USER')">your department manager</span>
  </p>
</div>

<div *ngIf="!canApproveLeave(leave) && leave.status === 'PENDING' && isAdmin">
  <p class="warning-message">
    ⚠️ You cannot approve this leave request
    <span *ngIf="leave.employeeRole === 'ADMIN'">
      (ADMIN leaves require SUPER_ADMIN approval)
    </span>
  </p>
</div>
```

### 3. Filter Leave Requests by Approval Authority

```typescript
// leave-list.component.ts
filterLeavesByApprovalAuthority(leaves: LeaveDTO[]): LeaveDTO[] {
  const currentUser = this.authService.getCurrentUser();
  
  return leaves.filter(leave => {
    // Don't show own leaves in approval queue
    if (leave.employeeId === currentUser.employeeId) {
      return false;
    }
    
    // SUPER_ADMIN sees all pending leaves
    if (currentUser.roles.includes('SUPER_ADMIN')) {
      return true;
    }
    
    // ADMIN only sees USER leaves in their department
    if (currentUser.roles.includes('ADMIN')) {
      const leaveUser = this.getUserByEmployeeId(leave.employeeId);
      return leaveUser?.roles?.includes('USER') && 
             leave.departmentId === currentUser.departmentId;
    }
    
    return false;
  });
}
```

### 4. Update Leave Status Display

```html
<!-- Show who can approve -->
<div class="approval-info" *ngIf="leave.status === 'PENDING'">
  <span class="label">Pending Approval From:</span>
  <span class="approver">
    <span *ngIf="leave.employeeRole === 'USER'">Department Manager</span>
    <span *ngIf="leave.employeeRole === 'ADMIN'">SUPER_ADMIN</span>
    <span *ngIf="leave.employeeRole === 'SUPER_ADMIN'">System Administrator</span>
  </span>
</div>
```

## 🧪 Testing Scenarios

### Test Case 1: ADMIN Self-Approval (Should Fail)
```bash
# Login as ADMIN user
POST /api/auth/login
{ "username": "admin1", "password": "password" }

# Apply leave
POST /api/leaves
{ "employeeId": 1, "leaveType": "ANNUAL", ... }

# Try to approve own leave (should fail)
POST /api/leaves/1/approve
Response: 403 - "You cannot approve your own leave request. Please contact your supervisor."
```

### Test Case 2: ADMIN Approving USER Leave (Should Succeed)
```bash
# Login as ADMIN user
POST /api/auth/login
{ "username": "admin1", "password": "password" }

# USER in same department applies leave
POST /api/leaves
{ "employeeId": 5, "leaveType": "SICK", ... }

# ADMIN approves (should succeed)
POST /api/leaves/2/approve
Response: 200 - Leave approved
```

### Test Case 3: ADMIN Approving Another ADMIN Leave (Should Fail)
```bash
# Login as ADMIN user
POST /api/auth/login
{ "username": "admin1", "password": "password" }

# Another ADMIN applies leave
POST /api/leaves
{ "employeeId": 2, "leaveType": "ANNUAL", ... }

# Try to approve (should fail)
POST /api/leaves/3/approve
Response: 403 - "ADMIN leave requests can only be approved by SUPER_ADMIN."
```

### Test Case 4: SUPER_ADMIN Approving ADMIN Leave (Should Succeed)
```bash
# Login as SUPER_ADMIN
POST /api/auth/login
{ "username": "superadmin", "password": "password" }

# ADMIN applies leave
POST /api/leaves
{ "employeeId": 1, "leaveType": "ANNUAL", ... }

# SUPER_ADMIN approves (should succeed)
POST /api/leaves/4/approve
Response: 200 - Leave approved
```

## 📊 Access Control Matrix

| Approver Role | Can Approve USER Leaves | Can Approve ADMIN Leaves | Can Approve SUPER_ADMIN Leaves | Can Approve Own Leave |
|---------------|------------------------|--------------------------|--------------------------------|----------------------|
| **USER** | ❌ No | ❌ No | ❌ No | ❌ No |
| **ADMIN** | ✅ Yes (own dept) | ❌ No | ❌ No | ❌ No |
| **SUPER_ADMIN** | ✅ Yes (all) | ✅ Yes (all) | ⚠️ Special case | ❌ No |
| **ROOT** | ✅ Yes (all orgs) | ✅ Yes (all orgs) | ✅ Yes (all orgs) | ❌ No |

## 🔒 Security Implementation

### Backend Validation (LeaveService.java)

The validation logic checks three conditions:

1. **Self-Approval Check:**
   ```java
   if (currentUser.getId().equals(leaveEmployee.getUserId())) {
       throw new RuntimeException("You cannot approve your own leave request.");
   }
   ```

2. **ADMIN Leave Authority Check:**
   ```java
   if (leaveUser != null && leaveUser.getRoles().contains("ADMIN")) {
       if (!securityUtils.isSuperAdmin()) {
           throw new RuntimeException("ADMIN leave requests can only be approved by SUPER_ADMIN.");
       }
   }
   ```

3. **Department & Role Check for ADMIN:**
   ```java
   if (securityUtils.isAdmin() && !securityUtils.isSuperAdmin()) {
       // Verify USER role
       if (leaveUser.getRoles().contains("ADMIN") || leaveUser.getRoles().contains("SUPER_ADMIN")) {
           throw new RuntimeException("You can only approve leaves for regular employees.");
       }
       
       // Verify same department
       if (!adminEmployee.getDepartment().getId().equals(leaveEmployee.getDepartment().getId())) {
           throw new RuntimeException("You can only approve leaves for employees in your department.");
       }
   }
   ```

## 📝 Implementation Summary

### Files Modified
- ✅ `LeaveService.java` - Updated `approveLeave()` and `rejectLeave()` methods

### Changes Made
1. ✅ Added self-approval prevention
2. ✅ Added ADMIN leave authority validation (SUPER_ADMIN only)
3. ✅ Added department boundary validation
4. ✅ Added role-based approval validation
5. ✅ Applied same rules to both approve and reject operations

### Error Handling
- ✅ Clear error messages for each violation
- ✅ Specific guidance on who can approve
- ✅ Department and role validation

## 🎯 User Experience Flow

### For ADMIN Users:
1. **Apply Leave** → Status: PENDING
2. **Wait for SUPER_ADMIN** approval
3. **Cannot self-approve** (prevented)
4. **Can approve** USER leaves in their department
5. **Receive notification** when SUPER_ADMIN approves/rejects

### For SUPER_ADMIN Users:
1. **See all pending leaves** in organization
2. **Can approve** all USER leaves
3. **Can approve** all ADMIN leaves
4. **Cannot approve** own leaves
5. **Full authority** over leave approvals

### For USER Employees:
1. **Apply Leave** → Status: PENDING
2. **Wait for department ADMIN** approval
3. **Cannot self-approve** (no approval rights)
4. **Receive notification** when approved/rejected

## ⚠️ Important Notes

1. **No Self-Approval**: No user can approve their own leave, regardless of role
2. **ADMIN Authority Limited**: ADMINs can only approve USER leaves in their department
3. **SUPER_ADMIN Required**: Only SUPER_ADMIN can approve ADMIN leaves
4. **Department Boundary**: ADMINs are restricted to their department
5. **Same Rules Apply**: Both approval and rejection follow the same rules

## 🚀 Deployment Checklist

- [x] Backend validation implemented
- [x] Error messages configured
- [x] Security checks in place
- [ ] Frontend UI updates (hide buttons)
- [ ] Frontend error handling
- [ ] Frontend role-based filtering
- [ ] User notification system
- [ ] Integration testing
- [ ] User acceptance testing

## 📞 Support

If you encounter issues:
1. Check backend logs for detailed error messages
2. Verify user roles are correctly assigned
3. Confirm department assignments
4. Test with different role combinations

---

**Implementation Date**: November 5, 2025
**Status**: ✅ Backend Complete - Frontend Pending
**Next Action**: Implement frontend UI updates

