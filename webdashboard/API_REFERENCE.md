# CallCloud Admin API - Quick Reference

## 🔗 Base URL
```
https://calltrack.mylistings.in/callcloud/api
```

## 🔐 Authentication

All requests (except signup/login) require authentication header:
```
Authorization: Bearer {your_token}
```

---

## 📋 API Endpoints

### AUTH

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/auth.php?action=signup` | Create account |
| POST | `/auth.php?action=login` | User login |
| POST | `/auth.php?action=logout` | User logout |
| GET | `/auth.php?action=verify` | Verify token |

### EMPLOYEES

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/employees.php` | Get all employees |
| GET | `/employees.php?action=stats` | Get statistics |
| POST | `/employees.php` | Create employee |
| PUT | `/employees.php?id={id}` | Update employee |
| DELETE | `/employees.php?id={id}` | Delete employee |

### CALLS

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/calls.php` | Get all calls |
| GET | `/calls.php?action=stats` | Get statistics |
| POST | `/calls.php` | Create call log |

**Query Parameters:**
- `direction` - inbound/outbound/all
- `dateFilter` - today/week/month/all
- `search` - search term

### RECORDINGS

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/recordings.php` | Get all recordings |
| GET | `/recordings.php?search={term}` | Search recordings |
| GET | `/recordings.php?action=stats` | Get statistics |
| POST | `/recordings.php` | Create recording |
| DELETE | `/recordings.php?id={id}` | Delete recording |

### REPORTS

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/reports.php?type=overview` | Overview report |
| GET | `/reports.php?type=employee` | Employee performance |
| GET | `/reports.php?type=department` | Department breakdown |
| GET | `/reports.php?type=calls` | Call analytics |

**Query Parameters:**
- `type` - overview/employee/department/calls
- `dateRange` - today/week/month/quarter/year/all

---

## 📝 Request Examples

### Signup
```bash
curl -X POST "https://your-domain.com/callcloud/api/auth.php?action=signup" \
  -H "Content-Type: application/json" \
  -d '{
    "organizationName": "Acme Corp",
    "organizationId": "ABC123",
    "adminName": "John Doe",
    "email": "john@acme.com",
    "password": "password123"
  }'
```

### Login
```bash
curl -X POST "https://your-domain.com/callcloud/api/auth.php?action=login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@acme.com",
    "password": "password123"
  }'
```

### Create Employee
```bash
curl -X POST "https://your-domain.com/callcloud/api/employees.php" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Jane Smith",
    "email": "jane@acme.com",
    "phone": "+1234567890",
    "department": "Sales",
    "role": "Manager"
  }'
```

### Get Calls
```bash
curl -X GET "https://your-domain.com/callcloud/api/calls.php?direction=inbound&dateFilter=week" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Generate Report
```bash
curl -X GET "https://your-domain.com/callcloud/api/reports.php?type=overview&dateRange=month" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## 📊 Response Format

### Success Response
```json
{
  "status": true,
  "message": "Success message",
  "data": {
    // Response data
  }
}
```

### Error Response
```json
{
  "status": false,
  "message": "Error message",
  "data": {}
}
```

---

## 🔢 HTTP Status Codes

| Code | Meaning |
|------|---------|
| 200 | Success |
| 400 | Bad Request |
| 401 | Unauthorized |
| 404 | Not Found |
| 405 | Method Not Allowed |
| 500 | Server Error |

---

## 🎯 Frontend Usage

```typescript
import api from './api/client';

// Authentication
await api.signup(userData);
await api.login(email, password);
await api.logout();

// Employees
await api.getEmployees();
await api.getEmployeeStats();
await api.createEmployee(data);
await api.updateEmployee(id, data);
await api.deleteEmployee(id);

// Calls
await api.getCalls(filters);
await api.getCallStats();
await api.createCall(data);

// Recordings
await api.getRecordings(search);
await api.getRecordingStats();
await api.createRecording(data);
await api.deleteRecording(id);

// Reports
await api.getOverviewReport(dateRange);
await api.getEmployeePerformance(dateRange);
await api.getDepartmentBreakdown(dateRange);
await api.getCallAnalytics(dateRange);
```

---

**📖 For complete documentation, see `BACKEND_DEPLOYMENT.md`**
