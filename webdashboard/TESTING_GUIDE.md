# CallCloud Admin - Testing Guide

## 🧪 Complete Testing Checklist

### Pre-Testing Setup

- [ ] Backend deployed to server
- [ ] Database initialized successfully
- [ ] Secret token configured correctly
- [ ] Frontend dev server running

---

## 1️⃣ Authentication Testing

### Signup Flow
1. Open http://localhost:5173
2. Click "Create Account"
3. Fill in form:
   - Organization Name: "Test Company"
   - Click "Generate" for Org ID
   - Admin Name: "Test Admin"
   - Email: "test@example.com"
   - Password: "password123"
   - Confirm Password: "password123"
4. Click "Create Account"

**Expected Result:**
- ✅ Redirected to dashboard
- ✅ See organization name in header
- ✅ Token stored in localStorage
- ✅ User data displayed in sidebar

### Login Flow
1. Logout from dashboard
2. Return to login screen
3. Enter credentials:
   - Email: "test@example.com"
   - Password: "password123"
4. Click "Sign In"

**Expected Result:**
- ✅ Successful login
- ✅ Dashboard loads with user data
- ✅ Token refreshed

### Token Verification
1. Open browser DevTools → Console
2. Run: `localStorage.getItem('callcloud_auth_token')`
3. Copy the token
4. Test API:
   ```javascript
   fetch('https://your-domain.com/callcloud/api/auth.php?action=verify', {
     headers: { 'Authorization': 'Bearer ' + localStorage.getItem('callcloud_auth_token') }
   }).then(r => r.json()).then(console.log)
   ```

**Expected Result:**
- ✅ Token exists in localStorage
- ✅ API returns user data
- ✅ status: true

---

## 2️⃣ Employee Management Testing

### View Employees
1. Click "Employees" in sidebar
2. Observe the employee list (empty initially)

**Expected Result:**
- ✅ Empty state or existing employees shown
- ✅ Statistics displayed (Total, Active, Calls Today)
- ✅ Search bar functional

### Create Employee
1. Click "Add Employee" button
2. Fill in form:
   - Name: "John Smith"
   - Email: "john@test.com"
   - Phone: "+1234567890"
   - Department: "Sales"
   - Role: "Sales Manager"
3. Click "Add Employee"

**Expected Result:**
- ✅ Employee added to list
- ✅ Modal closes
- ✅ Statistics update
- ✅ Employee appears in table

### Search Employee
1. Type "John" in search bar
2. Observe filtered results

**Expected Result:**
- ✅ Only matching employees shown
- ✅ Real-time filtering works
- ✅ Clear search shows all employees

### Update Employee (if implemented)
1. Click edit button on employee
2. Change department to "Marketing"
3. Save changes

**Expected Result:**
- ✅ Employee updated in list
- ✅ Changes persist on reload

---

## 3️⃣ Calls Testing

### View Calls
1. Click "Calls" in sidebar
2. Observe call logs

**Expected Result:**
- ✅ Call statistics displayed
- ✅ Call list shows (or empty state)
- ✅ Filters available

### Filter Calls
1. Click filter buttons:
   - Click "Inbound" filter
   - Click "Outbound" filter
   - Click "All Calls"
2. Change date filter dropdown

**Expected Result:**
- ✅ Active filter highlighted
- ✅ Calls filtered correctly
- ✅ Statistics update accordingly
- ✅ Date filtering works

### Search Calls
1. Type contact name or phone number
2. Observe results

**Expected Result:**
- ✅ Matching calls shown
- ✅ Real-time search works
- ✅ Search across multiple fields

---

## 4️⃣ Recordings Testing

### View Recordings
1. Click "Recordings" in sidebar
2. Observe recordings grid

**Expected Result:**
- ✅ Recording statistics displayed
- ✅ Recordings in grid layout
- ✅ Metadata visible (duration, size, date)

### Player Controls (Mock)
1. Click play button on recording
2. Observe player state

**Expected Result:**
- ✅ Play button changes to pause
- ✅ Timeline updates (in mock)
- ✅ Time displays correctly

### Search Recordings
1. Type search term
2. Observe filtered results

**Expected Result:**
- ✅ Matching recordings shown
- ✅ Search by title, contact, tags
- ✅ Real-time filtering

---

## 5️⃣ Reports Testing

### Overview Report
1. Click "Reports" in sidebar
2. View default overview
3. Change date range dropdown

**Expected Result:**
- ✅ Key metrics displayed
- ✅ Charts render correctly
- ✅ Date range updates data
- ✅ Trend indicators show

### Top Performers
1. Scroll to "Top Performers" section
2. Observe ranking

**Expected Result:**
- ✅ Employees ranked by performance
- ✅ Scores calculated
- ✅ Visual indicators (rank badges, circles)

### Department Breakdown
1. Scroll to department section
2. Observe breakdown

**Expected Result:**
- ✅ All departments listed
- ✅ Percentages calculated
- ✅ Progress bars accurate
- ✅ Call counts shown

### Export Reports
1. Click "Export PDF" button
2. Click "Export CSV" button

**Expected Result:**
- ✅ Alert shows (mock functionality)
- ✅ Buttons are functional

---

## 6️⃣ Settings Testing

### View Settings
1. Click "Settings" in sidebar
2. Observe settings sections

