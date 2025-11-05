# Attendance System - Quick Start Guide

## 🚀 Start the System

### 1. Backend (Terminal 1):
```bash
cd C:\Users\waseem.uddin\EmployeeManagementSystem
mvnw.cmd clean package -DskipTests
java -jar target/employee-management-system-0.0.1-SNAPSHOT.jar
```

### 2. Frontend (Terminal 2):
```bash
cd frontend
npm start
```

### 3. Open Browser:
```
http://localhost:4200
```

---

## 📝 Quick Test Steps

### As User:

1. **Login** → Navigate to `/attendance`
2. **Check In:**
   - Select "Office" or "Work From Home"
   - Add note (optional): "Starting work on project X"
   - Click "🟢 Check In"
   - ✅ Should see: "Checked in successfully!"
   - ✅ Timer starts showing elapsed time

3. **View Status:**
   - See green card "✅ Checked In"
   - Location badge shows "Office" or "Work From Home"
   - Timer shows "0h 5m" (or current elapsed)

4. **Check Out:**
   - Add note (optional): "Completed tasks for today"
   - Click "🔴 Check Out"
   - ✅ Should see: "Checked out successfully! Total hours: X.XX"

5. **View History:**
   - Scroll to "📊 Attendance History"
   - See today's record with hours calculated
   - Filter by date range
   - See all past records

### As Admin:

1. **View Employee Hours:**
   - Go to "Employees" page
   - Click "📊 Hours" button on any employee
   - ✅ Modal opens showing:
     - Employee name and status
     - Total hours this week
     - Total hours this month
     - Weekly attendance breakdown

2. **View Dashboard (Future):**
   - Dashboard stats endpoint ready
   - Shows: Total employees, On leave, Checked in, By location

---

## 📊 Example Data

### After Check-in at 9:00 AM:
```json
{
  "id": 1,
  "employeeId": 1,
  "employeeName": "John Doe",
  "checkInTime": "2025-11-01T09:00:00",
  "checkOutTime": null,
  "workDate": "2025-11-01",
  "workLocation": "OFFICE",
  "workLocationDisplay": "Office",
  "hoursWorked": null,
  "isActive": true
}
```

### After Check-out at 5:30 PM:
```json
{
  "id": 1,
  "employeeId": 1,
  "employeeName": "John Doe",
  "checkInTime": "2025-11-01T09:00:00",
  "checkOutTime": "2025-11-01T17:30:00",
  "workDate": "2025-11-01",
  "workLocation": "OFFICE",
  "workLocationDisplay": "Office",
  "hoursWorked": 8.5,
  "isActive": false
}
```

---

## 🎯 Features to Test

### Check-in Features:
- [ ] Can select work location
- [ ] Can add notes
- [ ] Can only check in once
- [ ] Timer starts after check-in
- [ ] Status card turns green

### Check-out Features:
- [ ] Can add notes
- [ ] Hours calculated automatically
- [ ] Timer stops
- [ ] Record appears in history
- [ ] Status card turns gray

### History Features:
- [ ] Shows all past records
- [ ] Date range filter works
- [ ] Max 3 months validation
- [ ] Hours displayed correctly
- [ ] Location shown

### Work Summary (Admin):
- [ ] Modal opens on click
- [ ] Shows employee info
- [ ] Weekly hours correct
- [ ] Monthly hours correct
- [ ] Weekly attendance listed
- [ ] Current status shown

---

## ⚠️ Common Issues

### Issue: Cannot check in
**Reason:** Already checked in  
**Solution:** Check out first

### Issue: Date range error
**Reason:** Range exceeds 3 months  
**Solution:** Select shorter date range

### Issue: 403 Forbidden
**Reason:** Not logged in or wrong user  
**Solution:** Login with correct credentials

---

## 🎨 UI Indicators

### Status Colors:
- 🟢 **Green Card** = Checked In
- ⭕ **Gray Card** = Not Checked In
- 🟡 **Yellow Badge** = In Progress
- 🟢 **Green Badge** = Completed (with hours)

### Location Icons:
- 🏢 Office
- 🏠 Work From Home
- 🏛️ Client Site
- 🚗 Field Work
- 🔄 Hybrid

---

## 📱 Mobile Testing

1. Open DevTools (F12)
2. Toggle device toolbar
3. Test on iPhone/Android sizes
4. Check:
   - [ ] Navigation hamburger works
   - [ ] Forms are usable
   - [ ] Cards stack properly
   - [ ] Buttons are tappable
   - [ ] Modal is scrollable

---

## ✅ Success Criteria

After testing, you should be able to:

✅ Check in with location  
✅ See elapsed timer  
✅ Check out and see hours  
✅ View attendance history  
✅ Filter by date range  
✅ (Admin) View employee work summary  
✅ See weekly and monthly totals  
✅ Use on mobile devices  

---

**Status**: Ready for Testing  
**Time to Test**: ~5-10 minutes  
**Result**: Fully functional attendance system

