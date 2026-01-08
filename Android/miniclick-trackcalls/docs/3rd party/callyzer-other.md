# Finding Device Base Settings - Complete Documentation

> **Last Updated:** 2026-01-09  
> **App:** Callyzer Pro  
> **Package:** `com.websoptimization.callyzerpro`

---

## Table of Contents

1. [Overview](#1-overview)
2. [Device-Specific Behavior Matrix](#2-device-specific-behavior-matrix)
3. [Android Version Differences](#3-android-version-differences)
4. [Recording Path Detection by Manufacturer](#4-recording-path-detection-by-manufacturer)
5. [Crash Analytics & Firebase Integration](#5-crash-analytics--firebase-integration)
6. [Webhook Configuration & Events](#6-webhook-configuration--events)
7. [Plan Checks & Feature Gating](#7-plan-checks--feature-gating)
8. [Credentials & Authentication Flow](#8-credentials--authentication-flow)
9. [Troubleshooting Guide](#9-troubleshooting-guide)
10. [Device Compatibility Reference](#10-device-compatibility-reference)

---

## 1. Overview

Callyzer Pro is a call analytics and recording management application that synchronizes with device call recordings (it does **not** record calls itself due to Android restrictions). The app's behavior varies significantly based on:

- **Device Manufacturer** (Samsung, Xiaomi, OnePlus, etc.)
- **Android Version** (9, 10, 11, 12, 13, 14+)
- **ROM Type** (Stock, MIUI, ColorOS, OneUI, etc.)
- **User Plan** (Free, Pro, Business)
- **Storage Permissions** granted

---

## 2. Device-Specific Behavior Matrix

### Overview Table

| Device/Manufacturer | Recording Path | Default Format | Special Handling | Known Issues |
|---------------------|----------------|----------------|------------------|--------------|
| **Samsung** | `/Recordings/Call/` | `.m4a` | Filename parsing (Name_Number_Date) | OneUI 4+ requires SAF |
| **Xiaomi/Poco** | `/MIUI/sound_recorder/call_rec/` | `.mp3`, `.aac` | Deep nested paths, MIUI-specific | Aggressive battery optimization |
| **OnePlus** | `/Recordings/PhoneRecord/` | `.amr`, `.wav` | OxygenOS-specific paths | Scoped storage strict enforcement |
| **Oppo/Realme** | `/ColorOS/Recordings/` | `.aac`, `.ocr` | OCR format needs conversion | Hidden recordings from MediaStore |
| **Vivo** | `/Recordings/` | `.amr` | Funtouch OS specific | Limited MediaStore access |
| **Huawei/Honor** | `/Sounds/CallRecord/` | `.amr`, `.aac` | EMUI/HarmonyOS differences | No Google Play Services |
| **Pixel/Stock** | `/Download/` or none | `.m4a` | Varies by Android version | Recording disabled by default |
| **Motorola** | `/Recordings/` | `.amr` | Near-stock behavior | Minimal customization |
| **Asus** | `/Recordings/Call/` | `.m4a` | ZenUI specific | Standard behavior |
| **LG** | `/Recordings/` | `.amr`, `.3gp` | Legacy device support | End of updates |

---

## 3. Android Version Differences

### Android 9 (API 28) and Below

```
┌─────────────────────────────────────────────────────────────┐
│ PERMISSIONS: READ_EXTERNAL_STORAGE grants FULL access       │
│ BEHAVIOR: Direct file system scanning allowed               │
│ RECORDING ACCESS: Unrestricted                              │
│ MEDIASTORE: Optional (direct path access works)             │
└─────────────────────────────────────────────────────────────┘
```

**Key Points:**
- ✅ Full access to `/sdcard/` directory
- ✅ Can directly read recording files from any app
- ✅ No scoped storage restrictions
- ⚠️ Legacy apps may be deprecated

---

### Android 10 (API 29)

```
┌─────────────────────────────────────────────────────────────┐
│ PERMISSIONS: Scoped Storage INTRODUCED (opt-out available) │
│ BEHAVIOR: Apps can only see their own files by default     │
│ RECORDING ACCESS: MediaStore API required                   │
│ LEGACY FLAG: requestLegacyExternalStorage=true (temporary)  │
└─────────────────────────────────────────────────────────────┘
```

**Key Points:**
- ⚠️ Scoped storage introduced
- ✅ `requestLegacyExternalStorage="true"` still works
- 🔄 MediaStore becomes primary for cross-app file access
- 📝 Files created before upgrade still accessible

---

### Android 11 (API 30)

```
┌─────────────────────────────────────────────────────────────┐
│ PERMISSIONS: MANAGE_EXTERNAL_STORAGE (special permission)  │
│ BEHAVIOR: Legacy flag IGNORED                              │
│ RECORDING ACCESS: MediaStore API OR SAF required           │
│ PLAY STORE: Special approval needed for full access        │
└─────────────────────────────────────────────────────────────┘
```

**Key Points:**
- ❌ `requestLegacyExternalStorage` no longer works
- 🔐 Requires `MANAGE_EXTERNAL_STORAGE` for full access
- 📋 Play Store policy restricts broad storage permissions
- 🗂️ SAF (Storage Access Framework) for user-granted folder access

---

### Android 12/13 (API 31-33)

```
┌─────────────────────────────────────────────────────────────┐
│ PERMISSIONS: MANAGE_EXTERNAL_STORAGE stricter enforcement  │
│ BEHAVIOR: Photo picker, granular media permissions         │
│ RECORDING ACCESS: READ_MEDIA_AUDIO permission added        │
│ APPROXIMATION: Location accuracy affects caller detection  │
└─────────────────────────────────────────────────────────────┘
```

**Key Points:**
- 🆕 `READ_MEDIA_AUDIO` permission for audio files (API 33)
- 🔒 Stricter enforcement of storage access
- 📍 Approximate location permission affects some features
- 🔄 Background restrictions tightened

---

### Android 14+ (API 34+)

```
┌─────────────────────────────────────────────────────────────┐
│ PERMISSIONS: Partial media access, user choice             │
│ BEHAVIOR: Selected photos/files granted per-session       │
│ RECORDING ACCESS: May require explicit folder selection    │
│ HEALTH: Health Connect integration potentially available   │
└─────────────────────────────────────────────────────────────┘
```

**Key Points:**
- 🆕 Partial media access by user selection
- 📱 Per-app language preferences
- 🔐 Enhanced privacy controls
- ⚠️ May require additional user interaction for access

---

## 4. Recording Path Detection by Manufacturer

### Primary Detection Method: Time-Window Matching

```kotlin
// Pseudo-code representation
fun findRecording(callStartTime: Long, callEndTime: Long, phoneNumber: String): File? {
    
    // Step 1: MediaStore Query (Primary)
    val mediaStoreResult = queryMediaStore(
        selection = "date_added BETWEEN ? AND ?",
        args = arrayOf(
            (callStartTime - BUFFER_MS).toString(),
            (callEndTime + BUFFER_MS).toString()
        ),
        mimeTypes = arrayOf("audio/*")
    )
    
    if (mediaStoreResult != null) return mediaStoreResult
    
    // Step 2: Path Scanning (Fallback)
    return scanKnownPaths(phoneNumber, callStartTime)
}
```

### Manufacturer-Specific Path Configuration

#### Samsung (OneUI)

```
PRIMARY PATHS:
├── /storage/emulated/0/Recordings/Call/
├── /storage/emulated/0/Call/
└── /storage/emulated/0/DCIM/Call/

FILENAME PATTERN:
├── Call recording {Name} {Number} {Date}.m4a
├── Record_{Number}_{Timestamp}.m4a
└── {Contact}_{Date}_{Time}.m4a

PARSING REGEX:
/Call[_ ]?recording[_ ]?(.+?)_(\d+)_(\d{4}-?\d{2}-?\d{2})/i
```

#### Xiaomi/Poco/Redmi (MIUI)

```
PRIMARY PATHS:
├── /storage/emulated/0/MIUI/sound_recorder/call_rec/
├── /storage/emulated/0/Recordings/Call/
├── /storage/emulated/0/Recorder/
└── /sdcard/MIUI/sound_recorder/

FILENAME PATTERN:
├── Rec_{Number}_{DateTime}.mp3
├── {Number}({Name})_{Date}_{Time}.mp3
└── call_{Timestamp}.aac

SPECIAL HANDLING:
- MIUI hides recordings from MediaStore
- Requires explicit path scanning
- Battery optimization may kill background service
```

#### OnePlus (OxygenOS)

```
PRIMARY PATHS:
├── /storage/emulated/0/Recordings/PhoneRecord/
├── /storage/emulated/0/Record/PhoneRecord/
└── /storage/emulated/0/Recordings/

FILENAME PATTERN:
├── Record_{Number}_{Date}_{Time}.amr
├── {Number}_{Timestamp}.wav
└── PhoneRec_{Date}_{Number}.amr
```

#### Oppo/Realme (ColorOS)

```
PRIMARY PATHS:
├── /storage/emulated/0/ColorOS/Recordings/
├── /storage/emulated/0/Recordings/Call/
├── /storage/emulated/0/DCIM/Recorder/
└── /storage/emulated/0/Music/Recordings/

FILENAME PATTERN:
├── {Number}_{Date}_{Time}.aac
├── Rec_{Number}.ocr  (proprietary format)
└── Call_{Timestamp}.m4a

SPECIAL HANDLING:
- OCR format may need conversion
- ColorOS security may restrict access
```

#### Huawei/Honor (EMUI/HarmonyOS)

```
PRIMARY PATHS:
├── /storage/emulated/0/Sounds/CallRecord/
├── /storage/emulated/0/Recordings/
├── /storage/emulated/0/Record/
└── /storage/emulated/0/HuaweiBackup/CallRecord/

FILENAME PATTERN:
├── {Number}_{Date}_{Time}.amr
├── record_{Timestamp}.aac
└── CallRecord_{Date}.amr

SPECIAL NOTES:
- No Google Play Services (HMS only)
- May require AppGallery version
- Different API implementations
```

---

## 5. Crash Analytics & Firebase Integration

### Firebase Crashlytics Configuration

```properties
# From firebase-crashlytics.properties
version=18.2.6
client=firebase-crashlytics
firebase-crashlytics_client=18.2.6
```

### Crash Categories & Handling

#### Category 1: Permission-Related Crashes

| Crash Type | Cause | Device Pattern | Resolution |
|------------|-------|----------------|------------|
| `SecurityException` | Missing storage permission | All Android 11+ | Request MANAGE_EXTERNAL_STORAGE |
| `FileNotFoundException` | Scoped storage blocking | Android 10+ | Use MediaStore API |
| `IllegalStateException` | Background process killed | MIUI, ColorOS | Add to battery whitelist |

#### Category 2: Storage Access Crashes

```
COMMON STACKTRACES:

1. java.io.FileNotFoundException: /storage/emulated/0/MIUI/...
   ├── Cause: Scoped storage restriction
   └── Fix: Use SAF or MediaStore

2. SecurityException: Permission denial: reading...
   ├── Cause: Runtime permission not granted
   └── Fix: Request READ_MEDIA_AUDIO (API 33+)

3. IllegalStateException: Cannot access storage
   ├── Cause: Storage not mounted
   └── Fix: Check storage state before access
```

#### Category 3: Device-Specific Crashes

| Device | Common Crash | Root Cause | Mitigation |
|--------|--------------|------------|------------|
| **Samsung** | `DeadObjectException` | Bixby integration conflict | Disable Bixby routines |
| **Xiaomi** | `ANR in BroadcastReceiver` | Aggressive power saving | Autostart permission |
| **Huawei** | `HwInstrumentation` crash | HMS/GMS conflict | Use HMS fallback |
| **Oppo** | `ColorOSPermissionException` | Custom permission model | Request ColorOS permissions |

### Crash Reporting Flow

```
┌─────────────┐    ┌──────────────┐    ┌─────────────────┐
│   App       │───▶│  Crashlytics │───▶│ Firebase Console│
│  Crash      │    │   SDK        │    │  Dashboard      │
└─────────────┘    └──────────────┘    └─────────────────┘
        │                                       │
        ▼                                       ▼
┌─────────────────┐                   ┌─────────────────┐
│ Custom Keys     │                   │ Crash Grouping  │
│ - Device Model  │                   │ - By Version    │
│ - Android Ver   │                   │ - By Device     │
│ - Permission    │                   │ - By User       │
│   Status        │                   │   Segment       │
└─────────────────┘                   └─────────────────┘
```

---

## 6. Webhook Configuration & Events

### Webhook Event Types

| Event | Trigger | Payload Contains | Use Case |
|-------|---------|------------------|----------|
| `call.ended` | Call terminated | Number, duration, direction, timestamp | CRM integration |
| `recording.attached` | Recording found | File path, size, format, call ID | Backup triggers |
| `recording.uploaded` | Cloud upload complete | Cloud URL, call metadata | Analytics sync |
| `contact.updated` | Contact modified | Contact changes, labels | External sync |
| `sync.completed` | Bulk sync done | Sync stats, new records count | Dashboard refresh |
| `error.occurred` | Critical error | Error type, device info | Monitoring |

### Webhook Payload Structure

```json
{
  "event_type": "call.ended",
  "timestamp": "2026-01-09T04:24:53+05:30",
  "device_info": {
    "device_id": "abc123xyz",
    "model": "Samsung Galaxy S23",
    "android_version": "14",
    "app_version": "4.5.61140579"
  },
  "data": {
    "call_id": "call_789456123",
    "phone_number": "+919876543210",
    "contact_name": "John Doe",
    "direction": "outgoing",
    "duration_seconds": 245,
    "call_type": "connected",
    "recording_status": "attached",
    "recording_path": "/storage/emulated/0/Recordings/Call/..."
  },
  "user": {
    "user_id": "user_456",
    "plan": "pro",
    "paired_device_id": "device_pair_789"
  }
}
```

### Webhook Integration Endpoints

```
SUPPORTED INTEGRATIONS:
├── Custom URL (user-defined)
│   └── POST requests with JSON payload
├── Zapier Integration
│   └── Trigger URL provided by user
├── Make (Integromat)
│   └── Webhook module URL
├── Google Sheets (via Apps Script)
│   └── Web App deployment URL
└── Slack Webhook
    └── Incoming webhook URL
```

### Retry Logic

```
RETRY CONFIGURATION:
├── Initial Retry: 30 seconds
├── Retry Interval: Exponential backoff
│   └── 30s → 60s → 120s → 300s → 600s
├── Max Retries: 5
├── Failure Action: Queue for later (WorkManager)
└── Final Failure: Log to crash analytics
```

---

## 7. Plan Checks & Feature Gating

### Plan Tier Matrix

| Feature | Free | Pro | Business |
|---------|------|-----|----------|
| Call History Sync | ✅ Last 30 | ✅ Unlimited | ✅ Unlimited |
| Recording Attachment | ✅ Limited | ✅ Unlimited | ✅ Unlimited |
| Cloud Upload | ❌ | ✅ 5GB/month | ✅ Unlimited |
| Webhook Integration | ❌ | ❌ | ✅ |
| Multi-Device Pairing | ❌ | ✅ 2 devices | ✅ 10 devices |
| Export (CSV/PDF) | ❌ | ✅ | ✅ |
| Advanced Analytics | ❌ | ✅ | ✅ |
| API Access | ❌ | ❌ | ✅ |
| Priority Support | ❌ | ✅ | ✅ |
| White-labeling | ❌ | ❌ | ✅ |

### Plan Check Implementation

```kotlin
// Plan verification flow
object PlanChecker {
    
    enum class Plan { FREE, PRO, BUSINESS }
    
    fun checkFeatureAccess(feature: Feature): Boolean {
        val currentPlan = getCurrentPlan()
        val requiredPlan = feature.requiredPlan
        
        return when {
            currentPlan == Plan.BUSINESS -> true
            currentPlan == Plan.PRO -> requiredPlan != Plan.BUSINESS
            else -> requiredPlan == Plan.FREE
        }
    }
    
    fun verifyPlanStatus(): PlanStatus {
        // 1. Check local cache
        val cached = getCachedPlanStatus()
        if (cached.isValid()) return cached
        
        // 2. Server verification
        val serverStatus = api.verifyPlan()
        
        // 3. Update local storage
        cachePlanStatus(serverStatus)
        
        return serverStatus
    }
}
```

### Plan Validation Frequency

```
VALIDATION TRIGGERS:
├── App Launch: Always verify
├── Feature Access: Check before action
├── Periodic: Every 24 hours (background)
├── Network Change: Re-verify on reconnect
└── Manual: User-triggered "Refresh subscription"

OFFLINE BEHAVIOR:
├── Grace Period: 7 days
├── Feature Access: Based on last known status
├── Recording: Continues locally
└── Sync: Queued for later
```

---

## 8. Credentials & Authentication Flow

### User Authentication

```
AUTHENTICATION FLOW:
┌─────────────┐    ┌──────────────┐    ┌─────────────────┐
│   User      │───▶│  Login API   │───▶│ JWT Token       │
│  Credentials│    │  (HTTPS)     │    │ Generation      │
└─────────────┘    └──────────────┘    └─────────────────┘
        │                                       │
        ▼                                       ▼
┌─────────────────┐                   ┌─────────────────┐
│ Input Fields    │                   │ Token Storage   │
│ - Email/Phone   │                   │ - Access Token  │
│ - Password      │                   │ - Refresh Token │
│ - OTP (2FA)     │                   │ - Expiry Time   │
└─────────────────┘                   └─────────────────┘
```

### Device Pairing Credentials

```
DEVICE PAIRING FLOW:
1. User initiates pairing from web dashboard
2. Server generates pairing code (6 alphanumeric)
3. User enters code in mobile app
4. App sends: { code, device_id, device_info }
5. Server validates and links device to user account
6. Returns: { pair_status, sync_token, settings }

PAIRING DATA STORED:
├── Device ID (unique identifier)
├── Pair Token (refresh-capable)
├── User ID (linked account)
├── Pair Timestamp
├── Device Name (user-assigned)
└── Sync Settings (preferences)
```

### API Credentials Management

```
CREDENTIAL STORAGE:
├── Encrypted SharedPreferences (Android Keystore backed)
│   └── Auth tokens
│   └── API keys
│   └── User preferences
├── Room Database (Encrypted)
│   └── Sync data
│   └── Call records
│   └── Cached contacts
└── Secure File Storage
    └── Temporary credentials
    └── Session data

SECURITY MEASURES:
├── AES-256 encryption for stored credentials
├── SSL pinning for API connections
├── Token rotation every 7 days
├── Biometric authentication option
└── Auto-logout on suspicious activity
```

### Firebase/Third-Party Credentials

```properties
# Firebase Configuration (from properties files)
firebase-common_client=20.0.0
firebase-crashlytics_client=18.2.6
firebase-analytics (integrated)
firebase-messaging (push notifications)
firebase-installations (device registration)

PLAY SERVICES DEPENDENCIES:
play-services-measurement (analytics)
play-services-cloud-messaging (FCM)
play-services-ads-identifier (attribution)
```

---

## 9. Troubleshooting Guide

### Common Issues by Category

#### Recording Not Found

```
DIAGNOSTIC STEPS:
1. Check if system recorder is enabled
   └── Settings → Phone → Call Recording
2. Verify recording path
   └── Callyzer Settings → Recording Path → Test
3. Grant storage permissions
   └── App Info → Permissions → Storage → Allow
4. For Android 11+:
   └── Enable "All Files Access" in Special Permissions

DEVICE-SPECIFIC FIXES:
├── Samsung: Enable "Auto record" in Phone app
├── Xiaomi: Add to Autostart + Battery Whitelist
├── OnePlus: Disable Battery Optimization
└── Oppo: Enable recording in ColorOS settings
```

#### Sync Issues

```
RESOLUTION STEPS:
1. Check internet connectivity
2. Verify pairing status
   └── Settings → Web Sync → Check Connection
3. Force re-sync
   └── Settings → Data → Force Sync
4. Clear app cache (/data/data/com.websoptimization.callyzerpro/cache)
5. Re-pair device if persistent

ERROR CODES:
├── SYNC_001: Network timeout → Retry with stable connection
├── SYNC_002: Auth expired → Re-login required
├── SYNC_003: Device unpaired → Re-pair from dashboard
├── SYNC_004: Server error → Wait and retry
└── SYNC_005: Plan expired → Renew subscription
```

#### Crash Recovery

```
CRASH DIAGNOSTIC:
1. Check Crashlytics dashboard for error pattern
2. Identify device/OS combination
3. Reproduce in controlled environment
4. Apply device-specific workaround

COMMON FIXES:
├── Clear app data (last resort)
├── Update to latest app version
├── Check for OS updates
├── Disable conflicting apps (battery savers, etc.)
└── Submit crash report via app
```

---

## 10. Device Compatibility Reference

### Fully Supported Devices

| Brand | Models | Android Versions | Notes |
|-------|--------|------------------|-------|
| Samsung | S series, A series, M series | 9-14 | Full feature support |
| Xiaomi/Poco | All | 9-14 | Requires autostart |
| OnePlus | All | 9-14 | Standard support |
| Pixel | 3 and above | 10-14 | Limited recording |
| Motorola | G series, Edge | 10-14 | Near-stock support |

### Partially Supported Devices

| Brand | Models | Limitations |
|-------|--------|-------------|
| Oppo/Realme | All | OCR format may not convert |
| Vivo | Select models | Limited MediaStore access |
| Huawei/Honor | All (HMS) | No GMS, uses HMS version |
| Nokia | Select HMD | Some models lack recording |

### Known Incompatible Configurations

| Configuration | Reason | Workaround |
|---------------|--------|------------|
| Android Go edition | Limited resources | Not recommended |
| Custom ROMs without certified Play Protect | Signature mismatch | Use APK sideload |
| Rooted with Magisk Hide | Detection issues | Disable for app |
| Work Profile (Samsung Knox) | Isolation | Use personal profile |

---

## Appendix A: File Format Support

| Format | Extension | Support Level | Notes |
|--------|-----------|---------------|-------|
| AAC | `.aac`, `.m4a` | ✅ Full | Preferred format |
| MP3 | `.mp3` | ✅ Full | Universal support |
| AMR | `.amr`, `.amr-nb`, `.amr-wb` | ✅ Full | Common on older devices |
| WAV | `.wav` | ✅ Full | Large files |
| OGG | `.ogg`, `.opus` | ⚠️ Partial | Some devices only |
| OCR | `.ocr` | ⚠️ Partial | ColorOS proprietary |
| 3GP | `.3gp`, `.3gpp` | ✅ Full | Legacy support |

---

## Appendix B: Quick Reference Card

```
┌────────────────────────────────────────────────────────────────────┐
│                    CALLYZER PRO - QUICK REFERENCE                  │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│ PERMISSIONS NEEDED (by Android Version):                          │
│ ├── Android 9:  READ_EXTERNAL_STORAGE                             │
│ ├── Android 10: READ_EXTERNAL_STORAGE + Legacy flag               │
│ ├── Android 11+: MANAGE_EXTERNAL_STORAGE OR SAF                   │
│ └── Android 13+: READ_MEDIA_AUDIO                                 │
│                                                                    │
│ RECORDING PATHS (Top Manufacturers):                              │
│ ├── Samsung:  /Recordings/Call/                                   │
│ ├── Xiaomi:   /MIUI/sound_recorder/call_rec/                      │
│ ├── OnePlus:  /Recordings/PhoneRecord/                            │
│ └── Oppo:     /ColorOS/Recordings/                                │
│                                                                    │
│ COMMON ISSUES & FIXES:                                            │
│ ├── No recordings → Check path & permissions                      │
│ ├── Sync fails → Verify pairing & network                         │
│ ├── App killed → Add to battery whitelist                         │
│ └── Upload fails → Check plan limits                              │
│                                                                    │
│ SUPPORT: support@callyzer.com | Dashboard: app.callyzer.com       │
└────────────────────────────────────────────────────────────────────┘
```

---

*This document is maintained by the Callyzer Pro development team. For updates or corrections, please submit via the internal documentation system.*
