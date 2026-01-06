# Enterprise Device Controller (Device Admin)

A powerful Android Device Management application designed for enterprise-level device control, EMI enforcement, and theft protection.

## 🚀 Key Features

- **Device Owner Integration**: Uses Android's Device Policy Manager (DPM) for deep system-level control.
- **Enterprise Lock Screen**: A persistent, full-screen lock overlay that prevents user interaction when a device is "Frozen".
- **Self-Defense (Uninstall Protection)**: Blocks uninstallation, force-stopping, and clearing data via Accessibility Services and Device Admin policies.
- **Invisible Stealth Mode**: Disguises the app icon as "My Downloads" and can hide it from the launcher on supported Android versions.
- **Real-time Server Control**: Syncs with a central server (`status.php`) every 15 minutes to update device state (Lock/Unlock/Protect).
- **Master PIN Override**: Built-in master override (`1133`) to bypass locks and clear restrictions for administrators.
- **Auto-Uninstall**: Remotely trigger a silent, complete removal of the application from the server.
- **Hardware Lockdown**: Disables USB debugging, factory resets, safe boot, and status bar expansion to prevent bypasses.

---

## 📱 Screen Functionalities

### 1. **Setup Screen (MainActivity)**
The "Mission Control" for the device administrator.
- **Device Registration**: Assign a unique ID to the device to link it with the server.
- **Permission Management**: 
  - **Device Admin**: Grants system management privileges.
  - **Accessibility**: Enables the "Self-Defense" layer to block settings and uninstall attempts.
  - **Overlay**: Allows the Lock Screen to appear over other apps.
- **Stealth Toggle**: Option to hide the app icon immediately after setup.
- **Admin Zone**: 
  - **Test Lock Screen**: Manually trigger the lock screen to verify functionality.
  - **Remove Protection**: Requires Master PIN to disable all restrictions and allow uninstallation.

### 2. **Lock Screen (LockActivity)**
A secure overlay that activates when the device is "Freezed".
- **Status Display**: Shows pending EMI amounts and custom messages from the server.
- **Update Sync**: A manual "Check Status" button to instantly poll the server for an unlock command.
- **Emergency Call**: Direct button to dial support numbers defined by the server.
- **Master Unlock**: Hidden option to enter the Master PIN (`1133`) to regain full control.
- **Break Request**: Grants a temporary 2-minute "break" from the lock screen using a local timer.

### 3. **The "Stealth" Layer (MainActivityAlias)**
- Once setup is complete, the app changes its identity.
- The default name becomes **"My Downloads"**.
- Clicking the app icon normally opens the system **Downloads** folder to divert suspicion.
- To access the Admin Panel again, one must use the specific launch flag or trigger a server state change.

---

## 🛠 Technical Architecture

### **Communication Layer (`StatusWorker`)**
- A background worker running every 15 minutes.
- Fetches JSON status from `https://api.miniclickcrm.com/admin/status.php`.
- **Response Fields**:
  - `is_freezed`: Activates/Deactivates the Lock Screen.
  - `is_protected`: Enforces/Clears uninstall blocks and USB restrictions.
  - `auto_uninstall`: Triggers a silent `PackageInstaller` session to remove the app.
  - `update_url`: Downloads and silently installs new APK versions.

### **Enforcement Layer (`LockAccessibilityService`)**
- Monitors windows for keywords like "Uninstall", "Deactivate", and "com.example.deviceadmin".
- If a restricted action is detected, it automatically navigates the user **Home** and shows a "Blocked by Administrator" toast.

### **Persistence Layer (`MyDeviceAdminReceiver` & `BootReceiver`)**
- **Boot Persistence**: The `BootReceiver` ensures that if the device was locked before a reboot, the Lock Screen appears immediately upon restart.
- **Admin Persistence**: Prevents the user from deactivating "Device Admin" privileges without the Master PIN.

---

## 🔐 Master Credentials
- **Master PIN**: `1133` (Used for unlocking and removing protection).
- **Default Port**: Communication via standard HTTPS (Port 443).

---

## ⚠️ Requirements
- **Android Version**: 8.0 (Oreo) or higher.
- **Installation**: Must be set as **Device Owner** (via ADB) for maximum functionality (Silent uninstall, Status bar block).
