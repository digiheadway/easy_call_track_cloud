# Deployment Path Update - Root Directory

## Date: 2026-01-01

## Overview
Updated all deployment configurations to deploy backend API files to the root directory instead of `/callcloud/` subdirectory.

---

## Changes Made

### 1. Android App - Network Client
**File:** `app/src/main/java/com/calltracker/manager/network/NetworkClient.kt`

**No changes needed** - Already configured for root path:
```kotlin
private const val BASE_URL = "https://calltrack.mylistings.in/api/"
```

This will call: `https://calltrack.mylistings.in/api/sync_app.php` ✓

---

### 2. Deployment Scripts

#### deploy.sh
**File:** `webdashboard/deploy.sh`

**Change:**
```bash
# Before
REMOTE_BASE_PATH="/callcloud"

# After
REMOTE_BASE_PATH=""  # Empty = root directory
```

#### deploy.cjs
**File:** `webdashboard/deploy.cjs`

**Change:**
```javascript
// Before
REMOTE_BASE_PATH: '/callcloud',

// After
REMOTE_BASE_PATH: '', // Empty = root directory
```

#### deploy-backend.js
**File:** `webdashboard/deploy-backend.js`

**Change:**
```javascript
// Before
REMOTE_BASE_PATH: '/callcloud'

// After
REMOTE_BASE_PATH: '' // Empty = root directory
```

---

## Deployment Structure

### Before (Subdirectory)
```
https://calltrack.mylistings.in/
├── callcloud/
│   ├── config.php
│   ├── utils.php
│   ├── schema.sql
│   ├── init_database.php
│   └── api/
│       ├── auth.php
│       ├── employees.php
│       ├── calls.php
│       ├── recordings.php
│       ├── reports.php
│       ├── sync_app.php
│       └── contacts.php
```

### After (Root Directory)
```
https://calltrack.mylistings.in/
├── config.php
├── utils.php
├── schema.sql
├── init_database.php
├── ai_file_manager.php (already at root)
├── ai_mysql_manager.php (already at root)
└── api/
    ├── auth.php
    ├── employees.php
    ├── calls.php
    ├── recordings.php
    ├── reports.php
    ├── sync_app.php
    ├── contacts.php
    └── numbers.php
```

---

## API Endpoints

### Android App Calls
```
https://calltrack.mylistings.in/api/sync_app.php
```

### Web Dashboard Calls
```
https://calltrack.mylistings.in/api/auth.php
https://calltrack.mylistings.in/api/employees.php
https://calltrack.mylistings.in/api/calls.php
https://calltrack.mylistings.in/api/recordings.php
https://calltrack.mylistings.in/api/reports.php
https://calltrack.mylistings.in/api/contacts.php
https://calltrack.mylistings.in/api/numbers.php
```

### Utility APIs (Root)
```
https://calltrack.mylistings.in/ai_file_manager.php
https://calltrack.mylistings.in/ai_mysql_manager.php
https://calltrack.mylistings.in/init_database.php
```

---

## Deployment Commands

### Deploy Backend Only
```bash
node deploy-backend.js
```

This will:
1. Upload PHP config and utility files to root
2. Upload all API files to `/api/` folder
3. Create necessary folder structure

### Deploy Full Stack (Backend + Frontend)
```bash
node deploy.cjs
```

This will:
1. Build frontend (React app)
2. Upload backend PHP files to root
3. Upload frontend build to appropriate location

---

## Benefits of Root Deployment

1. **Simpler URLs**: No need for `/callcloud/` in paths
2. **Cleaner Structure**: Files at root level
3. **Easier Maintenance**: Less path configuration
4. **Consistency**: Matches existing utility files location

---

## Next Steps

After updating deployment configs:

1. **Re-deploy backend:**
   ```bash
   node deploy-backend.js
   ```

2. **Test API endpoints:**
   - Android app: Join Organisation modal
   - Web dashboard: Login and data fetching

3. **Verify file locations** on server using file manager:
   ```
   https://calltrack.mylistings.in/ai_file_manager.php
   ```

---

## Notes

- File manager (`ai_file_manager.php`) was already at root
- MySQL manager (`ai_mysql_manager.php`) was already at root
- Only moving PHP backend files from `/callcloud/` to root
- Frontend web app deployment location unchanged

---

## Summary

All deployment scripts now deploy to root directory for cleaner, simpler structure:

- ✅ `deploy.sh` → Root deployment
- ✅ `deploy.cjs` → Root deployment
- ✅ `deploy-backend.js` → Root deployment
- ✅ Android app → Already configured for root `/api/`

Ready to deploy! 🚀
