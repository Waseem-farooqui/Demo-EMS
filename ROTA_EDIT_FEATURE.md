# Rota Editing Feature Implementation

## Overview
Implemented functionality to allow ADMIN and SUPER_ADMIN users to edit uploaded rotas.

## Changes Made

### Backend

#### 1. New DTO Created
**File:** `RotaScheduleEntryDTO.java`
- Individual rota schedule entry DTO for editing
- Contains fields: id, rotaId, employeeId, employeeName, scheduleDate, dayOfWeek, startTime, endTime, duty, isOffDay

#### 2. RotaService.java - New Methods Added
- `updateRotaSchedule()` - Update a single rota schedule entry
- `getRotaSchedules()` - Get all schedules for a rota (for editing)
- `getRotaSchedule()` - Get a single schedule by ID
- `deleteRotaSchedule()` - Delete a single rota schedule
- `convertToScheduleEntryDTO()` - Convert entity to DTO

**Features:**
- ✅ Only ADMIN and SUPER_ADMIN can edit/delete schedules
- ✅ Full audit logging with change history
- ✅ Tracks old and new values
- ✅ Records username, IP address, user agent
- ✅ Validates user permissions before allowing edits

#### 3. RotaController.java - New Endpoints Added

**GET `/api/rota/{rotaId}/schedules`**
- Get all schedules for a rota (for editing)
- Access: ADMIN, SUPER_ADMIN
- Returns: List<RotaScheduleEntryDTO>

**GET `/api/rota/schedules/{scheduleId}`**
- Get a single schedule by ID
- Access: ADMIN, SUPER_ADMIN
- Returns: RotaScheduleEntryDTO

**PUT `/api/rota/schedules/{scheduleId}`**
- Update a rota schedule
- Access: ADMIN, SUPER_ADMIN
- Body: RotaScheduleUpdateDTO
- Returns: Updated schedule + success message

**DELETE `/api/rota/schedules/{scheduleId}`**
- Delete a single rota schedule
- Access: ADMIN, SUPER_ADMIN
- Returns: Success message

## API Usage Examples

### 1. Get All Schedules for a Rota
```http
GET /api/rota/1/schedules
Authorization: Bearer {token}
```

**Response:**
```json
[
  {
    "id": 1,
    "rotaId": 1,
    "employeeId": 5,
    "employeeName": "John Doe",
    "scheduleDate": "2025-11-05",
    "dayOfWeek": "Tuesday",
    "startTime": "09:00:00",
    "endTime": "17:00:00",
    "duty": "09:00-17:00",
    "isOffDay": false
  },
  ...
]
```

### 2. Update a Schedule
```http
PUT /api/rota/schedules/1
Authorization: Bearer {token}
Content-Type: application/json

{
  "duty": "10:00-18:00",
  "startTime": "10:00:00",
  "endTime": "18:00:00",
  "isOffDay": false
}
```

**Response:**
```json
{
  "success": true,
  "message": "Rota schedule updated successfully",
  "schedule": {
    "id": 1,
    "employeeId": 5,
    "employeeName": "John Doe",
    "duty": "10:00-18:00",
    "startTime": "10:00:00",
    "endTime": "18:00:00",
    "isOffDay": false
  }
}
```

### 3. Delete a Schedule
```http
DELETE /api/rota/schedules/1
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true,
  "message": "Rota schedule deleted successfully"
}
```

## Audit Logging

All edit and delete operations are logged in the `rota_change_logs` table with:
- Change type (UPDATE/DELETE)
- Old and new values (JSON)
- Change description
- User who made the change
- Timestamp
- IP address
- User agent

## Access Control

### Permissions Matrix
| Action | USER | ADMIN | SUPER_ADMIN |
|--------|------|-------|-------------|
| View Rota Schedules | ❌ | ✅ | ✅ |
| Edit Rota Schedule | ❌ | ✅ | ✅ |
| Delete Rota Schedule | ❌ | ✅ | ✅ |
| View Change Logs | ❌ | ✅ | ✅ |

## Frontend Integration (TODO)

To integrate this feature in the frontend, you would need to:

1. **Create Edit Rota Page**
   - Component: `rota-edit.component.ts`
   - Route: `/rotas/edit/:rotaId`
   - Features:
     - Display all schedules in editable table
     - Inline editing or modal dialog
     - Save/Cancel buttons
     - Delete schedule option

2. **API Service Methods**
```typescript
getRotaSchedules(rotaId: number): Observable<RotaScheduleEntry[]>
updateSchedule(scheduleId: number, data: UpdateScheduleDTO): Observable<any>
deleteSchedule(scheduleId: number): Observable<any>
```

3. **UI Components**
   - Edit button on rota list
   - Editable table/form for schedules
   - Confirmation dialogs for delete
   - Toast notifications for success/error

## Testing

### Test Scenarios

1. **ADMIN can edit schedules**
   - Login as ADMIN
   - Navigate to rota list
   - Click edit on a rota
   - Modify schedule times
   - Save changes
   - Verify changes are saved

2. **USER cannot edit schedules**
   - Login as USER
   - Try to access edit endpoint
   - Should receive 403 Forbidden

3. **Audit logs are created**
   - Make changes to a schedule
   - Query `rota_change_logs` table
   - Verify log entry exists with correct data

## Database Schema

No schema changes required - uses existing tables:
- `rota_schedules` - Schedule data
- `rota_change_logs` - Audit trail

## Notes

- ✅ All methods include proper error handling
- ✅ Access control enforced at service layer
- ✅ All changes are audited
- ✅ Supports both individual and batch operations
- ✅ Works with both Excel and OCR-based rotas

---

**Status**: ✅ Backend Implementation Complete
**Date**: November 5, 2025

