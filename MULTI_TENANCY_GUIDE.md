# Multi-Tenancy Implementation Guide

## Overview
This implementation adds multi-tenancy support to the Employee Management System with the following architecture:

```
ROOT (System Admin)
└── Organization 1
    ├── SUPER_ADMIN (Organization Admin)
    ├── Departments
    ├── ADMIN (Department Admin)
    └── USERS (Employees)
└── Organization 2
    ├── SUPER_ADMIN
    ├── Departments
    ├── ADMIN
    └── USERS
```

## Key Features

1. **ROOT User**: Single system administrator who can create organizations
2. **Organizations**: Isolated tenants with their own data
3. **Organization Logo**: Each organization can upload their own logo
4. **Data Isolation**: Users can only see data from their organization
5. **SUPER_ADMIN per Organization**: Each organization has its own super admin
6. **Existing Roles Preserved**: ADMIN and USER roles work the same within organizations

## Database Changes

### New Tables
- `organizations`: Stores organization details and logos

### Modified Tables
- `users`: Added `organization_id` column
- `employees`: Added `organization_id` column
- `departments`: Added `organization_id` column

## User Roles Hierarchy

### ROOT
- **Count**: Only 1 in the entire system
- **Scope**: All organizations
- **Permissions**:
  - Create new organizations
  - View all organizations
  - Access all data across organizations
  - Manage system-wide settings
- **Organization ID**: NULL

### SUPER_ADMIN
- **Count**: 1 per organization
- **Scope**: Single organization
- **Permissions**:
  - View all employees in their organization
  - View all documents in their organization
  - Manage all departments in their organization
  - Upload organization logo
  - Cannot access other organizations
- **Organization ID**: Set to their organization

### ADMIN
- **Count**: Multiple per organization (1 per department)
- **Scope**: Single department within organization
- **Permissions**:
  - View employees in their department (USER role only)
  - View documents of employees in their department
  - Cannot view other ADMINs or SUPER_ADMIN data
  - Cannot access other organizations
- **Organization ID**: Set to their organization

### USER
- **Count**: Multiple per organization
- **Scope**: Self only
- **Permissions**:
  - View their own profile
  - View their own documents
  - Upload their own documents
- **Organization ID**: Set to their organization

## Installation Steps

### 1. Run Database Migration

```sql
-- Execute the migration script
mysql -u your_username -p your_database < src/main/resources/db/migration/multi_tenancy_migration.sql

-- Create ROOT user
mysql -u your_username -p your_database < src/main/resources/db/migration/create_root_user.sql
```

### 2. Generate Correct Password Hash for ROOT

The ROOT user password needs to be properly hashed. Run this Java code to generate the hash:

```java
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "Root@123456"; // Your desired ROOT password
        String hash = encoder.encode(password);
        System.out.println("Password hash: " + hash);
    }
}
```

Then update the `create_root_user.sql` script with the generated hash.

### 3. Migrate Existing Data (If Applicable)

If you have existing users, employees, and departments, create a default organization and assign them:

```sql
-- Create default organization
INSERT INTO organizations (name, description, is_active, created_at, updated_at)
VALUES ('Default Organization', 'Legacy data organization', TRUE, NOW(), NOW());

-- Get the organization ID
SET @org_id = LAST_INSERT_ID();

-- Update existing users
UPDATE users SET organization_id = @org_id WHERE organization_id IS NULL AND username != 'root';

-- Update existing employees
UPDATE employees SET organization_id = @org_id WHERE organization_id IS NULL;

-- Update existing departments
UPDATE departments SET organization_id = @org_id WHERE organization_id IS NULL;
```

### 4. Restart Backend Application

```bash
# Stop the application
# Then restart:
mvn spring-boot:run
```

## Usage Workflow

### 1. ROOT Creates Organization

**Login as ROOT**:
- Username: `root`
- Password: `Root@123456` (or what you set)

**Create Organization**:
```bash
POST /api/organizations
{
  "organizationName": "Acme Corporation",
  "superAdminUsername": "acme_admin",
  "superAdminEmail": "admin@acme.com",
  "password": "SecurePass123!",
  "superAdminFullName": "John Admin",
  "organizationDescription": "Software development company",
  "contactEmail": "contact@acme.com",
  "contactPhone": "+1-234-567-8900",
  "address": "123 Business St, City, Country"
}
```

This creates:
- ✅ New organization
- ✅ SUPER_ADMIN user
- ✅ Employee profile for SUPER_ADMIN
- ✅ Default "General" department

### 2. SUPER_ADMIN Uploads Organization Logo

**Login as SUPER_ADMIN** (credentials from step 1)

**Upload Logo**:
```bash
POST /api/organizations/{organizationId}/logo
Content-Type: multipart/form-data
file: [image file]
```

### 3. SUPER_ADMIN Creates Departments

```bash
POST /api/departments
{
  "name": "Engineering",
  "code": "ENG",
  "description": "Software Engineering Department"
}
```

### 4. SUPER_ADMIN Creates ADMIN Users

Create department admin:
```bash
POST /api/employees/create-with-account
{
  "fullName": "Jane Manager",
  "workEmail": "jane@acme.com",
  "jobTitle": "Engineering Manager",
  "role": "ADMIN",
  "departmentId": [department_id]
}
```

### 5. ADMIN Creates USER Employees

Department admins can now create regular employees in their department.

## API Endpoints

### Organization Management (ROOT Only)

