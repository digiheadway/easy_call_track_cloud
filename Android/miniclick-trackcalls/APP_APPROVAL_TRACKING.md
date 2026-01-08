# App Approval Process Tracking

This document tracks the issues raised by the Google Play Store review team, proposed solutions, and the final assets required for approval.

---

## 🚩 Current Review Issues (Jan 2026)

### 1. Permissions and APIs that Access Sensitive Information policy: Unable to verify core functionality of app
*   **Feedback:** Your declared that your permission use case is the core functionality of your app. However, after review, we found that your app does not match the declared use case(s). 
*   **Details:** Your in-app experience does not match the core functionality for your declared use case: **Enterprise archive, enterprise CRM, and / or enterprise device management**.
*   **Required Action:** Either change the app to meet the declared core functionality or select a use case that matches the app's functionality.

### 2. Permissions and APIs that Access Sensitive Information policy: Requested permissions do not match core functionality of the app
*   **Feedback:** Your store listing information does not match the core functionality for your declared use case. 
*   **Details:** Declared use case: **Default Phone handler** (and any other core functionality usage while default handler), **Caller ID, spam detection, and /or spam blocking**.
*   **Required Action:** Address the mismatch between the store listing information and the app's core functionality.

### 3. Permissions and APIs that Access Sensitive Information policy: Unable to trigger default handler prompt prior to runtime permissions
*   **Feedback:** If your app is a default handler, the default handler prompt must precede the runtime permission prompt.
*   **Details:** Your app doesn’t appear to properly trigger the default handler prompt prior to the runtime permissions prompt.
*   **Required Action:** Add the default handler prompt prior to any runtime permissions prompt.

---

## 🛠 Proposed Solutions & Action Plan

### A. Technical Fix: Default Dialer Prompt Order (Addressing Issue 3)
Google strict policy requires that if an app requests the `ROLE_DIALER` (Default Phone Handler), it must ask for this role **BEFORE** asking for runtime permissions like `READ_CALL_LOG` or `READ_CONTACTS`.

**Implemented Status:** ✅ Completed in `SetupGuide.kt`.
1.  **Logic:** The "Set as Default Dialer" step is now position #1.
2.  **Flow:** Runtime permissions are only requested after the Default Dialer role is handled.

### B. Use Case Realignment
*   **Action:** Uncheck "Enterprise CRM" and "Spam Detection" if not applicable.
*   **Focus:** Align Store Listing with "Call Tracking", "Smart Dialing", and "History Organization".

---

## 📝 1. Optimized Play Store Description

### A. Rich Text Version (For Reading)
**App Title:** MiniClick Calls: Smart Dialer & Tracker

**Short Description:**
Smart Dialer with call tracking, notes, and cloud organization for your business.

**Full Description:**
MiniClick Calls is your intelligent companion for professional call management. Designed for individuals and teams who need a better way to track, organize, and archive their communications, MiniClick combines a high-performance Dialer with powerful organization tools.

**Why MiniClick Calls?**
To provide a seamless experience, MiniClick can be set as your **Default Phone Handler**. This allows you to manage calls directly through our clean, intuitive interface while unlocking advanced features like real-time notes and smart history.

**Key Features:**

✅ **Smart Dialer:** A sleek, modern dialpad to manage your calls with ease.
✅ **Auto Call Tracking:** Automatically log call durations, timestamps, and types (incoming, outgoing, missed).
✅ **CRM-Style Notes:** Add private notes and color-coded labels to your contacts and calls. Never forget an important detail again.
✅ **Powerful Filtering:** Find any call in seconds. Filter by date, call type, labels, or contact name.
✅ **Cloud Archive (Optional):** Securely sync your call history to a private dashboard to access your data from any device.
✅ **Dual SIM Support:** Separate your professional and personal life by choosing which SIM to track.
✅ **Privacy First:** Built-in "Ignore" list to ensure personal numbers are never tracked or synced.

**Permissions & Privacy:**
MiniClick is committed to transparency. To function as your primary communication tool, we require:
- **Default Phone Handler (Role Dialer):** To provide the dialing interface and manage active calls.
- **Call Logs:** To display and organize your communication history.
- **Contacts:** To identify callers and link notes to the right person.

*Your data is yours. We do not sell or share your personal information with third parties.*

---

