# Device Admin Protection System - Project Documentation

## Table of Contents
1. [Project Overview](#project-overview)
2. [Project Structure](#project-structure)
3. [Current Status](#current-status)
4. [Pending Changes](#pending-changes)
5. [Known Issues](#known-issues)

---

## Project Overview

The **Device Admin Protection System** is an Android application designed for financial institutions and loan companies to protect their assets (mobile devices). The app ensures that borrowers adhere to loan terms by providing remote management and locking capabilities.

### Core Features
- ✅ Remote device locking/unlocking via FCM & SMS
- ✅ Device Admin integration for system-level control
- ✅ Kiosk-style lock screen that cannot be bypassed
- ✅ Temporal unlock (unlock for X minutes, then auto-lock)
- ✅ Protection mode (prevent uninstallation)
- ✅ API-based status checking with pairing codes
- ✅ FCM token registration for push notifications
- ✅ Boot persistence (auto-start on reboot)

### Target Platform
- **Minimum SDK**: Android 7.0 (API 24)
- **Target SDK**: Android 14 (API 35)

---

## Project Structure

```
DeviceAdmin3/
├── app/
│   ├── build.gradle.kts          # App-level Gradle config
│   ├── google-services.json      # Firebase configuration
│   └── src/main/
│       ├── AndroidManifest.xml   # App manifest with permissions
│       ├── java/com/miniclickcrm/deviceadmin3/
│       │   ├── MainActivity.kt           # Main dashboard UI
│       │   ├── LockScreenActivity.kt     # Full-screen lock UI
│       │   ├── api/
│       │   │   ├── ApiService.kt         # Retrofit API interface
│       │   │   └── RetrofitClient.kt     # HTTP client singleton
│       │   ├── fcm/
│       │   │   └── MyFirebaseMessagingService.kt  # FCM handler
│       │   ├── manager/
│       │   │   ├── AutoRulesEngine.kt    # Auto-lock rules (placeholder)
│       │   │   ├── DeviceManager.kt      # Device policy wrapper
│       │   │   └── SecurityManager.kt    # Code verification
│       │   ├── receiver/
│       │   │   ├── BootReceiver.kt       # Boot completed handler
│       │   │   ├── MyAdminReceiver.kt    # Device admin receiver
│       │   │   └── SmsReceiver.kt        # SMS command handler
│       │   ├── service/
│       │   │   └── MainService.kt        # Foreground service
│       │   └── utils/
│       │       ├── FcmTokenManager.kt    # FCM token management
│       │       └── StatusReport.kt       # Safety rating calculator
│       └── res/
│           ├── values/
│           │   ├── strings.xml
│           │   └── themes.xml
│           └── xml/
│               └── device_admin_receiver.xml  # Admin policies
│
├── phpserver/                    # Server-side PHP files
│   ├── check.php                 # Device status & token registration
│   ├── pushchanges.php           # Send FCM push notifications
│   ├── device_tokens.json        # Token storage (auto-generated)
│   └── README.md                 # API documentation
│
├── docs/                         # Project documentation
│   ├── PROJECT_STATUS.md         # This file
│   └── API_REFERENCE.md          # API documentation
│
├── gradle/
│   └── libs.versions.toml        # Dependency version catalog
├── build.gradle.kts              # Root Gradle config
├── settings.gradle.kts
└── PROJECT_REQUIREMENTS.md       # Original requirements
```

---

## Current Status

### ✅ Completed Features

| Feature | Status | Notes |
|---------|--------|-------|
| Device Admin Receiver | ✅ Done | Full policy support |
| Lock Screen Activity | ✅ Done | Kiosk mode, blocks back/home |
| FCM Integration | ✅ Done | Push notifications working |
| SMS Remote Control | ✅ Done | LOCK/UNLOCK/REMOVE commands |
| API Status Check | ✅ Done | With FCM token registration |
| Boot Persistence | ✅ Done | Auto-start on reboot |
| Foreground Service | ✅ Done | Keeps app alive |
| Protection Mode | ✅ Done | Block uninstallation |
| Temporal Unlock | ✅ Done | Time-limited unlock |
| PHP Server API | ✅ Done | check.php, pushchanges.php |

### 🔧 Configuration Required

| Item | Location | Action Needed |
|------|----------|---------------|
| Firebase | `app/google-services.json` | ✅ Already configured |
| API URL | `api/RetrofitClient.kt` | ✅ Set to api.miniclickcrm.com |
| FCM Server Key | `phpserver/pushchanges.php` | ✅ Key added |

---

## Pending Changes

### High Priority
1. **Device Owner Provisioning**
   - Need QR code generation for fresh device setup
   - ADB command documentation for testing

2. **Pairing Code Storage**
   - App should persist pairing code locally
   - Auto-check status on app launch

3. **Contact Manager Button**
   - Wire up the `call_to` number from API
   - Open dialer when tapped

### Medium Priority


5. **Auto Rules Engine**
   - Placeholder exists, needs implementation
   - Date-based auto-lock
   - Connectivity timeout lock

6. **Custom Lock Screen Message**
   - Pass `message` from API to lock screen
   - Display manager's custom text

### Low Priority
7. **Permission Request Flow**
   - Proper runtime permission handling
   - Battery optimization exemption request

8. **Lock Screen Wallpaper**
   - Custom wallpaper when locked
   - Currently uses gray background

---

## Known Issues

### 🔴 Critical
- None currently

### 🟡 Medium
1. **SMS Receiver Priority**
   - May not intercept SMS on some OEMs
   - Need ordered broadcast testing

2. **Device Owner Mode Only**
   - `setUninstallBlocked()` only works in Device Owner mode
   - Standard admin can still be removed by user

### 🟢 Minor
3. **UI Polish**
   - Dashboard could use better styling
   - Lock screen needs branding

4. **Logging**
   - Add proper logging framework
   - Currently using basic Log.d()

---

## Testing Checklist

- [ ] Install app on test device
- [ ] Activate Device Admin
- [ ] Enter pairing code (e.g., 1120)
- [ ] Verify FCM token appears in dashboard
- [ ] Test "TEST LOCK SCREEN" button
- [ ] Verify unlock code works (default: 000000)
- [ ] Test FCM push: `pushchanges.php?command=LOCK_DEVICE&pairingcode=1120`
- [ ] Test SMS command: Send "LOCK_DEVICE_FORCE" to device
- [ ] Reboot device, verify app auto-starts
- [ ] Test Remove Protection command

---

## Build & Deploy

### Android App
```bash
cd DeviceAdmin3
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

### PHP Server
Upload contents of `phpserver/` to:
```
https://api.miniclickcrm.com/admin/
```

Set permissions:
```bash
chmod 644 check.php pushchanges.php
chmod 666 device_tokens.json
```
