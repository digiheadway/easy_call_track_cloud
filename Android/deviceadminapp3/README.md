# Device Admin App v3 - Clean Architecture Edition

A powerful Android Device Management application designed for enterprise-level device control, EMI enforcement, and theft protection. **Completely rewritten from scratch with clean architecture principles.**

## 🚀 Key Features

- **Device Owner Integration**: Uses Android's Device Policy Manager (DPM) for deep system-level control
- **Enterprise Lock Screen**: Persistent full-screen lock overlay when device is "Frozen"
- **Self-Defense (Uninstall Protection)**: Blocks uninstallation via Accessibility Services and Device Admin policies
- **Invisible Stealth Mode**: Disguises the app icon as "My Downloads"
- **Real-time Server Control**: Syncs with central server every 15 minutes
- **Master PIN Override**: Built-in master override (`1133`) for administrators
- **Hardware Lockdown**: Disables USB debugging, factory resets, safe boot, status bar

---

## 📁 Project Architecture

```
app/src/main/java/com/deviceadmin/app/
├── DeviceAdminApplication.kt          # Application class
│
├── data/                              # Data Layer
│   ├── local/
│   │   └── PreferencesManager.kt      # SharedPreferences wrapper
│   ├── model/
│   │   ├── DeviceState.kt             # Domain models & enums
│   │   └── DeviceStatusResponse.kt    # API response model
│   ├── remote/
│   │   ├── ApiClientFactory.kt        # Retrofit client factory
│   │   └── DeviceApiService.kt        # API interface
│   └── repository/
│       └── DeviceRepository.kt        # Single source of truth
│
├── receiver/                          # Broadcast Receivers
│   ├── BootReceiver.kt                # Boot completion handler
│   ├── DeviceAdminReceiver.kt         # Device admin lifecycle
│   └── SecretCodeReceiver.kt          # Hidden admin access (*#*#1133#*#*)
│
├── service/
│   └── ProtectionAccessibilityService.kt  # Self-defense & lock enforcement
│
├── ui/                                # UI Layer
│   ├── lock/
│   │   └── LockScreenActivity.kt      # Lock screen UI
│   └── setup/
│       └── SetupActivity.kt           # Setup/Admin panel
│
├── util/                              # Utilities
│   ├── AppIconManager.kt              # Icon visibility management
│   ├── Constants.kt                   # App-wide constants
│   ├── DevicePolicyHelper.kt          # DPM operations wrapper
│   └── PermissionHelper.kt            # Permission checks
│
└── worker/
    └── StatusSyncWorker.kt            # Background sync worker
```

---

## 🎯 Key Improvements Over Previous Version

### Clean Architecture
- **Repository Pattern**: Single source of truth for all device state
- **Clear Separation**: Data, UI, and utility layers are properly separated
- **Dependency Injection Ready**: Easy to add DI framework later

### Type Safety
- **Enum Classes**: `PhoneState` and `ProtectionState` replace string comparisons
- **Null Safety**: Proper Kotlin null handling throughout
- **Data Classes**: Immutable data models

### Code Quality
- **Single Responsibility**: Each class has one clear purpose
- **Constants Object**: All magic values centralized
- **Extension Functions**: Cleaner code with Kotlin idioms
- **Coroutines**: Proper async/await instead of raw threads

### Android Best Practices
- **Activity Result APIs**: Modern `registerForActivityResult()` instead of deprecated methods
- **Lifecycle-aware**: CoroutineScope tied to lifecycle
- **WorkManager**: Proper background task scheduling
- **Material 3**: Updated theme and components

---

## 📱 Screen Functionalities

### 1. **Setup Screen (SetupActivity)**
- Device Registration: Assign unique ID to link with server
- Permission Management: Device Admin, Accessibility, Overlay
- Stealth Toggle: Option to hide app icon after setup
- Admin Zone: Test lock screen, remove protection

### 2. **Lock Screen (LockScreenActivity)**
- Status Display: Shows EMI amounts and custom messages
- Check Status: Manual server status check
- Emergency Call: Direct button to dial support
- Master Unlock: Hidden PIN (`1133`) to regain control
- Break Request: Temporary 2-minute break

---

## 🔐 Security

- **Master PIN**: `1133`
- **Secret Dial Code**: `*#*#1133#*#*`
- **Server Endpoint**: `https://api.miniclickcrm.com/admin/status.php`

---

## ⚙️ Requirements

- **Android Version**: 8.0 (Oreo, API 26) or higher
- **Target SDK**: 34 (Android 14)
- **For Full Features**: Must be set as **Device Owner** via ADB:
  ```bash
  adb shell dpm set-device-owner com.deviceadmin.app/.receiver.DeviceAdminReceiver
  ```

---

## 🛠️ Building

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

---

## 📝 License

Proprietary - Enterprise Use Only
