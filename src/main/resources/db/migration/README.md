# Database Migrations

This directory contains Flyway database migration scripts.

## Migration Naming Convention

All migration files must follow the pattern:
```
V{version}__{Description}.sql
```

**Examples:**
- ✅ `V13__Add_Document_View_Tracking.sql`
- ✅ `V14__Fix_Rota_Schedules_Table.sql`
- ❌ `add_organization_uuid.sql` (doesn't follow pattern)

## Current Migrations

| Version | File | Description |
|---------|------|-------------|
| V13 | `V13__Add_Document_View_Tracking.sql` | Adds document view tracking columns |
| V14 | `V14__Fix_Rota_Schedules_Table.sql` | Fixes rota_schedules table schema |

## Legacy Files

The following files don't follow Flyway's naming convention and won't be executed automatically:
- `add_organization_uuid.sql`
- `create_notifications_table.sql`
- `create_root_user.sql`
- `multi_tenancy_migration.sql`

**Note**: These files may have been manually executed or are legacy. If they contain important migrations, they should be converted to proper Flyway migrations with version numbers.

## Creating New Migrations

1. Use the next sequential version number
2. Follow the naming pattern: `V{version}__{Description}.sql`
3. Use `IF NOT EXISTS` where possible for idempotency
4. Test locally before committing
5. See `FLYWAY_MIGRATION_GUIDE.md` for detailed instructions

