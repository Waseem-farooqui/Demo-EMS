# Quick Test Guide - Notification System

## Start the Application

### Terminal 1 - Backend:
```bash
cd C:\Users\waseem.uddin\EmployeeManagementSystem
mvn spring-boot:run
```
Wait for: "Started EmployeeManagementSystemApplication"

### Terminal 2 - Frontend:
```bash
cd C:\Users\waseem.uddin\EmployeeManagementSystem\frontend
ng serve
```
Wait for: "Compiled successfully"

Open browser: http://localhost:4200

## Test Steps

### Step 1: Create Leave Request (as Employee)
1. Login as a USER (employee account)
2. Navigate to **Leaves** page (🏖️ icon in navbar)
3. Click "Apply Leave" button
4. Fill in the form:
   - Leave Type: Annual Leave
   - Start Date: Tomorrow
   - End Date: Day after tomorrow
   - Reason: "Personal matters"
5. Click "Submit"
6. **Logout**

### Step 2: Check Notification (as Admin)
1. Login as ADMIN or SUPER_ADMIN
2. **Look at the top navbar** - you should see:
   - 🔔 Bell icon (may be ringing/animated)
   - Red badge with number "1" 
3. Click the bell icon
4. Dropdown should appear showing:
   - "New Leave Request" notification
   - Employee name and leave details
   - Blue highlight (unread)
   - Time ago (e.g., "Just now" or "2m ago")

### Step 3: Interact with Notification
1. Click the notification in the dropdown
2. Should:
   - Mark as read (blue highlight disappears)
   - Badge count decreases
   - Navigate to Leaves page
3. Go back to any page and click bell again
4. Notification still shows but no longer highlighted
5. Try "Mark all as read" button
6. Try the × delete button on a notification

### Step 4: Test Auto-Refresh
1. Keep admin logged in
2. In another browser/incognito:
   - Login as different employee
   - Submit another leave request
3. Wait up to 30 seconds
4. Admin's bell icon should update automatically
5. Badge should show "2" or increment

## Expected Behavior

### For USER (Employee):
- **Cannot** see bell icon (they don't approve leaves)
- Receives notifications when their leave is approved/rejected

### For ADMIN:
- **CAN** see bell icon
- Receives notifications when:
  - Users in their department request leave
  - Other admins request leave (if they're SUPER_ADMIN)

### For SUPER_ADMIN:
- **CAN** see bell icon
- Receives notifications for:
  - ALL leave requests in the organization
  - Both USER and ADMIN leave requests

### For ROOT:
- **Cannot** see bell icon (ROOT manages organizations, not leaves)

## Visual Checks

✅ Bell icon visible in navbar (for ADMIN/SUPER_ADMIN)
✅ Red badge with count appears
✅ Bell animates/rings when unread notifications exist
✅ Dropdown opens on click
✅ Notifications display correctly
✅ Blue highlight for unread
✅ Time ago shows correctly
✅ Click marks as read
✅ Badge count updates
✅ Delete button works
✅ "Mark all as read" works
✅ Mobile responsive

## Troubleshooting

### No bell icon showing?
- Check you're logged in as ADMIN or SUPER_ADMIN (not USER or ROOT)
- Check browser console for errors
- Verify backend is running (http://localhost:8080/api/notifications/unread/count should return JSON)

### Bell shows but no notifications?
- Create a leave request first
- Check backend logs for "🔔 Creating leave request notification"
- Verify notification was created in database
- Check API: http://localhost:8080/api/notifications/recent

### Badge count not updating?
- Wait up to 30 seconds (polling interval)
- Check browser console for errors
- Verify API calls are succeeding

### Notifications not marking as read?
- Check browser network tab
- Verify PUT request to /api/notifications/{id}/read succeeds
- Check for CORS errors

## API Endpoints (for manual testing)

You can test these in Postman/browser (need auth token):

```
GET    /api/notifications              - All notifications
GET    /api/notifications/unread       - Unread only
GET    /api/notifications/unread/count - Count of unread
GET    /api/notifications/recent       - Last 10
PUT    /api/notifications/{id}/read    - Mark as read
PUT    /api/notifications/read-all     - Mark all as read
DELETE /api/notifications/{id}         - Delete notification
```

## Backend Logs to Watch

When leave is requested, you should see:
```
🔔 Creating leave request notification for leave ID: 123
USER leave request - notifying department ADMIN
✅ Notification created for user ID: 456
🔔 Total 2 notifications created for leave request
```

When notification is viewed:
```
✅ Notification 789 marked as read
```

## Success Criteria

✅ Employee submits leave
✅ Admin sees bell icon with badge
✅ Admin clicks bell and sees notification
✅ Admin clicks notification - marks as read
✅ Badge count decreases
✅ Notification navigates to leaves page
✅ System feels responsive and professional

## Issues Found?

Check these files:
1. `frontend/src/app/components/notification-dropdown/*`
2. `frontend/src/app/services/notification.service.ts`
3. `backend/src/.../controller/NotificationController.java`
4. `backend/src/.../service/NotificationService.java`

Browser Console: F12 → Console tab
Backend Logs: In the terminal running Spring Boot

