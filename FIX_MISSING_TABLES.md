# Fix Missing Database Tables

## Problem
The `documents` table (and possibly others) don't exist, causing errors like:
```
Table 'employee_management_system.documents' doesn't exist
```

## Solution

### Option 1: Create Tables Manually (Recommended)

I've created `create-tables.sql` that creates all tables. Run:

```bash
# Make script executable
chmod +x fix-missing-tables.sh

# Run the fix
sudo ./fix-missing-tables.sh
```

Or manually:
```bash
sudo docker-compose exec mysql mysql -u root -p"wuf27@1991" < create-tables.sql
```

### Option 2: Force JPA to Create Tables

Ensure `JPA_DDL_AUTO=update` is set in your `.env`:

```bash
# Check current setting
grep JPA_DDL_AUTO .env

# If not set, add it
echo "JPA_DDL_AUTO=update" >> .env
```

Then restart backend:
```bash
sudo docker-compose stop backend
sudo docker-compose rm -f backend
sudo docker-compose up -d backend
```

### Option 3: Use create-drop (WARNING: Deletes Data)

**⚠️ This will delete all existing data!**

```bash
# Temporarily set to create-drop
sudo docker-compose stop backend
sudo docker-compose rm -f backend

# Update .env
sed -i 's/^JPA_DDL_AUTO=.*/JPA_DDL_AUTO=create-drop/' .env

# Start backend (will create tables, then delete them on shutdown)
sudo docker-compose up -d backend

# Wait for tables to be created (check logs)
sudo docker-compose logs -f backend

# Once you see "Started EmployeeManagementSystemApplication", stop it
sudo docker-compose stop backend

# Change back to update
sed -i 's/^JPA_DDL_AUTO=.*/JPA_DDL_AUTO=update/' .env

# Start again
sudo docker-compose up -d backend
```

## Verify Tables Were Created

```bash
sudo docker-compose exec mysql mysql -u root -p"wuf27@1991" -e "
USE employee_management_system;
SHOW TABLES;
"
```

You should see:
- organizations
- users
- user_roles
- employees
- departments
- documents
- leaves
- leave_balances
- attendance
- rotas
- rota_schedules
- rota_change_logs
- notifications
- alert_configurations
- verification_tokens

## After Fixing

Once tables are created, restart the backend:
```bash
sudo docker-compose restart backend
```

Check logs to ensure no more table errors:
```bash
sudo docker-compose logs -f backend
```

