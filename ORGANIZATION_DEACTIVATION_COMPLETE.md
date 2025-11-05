# ✅ Organization Deactivation Feature - COMPLETE

## 🎯 Feature Implemented

**Requirement**: ROOT user can deactivate/activate organizations, which will block/restore access for ALL users in that organization, including SUPER_ADMIN and ADMIN.

**Solution**: Complete organization activation/deactivation system with backend enforcement and frontend UI.

---

## 🔒 How It Works

### **Deactivation Flow:**
```
1. ROOT clicks "⏸️ Deactivate" button on organization
   ↓
2. Confirmation dialog appears with warning
   ↓
3. POST /api/organizations/{id}/deactivate
   ↓
4. Backend:
   - Sets organization.isActive = false
   - Disables ALL users in that organization (user.enabled = false)
   - Includes SUPER_ADMIN, ADMIN, and USER roles
   ↓
5. Frontend shows success message
   ↓
6. Organization status changes to "⏸️ Inactive" in list
   ↓
7. All users of that organization CANNOT login
   ↓
8. Login attempts return 403 Forbidden with message:
   "⚠️ Access Denied: Your organization has been deactivated"
```

### **Activation Flow:**
```
1. ROOT clicks "✅ Activate" button on inactive organization
   ↓
2. Confirmation dialog appears
   ↓
3. POST /api/organizations/{id}/activate
   ↓
4. Backend:
   - Sets organization.isActive = true
   - Re-enables ALL users in that organization (user.enabled = true)
   ↓
5. Frontend shows success message
   ↓
6. Organization status changes to "✅ Active"
   ↓
7. All users can now login normally
```

---

## 🔧 Backend Changes

### **1. OrganizationController.java** ✅

**Added Endpoints:**

```java
@PostMapping("/{id}/deactivate")
@PreAuthorize("hasRole('ROOT')")
public ResponseEntity<?> deactivateOrganization(@PathVariable Long id)

@PostMapping("/{id}/activate")
@PreAuthorize("hasRole('ROOT')")
public ResponseEntity<?> activateOrganization(@PathVariable Long id)
```

**Authorization**: Only ROOT role can access

**Response**:
```json
{
  "success": true,
  "message": "Organization deactivated successfully. All users are now blocked.",
  "organization": { ... }
}
```

### **2. OrganizationService.java** ✅

**Added Methods:**

```java
public OrganizationDTO deactivateOrganization(Long id)
public OrganizationDTO activateOrganization(Long id)
```

**Logic:**
- Validates ROOT permission
- Updates organization.isActive status
- Finds ALL users in organization via organizationId
- Disables/enables each user account
- Logs count of affected users
- Returns updated organization DTO

### **3. UserRepository.java** ✅

**Added Method:**

```java
List<User> findByOrganizationId(Long organizationId);
```

**Purpose**: Find all users belonging to an organization for bulk enable/disable

### **4. AuthController.java** ✅

**Added Organization Check on Login:**

```java
// Check if user's organization is active
if (user.getOrganizationId() != null && !roles.contains("ROOT")) {
    Organization organization = organizationRepository.findById(user.getOrganizationId())
            .orElseThrow(() -> new RuntimeException("Organization not found"));
    
    if (!organization.getIsActive()) {
        // Block access
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new MessageResponse("⚠️ Access Denied: Your organization has been deactivated..."));
    }
}
```

**Effect**: Users from deactivated organizations cannot login (403 Forbidden)

---

## 🎨 Frontend Changes

### **1. root-dashboard.component.ts** ✅

**Added Methods:**

```typescript
deactivateOrganization(orgId: number, orgName: string): void
activateOrganization(orgId: number, orgName: string): void
```

**Features:**
- Confirmation dialogs with warnings
- HTTP POST requests to activate/deactivate endpoints
- Success/error alerts
- Auto-reload dashboard to show updated status

### **2. root-dashboard.component.html** ✅

**Updated Actions Column:**

```html
<button *ngIf="org.isActive" 
        (click)="deactivateOrganization(...)"
        class="btn btn-sm btn-warning">
  ⏸️ Deactivate
</button>

<button *ngIf="!org.isActive" 
        (click)="activateOrganization(...)"
        class="btn btn-sm btn-success">
  ✅ Activate
</button>
```

**Logic**: Shows "Deactivate" for active orgs, "Activate" for inactive orgs

### **3. root-dashboard.component.css** ✅

**Added Styles:**

```css
.btn-warning { background: #f59e0b; } /* Orange for deactivate */
.btn-success { background: #10b981; } /* Green for activate */
.action-buttons { display: flex; gap: 8px; }
```

---

## 🧪 Testing

### **Test 1: Deactivate Organization**

1. **Login as ROOT:**
   ```
   Username: root
   Password: Root@123456
   ```

2. **Go to ROOT Dashboard:**
   - URL: `http://localhost:4200/root/dashboard`

3. **Find Active Organization:**
   - Look for organization with "✅ Active" badge

4. **Click "⏸️ Deactivate" button:**
   - Confirmation dialog appears
   - Click "OK"

5. **Expected Result:**
   ```
   ✅ Backend logs: "Organization DEACTIVATED: [name] (ID: [id]). X users disabled."
   ✅ Success alert: "Organization deactivated successfully. All users are now blocked."
   ✅ Status badge changes to "⏸️ Inactive"
   ✅ Button changes to "✅ Activate"
   ```

6. **Try to Login as Deactivated Org User:**
   ```bash
   # Try to login as super admin of deactivated org
   POST /api/auth/login
   {
     "username": "admin@deactivated-org.com",
     "password": "Password123"
   }
   
   # Expected: HTTP 403 Forbidden
   # Response: "⚠️ Access Denied: Your organization has been deactivated..."
   ```

