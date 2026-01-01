# Pairing Code Verification & Device Enforcement Implementation

## Overview
This implementation adds pairing code verification and enforces that one employee can only run the app on one device at a time.

## Changes Made

### 1. Database Schema Changes
**File:** `webdashboard/php/schema.sql`

- Added `device_id` column to the `employees` table
- Added index on `device_id` for faster lookups
- This stores the unique device identifier for each employee

### 2. Backend API Updates
**File:** `webdashboard/php/api/sync_app.php`

#### New Action: `verify_pairing_code`
- Validates that the employee exists with the given `org_id` and `user_id`
- Checks if the device is already linked to another employee in the same organization
- Links the device to the employee on first verification
- Returns employee name on successful verification
- Returns appropriate errors for:
  - Invalid pairing code
  - Device already linked to another employee
  - Employee already linked to another device

#### Updated Action: `start_call`
- Now requires `device_id` parameter
- Verifies that the device_id matches the one registered for the employee
- Rejects calls from unauthorized devices
- This ensures only the paired device can upload call data

### 3. Android Network Layer
**File:** `app/src/main/java/com/calltracker/manager/network/CallCloudApi.kt`

- Added `verifyPairingCode()` API endpoint
- Updated `startCall()` to include `device_id` parameter

### 4. Android Worker Updates
**File:** `app/src/main/java/com/calltracker/manager/worker/UploadWorker.kt`

- Fetches device ID using `Settings.Secure.ANDROID_ID`
- Passes device ID to `startCall()` API
- This ensures all call uploads include device verification

### 5. Android ViewModel Updates
**File:** `app/src/main/java/com/calltracker/manager/ui/settings/SettingsViewModel.kt`

#### State Changes:
- Added `isVerifying: Boolean` to `SettingsUiState`
- Used to show loading indicator during verification

#### Logic Changes:
- `updatePairingCode()`: Now only updates UI state, doesn't save to repository
- `saveAccountInfo()`: 
  - Validates pairing code format
  - Calls `verifyPairingCode` API before saving
  - Only saves to repository after successful verification
  - Shows employee name in success message
  - Calls `onSuccess()` callback to close modal
  - Shows appropriate error messages for failures
- `leaveOrganisation()`: New method to clear organization data without verification

### 6. Android UI Updates
**File:** `app/src/main/java/com/calltracker/manager/ui/settings/SettingsScreen.kt`

#### AccountInfoModal:
- Button text changed from "Save Information" to "Save & Verify"
- Shows loading spinner with "Verifying..." text during verification
- Button is disabled during verification
- Modal auto-closes on successful verification
- Passes `onSuccess` callback to trigger modal dismissal

#### Leave Organisation:
- Now uses `leaveOrganisation()` method instead of `saveAccountInfo()`
- Properly clears organization without verification

## Security Features

1. **Device Binding**: Each employee account is bound to a specific device ID
2. **Pre-verification**: Pairing code is verified before any data is saved
3. **Call Upload Protection**: Every call upload is verified against the registered device
4. **One Device Per Employee**: An employee cannot use the app on multiple devices simultaneously
5. **One Employee Per Device**: A device cannot be used by multiple employees in the same organization

## User Flow

### First Time Setup:
1. User enters pairing code (ORGID-USERID format)
2. User enters at least one phone number
3. User clicks "Save & Verify"
4. App shows "Verifying..." loading state
5. Backend checks if employee exists and if device is already linked
6. If successful:
   - Device is linked to employee account
   - Settings are saved locally
   - Success message shows employee name
   - Modal closes automatically
7. If failed:
   - Error message is displayed
   - Settings are NOT saved
   - User can correct and retry

### Subsequent Use:
1. App automatically sends device_id with every call upload
2. Backend verifies device_id matches the registered one
3. Unauthorized devices are rejected

### Switching Devices:
- User must contact admin to reset their device_id in the database
- This prevents unauthorized device switching

## Error Messages

- "Pairing code invalid (ORGID and USERID required)"
- "Employee not found or invalid organization"
- "This device is already registered to another employee in this organization"
- "This employee account is already linked to another device. Please contact admin to reset"
- "Unauthorized: This device is not linked to this employee account"
- "Invalid employee or organization"

## Testing Checklist

- [ ] Verify pairing code with valid credentials on new device
- [ ] Try to pair same employee on two different devices (should fail)
- [ ] Try to pair two employees on same device in same org (should fail)
- [ ] Try to upload calls without verification (should fail)
- [ ] Leave organization and rejoin
- [ ] Test with invalid pairing code format
- [ ] Test network error handling
- [ ] Test loading states in UI

## Database Migration

To add the new column to existing databases, run:

```sql
ALTER TABLE employees 
ADD COLUMN device_id VARCHAR(255) DEFAULT NULL,
ADD INDEX idx_device_id (device_id);
```

Or re-run the schema initialization script.
