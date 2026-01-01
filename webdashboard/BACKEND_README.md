# CallCloud Admin - Complete Backend Integration

## 🎯 What's Been Created

A **fully functional PHP backend** for the CallCloud Admin Dashboard that integrates with your existing MySQL database via the remote MySQL Manager API.

### Backend Components

```
webdashboard/
├── php/
│   ├── config.php              # Configuration & settings
│   ├── utils.php               # Database, Auth, Response utilities
│   ├── schema.sql              # Complete database schema
│   ├── init_database.php       # Database initialization script
│   └── api/
│       ├── auth.php           # Authentication (signup, login, logout)
│       ├── employees.php      # Employee CRUD operations
│       ├── calls.php          # Call logs management
│       ├── recordings.php     # Recording management
│       └── reports.php        # Analytics & reports
├── src/
│   └── api/
│       └── client.ts          # Frontend API client
├── deploy.sh                   # Automated deployment script
├── BACKEND_DEPLOYMENT.md       # Detailed deployment guide
└── README.md                   # This file
```

## 🚀 Quick Start

### Step 1: Configure Secret Token

Edit `php/config.php` and set your secret token:

```php
define('API_SECRET_TOKEN', 'YOUR_SECRET_TOKEN_HERE');
```

This must match the token in your MySQL Manager at:
`https://calltrack.mylistings.in/ai_mysql_manager.php`

### Step 2: Deploy Backend

**Option A: Automatic Deployment (Recommended)**

1. Update the `SECRET_TOKEN` in `deploy.sh`
2. Run the deployment script:

```bash
./deploy.sh
```

**Option B: Manual Deployment**

1. Upload the entire `php/` directory to:
   ```
   https://calltrack.mylistings.in/callcloud/
   ```

2. Ensure this folder structure on your server:
   ```
   /public_html/callcloud/
   ├── config.php
   ├── utils.php
   ├── schema.sql
   ├── init_database.php
   └── api/
       ├── auth.php
       ├── employees.php
       ├── calls.php
       ├── recordings.php
       └── reports.php
   ```

### Step 3: Initialize Database

Visit this URL in your browser to create all database tables:

```
https://calltrack.mylistings.in/callcloud/init_database.php
```

You should see output showing tables being created successfully.

### Step 4: Update Frontend

The frontend API client is already configured to work with your backend. No changes needed! 🎉

The client is located at `src/api/client.ts` and is ready to use.

### Step 5: Test the Setup

1. Start the dev server (if not already running):
   ```bash
   npm run dev
   ```

2. Open http://localhost:5173

3. Try creating an account:
   - Click "Create Account"
   - Fill in organization details
   - Click "Generate" for a unique org ID
   - Complete signup

The backend will:
- ✅ Create your organization in the database
- ✅ Create your admin user account
- ✅ Generate and return an authentication token
- ✅ Store the session for 24 hours

## 📊 Database Schema

The backend creates these tables:

| Table | Purpose |
|-------|---------|
| `organizations` | Organization/company information |
| `users` | Admin users and authentication |
| `employees` | Employee records with departments/roles |
| `calls` | Call history and logs |
| `recordings` | Call recording metadata |
| `settings` | Organization-specific settings |
| `sessions` | Authentication tokens and sessions |

All tables include:
- Proper foreign key relationships
- Indexes for performance
- UTF-8MB4 encoding for emoji support
- Timestamps for tracking

## 🔌 API Endpoints Reference

### Authentication

```typescript
// Signup
const response = await api.signup({
  organizationName: "Acme Corp",
  organizationId: "ABC123",
  adminName: "John Doe",
  email: "john@acme.com",
  password: "password123"
});

// Login
const response = await api.login("john@acme.com", "password123");

// Logout
await api.logout();
```

### Employees

```typescript
// Get all employees
const employees = await api.getEmployees();

// Get statistics
const stats = await api.getEmployeeStats();

// Create employee
const employee = await api.createEmployee({
  name: "Jane Smith",
  email: "jane@acme.com",
  phone: "+1234567890",
  department: "Sales",
  role: "Manager"
});

// Update employee
await api.updateEmployee(employeeId, { department: "Marketing" });

// Delete employee
await api.deleteEmployee(employeeId);
```

