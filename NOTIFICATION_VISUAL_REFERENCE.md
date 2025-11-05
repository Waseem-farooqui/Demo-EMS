# Notification System - Visual Reference

## Navbar - Bell Icon States

### 1. No Unread Notifications
```
┌─────────────────────────────────────────────────────────────┐
│ 👥 EMS   Employees  Attendance  ROTA  Documents  Leaves     │
│                                         🔔  John Doe  Admin  │
└─────────────────────────────────────────────────────────────┘
```

### 2. With Unread Notifications (Animated)
```
┌─────────────────────────────────────────────────────────────┐
│ 👥 EMS   Employees  Attendance  ROTA  Documents  Leaves     │
│                                       🔔[3]  John Doe  Admin │
│                                       ↑ red badge            │
└─────────────────────────────────────────────────────────────┘
```

## Notification Dropdown - Expanded View

### When Bell is Clicked:
```
┌─────────────────────────────────────────────────────────────┐
│ 👥 EMS   Employees  Attendance  ROTA  Documents  Leaves     │
│                                       🔔[3]  John Doe  Admin │
└───────────────────────────────────────┬─────────────────────┘
                                        │
        ┌───────────────────────────────▼──────────────────┐
        │ Notifications          [Mark all as read]        │
        ├──────────────────────────────────────────────────┤
        │ ┃ 🏖️  New Leave Request                      × │
        │ ┃     Jane Smith has requested Annual Leave     │
        │ ┃     from 2025-11-10 to 2025-11-12             │
        │ ┃     5m ago                                     │
        │ ┃                                                 │ ← Blue bar = unread
        ├──────────────────────────────────────────────────┤
        │ ┃ 🏖️  New Leave Request                      × │
        │ ┃     Bob Johnson has requested Sick Leave      │
        │ ┃     from 2025-11-08 to 2025-11-09             │
        │ ┃     2h ago                                     │
        ├──────────────────────────────────────────────────┤
        │   ✅  Leave Approved                          × │
        │       Your Annual Leave has been approved        │
        │       by Sarah Manager                           │
        │       1d ago                                     │ ← No bar = read
        ├──────────────────────────────────────────────────┤
        │              View all notifications              │
        └──────────────────────────────────────────────────┘
```

## Empty State - No Notifications
```
        ┌──────────────────────────────────────────────────┐
        │ Notifications                                    │
        ├──────────────────────────────────────────────────┤
        │                                                  │
        │                    🔕                            │
        │                                                  │
        │             No notifications yet                 │
        │                                                  │
        └──────────────────────────────────────────────────┘
```

## Notification Types & Icons

### 1. Leave Request (Sent to Admins)
```
┌────────────────────────────────────────────────┐
│ 🏖️  New Leave Request                      × │
│     [Employee Name] has requested              │
│     [Leave Type] from [Start] to [End]         │
│     [Time ago]                                 │
└────────────────────────────────────────────────┘
```

### 2. Leave Approved (Sent to Employee)
```
┌────────────────────────────────────────────────┐
│ ✅  Leave Request Approved                  × │
│     Your [Leave Type] from [Start] to [End]    │
│     has been approved by [Approver Name]       │
│     [Time ago]                                 │
└────────────────────────────────────────────────┘
```

### 3. Leave Rejected (Sent to Employee)
```
┌────────────────────────────────────────────────┐
│ ❌  Leave Request Rejected                  × │
│     Your [Leave Type] from [Start] to [End]    │
│     has been rejected by [Rejector]. Reason:   │
│     [Rejection reason]                         │
│     [Time ago]                                 │
└────────────────────────────────────────────────┘
```

## Color Scheme

