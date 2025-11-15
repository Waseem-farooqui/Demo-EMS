# Database Migration Fix - Missing Tables Issue

## Problem
The `documents` table (and potentially other tables) are not being created in production, causing:
```
java.sql.SQLSyntaxErrorException: Table 'employee_management_system.documents' doesn't exist
```

## Root Causes Identified

1. **Missing `init.sql` file** - `compose.yaml` references `./database/init.sql` but it didn't exist
2. **No Flyway configured** - Migration files in `src/main/resources/db/migration/` are not being executed
3. **JPA ddl-auto dependency** - Relying solely on JPA's `ddl-auto=update` which may fail if:
   - Backend starts before database is fully ready
   - Entity scanning fails
   - Connection issues occur

## Solutions Applied

### ✅ Solution 1: Created `database/init.sql`
Created the missing initialization script that:
- Creates the database with proper character set
- Ensures database exists before backend starts

### ✅ Solution 2: Verify JPA Configuration
Ensure `application-prod.properties` has:
```properties
spring.jpa.hibernate.ddl-auto=update
```

### 🔧 Solution 3: Add Flyway (Recommended for Production)

**Step 1: Add Flyway dependency to `pom.xml`**
```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
</dependency>
```

**Step 2: Configure Flyway in `application-prod.properties`**
```properties
# Flyway Configuration
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.locations=classpath:db/migration
spring.flyway.table=flyway_schema_history
```

**Step 3: Rename migration files to Flyway format**
- Files should be named: `V1__Description.sql`, `V2__Description.sql`, etc.
- Current files need renaming:
  - `add_organization_uuid.sql` → `V1__Add_Organization_UUID.sql`
  - `create_notifications_table.sql` → `V2__Create_Notifications_Table.sql`
  - `create_root_user.sql` → `V3__Create_Root_User.sql`
  - `multi_tenancy_migration.sql` → `V4__Multi_Tenancy_Migration.sql`
  - `V13__Add_Document_View_Tracking.sql` → Keep as is (already correct format)

**Step 4: Create initial schema migration**
Create `V1__Initial_Schema.sql` with all table creation statements.

## Immediate Fix (Without Flyway)

If you can't add Flyway right now, ensure:

1. **Backend waits for MySQL to be fully ready**
   Update `compose.yaml` backend service:
   ```yaml
   depends_on:
     mysql:
       condition: service_healthy
   ```
   This is already configured, but you may need to increase wait time.

2. **Verify database connection**
   Check backend logs for connection errors:
   ```bash
   docker-compose logs backend | grep -i "database\|connection\|jpa"
   ```

3. **Force table creation on startup**
   Temporarily change in `application-prod.properties`:
   ```properties
   spring.jpa.hibernate.ddl-auto=create-drop  # WARNING: This drops all data!
   ```
   Then change back to `update` after first successful startup.

## Verification Steps

1. **Check if database exists:**
   ```bash
   docker-compose exec mysql mysql -u root -p -e "SHOW DATABASES;"
   ```

2. **Check if tables exist:**
   ```bash
   docker-compose exec mysql mysql -u root -p employee_management_system -e "SHOW TABLES;"
   ```

3. **Check backend logs:**
   ```bash
   docker-compose logs backend | grep -i "table\|migration\|flyway"
   ```

4. **Verify JPA created tables:**
   ```bash
   docker-compose exec mysql mysql -u root -p employee_management_system -e "DESCRIBE documents;"
   ```

## Recommended Approach

**For Production:**
1. Add Flyway dependency
2. Create proper migration files
3. Set `spring.jpa.hibernate.ddl-auto=validate` (after migrations)
4. Use Flyway for all schema changes

**For Quick Fix:**
1. Ensure `init.sql` exists (✅ Done)
2. Restart containers:
   ```bash
   docker-compose down -v  # Remove volumes to start fresh
   docker-compose up -d --build
   ```
3. Monitor backend logs to ensure tables are created
4. If still failing, check backend logs for JPA errors

## Troubleshooting

### Tables still not created?

1. **Check backend startup logs:**
   ```bash
   docker-compose logs backend --tail=100
   ```

2. **Verify database connection:**
   ```bash
   docker-compose exec backend curl http://localhost:8080/api/actuator/health
   ```

3. **Check MySQL logs:**
   ```bash
   docker-compose logs mysql --tail=50
   ```

4. **Manually verify database:**
   ```bash
   docker-compose exec mysql mysql -u root -p
   USE employee_management_system;
   SHOW TABLES;
   ```

### If JPA isn't creating tables:

1. Check entity package scanning in main application class
2. Verify `@Entity` annotations on all entity classes
3. Check for any JPA configuration errors in logs
4. Ensure database user has CREATE TABLE privileges

## Next Steps

1. ✅ Created `database/init.sql`
2. ⚠️ Add Flyway for proper migration management (recommended)
3. ⚠️ Create comprehensive initial schema migration
4. ⚠️ Test in fresh environment
5. ⚠️ Update deployment documentation

