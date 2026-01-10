# Context Files - DeviceAdmin3

This document provides a quick reference to all files in the project with their purpose and key contents.

---

## 📱 Android App Files

### Core Activities

| File | Purpose |
|------|---------|
| `MainActivity.kt` | Main dashboard with admin controls, pairing code input, FCM token display |
| `LockScreenActivity.kt` | Full-screen kiosk lock, blocks back/home buttons, unlock code entry |

### API Layer

| File | Purpose |
|------|---------|
| `api/ApiService.kt` | Retrofit interface - `checkStatus(pairingcode, fcm_token)` |
| `api/RetrofitClient.kt` | HTTP client singleton, BASE_URL: `https://api.miniclickcrm.com/admin/` |

### Firebase

| File | Purpose |
|------|---------|
| `fcm/MyFirebaseMessagingService.kt` | Handles FCM push: LOCK, UNLOCK, TEMPORAL_UNLOCK, REMOVE_PROTECTION |
| `app/google-services.json` | Firebase config (project: deviceadmin-d549b) |

### Managers

| File | Purpose |
|------|---------|
| `manager/DeviceManager.kt` | Wraps DevicePolicyManager - lock, uninstall block, user restrictions |
| `manager/SecurityManager.kt` | OTC storage, recovery key verification |
| `manager/AutoRulesEngine.kt` | Placeholder for auto-lock rules |

### Receivers

| File | Purpose |
|------|---------|
| `receiver/MyAdminReceiver.kt` | Device Admin callbacks (enabled/disabled) |
| `receiver/BootReceiver.kt` | Starts MainService on BOOT_COMPLETED |
| `receiver/SmsReceiver.kt` | SMS commands: LOCK_DEVICE_FORCE, UNLOCK_DEVICE_FORCE, REMOVE_PROTECTION_FORCE |

### Services

| File | Purpose |
|------|---------|
| `service/MainService.kt` | Foreground service for persistence |

### Utilities

| File | Purpose |
|------|---------|
| `utils/FcmTokenManager.kt` | Get/save FCM token |
| `utils/StatusReport.kt` | Safety rating calculator |

### Resources

| File | Purpose |
|------|---------|
| `res/xml/device_admin_receiver.xml` | Device admin policies (lock, wipe, etc.) |
| `res/values/strings.xml` | App name, admin label/description |
| `res/values/themes.xml` | App theme + fullscreen lock theme |

### Configuration

| File | Purpose |
|------|---------|
| `AndroidManifest.xml` | Permissions, activities, receivers, services |
| `build.gradle.kts` (app) | Dependencies, SDK versions |
| `gradle/libs.versions.toml` | Version catalog |

---

## 🖥️ PHP Server Files

| File | Purpose |
|------|---------|
| `phpserver/check.php` | Device status check, FCM token registration |
| `phpserver/pushchanges.php` | Send FCM push notifications to devices |
| `phpserver/device_tokens.json` | Stores pairing codes with FCM tokens |

---

## 📚 Documentation Files

| File | Purpose |
|------|---------|
| `PROJECT_REQUIREMENTS.md` | Original project requirements |
| `docs/PROJECT_STATUS.md` | Current status, structure, pending, issues |
| `docs/API_REFERENCE.md` | Full API documentation |
| `docs/PHP_SERVER.md` | PHP server setup & usage |
| `docs/CONTEXT_FILES.md` | This file - quick reference |

---

## 🔑 Key Constants & Values

### API Endpoint
```
https://api.miniclickcrm.com/admin/check.php
```

### FCM Commands
- `LOCK_DEVICE`
- `UNLOCK_DEVICE`
- `TEMPORAL_UNLOCK`
- `REMOVE_PROTECTION`

### SMS Commands
- `LOCK_DEVICE_FORCE`
- `UNLOCK_DEVICE_FORCE`
- `REMOVE_PROTECTION_FORCE`

### Default Recovery Key
```
000000
```

### Sample Pairing Codes
- `1120` - is_freezed: true, is_protected: true
- `1121` - is_freezed: false, is_protected: true
- `1122` - is_freezed: true, is_protected: false
- `1123` - is_freezed: false, is_protected: false
- `1124` - is_freezed: true, is_protected: true

---

## 📁 Full Directory Tree

```
DeviceAdmin3/
├── app/
│   ├── build.gradle.kts
│   ├── google-services.json
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/miniclickcrm/deviceadmin3/
│       │   ├── MainActivity.kt
│       │   ├── LockScreenActivity.kt
│       │   ├── api/
│       │   │   ├── ApiService.kt
│       │   │   └── RetrofitClient.kt
│       │   ├── fcm/
│       │   │   └── MyFirebaseMessagingService.kt
│       │   ├── manager/
│       │   │   ├── AutoRulesEngine.kt
│       │   │   ├── DeviceManager.kt
│       │   │   └── SecurityManager.kt
│       │   ├── receiver/
│       │   │   ├── BootReceiver.kt
│       │   │   ├── MyAdminReceiver.kt
│       │   │   └── SmsReceiver.kt
│       │   ├── service/
│       │   │   └── MainService.kt
│       │   └── utils/
│       │       ├── FcmTokenManager.kt
│       │       └── StatusReport.kt
│       └── res/
│           ├── values/
│           │   ├── strings.xml
│           │   └── themes.xml
│           └── xml/
│               └── device_admin_receiver.xml
├── docs/
│   ├── API_REFERENCE.md
│   ├── CONTEXT_FILES.md
│   ├── PHP_SERVER.md
│   └── PROJECT_STATUS.md
├── phpserver/
│   ├── check.php
│   ├── device_tokens.json
│   └── pushchanges.php
├── gradle/
│   └── libs.versions.toml
├── build.gradle.kts
├── settings.gradle.kts
└── PROJECT_REQUIREMENTS.md
```