### Calls

```typescript
// Get calls with filters
const calls = await api.getCalls({
  direction: "inbound",
  dateFilter: "week",
  search: "John"
});

// Get call statistics
const stats = await api.getCallStats();

// Log a call
const call = await api.createCall({
  employee_id: 1,
  contact_name: "ABC Corp",
  phone_number: "+1234567890",
  direction: "outbound",
  duration: "12:34",
  status: "completed",
  has_recording: true
});
```

### Recordings

```typescript
// Get all recordings
const recordings = await api.getRecordings();

// Search recordings
const results = await api.getRecordings("client meeting");

// Get statistics
const stats = await api.getRecordingStats();

// Create recording
const recording = await api.createRecording({
  employee_id: 1,
  call_id: 5,
  title: "Sales Call - ABC Corp",
  contact_name: "ABC Corporation",
  duration: "12:34",
  file_size: "8.2 MB",
  tags: ["Sales", "Important"]
});
```

### Reports

```typescript
// OverviewReport
const overview = await api.getOverviewReport("month");

// Employee performance
const performance = await api.getEmployeePerformance("quarter");

// Department breakdown
const breakdown = await api.getDepartmentBreakdown("year");

// Call analytics
const analytics = await api.getCallAnalytics("week");
```

## 🔒 Security Features

1. **Password Hashing** - All passwords use bcrypt
2. **Token-based Auth** - JWT-style bearer tokens
3. **SQL Injection Protection** - All inputs escaped
4. **CORS Configuration** - Configurable allowed origins
5. **Session Expiry** - Tokens expire after 24 hours
6. **Organization Isolation** - Users can only access their org's data

## 🛠️ Development

### Testing Locally

The backend uses remote MySQL Manager, so you can test locally:

1. Update `API_BASE_URL` in `src/api/client.ts` to point to your deployed backend
2. Run `npm run dev`
3. All API calls will go to the remote backend

### Adding New Endpoints

1. Create new PHP file in `php/api/`
2. Include config and utils: `require_once '../config.php';`
3. Use `Auth::requireAuth()` for protected endpoints
4. Use `Database::` methods for queries
5. Return data with `Response::success()` or `Response::error()`
6. Add corresponding methods to `src/api/client.ts`

## 📝 Environment Variables

For production, consider creating `.env` file:

```env
API_SECRET_TOKEN=your_secret_token_here
DB_NAME=u542940820_easycalls
MYSQL_MANAGER_URL=https://your-domain.com/ai_mysql_manager.php
```

## 🐛 Troubleshooting

### "Database connection failed"
- Check secret token matches in config.php and MySQL Manager
- Verify MySQL Manager URL is accessible
- Test MySQL Manager independently

### "Unauthorized" errors
- Token might be expired (default 24 hours)
- Check Authorization header format
- Verify token is being sent from frontend

### CORS errors
- Update `CORS_ALLOWED_ORIGINS` in config.php
- Ensure your domain is allowed
- Check browser console for specific error

### Database initialization fails
- Check MySQL credentials in ai_mysql_manager.php
- Verify database exists
- Check for existing tables (reinit may fail)

## 📚 Additional Resources

- **Deployment Guide**: See `BACKEND_DEPLOYMENT.md` for detailed deployment instructions
- **API Documentation**: Full endpoint documentation in deployment guide
- **Database Schema**: See `php/schema.sql` for complete schema

## 🎉 You're All Set!

Your CallCloud Admin Dashboard now has:
- ✅ Complete PHP backend with REST API
- ✅ MySQL database integration
- ✅ Authentication and authorization
- ✅ Full CRUD operations for all entities
- ✅ Analytics and reporting
- ✅ Secure session management
- ✅ Frontend API client ready to use

Just deploy, initialize the database, and start using the dashboard!

---

**Built with ❤️ for CallCloud**