### Unread Notification:
- Background: Light blue (#f0f7ff)
- Left border: Blue bar (4px, #4a90e2)
- Text: Dark (#1a1a1a)

### Read Notification:
- Background: White
- No left border
- Text: Gray (#666)

### Hover State:
- Background: Light gray (#f8f9fa)
- Cursor: Pointer

## Responsive Design

### Desktop (>768px):
```
Navbar: Full width with all elements
Dropdown: 380px wide, positioned below bell
          max-height: 500px, scrollable
```

### Mobile (≤768px):
```
Navbar: Hamburger menu
Dropdown: Full width (with 8px margins)
          Fixed position at top
          max-height: calc(100vh - 80px)
```

## Animation Effects

### Bell Icon:
- **No notifications:** Static
- **Has notifications:** Gentle ring animation (rotates ±10deg)

### Badge:
- Red background (#ff4444)
- White text, bold
- Shows "99+" if count > 99

### Dropdown:
- Smooth slide-down transition
- Backdrop overlay (transparent, closes on click)

## User Interactions

### Clicking Bell:
1. Opens dropdown
2. Loads recent notifications
3. Shows current unread count

### Clicking Notification:
1. If unread → marks as read (API call)
2. Removes blue highlight
3. Decreases badge count
4. Navigates to related page (e.g., /leaves)
5. Closes dropdown

### Clicking "Mark all as read":
1. API call to mark all as read
2. Removes all blue highlights
3. Badge count → 0
4. Keeps dropdown open

### Clicking Delete (×):
1. API call to delete notification
2. Removes from list
3. Decreases count if was unread
4. No navigation

### Clicking Backdrop:
1. Closes dropdown
2. No other action

## Time Display Format

```
< 1 minute:    "Just now"
1-59 minutes:  "5m ago", "23m ago"
1-23 hours:    "2h ago", "15h ago"
1-6 days:      "3d ago", "5d ago"
7+ days:       "11/5/2025" (local date format)
```

## Component Structure

```
app-notification-dropdown
├── notification-container
│   ├── notification-bell (button)
│   │   ├── bell-icon 🔔
│   │   └── notification-badge [count]
│   │
│   ├── notification-dropdown (panel)
│   │   ├── dropdown-header
│   │   │   ├── h3 "Notifications"
│   │   │   └── mark-all-read button
│   │   │
│   │   ├── notification-list (scrollable)
│   │   │   ├── notification-item (*ngFor)
│   │   │   │   ├── notification-icon
│   │   │   │   ├── notification-content
│   │   │   │   │   ├── notification-title
│   │   │   │   │   ├── notification-message
│   │   │   │   │   └── notification-time
│   │   │   │   └── delete-notification button
│   │   │   │
│   │   │   └── empty-state (if no notifications)
│   │   │
│   │   └── dropdown-footer
│   │       └── view-all link
│   │
│   └── notification-backdrop (overlay)
```

## Who Sees What?

### ROOT User:
- ❌ No bell icon (manages orgs, not leaves)

### USER (Employee):
- ❌ No bell icon for leave requests
- ✅ Bell shows when their leave is approved/rejected

### ADMIN (Department Manager):
- ✅ Bell icon visible
- ✅ Sees leave requests from users in their department
- ✅ Sees leave requests from other admins (if SUPER_ADMIN)

### SUPER_ADMIN (CEO/Owner):
- ✅ Bell icon visible
- ✅ Sees ALL leave requests in the organization
- ✅ Sees requests from both users and admins

## Integration Points

### Frontend → Backend:
- Polling every 30 seconds: `GET /api/notifications/unread/count`
- On bell click: `GET /api/notifications/recent`
- On notification click: `PUT /api/notifications/{id}/read`
- On mark all: `PUT /api/notifications/read-all`
- On delete: `DELETE /api/notifications/{id}`

### Backend Triggers:
- Leave requested → `createLeaveRequestNotification()`
- Leave approved → `createLeaveApprovalNotification()`
- Leave rejected → `createLeaveRejectionNotification()`

## Summary

The notification system provides a **professional, intuitive, and responsive** way for admins to be notified of leave requests and for employees to be notified of leave decisions. The bell icon is prominently displayed in the navbar, with clear visual indicators for unread notifications, and a feature-rich dropdown for managing notifications.

