# Flyway Database Migration Guide

## Overview

This project uses **Flyway** for database schema version control and migrations. Flyway automatically applies database migrations when the application starts, ensuring your database schema is always up-to-date.

## How It Works

1. **Migration Files**: SQL migration files are stored in `src/main/resources/db/migration/`
2. **Naming Convention**: Files must follow the pattern `V{version}__{description}.sql`
   - Example: `V14__Fix_Rota_Schedules_Table.sql`
   - Version numbers must be sequential and unique
3. **Automatic Execution**: Flyway runs migrations automatically on application startup
4. **Version Tracking**: Flyway tracks which migrations have been applied in the `flyway_schema_history` table

## Creating a New Migration

### Step 1: Create the Migration File

Create a new SQL file in `src/main/resources/db/migration/` with the naming pattern:

```
V{next_version_number}__{Descriptive_Name}.sql
```

**Example:**
```
V15__Add_New_Feature_Table.sql
```

### Step 2: Write the SQL

Write your SQL migration script. Always use `IF NOT EXISTS` where possible to make migrations idempotent:

```sql
-- Example migration
ALTER TABLE employees 
ADD COLUMN IF NOT EXISTS new_column VARCHAR(255) NULL;

CREATE INDEX IF NOT EXISTS idx_new_column ON employees(new_column);
```

### Step 3: Test the Migration

1. **Local Testing**: Run the application locally and verify the migration executes successfully
2. **Check Logs**: Look for Flyway migration logs in the application startup logs
3. **Verify Schema**: Check that the database schema changes were applied correctly

### Step 4: Commit and Deploy

1. Commit the migration file to version control
2. Deploy the application - Flyway will automatically run the migration on startup

## Migration Best Practices

### ✅ DO:

- **Use IF NOT EXISTS**: Make migrations idempotent where possible
- **Test Locally First**: Always test migrations before deploying
- **Use Transactions**: Wrap migrations in transactions when possible
- **Version Sequentially**: Use sequential version numbers (V14, V15, V16, etc.)
- **Descriptive Names**: Use clear, descriptive names in migration files
- **Add Indexes**: Create indexes for new columns that will be queried
- **Add Foreign Keys**: Add foreign key constraints for referential integrity

### ❌ DON'T:

- **Don't Modify Existing Migrations**: Once a migration is applied, never modify it
- **Don't Skip Versions**: Use sequential version numbers
- **Don't Use DROP**: Avoid DROP statements unless absolutely necessary
- **Don't Hardcode Data**: Use parameterized queries or environment variables
- **Don't Ignore Errors**: Always handle migration errors properly

## Current Migrations

| Version | Description | Date |
|---------|-------------|------|
| V13 | Add Document View Tracking | - |
| V14 | Fix Rota Schedules Table | 2025-01-16 |

## Configuration

### Production (`application-prod.properties`)

```properties
# Flyway Configuration
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=0
spring.flyway.locations=classpath:db/migration
spring.flyway.validate-on-migrate=true
spring.flyway.clean-disabled=true
spring.flyway.out-of-order=false

# JPA should use 'validate' when using Flyway
spring.jpa.hibernate.ddl-auto=validate
```

### Development (`application.properties`)

```properties
# Flyway disabled for H2 in-memory database
spring.flyway.enabled=false
# JPA handles schema for H2
spring.jpa.hibernate.ddl-auto=update
```

## Troubleshooting

### Migration Fails on Startup

**Error**: `Migration checksum mismatch`

**Solution**: 
- Check if the migration file was modified after being applied
- If intentional, you may need to repair: `flyway repair` (not recommended in production)

**Error**: `Migration version mismatch`

**Solution**:
- Ensure version numbers are sequential
- Check `flyway_schema_history` table for applied migrations

### Migration Not Running

**Check**:
1. Is Flyway enabled? (`spring.flyway.enabled=true`)
2. Is the file in the correct location? (`src/main/resources/db/migration/`)
3. Does the filename follow the naming convention? (`V{version}__{name}.sql`)
4. Check application logs for Flyway messages

### Rollback Migrations

Flyway doesn't support automatic rollbacks. To rollback:

1. Create a new migration that reverses the changes
2. Or manually revert the changes in the database
3. Update the `flyway_schema_history` table if needed

## Manual Migration Execution

If you need to run migrations manually:

```bash
# Using Flyway CLI (if installed)
flyway migrate -url=jdbc:mysql://localhost:3306/employee_management_system \
               -user=emsuser \
               -password=emspassword \
               -locations=filesystem:src/main/resources/db/migration

# Or via MySQL directly
mysql -u emsuser -p employee_management_system < src/main/resources/db/migration/V14__Fix_Rota_Schedules_Table.sql
```

## Migration File Template

```sql
-- ===================================================================
-- Flyway Migration: {Brief Description}
-- Version: V{version}
-- Description: {Detailed description of what this migration does}
-- Date: {YYYY-MM-DD}
-- ===================================================================

-- Your SQL statements here
-- Use IF NOT EXISTS where possible for idempotency

ALTER TABLE table_name 
ADD COLUMN IF NOT EXISTS new_column VARCHAR(255) NULL;

CREATE INDEX IF NOT EXISTS idx_new_column ON table_name(new_column);

-- Add comments explaining complex operations
-- Example: Adding foreign key constraint
ALTER TABLE child_table 
ADD CONSTRAINT fk_child_parent 
FOREIGN KEY (parent_id) REFERENCES parent_table(id) ON DELETE CASCADE;
```

## Integration with Docker

When deploying with Docker:

1. Migrations are automatically included in the JAR file
2. Flyway runs on application startup
3. Ensure database is accessible before application starts
4. Use health checks to verify database connectivity

## Additional Resources

- [Flyway Documentation](https://flywaydb.org/documentation/)
- [Spring Boot Flyway Integration](https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.data-initialization.migration-tool.flyway)

