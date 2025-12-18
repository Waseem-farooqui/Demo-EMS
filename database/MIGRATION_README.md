# Database Migration: Remove Blood Group and Emergency Contact Columns

This migration removes the following deprecated columns from the `employees` table:
- `blood_group`
- `emergency_contact_name`
- `emergency_contact_phone`
- `emergency_contact_relationship`

## Files Included

1. **`remove-blood-group-emergency-contact.sql`** - SQL script for direct execution
2. **`remove-blood-group-emergency-contact.sh`** - Bash script with safety checks and backup

## Prerequisites

- MySQL/MariaDB database access
- Database backup (recommended)
- Appropriate database credentials

## Option 1: Using the SQL Script Directly

### Step 1: Backup Your Database

```bash
# Create a backup before running the migration
mysqldump -u [username] -p [database_name] > backup_before_migration.sql
```

### Step 2: Run the SQL Script

```bash
# Method 1: Command line
mysql -u [username] -p [database_name] < database/remove-blood-group-emergency-contact.sql

# Method 2: MySQL interactive
mysql -u [username] -p
USE [database_name];
SOURCE database/remove-blood-group-emergency-contact.sql;
```

### Step 3: Verify

The script will automatically verify that columns have been removed. You can also manually check:

```sql
SHOW COLUMNS FROM employees LIKE '%blood%';
SHOW COLUMNS FROM employees LIKE '%emergency%';
```

## Option 2: Using the Bash Script (Linux/Unix/Mac)

### Step 1: Make Script Executable

```bash
chmod +x remove-blood-group-emergency-contact.sh
```

### Step 2: Configure Environment Variables (Optional)

```bash
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=employeedb
export DB_USER=empuser
```

### Step 3: Run the Script

```bash
./remove-blood-group-emergency-contact.sh
```

The script will:
- Check for MySQL client
- Verify SQL file exists
- Create a database backup
- Verify database connection
- Check if columns exist
- Run the migration
- Verify migration success

## Option 3: Manual SQL Execution

If you prefer to run the commands manually:

```sql
USE employeedb;

-- Drop columns (only if they exist)
ALTER TABLE employees DROP COLUMN IF EXISTS blood_group;
ALTER TABLE employees DROP COLUMN IF EXISTS emergency_contact_name;
ALTER TABLE employees DROP COLUMN IF EXISTS emergency_contact_phone;
ALTER TABLE employees DROP COLUMN IF EXISTS emergency_contact_relationship;
```

**Note:** MySQL doesn't support `DROP COLUMN IF EXISTS` directly. The SQL script uses prepared statements to check existence first.

## Rollback (If Needed)

If you need to restore the columns after migration:

```sql
ALTER TABLE employees 
ADD COLUMN blood_group VARCHAR(10) NULL AFTER next_of_kin_address,
ADD COLUMN emergency_contact_name VARCHAR(255) NULL AFTER blood_group,
ADD COLUMN emergency_contact_phone VARCHAR(50) NULL AFTER emergency_contact_name,
ADD COLUMN emergency_contact_relationship VARCHAR(100) NULL AFTER emergency_contact_phone;
```

Or restore from backup:

```bash
mysql -u [username] -p [database_name] < backup_before_migration.sql
```

## Safety Features

The SQL script includes:
- ✅ Column existence checks before dropping
- ✅ Verification after migration
- ✅ Clear status messages
- ✅ Transaction support (where applicable)

The Bash script includes:
- ✅ Pre-flight checks
- ✅ Automatic backup creation
- ✅ Connection verification
- ✅ Column existence verification
- ✅ Migration verification
- ✅ Error handling

## Post-Migration Checklist

After running the migration:

1. ✅ Verify columns are removed: `SHOW COLUMNS FROM employees;`
2. ✅ Test application functionality
3. ✅ Verify employee creation/editing works
4. ✅ Check employee list/details pages
5. ✅ Verify user creation flow
6. ✅ Test API endpoints related to employees

## Troubleshooting

### Error: Column doesn't exist
- This is normal if columns were already removed
- The script will skip non-existent columns

### Error: Access denied
- Verify database credentials
- Ensure user has ALTER TABLE permissions

### Error: Table locked
- Wait for any running queries to complete
- Check for long-running transactions

### Migration Failed
- Restore from backup if needed
- Check MySQL error logs
- Verify table structure: `DESCRIBE employees;`

## Support

If you encounter issues:
1. Check MySQL error logs
2. Verify database permissions
3. Ensure no application is using these columns
4. Review the migration script output

## Notes

- The columns will be permanently removed from the database
- Existing data in these columns will be lost (ensure backup)
- The application code has already been updated to not use these fields
- This migration is safe to run after deploying the application code changes