```
POST   /api/organizations              - Create organization
GET    /api/organizations              - List all organizations
GET    /api/organizations/{id}         - Get organization details
PUT    /api/organizations/{id}         - Update organization
POST   /api/organizations/{id}/logo    - Upload logo
GET    /api/organizations/{id}/logo    - Get logo
```

### Access Control Summary

| Endpoint | ROOT | SUPER_ADMIN | ADMIN | USER |
|----------|------|-------------|-------|------|
| Create Organization | ✅ | ❌ | ❌ | ❌ |
| View All Organizations | ✅ | ❌ | ❌ | ❌ |
| View Own Organization | ✅ | ✅ | ❌ | ❌ |
| Upload Org Logo | ✅ | ✅ | ❌ | ❌ |
| View All Employees (Org) | ✅ | ✅ | ❌ | ❌ |
| View Dept Employees | ✅ | ✅ | ✅ | ❌ |
| View Own Profile | ✅ | ✅ | ✅ | ✅ |

## Frontend Integration

### 1. Login Screen

The login screen should remain the same. Users login with username/password. The system automatically determines their organization from their user record.

### 2. Header/Navigation

For SUPER_ADMIN and ADMIN users, display organization logo and name in header:

```typescript
organizationService.getOrganization(currentUser.organizationId).subscribe(org => {
  this.organizationName = org.name;
  this.organizationLogoUrl = org.logoUrl;
});
```

### 3. Organization Management (ROOT Only)

Create a new component for ROOT user to manage organizations:

```typescript
// organization-management.component.ts
createOrganization(data: CreateOrganizationRequest) {
  this.organizationService.createOrganization(data).subscribe(
    response => {
      this.messageService.success('Organization created successfully!');
      this.loadOrganizations();
    },
    error => {
      this.messageService.error(error.error.message);
    }
  );
}
```

### 4. Logo Upload (SUPER_ADMIN)

```typescript
uploadLogo(file: File, organizationId: number) {
  const formData = new FormData();
  formData.append('file', file);
  
  this.organizationService.uploadLogo(organizationId, formData).subscribe(
    response => {
      this.messageService.success('Logo uploaded successfully!');
      this.loadOrganization();
    },
    error => {
      this.messageService.error(error.error.message);
    }
  );
}
```

## Security Considerations

1. **ROOT Password**: Change the default ROOT password immediately after installation
2. **One ROOT Only**: The system should never have more than one ROOT user
3. **Organization Isolation**: All queries must filter by organization_id
4. **Logo File Size**: Limited to 5MB
5. **Logo File Type**: Only image files allowed

## Data Isolation

The system enforces data isolation at multiple levels:

1. **Database Level**: All queries filter by `organization_id`
2. **Service Level**: `SecurityUtils` checks organization boundaries
3. **Repository Level**: Custom queries include organization filters

Example from DocumentService:
```java
if (!currentUser.getOrganizationId().equals(employee.getOrganizationId())) {
    throw new RuntimeException("Access denied: Different organization");
}
```

## Testing

### Test Scenarios

1. ✅ ROOT can create multiple organizations
2. ✅ SUPER_ADMIN in Org A cannot see Org B data
3. ✅ ADMIN in Dept 1 cannot see Dept 2 data (same org)
4. ✅ USER can only see own data
5. ✅ Organization logo displays correctly
6. ✅ Login works for users in different organizations

### Test Data Setup

```sql
-- Create test organization
INSERT INTO organizations (name, description, is_active, created_at, updated_at)
VALUES ('Test Corp', 'Test organization', TRUE, NOW(), NOW());

-- Create test super admin
-- (Use the API endpoint for this)
```

## Troubleshooting

### Issue: ROOT user cannot login
**Solution**: Verify ROOT user exists and has correct password hash:
```sql
SELECT username, email, enabled FROM users WHERE username = 'root';
SELECT role FROM user_roles WHERE user_id = (SELECT id FROM users WHERE username = 'root');
```

### Issue: SUPER_ADMIN sees no data
**Solution**: Verify organization_id is set correctly:
```sql
SELECT id, username, organization_id FROM users WHERE roles LIKE '%SUPER_ADMIN%';
```

### Issue: Users see data from other organizations
**Solution**: Check all queries have organization_id filters and verify SecurityUtils.belongsToSameOrganization() is being called.

## Migration from Single-Tenant

If you're migrating from a single-tenant system:

1. Run the migration scripts
2. Create a default organization
3. Assign all existing users/employees/departments to default organization
4. Create SUPER_ADMIN for default organization
5. Test thoroughly before creating additional organizations

## Future Enhancements

1. Organization-specific themes/colors
2. Organization-specific email domains
3. Organization settings and configurations
4. Organization usage statistics
5. Organization billing/subscription management
6. Cross-organization reporting (ROOT only)

## Support

For issues or questions:
1. Check the implementation guide
2. Verify database migrations ran successfully
3. Check application logs for security/access errors
4. Verify user roles and organization assignments

## Important Notes

⚠️ **CRITICAL**: 
- Only ONE ROOT user should exist
- ROOT password must be changed after first login
- Always filter queries by organization_id (except for ROOT)
- Test thoroughly before deploying to production
- Backup database before running migrations

✅ **Benefits**:
- Complete data isolation between organizations
- Scalable multi-tenant architecture
- Maintains existing user role hierarchy
- Each organization has independent administration
- Supports unlimited organizations

