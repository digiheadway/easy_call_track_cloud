# Project Requirements: Device Admin Protection System

## 1. Project Overview
The **Device Admin Protection System** is an Android application designed for financial institutions and loan companies to protect their assets (mobile devices). The app ensures that borrowers adhere to loan terms by providing remote management and locking capabilities.

The core goal is to prevent device misuse, unauthorized uninstallation, and to provide a "kill-switch" if a loan goes into default.

---

## 2. Target Platform
*   **OS Support:** Android 7.0 (API Level 24) and above.
*   **Device Compatibility:** Optimized for all major Android OEMs.

---

## 3. Installation & Deployment
The app supports two primary deployment methods:
1.  **Fresh Device Setup:** Integration during the initial device setup (Work Managed Device / Device Owner mode via QR code or Zero-touch).
2.  **Standard Installation:** Manual installation on an already active device (Legacy Device Admin mode).

---

## 4. Functional Requirements

### 4.1 Locking Mechanisms
*   **Remote Lock:** Triggered via Management API or Firebase Cloud Messaging (FCM).
*   **Auto Rules:** System-side rules that trigger a lock (e.g., date-based, lost connectivity for X days, or failed check-ins).
*   **Temporal Unlock:** Capability to unlock the device for a specific duration (e.g., 2 hours for payment processing) before automatically re-locking.

### 4.2 Unlocking Mechanisms
*   **Remote Unlock:** Sent via API/FCM.
*   **SMS-Based Unlock:**
    *   Device receives an encrypted SMS command.
    *   Support for One-Time Use Codes (OTCs).
    *   System can store batches of 10 codes locally for offline emergency use.
    *   Key-pair/2FA encryption for secure code generation.
*   **Manual Unlock (User Side):**
    *   Borrower can enter a recovery key.
    *   Constraint: Maximum 3 attempts per hour to prevent brute-forcing.

### 4.3 Manager Dashboard & Monitoring
*   **Device Status:** Real-time pairing code check for status verification.
*   **Permission Audit:** Monitor if required permissions (Admin, SMS, etc.) are active.
*   **Last Active Status:** Timestamp of the last heart-beat/sync.
*   **Safety Rating:** Aggregate score based on device health, security settings, and connectivity.
*   **Communication:** Send custom messages directly to the device lock screen.

---

## 5. Security & System Hardening (Protection Mode: ON)

When protection is active, the app must enforce "Super Protection" to prevent bypass:

### 5.1 System Level Restrictions
*   **Disable Uninstallation:** App cannot be uninstalled or removed from Device Administrators.
*   **Data Integrity:** Disable "Clear Data" and "Clear Cache" in System Settings.
*   **Settings Lockdown:** Block access to critical settings:
    *   USB Debugging (Developer Options).
    *   App Manager / Package Manager.
    *   Factory Reset (optional based on policy).
*   **Permission Locking:** Prevent the user from toggling off critical permissions.

### 5.2 Lock Screen Behavior (When Locked)
*   **Kiosk Mode:** User cannot minimize or close the app.
*   **Hardware Interruption:** 
    *   Disable/Override "Power Button Hold" menu (to prevent unauthorized shutdown/restart).
    *   Block "Volume Button" combinations.
*   **Visual Indicators:** Set custom "Locked" wallpaper.
*   **Persistence:** If the device restarts, the app must auto-launch immediately (Foreground/Background).
*   **Battery Optimization:** Enforce "Save Battery Mode" when locked.

### 5.3 Lock Screen Interactive Options
While the device is locked, the user should ONLY see:
1.  **Current Status:** Why the device is locked.
2.  **Manager Message:** Custom text from the lender.
3.  **Check Status:** Button to refresh status from server.
4.  **Contact Manager:** Direct button to call the manager’s phone number.
5.  **Request Unlock:** Notify manager via API to request a temporary unlock.
6.  **Manual Unlock:** Entry field for 2FA/Recovery keys.

---

## 6. Protection Mode: OFF
*   When Protection is toggled OFF by the manager:
    *   The app behaves like a normal utility.
    *   The user can uninstall the app easily.
    *   All system restrictions (USB debugging, power menu) are lifted.

---

## 7. Technical Infrastructure
*   **Messaging:** Firebase Cloud Messaging (FCM) or high-priority MQTT/Websockets.
*   **Security:** Encryption (AES/RSA) for SMS communication and pairing codes.
*   **Persistence:** `BOOT_COMPLETED` receivers and Foreground Services for high availability.
*   **API:** RESTful API for fetching device info and posting status updates.