### B. 📋 Plain Text Copy-Paste Version (Optimized for Console)
```text
🚀 MiniClick Calls: Your Intelligent Smart Dialer & Call Tracker

MiniClick Calls is your companion for high-performance communication and professional call management. Designed for those who need a better way to track, organize, and archive their business interactions, MiniClick combines a premium Dialer with powerful productivity tools.

✨ WHY MINICLICK CALLS?
To provide a seamless experience, MiniClick can be set as your 📱 Default Phone Handler. This allows you to manage calls directly through our intuitive interface while unlocking advanced features like real-time notes and smart history.

💎 KEY FEATURES:

🔹 SMART DIALER – A sleek, modern dialpad to manage your professional calls with ease.
🔹 AUTO CALL TRACKING – Automatically log call durations, timestamps, and types (Inbound, Outbound, Missed).
🔹 SMART NOTES – Attach private notes and color-coded labels to your contacts. Never forget a detail again!
🔹 POWERFUL SEARCH – Find any call in seconds. Filter by date, call type, labels, or name.
🔹 CLOUD ARCHIVE (Optional) – Securely sync your call history to a private dashboard for any-device access.
🔹 DUAL SIM SUPPORT – Perfect for business! Choose which SIM to track and which to keep private.
🔹 PRIVACY FIRST – Built-in "Ignore" list to ensure personal numbers are never tracked or synced.

🛡️ PERMISSIONS & PRIVACY:
MiniClick is committed to transparency. To function as your primary communication tool, we require:
1️⃣ Default Phone Handler (Role Dialer): To provide the calling interface and manage active calls.
2️⃣ Call Logs: To display and organize your communication history.
3️⃣ Contacts: To identify callers and link your professional notes.

🔐 Your data is YOURS. We do not sell or share personal information.

Download MiniClick Calls today and take control of your communication! 📈
```

---

## 📸 2. Screenshot Guide (Required Visual Assets)
Provide **6-8 high-quality screenshots** matching these scenes:

1.  **The Smart Dialer:** Show the clean dialpad (from `DialerScreen.kt`).
2.  **Call History Timeline:** Show the main "Calls" screen with various call types and the "Synced" cloud icons.
3.  **Note-Taking Interface:** Use a screenshot of the "Person Interaction Sheet" where a note is being typed.
4.  **Organized Contacts:** Show the "Persons" screen showing grouped history and color-coded labels.
5.  **Analytics/Reports:** Show the "Reports" screen with the weekly activity graph to demonstrate the "tracking" value.
6.  **Dual SIM Setup:** A shot of the SIM settings to show how users can control their privacy.
7.  **Filter Modal:** Show the filtering options (Incoming, Outgoing, specific dates) to highlight the organization features.
8.  **The In-Call Experience (ESSENTIAL):** A screenshot of the active call screen (`InCallActivity.kt`) with Mute/Keypad buttons visible. Google requires this for the Dialer role.

---

## 📹 3. Verification Video Script (MANDATORY)
Google reviewers must see a video demonstration. Follow this exact flow:

1.  **Fresh Install:** Open the app for the first time.
2.  **Order of Prompts (CRITICAL):**
    *   Show the **"Set as Default Phone App"** prompt appearing **FIRST**.
    *   Accept it.
    *   Show runtime permissions (Call Log, Contacts) appearing **AFTER** the Default Dialer selection.
3.  **Core Feature Demo:**
    *   Open the dialer and type a number.
    *   **Place a Call**: Show that the call UI (`InCallActivity`) opens **inside** MiniClick.
    *   Show the call history list.
    *   Add a Note to a call ("Client follow-up").
    *   Show the Reports screen.

---

## 📅 Timeline & Tracking

| ID | Description | Status | Responsible |
| :--- | :--- | :--- | :--- |
| 1 | Implement Default Dialer Prompt in Setup Guide (Position #1) | ✅ Completed | AI |
| 2 | Update Play Store Description & Screenshots in Console | ✅ Completed | User |
| 3 | Re-submit Permission Declaration Form (Realignment) | ✅ Completed | User |
| 4 | Record and upload New Verification Video (Correct Order) | ⏳ Pending | User |
| 5 | Implement Smart Calling Logic (Direct calling when default) | ✅ Completed | AI |

---
*Updated by Antigravity AI on Jan 8, 2026.*
