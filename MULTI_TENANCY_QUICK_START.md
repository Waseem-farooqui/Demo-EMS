# 🚀 Multi-Tenancy Quick Start Guide

## ⚡ 5-Minute Setup

### 1️⃣ Run Database Migrations (1 minute)

```bash
# Open MySQL client
mysql -u root -p your_database_name

# Run migrations
source C:/Users/waseem.uddin/EmployeeManagementSystem/src/main/resources/db/migration/multi_tenancy_migration.sql

# Create ROOT user
source C:/Users/waseem.uddin/EmployeeManagementSystem/src/main/resources/db/migration/create_root_user.sql
```

### 2️⃣ Restart Backend (1 minute)

```bash
# Stop current backend (Ctrl+C)
# Restart
cd C:/Users/waseem.uddin/EmployeeManagementSystem
mvn spring-boot:run
```

### 3️⃣ Login as ROOT (1 minute)

**Credentials:**
- Username: `root`
- Password: `Root@123456`

### 4️⃣ Create Your First Organization (2 minutes)

```bash
POST http://localhost:8080/api/organizations
Content-Type: application/json
Authorization: Bearer {your_root_token}

{
  "organizationName": "My Company",
  "superAdminUsername": "admin",
  "superAdminEmail": "admin@mycompany.com",
  "password": "Admin@123",
  "superAdminFullName": "Company Admin",
  "organizationDescription": "My first organization",
  "contactEmail": "contact@mycompany.com"
}
```

✅ **Done!** You now have:
- One organization
- One SUPER_ADMIN user
- One default department
- Multi-tenant system ready

## 📋 What You Get

```
Your System Structure:
├── ROOT User (you)
└── My Company (Organization 1)
    ├── SUPER_ADMIN: admin@mycompany.com
    └── Department: General (default)
```

## 🎯 Next Steps

1. **Login as SUPER_ADMIN**:
   - Username: `admin`
   - Password: `Admin@123`

2. **Upload Organization Logo**:
   ```bash
   POST http://localhost:8080/api/organizations/1/logo
   Content-Type: multipart/form-data
   file: [your_logo.png]
   ```

3. **Create Departments** (as SUPER_ADMIN)
4. **Create ADMIN users** for departments
5. **Create regular employees**

## 🔑 Default Credentials

| User Type | Username | Password | Access Level |
|-----------|----------|----------|--------------|
| ROOT | `root` | `Root@123456` | All organizations |
| SUPER_ADMIN | (you create) | (you set) | One organization |

## ⚠️ Security Reminders

1. ✅ Change ROOT password after first login
2. ✅ Only ONE ROOT user should exist
3. ✅ Use strong passwords for SUPER_ADMIN
4. ✅ Backup database before migrations

## 🆘 Quick Troubleshooting

**Can't login as ROOT?**
```sql
-- Check ROOT user exists
SELECT username, email FROM users WHERE username = 'root';
```

**Need to reset ROOT password?**
```sql
-- Generate new hash using BCryptPasswordEncoder
-- Then update:
UPDATE users SET password = '{new_hash}' WHERE username = 'root';
```

## 📞 Need Help?

See detailed guides:
- `MULTI_TENANCY_GUIDE.md` - Complete guide
- `MULTI_TENANCY_IMPLEMENTATION_SUMMARY.md` - Full documentation

---

**Setup Time**: ~5 minutes
**Status**: ✅ Production Ready

