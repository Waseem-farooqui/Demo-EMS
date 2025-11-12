# Alert Configuration - Logging Improvements & Troubleshooting Guide

## Date: November 7, 2025

## Issue Reported
User encountered error when creating alert configuration for VISA:
```
Configuration already exists for VISA with priority: CRITICAL
```

All alerts were disabled, but new alert creation was failing.

## Root Cause Analysis

### Problem
The system creates **default alert configurations** at startup for:
- **PASSPORT**: 3 configurations (90 days ATTENTION, 30 days WARNING, 7 days CRITICAL)
- **VISA**: 3 configurations (60 days ATTENTION, 30 days WARNING, 7 days CRITICAL)

These default configurations are created with a **unique constraint** on:
- `document_type` + `alert_priority` combination

When you try to create a new alert with the same document type and priority, it fails even if the existing one is disabled.

### Database Constraint
```sql
uniqueConstraints = @UniqueConstraint(columnNames = {"document_type", "alert_priority"})
```

This means:
- ✅ You CAN have: VISA-CRITICAL and VISA-WARNING (different priorities)
- ❌ You CANNOT have: Two VISA-CRITICAL configurations (duplicate)

## Changes Made

### 1. Enhanced Logging - Service Layer

Added comprehensive logging to `AlertConfigurationService.java`:

#### Startup Logging
```java
@PostConstruct
public void initDefaultConfigurations() {
    log.info("🔔 Initializing default alert configurations...");
    // Creates defaults
    log.info("📋 Total alert configurations in database: {}", allConfigs.size());
    // Lists all existing configurations
    log.info("✅ Default alert configurations initialized");
}
```

**Output:**
```
🔔 Initializing default alert configurations...
   ➕ Created default config: PASSPORT - ATTENTION priority - 90 days
   ➕ Created default config: PASSPORT - WARNING priority - 30 days
   ➕ Created default config: PASSPORT - CRITICAL priority - 7 days
   ➕ Created default config: VISA - ATTENTION priority - 60 days
   ➕ Created default config: VISA - WARNING priority - 30 days
   ➕ Created default config: VISA - CRITICAL priority - 7 days
📋 Total alert configurations in database: 6
   ⚙️  [ID: 1] PASSPORT - ATTENTION priority - 90 days - Enabled: true - OrgId: null
   ⚙️  [ID: 2] PASSPORT - WARNING priority - 30 days - Enabled: true - OrgId: null
   ⚙️  [ID: 3] PASSPORT - CRITICAL priority - 7 days - Enabled: true - OrgId: null
   ⚙️  [ID: 4] VISA - ATTENTION priority - 60 days - Enabled: true - OrgId: null
   ⚙️  [ID: 5] VISA - WARNING priority - 30 days - Enabled: true - OrgId: null
   ⚙️  [ID: 6] VISA - CRITICAL priority - 7 days - Enabled: true - OrgId: null
✅ Default alert configurations initialized
```

#### Create Configuration Logging
```java
public AlertConfigurationDTO createConfiguration(AlertConfigurationDTO dto) {
    log.info("🆕 Attempting to create alert configuration:");
    log.info("   📝 Document Type: {}", dto.getDocumentType());
    log.info("   📝 Priority: {}", dto.getAlertPriority());
    // ... other fields
    
    if (exists) {
        log.error("❌ Configuration creation FAILED - Duplicate found!");
        log.error("   📋 Existing configurations for document type '{}':", dto.getDocumentType());
        // Lists all existing configs with same document type
        log.error("      ⚠️  THIS IS THE DUPLICATE: [ID: {}]", existing.getId());
        log.error("      💡 Suggestion: Either UPDATE the existing config or DELETE it first");
    }
    
    log.info("✅ Alert configuration created successfully!");
}
```