**Expected Result:**
- ✅ Organization info shown
- ✅ Account details displayed
- ✅ Preferences toggles visible
- ✅ Security section present

### Toggle Preferences
1. Toggle various preference switches:
   - Email Notifications
   - Call Alerts
   - Weekly Reports
   - Auto-Record Calls

**Expected Result:**
- ✅ Toggles respond smoothly
- ✅ Visual feedback on toggle
- ✅ States persist (when backend connected)

---

## 7️⃣ Navigation & UX Testing

### Sidebar Navigation
1. Click each navigation item:
   - Overview
   - Employees
   - Calls
   - Recordings
   - Reports
   - Settings

**Expected Result:**
- ✅ Active tab highlighted
- ✅ Content changes instantly
- ✅ No page reload
- ✅ Smooth transitions

### Sidebar Collapse
1. Click collapse button (← icon)
2. Observe sidebar

**Expected Result:**
- ✅ Sidebar collapses to icon view
- ✅ Icons remain visible
- ✅ Main content expands
- ✅ Toggle works both ways

### Responsive Design
1. Resize browser window
2. Test at different widths:
   - Desktop (1400px+)
   - Tablet (768px)
   - Mobile (375px)

**Expected Result:**
- ✅ Layout adapts smoothly
- ✅ No horizontal scroll
- ✅ All content accessible
- ✅ Touch targets appropriate

---

## 8️⃣ Backend Integration Testing

### Database Queries
Test with backend deployed:

```javascript
// In browser console
const api = window.CallCloudAPI;

// Test employee creation
await api.createEmployee({
  name: "Test Employee",
  email: "test.emp@example.com",
  phone: "+1987654321",
  department: "Support",
  role: "Support Agent"
});

// Test call logging
await api.createCall({
  employee_id: 1,
  contact_name: "Test Client",
  phone_number: "+1234567890",
  direction: "outbound",
  duration: "5:30",
  status: "completed",
  has_recording: false
});

// Test recording creation
await api.createRecording({
  employee_id: 1,
  title: "Test Recording",
  contact_name: "Test Contact",
  duration: "10:25",
  file_size: "5.2 MB",
  tags: ["Test", "Demo"]
});

// Test reports
await api.getOverviewReport("week");
await api.getEmployeePerformance("month");
```

**Expected Results:**
- ✅ All creates return success
- ✅ Data appears in UI immediately
- ✅ IDs are generated correctly
- ✅ Relationships maintained

---

## 9️⃣ Error Handling Testing

### Invalid Login
1. Try logging in with wrong password
2. Try logging in with non-existent email

**Expected Result:**
- ✅ Error message displayed
- ✅ User stays on login screen
- ✅ No navigation occurs

### Duplicate Org ID
1. Try creating account with existing org ID

**Expected Result:**
- ✅ Error message shown
- ✅ Form not cleared
- ✅ User can correct

### Token Expiry
1. Modify token in localStorage to invalid value
2. Try accessing protected endpoint

**Expected Result:**
- ✅ Redirected to login
- ✅ Token cleared
- ✅ Message shown

---

## 🔟 Performance Testing

### Load Time
1. Open DevTools → Network tab
2. Hard refresh page (Cmd+Shift+R / Ctrl+Shift+R)
3. Check timing

**Expected Results:**
- ✅ Initial load < 3 seconds
- ✅ Dashboard loads < 1 second
- ✅ API calls < 500ms

### Memory Usage
1. Open DevTools → Performance
2. Record session
3. Navigate through all pages
4. Stop recording

**Expected Results:**
- ✅ No memory leaks
- ✅ Smooth animations (60fps)
- ✅ Reasonable memory footprint

---

## 📋 Final Checklist

Before deployment:

- [ ] All authentication flows work
- [ ] All CRUD operations functional
- [ ] Filters and search working
- [ ] Reports generate correctly
- [ ] Navigation is smooth
- [ ] No console errors
- [ ] Responsive on all devices
- [ ] Backend integration complete
- [ ] Error handling robust
- [ ] Performance acceptable

---

## 🐛 Common Issues & Solutions

### Issue: "Unauthorized" errors
**Solution:** Check token in localStorage, verify it's being sent in headers

### Issue: CORS errors
**Solution:** Verify `CORS_ALLOWED_ORIGINS` in `config.php`

### Issue: Database connection failed
**Solution:** Check secret token matches in both places

### Issue: Data not showing
**Solution:** Check browser console for errors, verify API responses

### Issue: Slow performance
**Solution:** Check Network tab, may be API server response time

---

## 📊 Test Results Template

```
Testing Date: ___________
Tester: ___________

Authentication:        [ ] Pass [ ] Fail
Employees:             [ ] Pass [ ] Fail
Calls:                 [ ] Pass [ ] Fail
Recordings:            [ ] Pass [ ] Fail
Reports:               [ ] Pass [ ] Fail
Settings:              [ ] Pass [ ] Fail
Navigation:            [ ] Pass [ ] Fail
Backend Integration:   [ ] Pass [ ] Fail
Error Handling:        [ ] Pass [ ] Fail
Performance:           [ ] Pass [ ] Fail

Notes:
_________________________________
_________________________________
_________________________________
```

---

**Happy Testing! 🚀**
