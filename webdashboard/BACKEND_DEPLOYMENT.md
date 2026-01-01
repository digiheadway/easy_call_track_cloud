# CallCloud Admin Backend - Deployment Guide

## 📦 Overview

This backend provides a complete REST API for the CallCloud Admin Dashboard with MySQL database integration via remote API endpoints.

## 🗂️ Backend Structure

```
php/
├── config.php              # Configuration settings
├── utils.php               # Database, Auth, Response utilities
├── schema.sql              # Database schema
├── init_database.php       # Database initialization script
└── api/
    ├── auth.php           # Authentication endpoints
    ├── employees.php      # Employee management
    ├── calls.php          # Call logs
    ├── recordings.php     # Recording management
    └── reports.php        # Analytics & reports
```

## 🚀 Deployment Steps

### 1. Upload PHP Files

Upload the entire `php/` directory to your server:

```
https://calltrack.mylistings.in/callcloud/
```

Your file structure on the server should be:
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

### 2. Update Configuration

Edit `config.php` and update the `API_SECRET_TOKEN`:

```php
define('API_SECRET_TOKEN', 'YOUR_SECURE_SECRET_TOKEN_HERE');
```

**Important**: Use a strong, random token. This must match the token in your MySQL Manager and File Manager.

### 3. Initialize Database

Run the database initialization script once:

**Via Browser:**
```
https://calltrack.mylistings.in/callcloud/init_database.php
```

**Via Command Line (if SSH access available):**
```bash
cd /public_html/callcloud
php init_database.php
```

This will create all necessary tables:
- organizations
- users
- employees
- calls
- recordings
- settings
- sessions

### 4. Verify Setup

Test the API by accessing:
```
https://calltrack.mylistings.in/callcloud/api/auth.php?action=verify
```

You should get a 401 Unauthorized response (this is correct - it means the API is working).

### 5. Update Frontend Configuration

Update the API base URL in `src/api/client.ts`:

```typescript
const API_BASE_URL = 'https://calltrack.mylistings.in/callcloud/api';
```

## 🔌 API Endpoints

### Authentication

- **POST** `/api/auth.php?action=signup` - Create new account
- **POST** `/api/auth.php?action=login` - User login  
- **POST** `/api/auth.php?action=logout` - User logout
- **GET** `/api/auth.php?action=verify` - Verify token

### Employees

- **GET** `/api/employees.php` - Get all employees
- **GET** `/api/employees.php?action=stats` - Get employee statistics
- **POST** `/api/employees.php` - Create employee
- **PUT** `/api/employees.php?id={id}` - Update employee
- **DELETE** `/api/employees.php?id={id}` - Delete employee

### Calls

- **GET** `/api/calls.php` - Get all calls (supports filters)
- **GET** `/api/calls.php?action=stats` - Get call statistics
- **POST** `/api/calls.php` - Create call log

**Filter Parameters:**
- `direction` - inbound/outbound/all
- `dateFilter` - today/week/month/all
- `search` - search term

### Recordings

- **GET** `/api/recordings.php` - Get all recordings
- **GET** `/api/recordings.php?action=stats` - Get recording statistics
- **POST** `/api/recordings.php` - Create recording
- **DELETE** `/api/recordings.php?id={id}` - Delete recording

### Reports

- **GET** `/api/reports.php?type=overview&dateRange=week` - Overview report
- **GET** `/api/reports.php?type=employee&dateRange=month` - Employee performance
- **GET** `/api/reports.php?type=department&dateRange=year` - Department breakdown
- **GET** `/api/reports.php?type=calls&dateRange=today` - Call analytics

**Report Types:** `overview`, `employee`, `department`, `calls`  
**Date Ranges:** `today`, `week`, `month`, `quarter`, `year`, `all`

## 🔐 Authentication

All API endpoints (except signup/login) require authentication via Bearer token:

```
Authorization: Bearer {token}
```

The token is automatically managed by the frontend API client and stored in localStorage.

## 📝 Example API Calls

### Signup
```javascript
POST /api/auth.php?action=signup
Content-Type: application/json

{
  "organizationName": "Acme Corp",
  "organizationId": "ABC123",
  "adminName": "John Doe",
  "email": "john@acme.com",
  "password": "securepass123"
}
```

### Login
```javascript
POST /api/auth.php?action=login
Content-Type: application/json

{
  "email": "john@acme.com",
  "password": "securepass123"
}
```

### Create Employee
```javascript
POST /api/employees.php
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "Jane Smith",
  "email": "jane@acme.com",
  "phone": "+1234567890",
  "department": "Sales",
  "role": "Sales Manager"
}
```

### Get Call Statistics
```javascript
GET /api/calls.php?action=stats
Authorization: Bearer {token}
```

## 🛠️ Frontend Integration

The frontend already has the API client configured. Simply import and use:

```typescript
import api from './api/client';

// Login
const response = await api.login('user@example.com', 'password');

// Get employees
const employees = await api.getEmployees();

// Create employee
const newEmployee = await api.createEmployee({
  name: 'Jane Doe',
  email: 'jane@example.com',
  phone: '+1234567890',
  department: 'Sales',
  role: 'Manager'
});
```

## 🐛 Troubleshooting

### CORS Issues
If you encounter CORS errors, verify that `CORS_ALLOWED_ORIGINS` in `config.php` is set correctly:
```php
define('CORS_ALLOWED_ORIGINS', '*'); // or specific domain
```

### Database Connection Failed
- Verify the secret token matches in both `config.php` and the MySQL Manager
- Check that MySQL Manager URL is correct
- Ensure database credentials are valid

### 401 Unauthorized
- Token might be expired (default: 24 hours)
- Token might be invalid
- Check Authorization header format: `Bearer {token}`

### File Upload Issues
- Check `MAX_FILE_SIZE` in config.php
- Verify server PHP upload limits
- Ensure `upload_max_filesize` and `post_max_size` in php.ini are sufficient

## 📊 Database Schema

The database includes the following tables:

1. **organizations** - Organization/company data
2. **users** - Admin users and authentication
3. **employees** - Employee records
4. **calls** - Call logs and history
5. **recordings** - Call recordings metadata
6. **settings** - Organization settings
7. **sessions** - Authentication sessions

All tables are automatically created by `init_database.php`.

## 🔒 Security Notes

1. **Change the secret token** - Never use the default token in production
2. **Use HTTPS** - Always deploy with SSL/TLS enabled
3. **Password Security** - Passwords are hashed using bcrypt
4. **Token Expiry** - Sessions expire after 24 hours (configurable)
5. **SQL Injection** - All inputs are escaped via `Database::escape()`
6. **CORS** - Configure allowed origins in production

## 📈 Performance Tips

1. Database queries are optimized with indexes
2. Use date range filters to limit result sets
3. Recording file paths are stored in DB, not file contents
4. Session cleanup can be automated via cron:
   ```sql
   DELETE FROM sessions WHERE expires_at < NOW()
   ```

## 🆘 Support

For issues or questions:
1. Check error logs in your hosting control panel
2. Verify all file permissions are correct (644 for PHP files)
3. Test MySQL Manager endpoint independently
4. Review browser console for detailed error messages

---

**🎉 Your CallCloud Admin backend is ready to use!**