### **Test 2: Activate Organization**

1. **As ROOT, click "✅ Activate" on inactive organization**

2. **Confirmation dialog → Click OK**

3. **Expected Result:**
   ```
   ✅ Backend logs: "Organization ACTIVATED: [name] (ID: [id]). X users enabled."
   ✅ Success alert: "Organization activated successfully. All users can now access."
   ✅ Status badge changes to "✅ Active"
   ✅ Button changes to "⏸️ Deactivate"
   ```

4. **Deactivated Users Can Now Login:**
   ```bash
   POST /api/auth/login
   {
     "username": "admin@reactivated-org.com",
     "password": "Password123"
   }
   
   # Expected: HTTP 200 OK
   # Response: JWT token and user data
   ```

### **Test 3: Non-ROOT Cannot Deactivate**

```bash
# Try as SUPER_ADMIN
curl -X POST http://localhost:8080/api/organizations/1/deactivate \
  -H "Authorization: Bearer {SUPER_ADMIN_TOKEN}"

# Expected: HTTP 403 Forbidden
# Only ROOT can deactivate organizations
```

---

## 🔐 Security Implementation

### **Access Control:**

| Role | Deactivate Org | Activate Org | View Status |
|------|---------------|--------------|-------------|
| ROOT | ✅ Yes | ✅ Yes | ✅ Yes |
| SUPER_ADMIN | ❌ No (403) | ❌ No (403) | ❌ No (403) |
| ADMIN | ❌ No (403) | ❌ No (403) | ❌ No (403) |
| USER | ❌ No (403) | ❌ No (403) | ❌ No (403) |

### **Enforcement Layers:**

**Layer 1: API Authorization**
```java
@PreAuthorize("hasRole('ROOT')")
```
Blocks non-ROOT users at controller level

**Layer 2: Service Validation**
```java
if (!currentUser.getRoles().contains("ROOT")) {
    throw new RuntimeException("Access denied...");
}
```
Double-check in service layer

**Layer 3: Login Block**
```java
if (!organization.getIsActive()) {
    return 403 Forbidden;
}
```
Prevents login for deactivated org users

**Layer 4: User Account Disable**
```java
user.setEnabled(false);
```
Database-level user account disabling

---

## 📊 Database Impact

### **When Organization is Deactivated:**

**Organization Table:**
```sql
UPDATE organizations 
SET is_active = false 
WHERE id = ?
```

**Users Table (ALL users in org):**
```sql
UPDATE users 
SET enabled = false 
WHERE organization_id = ?
```

### **Affected Users:**
- ✅ SUPER_ADMIN (CEO) - Disabled
- ✅ ADMIN (Department Managers) - Disabled  
- ✅ USER (Regular Employees) - Disabled
- ❌ ROOT - Not affected (no organizationId)

---

## 🚀 API Endpoints

### **Deactivate Organization**
```
POST /api/organizations/{id}/deactivate
Authorization: Bearer {ROOT_TOKEN}

Response 200:
{
  "success": true,
  "message": "Organization deactivated successfully. All users are now blocked.",
  "organization": {
    "id": 1,
    "name": "Acme Corp",
    "isActive": false,
    ...
  }
}

Response 403:
{
  "success": false,
  "message": "Access denied. Only ROOT can deactivate organizations."
}
```

### **Activate Organization**
```
POST /api/organizations/{id}/activate
Authorization: Bearer {ROOT_TOKEN}

Response 200:
{
  "success": true,
  "message": "Organization activated successfully. All users can now access the system.",
  "organization": {
    "id": 1,
    "name": "Acme Corp",
    "isActive": true,
    ...
  }
}
```

### **Login (Organization Status Check)**
```
POST /api/auth/login
{
  "username": "user@deactivated-org.com",
  "password": "password"
}

Response 403 (if org is deactivated):
{
  "message": "⚠️ Access Denied: Your organization has been deactivated by the system administrator. Please contact support for assistance."
}
```

---

## ✅ Files Modified

### **Backend (5 files):**
1. ✅ `OrganizationController.java` - Added deactivate/activate endpoints
2. ✅ `OrganizationService.java` - Added deactivate/activate methods
3. ✅ `UserRepository.java` - Added findByOrganizationId method
4. ✅ `AuthController.java` - Added organization status check on login
5. ✅ `OrganizationDTO.java` - (already had isActive field)

### **Frontend (3 files):**
1. ✅ `root-dashboard.component.ts` - Added deactivate/activate methods
2. ✅ `root-dashboard.component.html` - Added activate/deactivate buttons
3. ✅ `root-dashboard.component.css` - Added button styles

---

## 🎉 Summary

### **What Was Implemented:**
✅ ROOT can deactivate organizations (block all users)
✅ ROOT can activate organizations (restore all users)
✅ Deactivation disables ALL users (SUPER_ADMIN, ADMIN, USER)
✅ Login blocked for deactivated organization users
✅ Backend enforces organization status
✅ Frontend UI with activate/deactivate buttons
✅ Confirmation dialogs with warnings
✅ Success/error feedback
✅ Status badges update in real-time
✅ Authorization restricted to ROOT only

### **Security:**
🔒 Only ROOT can deactivate/activate organizations
🔒 Backend validates organization status on login
🔒 Database-level user disabling
🔒 Multi-layer security enforcement
🔒 Organization status check in authentication

### **User Impact:**
- ⚠️ When deactivated: ALL users cannot login (403 Forbidden)
- ✅ When activated: ALL users can login normally
- 👑 ROOT user: Not affected (no organization)

---

**Status**: 🟢 **COMPLETE AND READY FOR TESTING**

**Date**: November 5, 2025  
**Feature**: Organization Deactivation/Activation  
**Impact**: Organization-wide access control  
**Security**: ROOT-only operation