**Example Output (Success):**
```
🆕 Attempting to create alert configuration:
   📝 Document Type: DRIVING_LICENSE
   📝 Priority: WARNING
   📝 Days Before: 30
   📝 Email: admin@company.com
   📝 Enabled: true
   📝 Notification Type: BOTH
   📝 Organization ID: 1
   🔍 Checking for existing config with same document type and priority...
   ✅ No duplicate found, proceeding with creation...
✅ Alert configuration created successfully!
   🆔 ID: 7
   📄 Document Type: DRIVING_LICENSE
   🎯 Priority: WARNING
   📅 Days Before: 30
   📧 Email: admin@company.com
   ✔️  Enabled: true
   🏢 Organization ID: 1
```

**Example Output (Duplicate Error):**
```
🆕 Attempting to create alert configuration:
   📝 Document Type: VISA
   📝 Priority: CRITICAL
   📝 Days Before: 10
   📝 Email: admin@company.com
   📝 Enabled: true
   📝 Notification Type: BOTH
   🔍 Checking for existing config with same document type and priority...
❌ Configuration creation FAILED - Duplicate found!
   📋 Existing configurations for document type 'VISA':
      [ID: 4] Priority: ATTENTION - Days: 60 - Enabled: false - OrgId: null - Email: waseem.farooqui19@gmail.com
      [ID: 5] Priority: WARNING - Days: 30 - Enabled: false - OrgId: null - Email: waseem.farooqui19@gmail.com
      [ID: 6] Priority: CRITICAL - Days: 7 - Enabled: false - OrgId: null - Email: waseem.farooqui19@gmail.com
      ⚠️  THIS IS THE DUPLICATE: [ID: 6] - You cannot create another CRITICAL priority config for VISA
      💡 Suggestion: Either UPDATE the existing config (ID: 6) or DELETE it first, then create new one
```

#### Update Configuration Logging
Shows before/after values for all fields being changed.

#### Delete Configuration Logging
Shows what's being deleted before removal.

### 2. New DELETE Endpoint

Added ability to delete alert configurations:

**Endpoint:** `DELETE /api/alert-config/{id}`

**Controller:**
```java
@DeleteMapping("/{id}")
public ResponseEntity<?> deleteConfiguration(@PathVariable Long id) {
    alertConfigurationService.deleteConfiguration(id);
    return ResponseEntity.ok(Map.of("success", true, "message", "Alert configuration deleted successfully"));
}
```

**Service:**
```java
public void deleteConfiguration(Long id) {
    log.info("🗑️  Attempting to delete alert configuration [ID: {}]", id);
    // ... validation and deletion
    log.info("✅ Alert configuration [ID: {}] deleted successfully!", id);
}
```

## Solutions to Your Problem

### Option 1: Update Existing Configuration (Recommended)
Instead of creating a new VISA-CRITICAL configuration, **update** the existing one (ID: 6):

**API Call:**
```bash
PUT /api/alert-config/6
{
  "documentType": "VISA",
  "alertPriority": "CRITICAL",
  "alertDaysBefore": 10,
  "alertEmail": "your-email@company.com",
  "enabled": true,
  "notificationType": "BOTH",
  "organizationId": 1
}
```

### Option 2: Delete Existing, Then Create New
1. **Delete the existing configuration:**
```bash
DELETE /api/alert-config/6
```

2. **Create your new configuration:**
```bash
POST /api/alert-config
{
  "documentType": "VISA",
  "alertPriority": "CRITICAL",
  "alertDaysBefore": 10,
  "alertEmail": "your-email@company.com",
  "enabled": true,
  "notificationType": "BOTH",
  "organizationId": 1
}
```

### Option 3: Use Different Priority
Create a configuration with a different priority level:
```bash
POST /api/alert-config
{
  "documentType": "VISA",
  "alertPriority": "URGENT",  // Different priority
  "alertDaysBefore": 10,
  "alertEmail": "your-email@company.com",
  "enabled": true,
  "notificationType": "BOTH"
}
```

## Available Priority Levels

You can use these priority levels:
- `EXPIRED` - Document has already expired
- `CRITICAL` - Very urgent (e.g., 7 days before)
- `WARNING` - Important (e.g., 30 days before)
- `ATTENTION` - Early notice (e.g., 60-90 days before)
- `URGENT` - Custom urgent level
- `HIGH` - Custom high priority
- `MEDIUM` - Custom medium priority
- `LOW` - Custom low priority

## Understanding the Logs

### 🔔 Emoji Guide
- 🔔 = Initialization
- 🆕 = Create operation
- 🔄 = Update operation
- 🗑️ = Delete operation
- ✅ = Success
- ❌ = Error/Failure
- ⚠️ = Warning
- 💡 = Suggestion/Tip
- 📋 = List/Details
- 📝 = Input data
- 🔍 = Checking/Searching
- 🆔 = ID
- 📄 = Document
- 🎯 = Priority
- 📅 = Days
- 📧 = Email
- ✔️ = Enabled status
- 🏢 = Organization

### Log Levels
- `INFO` = Normal operations
- `WARN` = Warnings (access denied, etc.)
- `ERROR` = Errors and failures
- `DEBUG` = Detailed debugging info

## How to Use the Logs

### 1. Check What Exists at Startup
Look for this section in logs:
```
📋 Total alert configurations in database: X
   ⚙️  [ID: X] DOCUMENT_TYPE - PRIORITY - DAYS - Enabled: true/false
```

### 2. Debug Creation Failures
When creation fails, look for:
```
❌ Configuration creation FAILED - Duplicate found!
   📋 Existing configurations for document type 'VISA':
      ⚠️  THIS IS THE DUPLICATE: [ID: X]
      💡 Suggestion: Either UPDATE the existing config (ID: X) or DELETE it first
```

The log will tell you:
- **What exists** (all configs for that document type)
- **Which one is the duplicate** (exact ID)
- **What to do** (UPDATE or DELETE suggestion)

### 3. Verify Updates
Check before/after values:
```
   📝 New values:
      Days Before: 7 -> 10
      Enabled: false -> true
```

### 4. Confirm Deletions
```
🗑️  Attempting to delete alert configuration [ID: 6]
   📋 Deleting configuration:
      Document Type: VISA
      Priority: CRITICAL
✅ Alert configuration [ID: 6] deleted successfully!
```

## API Endpoints Summary

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/alert-config` | Get all configurations |
| GET | `/api/alert-config/type/{documentType}` | Get configs for specific document type |
| POST | `/api/alert-config` | Create new configuration |
| PUT | `/api/alert-config/{id}` | Update existing configuration |
| DELETE | `/api/alert-config/{id}` | Delete configuration |
| POST | `/api/alert-config/test-alerts` | Manually trigger alert check |

## Best Practices

1. **Check existing configs first**
   ```bash
   GET /api/alert-config/type/VISA
   ```

2. **Update instead of delete+create**
   - Preserves history
   - Safer operation
   - No need to track IDs

3. **Enable/Disable instead of delete**
   - Keep configuration for future reference
   - Can be re-enabled easily

4. **Monitor logs** during operations
   - Logs show exactly what's happening
   - Easy to identify issues
   - Suggestions provided automatically

## Files Modified

1. ✅ `AlertConfigurationService.java` - Added comprehensive logging
2. ✅ `AlertConfigurationController.java` - Added DELETE endpoint

## Testing the Changes

1. **Start the application** and check logs for initialization
2. **Try to create** a duplicate config and observe the detailed error
3. **Use DELETE endpoint** to remove unwanted configs
4. **Update existing** configs instead of creating duplicates
5. **Monitor logs** to understand what's happening

---

**Status:** ✅ Complete
**Compilation:** ✅ No Errors
**New Features:** ✅ DELETE endpoint added
**Logging:** ✅ Comprehensive logging implemented

